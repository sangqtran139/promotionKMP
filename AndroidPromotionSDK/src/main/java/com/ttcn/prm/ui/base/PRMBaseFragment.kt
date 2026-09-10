package com.ttcn.prm.ui.base

import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ttcn.prm.R
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureGate
import com.ttcn.prm.ui.feature.promotiondetail.PromotionDetailFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.ttcn.prm.ui.utils.PRMLocale
import android.content.Context
import androidx.annotation.StringRes

internal abstract class PRMBaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = requireNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView."
        }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /** Back hệ thống (phím back / cử chỉ vuốt) — toàn bộ cơ chế nằm ở [PrmSystemBackInterceptor]. */
    private val systemBack = PrmSystemBackInterceptor { closeTopSdkScreen() }

    // ─── Vòng đời ─────────────────────────────────────────────────────────────

    /**
     * Ép LIGHT cho mọi màn SDK: bọc inflater trong [ContextThemeWrapper] mang theme Light đầy đủ
     * [R.style.PRMForceLight]. Cần thiết vì host có thể dùng theme DayNight (vd Theme.Material3.DayNight);
     * ở dark mode, view SDK không set màu tường minh sẽ lấy màu chữ/nền TỐI từ theme host → lệch UI.
     * Inflate dưới theme Light này để màu ngầm định resolve ra sáng (bản v29 chặn thêm OS force-dark).
     * **Không đụng theme app host** — chỉ view inflate từ inflater này. Đối ứng
     * `overrideUserInterfaceStyle = .light` bên iOS.
     */
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val base = super.onGetLayoutInflater(savedInstanceState)
        // Bọc HAI lớp: locale của SDK (PRMLocale) rồi mới tới theme Light. Thứ tự này bắt buộc —
        // `ContextThemeWrapper` giữ nguyên `Resources` của context nó bọc, nên locale phải nằm
        // ở lớp trong; đảo lại thì `getString` trong màn SDK lại đọc theo locale của máy.
        val localized = PRMLocale.wrap(requireContext())
        val themed = ContextThemeWrapper(localized, R.style.PRMForceLight)
        return base.cloneInContext(themed)
    }

    /**
     * Đọc chuỗi theo **ngôn ngữ host chọn cho SDK**, không phải ngôn ngữ của máy.
     *
     * Phải có hàm riêng vì `Fragment.getString()` là `final` và đọc qua `requireContext()` —
     * context của Activity host. Bọc locale ở `onGetLayoutInflater` chỉ ảnh hưởng **inflater**
     * (và `binding.root.context`), không ảnh hưởng `Fragment.getResources()`. Thiếu phân biệt này
     * thì chữ trong layout đúng ngôn ngữ còn chữ gán từ code lại theo máy — lệch ngay trên một màn.
     *
     * Trong adapter/ViewHolder thì dùng `binding.root.context.getString(...)`: context đó đã bọc
     * sẵn vì view được inflate từ inflater của fragment.
     */
    protected fun prmString(@StringRes resId: Int): String =
        localizedContext.getString(resId)

    protected fun prmString(@StringRes resId: Int, vararg formatArgs: Any): String =
        localizedContext.getString(resId, *formatArgs)

    /** Bọc một lần cho cả vòng đời fragment — `PRMLocale.wrap` dựng `Context` mới mỗi lần gọi. */
    private val localizedContext: Context by lazy { PRMLocale.wrap(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        systemBack.registerDispatcherCallback(this)
        setupUI()
        observeData()
    }

    /**
     * Cộng thêm chiều cao navigation bar hệ thống vào `marginBottom` sẵn có của [anchor] — CHỈ khi
     * [anchor] **thật sự còn** tràn xuống dưới navigation bar sau khi mọi ancestor (kể cả host) đã áp
     * xong padding/insets của họ.
     *
     * KHÔNG tin thẳng giá trị insets nhận được ở [anchor] — dù đây là insets "chưa bị host consume",
     * một host hoàn toàn có thể tự pad container theo `navigationBars()` (pattern edge-to-edge rất
     * phổ biến, xem `MainActivity` app demo) mà KHÔNG gọi API "consume" insets, nên giá trị gốc vẫn
     * truyền nguyên xuống tới đây dù host đã lo xong rồi — tin nó thì cộng dư lần nữa, đẩy [anchor]
     * lên cao hơn mức cần (bug thật đã gặp, xác nhận bằng số đo trên máy).
     *
     * Cũng KHÔNG tự đo `getLocationOnScreen` (toạ độ tuyệt đối trên display) so với kích thước window
     * (cách cũ hơn nữa, xem lịch sử git) — hai hệ toạ độ đó chỉ trùng khi window nằm đúng góc (0,0)
     * của display, lệch nhau ở một số OEM/kiểu windowing.
     *
     * Cách đúng: dùng giá trị insets chỉ để biết **có nav bar hay không và cao bao nhiêu**, rồi xác
     * nhận lại bằng toạ độ THẬT của [anchor] SAU KHI layout ổn định (window-relative:
     * `getLocationInWindow` so với chiều cao [View.getRootView] — cùng hệ toạ độ, không lệch theo
     * multi-window/split-screen) — chỉ cộng margin khi toạ độ đó xác nhận [anchor] còn nằm trong vùng
     * nav bar. Vì vậy đúng trong MỌI trường hợp: host tự pad hay không, có nav bar hay không, và tự
     * cập nhật lại mỗi khi insets đổi (xoay màn hình, gập/mở máy gập, bật/tắt gesture nav...).
     *
     * Bắt `baseMargin` (giá trị margin gốc khai trong XML) **một lần duy nhất** tại thời điểm gọi —
     * không đọc lại `layoutParams.bottomMargin` mỗi lần insets đổi, vì lúc đó nó đã bị chính lần áp
     * trước ghi đè. Gọi lại hàm này nhiều lần trên cùng 1 view (Fragment không huỷ view, chỉ ẩn/hiện
     * lại) là no-op nhờ tag đánh dấu — tránh bắt nhầm `baseMargin` từ giá trị đã cộng dồn.
     *
     * ⚠️ [anchor] PHẢI luôn được layout (`visibility = INVISIBLE`, KHÔNG `GONE`) trong suốt vòng đời
     * view — view `GONE` không được đo/layout nên [View.doOnNextLayout] không có cơ hội chạy tới khi
     * nó lại `VISIBLE`, mất luôn margin tránh nav bar. Đã dính đúng bug này ở
     * `ChoosePromotionFragment.ctlApplyVoucher` (ẩn qua `GONE` lúc shimmer) — fix ở phía đó (đổi sang
     * `INVISIBLE`), không phải sửa hàm này.
     *
     * ⚠️ Set `bottomMargin` phải qua [View.post] (traversal MỚI, tách khỏi traversal hiện tại) —
     * KHÔNG set thẳng trong [View.doOnNextLayout]. Set thẳng trong đó vẫn đang nằm giữa traversal do
     * chính [View.requestLayout] gọi trước đó gây ra (Android tự chạy thêm một vòng measure/layout
     * NGAY TRONG cùng traversal khi có `requestLayout()` giữa chừng) — với parent là
     * `ConstraintLayout`, vòng "trong cùng traversal" này KHÔNG re-solve theo margin mới (đã xác nhận
     * bằng log đo trên máy thật: `bottomMargin` trong `LayoutParams` đổi đúng nhưng bounds cuối cùng
     * không đổi). Set trong `post {}` đẩy việc này sang một traversal HOÀN TOÀN mới, buộc
     * `ConstraintLayout` solve lại từ đầu với margin đúng.
     *
     * ⚠️ [isOverlappedByNavigationBar] đo **view CHA** của [anchor], KHÔNG đo chính [anchor] — đo
     * chính nó là tự ăn đuôi mình: `bottomMargin` mình vừa cộng làm chính [anchor] dịch lên, lần đo
     * SAU đó (do chính [View.requestLayout] ở đây kéo theo một lượt insets dispatch mới trên một số
     * máy/API cũ — model dispatch insets cũ gắn liền với layout pass, khác Pixel/API 30+) thấy "hết
     * bị che" → gỡ margin → lại bị che → đo lại → cộng lại... nút nhảy vị trí liên tục (bug thật gặp
     * trên Samsung Note 8). View cha (`ConstraintLayout` chứa [anchor]) có kích thước không phụ thuộc
     * margin của con, nên phép đo trên nó ổn định bất kể lặp lại bao nhiêu lần.
     */
    protected fun applyNavigationBarInset(anchor: View) {
        if (anchor.getTag(R.id.prm_tag_nav_bar_inset_margin) != null) return
        val baseMargin = (anchor.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        anchor.setTag(R.id.prm_tag_nav_bar_inset_margin, baseMargin)
        ViewCompat.setOnApplyWindowInsetsListener(anchor) { view, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            if (navBarInset <= 0) {
                view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = baseMargin }
            } else {
                // Đợi layout pass kế tiếp: cần toạ độ SAU KHI padding của ancestor (nếu có) đã áp —
                // đọc ngay trong callback này là toạ độ CŨ, còn từ trước khi ancestor kịp layout lại.
                view.doOnNextLayout {
                    val toApply = if (isOverlappedByNavigationBar(view, navBarInset)) navBarInset else 0
                    view.post {
                        view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = baseMargin + toApply }
                    }
                }
                view.requestLayout()
            }
            insets
        }
        ViewCompat.requestApplyInsets(anchor)
    }

    /**
     * Cộng thêm chiều cao navigation bar hệ thống vào `paddingBottom` sẵn có của [scrollable] — dùng
     * cho nội dung cuộn được (`RecyclerView`...) thay vì [applyNavigationBarInset], để item cuối cùng
     * không bị navigation bar che khi cuộn hết cỡ trên các thiết bị navigation bar to (3 nút). Cùng
     * cơ chế xác nhận bằng toạ độ thật như [applyNavigationBarInset] — xem KDoc ở đó.
     *
     * Tự bật `clipToPadding = false` nếu [scrollable] là `ViewGroup` — phần padding thêm chỉ nới rộng
     * vùng cuộn, không cắt mất nội dung item cuối khi nó cuộn vào đúng vùng padding đó.
     */
    protected fun applyNavigationBarInsetAsScrollPadding(scrollable: View) {
        if (scrollable.getTag(R.id.prm_tag_nav_bar_inset_padding) != null) return
        if (scrollable is ViewGroup) scrollable.clipToPadding = false
        val basePadding = scrollable.paddingBottom
        scrollable.setTag(R.id.prm_tag_nav_bar_inset_padding, basePadding)
        ViewCompat.setOnApplyWindowInsetsListener(scrollable) { view, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            if (navBarInset <= 0) {
                view.updatePadding(bottom = basePadding)
            } else {
                view.doOnNextLayout {
                    val toApply = if (isOverlappedByNavigationBar(view, navBarInset)) navBarInset else 0
                    view.updatePadding(bottom = basePadding + toApply)
                }
                view.requestLayout()
            }
            insets
        }
        ViewCompat.requestApplyInsets(scrollable)
    }

    /**
     * So toạ độ của **view cha trực tiếp** của [view] với vùng navigation bar hệ thống, SAU KHI
     * layout đã ổn định — cách duy nhất biết chắc container đang bị che thật hay khoảng đó đã được
     * ancestor (host) chừa sẵn rồi.
     *
     * Đo view CHA, KHÔNG đo chính [view] — xem cảnh báo trong KDoc của [applyNavigationBarInset] về
     * vòng lặp tự ăn đuôi mình nếu đo chính view đang được cộng margin/padding.
     *
     * `getLocationInWindow` + chiều cao [View.getRootView] — **cùng một hệ toạ độ (window-relative)**,
     * đúng trong mọi kiểu windowing (multi-window/split-screen/freeform), không như
     * `getLocationOnScreen` (toạ độ tuyệt đối trên display) so với kích thước window — hai hệ đó chỉ
     * trùng khi window nằm đúng góc (0,0) của display.
     */
    private fun isOverlappedByNavigationBar(view: View, navBarInset: Int): Boolean {
        val reference = view.parent as? View ?: view
        val location = IntArray(2)
        reference.getLocationInWindow(location)
        val referenceBottomInWindow = location[1] + reference.height
        val windowHeight = view.rootView.height
        return referenceBottomInWindow > windowHeight - navBarInset
    }

    /** Chỉ chặn back khi màn này đang hiện trên cùng → wrap ở `onResume`, gỡ ở `onPause`. */
    override fun onResume() {
        super.onResume()
        systemBack.wrapWindow(requireActivity().window)
    }

    override fun onPause() {
        systemBack.unwrapWindow(activity?.window)
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    open fun setupUI() {}
    open fun observeData() {}

    // ─── Tiện ích dùng chung ──────────────────────────────────────────────────

    protected fun <T> collectFlow(
        flow: Flow<T>,
        state: Lifecycle.State = Lifecycle.State.STARTED,
        collector: suspend (T) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(state) {
                flow.collect { collector(it) }
            }
        }
    }

    /**
     * Map mã lỗi (raw từ store) → chuỗi hiển thị — **dùng chung mọi màn** (đồng nhất iOS
     * `PromotionUIStrings.errorMessage`). Gom về đây thay cho `mapErrorMessage` lặp ở từng Fragment.
     */
    protected fun mapPromotionError(code: String): String = when (code) {
        ErrorCodes.MISSING_CUSTOMER_ID -> prmString(R.string.prm_missing_customer_id)
        ErrorCodes.NO_RESULT, "error_detail_unavailable" -> prmString(R.string.prm_no_result)
        // Lỗi mạng/timeout có câu RIÊNG: tầng data đã cất công phân loại (`NetworkException`), gộp
        // vào lỗi chung là vứt đi thông tin duy nhất mà user hành động được. Thấy rõ nhất ở nút
        // "Áp dụng" màn Chọn ưu đãi — mất mạng lúc validate thì phải báo đúng là mất mạng.
        ErrorCodes.NETWORK_ERROR -> prmString(R.string.prm_error_network)
        ErrorCodes.TIMEOUT -> prmString(R.string.prm_error_timeout)
        else -> prmString(R.string.prm_error_general)
    }

    // ─── Điều hướng giữa các màn SDK ──────────────────────────────────────────

    /**
     * FragmentManager **đang chứa màn này** — luôn là `parentFragmentManager`, KHÔNG phải
     * `activity.supportFragmentManager`.
     *
     * Mở và đóng phải dùng chung một FM, nếu không [closeTopSdkScreen] sẽ đọc một stack trong khi
     * entry nằm ở stack khác. Với host dùng Navigation, FM đó là `childFragmentManager` của
     * destination — xem `PromotionSDK.resolveFragmentManager`.
     */
    private val screenManager: FragmentManager get() = parentFragmentManager

    /**
     * Mở màn SDK mới **chồng lên** màn hiện tại, trong cùng container.
     *
     * Container lấy từ view cha của màn này — không cần nơi gọi truyền id, và tự đúng với mọi kiểu
     * nhúng của host. Cụ thể vì sao **không** dùng `activity.supportFragmentManager` +
     * `android.R.id.content`:
     * - Với host dùng Navigation, entry rơi vào back stack của Activity trong khi [closeTopSdkScreen]
     *   đọc `parentFragmentManager` — back không pop được nó.
     * - `android.R.id.content` **không** dùng được từ child FM: child FM chỉ tìm container bên trong
     *   view của fragment cha, không thấy content view của Activity → fragment add xong không có
     *   view, màn mở ra thành vô hình.
     *
     * **Dedup theo [tag]:** đã có màn cùng tag trong FM thì bỏ qua. Không có chốt này thì bấm nhanh
     * hai lần vào nút mở màn là chồng hai instance, và phải back hai lần mới thoát được một màn.
     * Cùng cách `PromotionSDK.openMyPromotion`/`openPromotionDetail` gác ở bề mặt host.
     *
     * @param tag Mặc định là tên class. Truyền tay khi muốn giữ một tag `prm_*` cố định (tag này
     *   đồng thời là tên entry trong back stack).
     */
    protected fun addFragment(
        fragment: Fragment,
        addToBackStack: Boolean = true,
        tag: String = fragment::class.java.simpleName,
    ) {
        val containerId = (view?.parent as? ViewGroup)?.id
        if (containerId == null || containerId == View.NO_ID) {
            // Thông báo cho DEV, không phải cho user → log, không toast.
            Log.e("PRMBaseFragment", "Cannot navigate: no valid container found.")
            return
        }
        if (screenManager.findFragmentByTag(tag) != null) return
        screenManager.beginTransaction()
            // Cho FragmentManager gộp/sắp xếp lại thao tác trong cùng transaction — bắt buộc để
            // animation và vòng đời chạy đúng khi add chồng màn. Cùng cách `PromotionSDK` đang làm.
            .setReorderingAllowed(true)
            .add(containerId, fragment, tag)
            .apply { if (addToBackStack) addToBackStack(tag) }
            .commit()
    }

    /** Như [addFragment] nhưng **thay** nội dung [containerId] thay vì chồng lên. */
    protected fun replaceFragment(
        fragment: Fragment,
        @IdRes containerId: Int,
        addToBackStack: Boolean = true
    ) {
        val tag = fragment::class.java.simpleName
        screenManager.beginTransaction()
            .replace(containerId, fragment, tag)
            .apply { if (addToBackStack) addToBackStack(tag) }
            .commit()
    }

    /**
     * Mở màn "Chi tiết ưu đãi", gác bởi cờ `VOUCHER_DETAIL`.
     * Cờ TẮT → thông báo PRM_MOB_021 và không điều hướng. Toast này đi thẳng
     * [PRMBaseConfirmDialog.showFeatureDisabled] — popup, luôn hiện.
     *
     * Song sinh của `BaseRouter.canRouteToDetail()` bên iOS: gom về base để cả ba màn gọi
     * (Ưu đãi của tôi, Tìm kiếm, Chọn ưu đãi) không thể quên gác.
     */
    // `internal` chứ không `protected`: điều hướng nội bộ giữa các màn SDK, không phải bề mặt cho
    // host subclass. Host mở chi tiết bằng `PromotionSDK.openPromotionDetail(...)`.
    //
    // [returnVoucherOnApply] mặc định `false` — ba màn gọi nó nhiều nhất ("Ưu đãi của tôi", Tìm kiếm)
    // đều muốn "Sử dụng ngay"; riêng "Chọn ưu đãi" truyền `true`.
    internal fun openPromotionDetail(
        voucherId: String,
        returnVoucherOnApply: Boolean = false,
    ) {
        if (!PromotionFeatureGate.canOpenVoucherDetail()) {
            PRMBaseConfirmDialog.showFeatureDisabled(requireContext(), parentFragmentManager)
            return
        }
        addFragment(PromotionDetailFragment.newInstance(voucherId, returnVoucherOnApply))
    }

    // ─── Back ─────────────────────────────────────────────────────────────────

    /**
     * Back của mọi màn SDK — nút back trên toolbar và back hệ thống đều đổ về đây.
     *
     * Đóng được màn của mình thì dừng. Hết màn SDK để đóng (host tự nhúng fragment, không qua back
     * stack) thì **trả quyền cho host** qua `onBackPressedDispatcher` — SDK **không** `finish()`, nó
     * không có quyền đóng Activity của host.
     *
     * ⚠️ Override thì **chỉ được thao tác qua `FragmentManager`**, đừng đụng state riêng của fragment
     * (binding, viewModel…): khi nhiều màn SDK cùng resumed, back vật lý có thể gọi hàm này trên màn
     * SDK **đầu tiên** trong stack chứ không phải màn đang hiện — xem [PrmSystemBackInterceptor.wrapWindow].
     */
    open fun goBack() {
        // Fragment có thể đã detach khỏi Activity trước khi hàm này chạy: `ChoosePromotionFragment`
        // gọi `goBack()` từ callback bất đồng bộ của `PRMOfferWidget.applySelectedOffers` (chạy trên
        // `viewScope` riêng của View, không theo lifecycle Fragment) — user bấm back hệ thống trong
        // lúc chờ API trả lời là fragment đã bị pop, callback về sau vẫn cố `requireActivity()` và
        // crash `IllegalStateException`. Cùng guard với `closeTopSdkScreen()`/`showErrorDialog()`.
        if (!isAdded) return
        if (closeTopSdkScreen()) return
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    /**
     * Đóng đúng **một** màn SDK: pop entry trên cùng của [screenManager]. `true` = đã đóng được,
     * `false` = không còn gì để đóng (nơi gọi dựa vào đó để nhường sự kiện cho host).
     *
     * Bốn chi tiết dưới đây, mỗi cái từng là một lỗi thật — đừng rút gọn:
     *
     * 1. **`screenManager`**, không phải FM của Activity — xem [screenManager].
     * 2. **`popBackStackImmediate`**, không phải `popBackStack`: bản async chỉ *xếp hàng* giao dịch,
     *    `backStackEntryCount` chưa giảm ngay → hai lần back liên tiếp cùng đọc số cũ rồi cùng
     *    enqueue, pop **hai** entry cho một lần bấm.
     * 3. **Truyền `id` của entry trên cùng**, không dùng `popBackStackImmediate()` không tham số. Bản
     *    không tham số uỷ quyền xuống primary navigation fragment TRƯỚC khi pop stack của chính nó:
     *    `if (mPrimaryNav != null && id < 0 && name == null) { ... }`. `NavHostFragment` tự đặt mình
     *    làm primary nav, nên với host dùng Navigation nó pop **màn của host** rồi trả `true` — SDK
     *    tưởng đã đóng màn mình nên nuốt luôn phím back: màn SDK ở lại, màn host lùi một nấc. Truyền
     *    `id` là thoát điều kiện uỷ quyền đó. `POP_BACK_STACK_INCLUSIVE` để pop **chính** entry đó
     *    (thiếu cờ này thì nó chỉ pop những entry nằm *trên*, tức không pop gì).
     * 4. **Chặn khi state đã lưu**: pop sau `onSaveInstanceState` ném `IllegalStateException`.
     */
    protected fun closeTopSdkScreen(): Boolean {
        if (!isAdded) return false
        val manager = screenManager
        if (manager.isStateSaved) return false
        val entryCount = manager.backStackEntryCount
        if (entryCount == 0) return false
        val topEntryId = manager.getBackStackEntryAt(entryCount - 1).id
        return manager.popBackStackImmediate(
            topEntryId,
            FragmentManager.POP_BACK_STACK_INCLUSIVE,
        )
    }

    /**
     * Báo lỗi cho user bằng **popup** ([PRMBaseConfirmDialog] 1 nút "Đóng").
     *
     * Thay cho `PromotionToastGate.showAlways` đã bỏ: toàn bộ cổng bật/tắt toast không còn, lỗi mà
     * user đang chờ kết quả thì luôn phải hiện. Đối ứng `PromotionToast.showAlways` bên iOS
     * (cũng dựng `PRMConfirmationDialog`).
     */
    protected fun showErrorDialog(message: CharSequence) {
        if (!isAdded) return
        PRMBaseConfirmDialog.showError(requireContext(), parentFragmentManager, message)
    }
}
