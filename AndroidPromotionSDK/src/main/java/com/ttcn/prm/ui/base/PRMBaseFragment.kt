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
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureGate
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
            Timber.tag(TAG_SYSTEM_BACK)
                .d("[$tag] OnBackPressedCallback fired, ownBackStack=${if (isAdded) parentFragmentManager.backStackEntryCount else -1}")
            if (popOwnBackStack()) return@addCallback

            // Tự tắt trước khi đẩy lên trên: nếu không, dispatcher chọn lại đúng callback này (vẫn
            // đang enabled) -> đệ quy vô hạn.
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
            // BẬT LẠI. Bản cũ tắt vĩnh viễn: fragment SDK còn sống mà callback đã chết thì lần back
            // sau rơi thẳng xuống NavController — host lùi màn trong khi màn SDK vẫn đang hiện.
            // `onBackPressed()` chạy đồng bộ nên tới đây nó đã xong; chỉ bật lại nếu fragment còn sống.
            if (isAdded) isEnabled = true
        }
    }

    /** Instance wrapper mà **chính fragment này** đã gắn — dùng để unwrap đúng chỗ ở [onPause] (xem đó). */
    private var installedWindowCallback: PrmBackKeyWindowCallback? = null

    /**
     * Chặn back hệ thống ở tầng `Window.Callback`: thay `window.callback` bằng wrapper chỉ override
     * `dispatchKeyEvent()`, phần còn lại delegate nguyên cho callback gốc. `dispatchKeyEvent()` là
     * điểm đầu tiên mọi `KeyEvent` của Window đi qua, nên back bị bắt trước khi tới
     * `Activity.dispatchKeyEvent()` / `onBackPressed()` của host.
     *
     * Wrap ở [onResume], unwrap ở [onPause] — chỉ chặn khi màn này đang hiện trên cùng.
     *
     * ⚠️ Nhiều fragment SDK cùng resumed (host `add()` không hide): fragment thứ hai thấy
     * `window.callback` đã là [PrmBackKeyWindowCallback] nên early-return, không wrap chồng. Back vật
     * lý lúc đó gọi `onBackFragment()` của fragment SDK **đầu tiên** trong stack, bất kể fragment nào
     * đang hiện trên cùng — xem [onBackFragment].
     *
     * Chạy song song với [registerBackPressedCallback]: predictive back (Android 13+) không sinh
     * `KeyEvent` nên wrapper này không bắt được, phần đó do `OnBackPressedCallback` lo. Hai lớp không
     * chồng nhau vì wrapper consume xong thì event không tới `Activity.onBackPressed()` nữa.
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
            if (!isAdded) {
                false
            } else {
                val handled = popOwnBackStack()
                Timber.tag(TAG_SYSTEM_BACK)
                    .d("[${this::class.java.simpleName}] Window.Callback saw KEYCODE_BACK, handled=$handled")
                // handled=false -> KHÔNG nuốt, để event đi tiếp tới host/NavController.
                handled
            }
        }
        installedWindowCallback = wrapper
        window.callback = wrapper
    }

    /**
     * Khôi phục `window.callback` **chỉ khi** nó vẫn đúng là instance mình đã gắn
     * ([installedWindowCallback], so bằng `===`, không so theo kiểu). Ai đó đã thay `window.callback`
     * sau lúc mình wrap thì bỏ qua, không đụng gì.
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

    /**
     * [onBackKey] trả `true` = SDK đã xử lý xong, **nuốt** event. Trả `false` = SDK không có gì để
     * xử lý, event **đi tiếp** xuống `delegate` như chưa hề bị chặn.
     *
     * Bản cũ luôn `return true`. Với host Navigation đó là hành vi phá hoại: wrapper gắn trên window
     * của **Activity** nên nó nuốt sạch phím back kể cả khi màn SDK không còn gì để pop —
     * `NavController` không bao giờ nhìn thấy sự kiện. Trả `false` để nhường thì tự nhiên hơn hẳn
     * việc gọi ngược `dispatcher.onBackPressed()` (dễ đệ quy, và bỏ qua các callback đứng trên).
     */
    private class PrmBackKeyWindowCallback(
        val delegate: Window.Callback,
        private val onBackKey: () -> Boolean,
    ) : Window.Callback by delegate {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (onBackKey()) return true
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

        // `parentFragmentManager`: màn SDK mới phải nằm CÙNG FM với màn SDK đang mở nó, nếu không
        // `popOwnBackStack()` đọc một stack mà entry lại nằm ở stack khác. Với host Navigation, FM
        // đó là `childFragmentManager` của destination (xem `PromotionSDK.resolveFragmentManager`).
        parentFragmentManager
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

        // Cùng lý do với addFragment: bám FM sở hữu fragment này, không phải FM của Activity.
        parentFragmentManager
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
        if (!popOwnBackStack()) {
            // Không còn gì của SDK để pop → trả quyền cho host (NavController, hoặc callback của họ).
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    /**
     * Pop **một** entry trong back stack của chính SDK. Trả `true` nếu đã pop, `false` nếu không còn
     * gì — nơi gọi dựa vào đó để quyết định có nhường sự kiện cho host hay không.
     *
     * Ba điểm khác bản cũ, mỗi điểm sửa một lỗi thật:
     *
     * 1. **`parentFragmentManager`, không phải `activity.supportFragmentManager`.** Đây là FM đang
     *    thật sự chứa fragment này — với host Navigation nó là `childFragmentManager` của destination
     *    (xem `PromotionSDK.resolveFragmentManager`), với host thường nó vẫn là FM của Activity. Bản
     *    cũ đọc FM của Activity nên với host Navigation nó đếm nhầm stack: `backStackEntryCount` không
     *    thấy destination nào của host, còn `popBackStack()` thì pop nhầm entry.
     *
     * 2. **`popBackStackImmediate()`, không phải `popBackStack()`.** Bản async chỉ *xếp hàng* giao
     *    dịch, `backStackEntryCount` không giảm ngay — hai lần back liên tiếp cùng đọc số cũ rồi cùng
     *    enqueue, pop **hai** entry cho một lần bấm.
     *
     * 3. **Chặn khi state đã lưu.** Pop sau `onSaveInstanceState` ném `IllegalStateException`.
     */
    protected fun popOwnBackStack(): Boolean {
        if (!isAdded) return false
        val manager = parentFragmentManager
        if (manager.isStateSaved) return false
        if (manager.backStackEntryCount == 0) return false
        return manager.popBackStackImmediate()
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