package com.ttcn.prm.ui.base

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
        val themed = ContextThemeWrapper(requireContext(), R.style.PRMForceLight)
        return base.cloneInContext(themed)
    }

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
     * Cộng thêm chiều cao navigation bar hệ thống vào `marginBottom` sẵn có của [anchor] — chỉ khi
     * [anchor] thật sự đang bị navigation bar che (xem [isOverlappedByNavigationBar]).
     *
     * **Gọi đúng lúc [anchor] đã hiển thị** (sau khi set `visibility = VISIBLE`) — view `GONE` không
     * được layout nên toạ độ đo được không đáng tin; hàm chỉ đợi đúng một layout pass kế tiếp qua
     * [View.doOnNextLayout] để đọc toạ độ thật, không tự dò theo vòng đời hay sự kiện nào khác.
     *
     * Idempotent với việc gọi lại nhiều lần trên cùng 1 view instance (vd Fragment không bị huỷ view
     * mà chỉ ẩn/hiện lại) — xem [R.id.prm_tag_nav_bar_inset_margin].
     */
    protected fun applyNavigationBarInset(anchor: View) {
        anchor.doOnNextLayout {
            val previouslyApplied = anchor.getTag(R.id.prm_tag_nav_bar_inset_margin) as? Int
            // Lần gọi THỨ HAI trở đi (Fragment không huỷ view, chỉ ẩn/hiện lại): toạ độ đọc được lúc
            // này đã bị chính lần áp trước đẩy lên rồi, đo lại theo `isOverlappedByNavigationBar` sẽ
            // LUÔN ra "không overlap" (vì đã dịch lên khỏi vùng nav bar) → tính nhầm `toApply = 0` →
            // set `bottomMargin` về đúng giá trị GỐC, xoá mất margin đã áp — nút lại bị nav bar đè.
            // Fix: giữ nguyên quyết định của lần đo ĐẦU (khi toạ độ còn nguyên bản, chưa bị sửa),
            // không đo lại từ toạ độ đã dịch.
            val toApply = previouslyApplied
                ?: run {
                    val navBarInset = currentNavigationBarInset(anchor)
                    if (navBarInset > 0 && isOverlappedByNavigationBar(anchor, navBarInset)) navBarInset else 0
                }
            val baseMargin = ((anchor.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0) - (previouslyApplied ?: 0)
            anchor.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = baseMargin + toApply
            }
            anchor.setTag(R.id.prm_tag_nav_bar_inset_margin, toApply)
        }
    }

    /**
     * Cộng thêm chiều cao navigation bar hệ thống vào `paddingBottom` sẵn có của [scrollable] — cũng
     * chỉ khi [scrollable] thật sự đang bị navigation bar che, dùng cho nội dung cuộn được
     * (`RecyclerView`...) thay vì [applyNavigationBarInset], để item cuối cùng không bị navigation bar
     * che khi cuộn hết cỡ trên các thiết bị navigation bar to (3 nút).
     *
     * Tự bật `clipToPadding = false` nếu [scrollable] là `ViewGroup` — phần padding thêm chỉ nới rộng
     * vùng cuộn, không cắt mất nội dung item cuối khi nó cuộn vào đúng vùng padding đó.
     *
     * Gọi đúng lúc [scrollable] đã hiển thị, cùng lý do với [applyNavigationBarInset]. Idempotent
     * tương tự — xem [R.id.prm_tag_nav_bar_inset_padding].
     */
    protected fun applyNavigationBarInsetAsScrollPadding(scrollable: View) {
        if (scrollable is ViewGroup) scrollable.clipToPadding = false
        scrollable.doOnNextLayout {
            val previouslyApplied = scrollable.getTag(R.id.prm_tag_nav_bar_inset_padding) as? Int
            // Cùng bug/fix với `applyNavigationBarInset` — xem comment ở đó. Lần gọi lại sau khi đã áp
            // padding thì toạ độ đo được đã dịch lên rồi, không đo lại được nữa.
            val toApply = previouslyApplied
                ?: run {
                    val navBarInset = currentNavigationBarInset(scrollable)
                    if (navBarInset > 0 && isOverlappedByNavigationBar(scrollable, navBarInset)) navBarInset else 0
                }
            val basePadding = scrollable.paddingBottom - (previouslyApplied ?: 0)
            scrollable.updatePadding(bottom = basePadding + toApply)
            scrollable.setTag(R.id.prm_tag_nav_bar_inset_padding, toApply)
        }
    }

    private fun currentNavigationBarInset(view: View): Int =
        ViewCompat.getRootWindowInsets(view)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            ?.bottom ?: 0

    /**
     * So toạ độ tuyệt đối trên màn hình của [view] với vùng navigation bar hệ thống — cách duy nhất
     * biết chắc view đang bị che thật hay chỉ đang được đo "phòng hờ": container SDK nhận được có
     * tràn xuống dưới navigation bar hay không phụ thuộc host, không suy luận được từ layout params.
     *
     * Lấy chiều cao **window thật chứa [view]** ([WindowManager.currentWindowMetrics], API 30+) thay
     * vì `resources.displayMetrics.heightPixels` (chiều cao *display*) — hai giá trị lệch nhau ở
     * multi-window/split-screen/desktop windowing (Android 16 đẩy mạnh trên tablet/Chromebook), lúc
     * đó app chỉ chiếm một phần màn hình nên so với chiều cao display sẽ luôn sai.
     */
    private fun isOverlappedByNavigationBar(view: View, navBarInset: Int): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewBottomOnScreen = location[1] + view.height
        val windowHeight = windowHeightOf(view)
        return viewBottomOnScreen > windowHeight - navBarInset
    }

    private fun windowHeightOf(view: View): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowManager = view.context.getSystemService(WindowManager::class.java)
            if (windowManager != null) return windowManager.currentWindowMetrics.bounds.height()
        }
        return view.resources.displayMetrics.heightPixels
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
        ErrorCodes.MISSING_CUSTOMER_ID -> getString(R.string.prm_missing_customer_id)
        ErrorCodes.NO_RESULT, "error_detail_unavailable" -> getString(R.string.prm_no_result)
        // Lỗi mạng/timeout có câu RIÊNG: tầng data đã cất công phân loại (`NetworkException`), gộp
        // vào lỗi chung là vứt đi thông tin duy nhất mà user hành động được. Thấy rõ nhất ở nút
        // "Áp dụng" màn Chọn ưu đãi — mất mạng lúc validate thì phải báo đúng là mất mạng.
        ErrorCodes.NETWORK_ERROR -> getString(R.string.prm_error_network)
        ErrorCodes.TIMEOUT -> getString(R.string.prm_error_timeout)
        else -> getString(R.string.prm_error_general)
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
        // gọi `goBack()` từ callback bất đồng bộ của `PRMEndowView.applySelectedOffers` (chạy trên
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
