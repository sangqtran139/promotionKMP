package com.ttcn.prm.ui.base

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IdRes
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

    protected fun showToast(message: CharSequence?) {
        // Toast bị gom sau [PromotionToastGate] — mặc định TẮT (lỗi vẫn được bắt, chỉ không hiện).
        if (!PromotionToastGate.isEnabled) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Map mã lỗi (raw từ store) → chuỗi hiển thị — **dùng chung mọi màn** (đồng nhất iOS
     * `PromotionUIStrings.errorMessage`). Gom về đây thay cho `mapErrorMessage` lặp ở từng Fragment.
     */
    protected fun mapPromotionError(code: String): String = when (code) {
        ErrorCodes.MISSING_CUSTOMER_ID -> getString(R.string.prm_missing_customer_id)
        ErrorCodes.NO_RESULT, "error_detail_unavailable" -> getString(R.string.prm_no_result)
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
     * nhúng của host.
     */
    protected fun addFragment(
        fragment: Fragment,
        addToBackStack: Boolean = true
    ) {
        val containerId = (view?.parent as? ViewGroup)?.id
        if (containerId == null || containerId == View.NO_ID) {
            showToast("Cannot navigate: no valid container found.")
            return
        }
        val tag = fragment::class.java.simpleName
        screenManager.beginTransaction()
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
     * [PromotionToastGate.showFeatureDisabled] nên **luôn hiện**, kể cả khi cổng toast chung đang TẮT.
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
            PromotionToastGate.showFeatureDisabled(requireContext())
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
}
