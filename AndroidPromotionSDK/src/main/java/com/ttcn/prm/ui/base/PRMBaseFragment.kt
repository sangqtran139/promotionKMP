package com.ttcn.prm.ui.base

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.activity.addCallback
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
import timber.log.Timber

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
        registerBackPressedCallback()
        setupUI()
        observeData()
    }

    /**
     * `OnBackPressedCallback` chuẩn — chỉ thật sự chạy khi host để `Activity.onBackPressed()` gọi
     * `super`/dispatcher (hoặc dùng predictive back, Android 13+ `enableOnBackInvokedCallback=true`,
     * cơ chế đó không đi qua `KeyEvent` nên [wrapWindowCallback] không bắt được). Với host override
     * `onBackPressed()` tuỳ biến không gọi `super` (đã xác nhận thực tế), callback này KHÔNG chạy —
     * lớp chặn thật sự cho case đó là [wrapWindowCallback].
     */
    private fun registerBackPressedCallback() {
        val tag = this::class.java.simpleName
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val manager = requireActivity().supportFragmentManager
            Timber.tag(TAG_SYSTEM_BACK).d("[$tag] OnBackPressedCallback fired, backStackEntryCount=${manager.backStackEntryCount}")
            if (manager.backStackEntryCount > 0) {
                manager.popBackStack()
            } else {
                // Tự tắt trước khi đẩy lên trên: nếu không, dispatcher chọn lại đúng callback này
                // (vẫn đang enabled) -> đệ quy vô hạn.
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /** Instance wrapper mà **chính fragment này** đã gắn — dùng để unwrap đúng chỗ ở [onPause] (xem đó). */
    private var installedWindowCallback: PrmBackKeyWindowCallback? = null

    /**
     * Chặn back hệ thống ở tầng `Window.Callback` — thay cho cách cũ dựa vào `View.requestFocus()` +
     * `setOnKeyListener`, vốn phụ thuộc đúng view nào đang GIỮ FOCUS trong cây View. Cách cũ chạy được
     * ở [PromotionDetailFragment] nhưng KHÔNG ổn định ở [MyPromotionFragment] vì màn đó có
     * `RecyclerView` (tabs + danh sách) load dữ liệu bất đồng bộ sau `onResume()`, nhiều khả năng tự
     * cướp focus sau khi mình request xong -> `setOnKeyListener` mất tác dụng tuỳ thời điểm dữ liệu về.
     *
     * `Window.Callback.dispatchKeyEvent()` là điểm ĐẦU TIÊN mọi KeyEvent của Window đi qua — `Activity`
     * chính là implementation mặc định của `Window.Callback` (`window.callback` trỏ vào `activity` lúc
     * `Activity.attach()`). Thay `window.callback` bằng 1 wrapper chỉ override `dispatchKeyEvent()`,
     * còn lại delegate nguyên cho callback gốc (`by delegate`) — bắt được back TRƯỚC KHI event tới
     * `Activity.dispatchKeyEvent()`/`onBackPressed()` của host, không phụ thuộc View nào đang focus,
     * không phụ thuộc host viết `onBackPressed()` thế nào.
     *
     * Wrap ở [onResume] / unwrap ở [onPause] để chỉ chặn đúng lúc màn này đang hiện trên cùng; đảm bảo
     * trả lại callback gốc khi rời màn, tránh host bị dính wrapper của SDK sau khi SDK đã lui.
     *
     * ⚠️ **Nhiều fragment SDK cùng resumed** (host `add()` không hide — vd `PromotionDetailFragment`
     * mở trên `MyPromotionFragment` qua [openPromotionDetail]/[addFragment]): fragment thứ hai thấy
     * `window.callback` đã là [PrmBackKeyWindowCallback] nên early-return, không tự wrap. Back vật lý
     * lúc đó luôn gọi `onBackFragment()` của fragment SDK **đầu tiên** trong stack, bất kể fragment nào
     * đang hiện trên cùng — xem cảnh báo ở [onBackFragment].
     *
     * Không thay thế [registerBackPressedCallback]: giữ cả hai vì predictive back (Android 13+,
     * `enableOnBackInvokedCallback=true`) không sinh `KeyEvent` nên wrapper này sẽ không bắt được, lúc
     * đó cần `OnBackPressedCallback` xử lý qua `OnBackInvokedDispatcher`. Hai lớp không đụng nhau: nếu
     * wrapper này consume xong (trả `true`, không gọi `delegate.dispatchKeyEvent`) thì event không bao
     * giờ tới `Activity.onBackPressed()` nữa nên dispatcher không được gọi lại lần 2 cho cùng 1 lần bấm.
     */
    override fun onResume() {
        super.onResume()
        val window = requireActivity().window
        val current = window.callback
        if (current is PrmBackKeyWindowCallback) return
        val wrapper = PrmBackKeyWindowCallback(current) {
            // Guard: nếu wrapper này lỡ còn sống sau khi fragment đã detach (vd bị 1 thư viện khác
            // ghi đè window.callback xen giữa, làm onPause() bên dưới không unwrap được), rơi qua
            // dispatchKeyEvent gốc thay vì gọi tiếp requireActivity() trên fragment đã chết.
            if (isAdded) {
                Timber.tag(TAG_SYSTEM_BACK).d("[${this::class.java.simpleName}] Window.Callback intercepted KEYCODE_BACK")
                onBackFragment()
            }
        }
        installedWindowCallback = wrapper
        window.callback = wrapper
    }

    /**
     * Chỉ khôi phục `window.callback` khi nó **vẫn đúng là instance mình đã gắn** ([installedWindowCallback],
     * so bằng `===`) — KHÔNG so theo kiểu (`is PrmBackKeyWindowCallback`). Nếu ai đó (thư viện khác,
     * fragment SDK khác) đã thay `window.callback` sau lúc mình wrap, so theo kiểu sẽ giật nhầm/giẫm
     * lên dây chuyền của người khác, có thể để lại 1 wrapper "chết" (trỏ fragment đã destroy) kẹt vĩnh
     * viễn trong window — back sau đó gọi `onBackFragment()` trên fragment đã detach → crash host ở
     * màn hình không liên quan gì tới SDK. Không phải mình đang giữ thì đơn giản bỏ qua, không đụng gì.
     */
    override fun onPause() {
        val window = activity?.window
        val wrapper = installedWindowCallback
        if (window != null && wrapper != null && window.callback === wrapper) {
            window.callback = wrapper.delegate
        }
        installedWindowCallback = null
        super.onPause()
    }

    private class PrmBackKeyWindowCallback(
        val delegate: Window.Callback,
        private val onBackKey: () -> Unit,
    ) : Window.Callback by delegate {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                onBackKey()
                return true
            }
            return delegate.dispatchKeyEvent(event)
        }
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
     * quyết định. Callback do [registerBackPressedCallback] đăng ký tự `isEnabled = false` trước khi
     * gọi lại `dispatcher.onBackPressed()` ở đúng tình huống này nên không đệ quy.
     *
     * ⚠️ **Không dùng state riêng của fragment ở override** (binding/viewModel...). Khi nhiều fragment
     * SDK cùng resumed, back vật lý gọi hàm này trên fragment SDK **đầu tiên** trong stack (xem cảnh
     * báo ở [onResume]), không nhất thiết là fragment đang hiện trên cùng — bản gốc chỉ thao tác qua
     * `FragmentManager` dùng chung nên vô can với việc đó, override cũng phải giữ tính chất này.
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

    private companion object {
        // Log để đối chiếu 2 lớp chặn back hệ thống (Window.Callback + OnBackPressedCallback) khi
        // tích hợp vào host thật — có thể gỡ bớt/hạ xuống mức verbose sau khi đã chạy ổn định.
        private const val TAG_SYSTEM_BACK = "PRMSystemBack"
    }
}