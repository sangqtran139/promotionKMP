package com.ttcn.prm.ui.base

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.ttcn.prm.R
import com.ttcn.promotionsdk.core.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.prm.ui.feature.promotion.promotiondetail.PromotionDetailFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class PRMBaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = requireNotNull(_binding) {
            "Binding is only valid between onCreateView and onDestroyView."
        }

    abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

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
        setupUI()
        observeData()
    }

    open fun setupUI() {}
    open fun observeData() {}

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

    /**
     * Mở màn "Chi tiết ưu đãi", gác bởi cờ `VOUCHER_DETAIL`.
     * Cờ TẮT → thông báo PRM_MOB_021 và không điều hướng.
     *
     * Song sinh của `BaseRouter.canRouteToDetail()` bên iOS: gom về base để cả ba màn gọi
     * (Ưu đãi của tôi, Tìm kiếm, Chọn ưu đãi) không thể quên gác.
     */
    protected fun openPromotionDetail(voucherId: String) {
        if (!PromotionFeatureGate.canOpenVoucherDetail()) {
            showToast(getString(R.string.prm_feature_disabled))
            return
        }
        addFragment(PromotionDetailFragment.newInstance(voucherId))
    }

    protected fun addFragment(
        fragment: Fragment,
        addToBackStack: Boolean = true
    ) {
        val parent = requireView().parent as? ViewGroup
        val containerId = parent?.id ?: run {
            showToast("Cannot navigate: no valid container found.")
            return
        }
        val tag = fragment::class.java.simpleName

        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .add(containerId, fragment, tag)
            .apply { if (addToBackStack) addToBackStack(tag) }
            .commit()
    }

    protected fun replaceFragment(
        fragment: Fragment,
        @IdRes containerId: Int,
        addToBackStack: Boolean = true
    ) {
        val tag = fragment::class.java.simpleName

        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(containerId, fragment, tag)
            .apply { if (addToBackStack) addToBackStack(tag) }
            .commit()
    }

    /**
     * Back của các màn SDK.
     *
     * Ngưỡng phải là `> 0`, **không phải `> 1`**: khi host mở màn SDK **đầu tiên** từ màn của họ
     * (`openMyPromotion` / `openPromotionDetail` — cả hai `addToBackStack`), back stack của Activity
     * chỉ có **1** entry, vì màn của host thường không nằm trên back stack. Với `> 1` thì lần back
     * đầu rơi vào nhánh `else` và **đóng luôn Activity của host** thay vì quay về màn host.
     *
     * Hết entry để pop (host tự nhúng fragment, không qua back stack) thì **không** `finish()` —
     * SDK không có quyền đóng Activity của host; đẩy sự kiện về `onBackPressedDispatcher` để host
     * quyết định (không màn nào của SDK đăng ký `OnBackPressedCallback` nên không có đệ quy).
     */
    open fun onBackFragment() {
        val activity = requireActivity()
        val manager = activity.supportFragmentManager
        if (manager.backStackEntryCount > 0) {
            manager.popBackStack()
        } else {
            activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}