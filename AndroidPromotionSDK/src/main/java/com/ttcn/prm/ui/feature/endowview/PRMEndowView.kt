package com.ttcn.prm.ui.feature.endowview

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.ttcn.prm.ui.di.promotionViewModelFactory
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.prm.R
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.databinding.PrmViewEndowBinding
import com.ttcn.prm.ui.feature.choosepromotion.adapter.ApplyPromotionAdapter
import androidx.core.view.isVisible
import com.ttcn.prm.entry.api.PromotionSDKError
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.presentation.endow.EndowApplyOutcome
import com.ttcn.promotionsdk.presentation.endow.EndowConfirmResult
import com.ttcn.promotionsdk.presentation.endow.EndowHostEvent
import com.ttcn.promotionsdk.presentation.endow.EndowHostNotifier
import com.ttcn.promotionsdk.presentation.endow.EndowState
import com.ttcn.promotionsdk.presentation.endow.EndowWidgetState
import com.ttcn.prm.ui.theme.token.DiscountBadgeToken
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.utils.applyTextColorIfSet
import com.ttcn.prm.ui.utils.findActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PRMEndowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // ─── Binding & Adapter ────────────────────────────────────────────────────

    // Ép LIGHT cho widget: inflate dưới theme Light (R.style.PRMForceLight) để text/màu ngầm định
    // không lấy màu tối từ theme host DayNight. Cùng cơ chế với PRMBaseFragment. Không đụng host.
    private val binding: PrmViewEndowBinding =
        PrmViewEndowBinding.inflate(
            LayoutInflater.from(ContextThemeWrapper(context, R.style.PRMForceLight)), this, true,
        )

    private val applyPromotionAdapter = ApplyPromotionAdapter()

    // ─── ViewModel — khởi tạo lazy để scope sẵn sàng khi onAttachedToWindow ──

    private var viewModel: EndowViewModel? = null

    // ─── Internal UI state ────────────────────────────────────────────────────

    private var currentState: EndowWidgetState = EndowWidgetState.NOT_APPLIED
    private var lastAppliedToken: DiscountBadgeToken? = null
    private var viewScope: CoroutineScope? = null

    /** Giữ để [onDetachedFromWindow] gỡ đúng observer mình đã gắn — xem chỗ gắn ở đó. */
    private var lifecycleObserver: DefaultLifecycleObserver? = null
    private var lifecycleOwnerRef: LifecycleOwner? = null

    /**
     * Quyết định "khi nào bắn callback host" — rule dùng chung ở `promotionLogic`, có test.
     * Trước đây là ba biến `lastNotifiedState`/`lastNotifiedCount`/`lastNotifiedAvailability` ngay
     * tại đây, và iOS có bản riêng chỉ hai biến — cùng hợp đồng public mà hai cách tính.
     *
     * Một instance mỗi widget: nó có trạng thái, hai `PRMEndowView` cùng màn phải đếm riêng.
     */
    private val hostNotifier = EndowHostNotifier()

    // ─── Read-only accessors (delegate to ViewModel state) ────────────────────

    // `myVouchers`/`otherVouchers` + hai cờ phân trang đã bỏ: chúng chỉ tồn tại để đẩy sang màn "Chọn
    // ưu đãi" làm dữ liệu preload, mà màn đó nay **luôn** tự gọi `findEligible` khi mở
    // (`ChoosePromotionIntent.SeedOnce`). Dữ liệu vẫn còn ở `EndowState` — nơi widget cất kết quả nạp
    // của chính nó — chỉ là không ai đọc nhờ qua đây nữa.

    val discountDetails: List<AppliedDiscount>
        get() = viewModel?.state?.value?.appliedDiscounts ?: emptyList()

    // ─── Public callbacks ─────────────────────────────────────────────────────

    /**
     * Lỗi từ widget — **kiểu công khai**, không phải mã thô.
     *
     * Trước đây trả `String`: host muốn phân biệt `PRM_MOB_021` phải hardcode chuỗi, vì hằng số mã
     * lỗi nằm trong `promotionLogic` (`implementation`, không có trên compile classpath của host).
     * Nay `when (error) { is PromotionSDKError.FeatureDisabled -> … }` là xong.
     *
     * `FeatureDisabled`: SDK **đã tự hiện popup**, host chỉ cần dừng luồng của mình.
     */
    var onError: ((error: PromotionSDKError) -> Unit)? = null

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        applyDefaultBackgroundIfHostDidNotSetOne()
        setupRecyclerView()
        setupClickListeners()
        applyToken(PromotionThemeRegistry.discountBadgeToken())
    }

    /**
     * Widget vốn KHÔNG có nền: `prm_view_endow.xml` không khai `android:background` nào. Trên host
     * dùng theme DayNight, ở dark mode cái lộ ra sau widget là nền tối của host, trong khi chữ bên
     * trong cứng ở màu sáng (`prm_color_222_cep` = #222222) → nhìn như widget "bị dark theme".
     *
     * [ContextThemeWrapper] ở chỗ inflate KHÔNG cứu được: nó chỉ đổi cách resolve **attribute** lúc
     * inflate, mà ở đây không có attribute nền nào để resolve. Khác iOS — `overrideUserInterfaceStyle`
     * ép cả subtree gồm cả nền, nên `PRMEndowView.swift` không dính.
     *
     * **SDK không tự quyết nền của host.** Chỉ sơn khi host chưa đặt gì: `android:background` khai
     * trong XML của host đã được constructor `View` đọc vào trước khi khối `init` này chạy, nên
     * `background != null` nghĩa là host đã có ý — giữ nguyên, không đè. Host đặt nền bằng code sau
     * khi view dựng xong thì lệnh của host chạy sau, cũng thắng.
     */
    private fun applyDefaultBackgroundIfHostDidNotSetOne() {
        if (background == null) {
            setBackgroundColor(ContextCompat.getColor(context, R.color.prm_white))
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        viewScope = scope

        // VM lấy từ `ViewModelStore` của màn host (qua cây view), KHÔNG tự tạo theo scope của View.
        //
        // Đây là chỗ sửa lỗi cũ: bản trước `EndowViewModel.create(viewScope)` nên store chết ở
        // `onDetachedFromWindow`. Host mở màn "Chọn ưu đãi" bằng `replace()` → widget detach →
        // `viewModel = null` → bấm "Áp dụng"/"Thanh toán" trả `PRM_ERROR_GENERAL` mà không gọi mạng.
        // iOS không dính vì `endowVM` là property của `PromotionSDKImpl`.
        //
        // Thiếu owner (host nhúng widget ngoài mọi Activity/Fragment có ViewModelStore) → không dựng
        // được VM; widget ẩn đi thay vì chạy nửa vời rồi lỗi khi bấm.
        val owner = findViewTreeViewModelStoreOwner()
        if (owner == null) {
            // Mọi ComponentActivity/Fragment đều cấp owner này, nên tới đây là host đã nhúng widget
            // vào một cây view không có ViewModelStore (Dialog/Window tự dựng, ViewGroup rời…).
            // Ẩn IM LẶNG là kiểu hỏng tệ nhất: host không hiểu vì sao widget biến mất. Báo ra
            // `onError` để host còn biết đường sửa chỗ nhúng.
            isVisible = false
            onError?.invoke(PromotionSDKError.from(ErrorCodes.GENERAL))
            return
        }
        val vm = ViewModelProvider(owner, promotionViewModelFactory())[EndowViewModel::class.java]
        viewModel = vm

        // Lưới an toàn cho trường hợp view bị huỷ mà không qua onDetachedFromWindow. Phải GỠ ở
        // [onDetachedFromWindow]: observer là inner class nên nó giữ luôn `this`, mà lifecycle của
        // host thì sống lâu hơn view rất nhiều — attach/detach vài vòng (RecyclerView tái dụng,
        // fragment show/hide) là chồng một đống observer, mỗi cái ghim một view chết lại tới khi
        // host destroy.
        // Tên KHÁC `owner` ở trên (ViewModelStoreOwner) — hai thứ khác nhau, trùng tên là lỗi compile.
        val lifecycleOwner = findViewTreeLifecycleOwner()
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseScope()
            }
        }
        lifecycleOwnerRef = lifecycleOwner
        lifecycleObserver = observer
        lifecycleOwner?.lifecycle?.addObserver(observer)

        // Observe state changes
        scope.launch {
            vm.state.collect { state ->
                renderState(state)
            }
        }

        binding.shimmerEndow.startShimmer()

        // Gác bởi cờ VOUCHER_SELECTION, y như `PromotionSDKImpl.applyFlag` bên iOS: áp cache hiện
        // có ngay lập tức (chưa có cache → bật lạc quan), rồi làm mới từ server và áp lại nếu đổi.
        applyFeatureFlag(vm.availabilityFromCache(), vm)
        scope.launch { applyFeatureFlag(vm.refreshAvailability(), vm) }
    }

    /**
     * Cờ TẮT → ẩn widget và không gọi API. Cờ BẬT → hiện và nạp ưu đãi (chỉ nạp một lần).
     *
     * Báo host mỗi lần đổi trạng thái, y như `PromotionSDKImpl.applyFlag` bên iOS: widget bị rút đi
     * là lúc host cần biết để thu gọn layout của mình. Chỉ bắn khi **đổi** để `applyFlag` gọi hai lần
     * (cache rồi server) không sinh callback trùng.
     */
    private fun applyFeatureFlag(enabled: Boolean, vm: EndowViewModel) {
        isVisible = enabled
        if (enabled) vm.loadInitial()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleObserver?.let { lifecycleOwnerRef?.lifecycle?.removeObserver(it) }
        lifecycleObserver = null
        lifecycleOwnerRef = null
        releaseScope()
    }

    /**
     * Chỉ gỡ **scope render** của View. KHÔNG đụng [viewModel]: nó thuộc `ViewModelStore` của host,
     * sống qua detach/attach và qua cả xoay màn — đó chính là điều bản `create(scope)` cũ không có.
     * `onCleared()` của ViewModel lo huỷ scope store.
     */
    private fun releaseScope() {
        viewScope?.cancel()
        viewScope = null
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Nhận kết quả validateStackableDiscounts từ host.
     * SDK tự xác định trạng thái UNAVAILABLE nếu bất kỳ item nào có [AppliedDiscount.valid] == false.
     */
    fun setDiscountDetails(details: List<AppliedDiscount>) {
        val hasInvalid = details.isNotEmpty() && details.any { !it.valid }
        viewModel?.setApplied(details, unavailable = hasInvalid)
    }

    /**
     * Nhận offers user chọn từ màn "Chọn ưu đãi" → [EndowStore] validate & áp (dùng chung iOS).
     * `internal`: [EligibleOffer] thuộc `promotionLogic`; host dùng qua [ChoosePromotionFragment.forEndowView].
     *
     * [onSettled] gọi **đúng một lần ở mọi nhánh** khi lượt validate ngã ngũ, kèm
     * [EndowApplyOutcome] — màn chọn dựa vào đó để đóng màn, disable ưu đãi, hay báo lỗi. Đối ứng
     * completion của `endowVM.validateAndApply` bên iOS.
     *
     * Trước đây tham số là `errorCode: String?`, tức nơi gọi chỉ phân biệt được "mạng hỏng" với "mọi
     * thứ khác": server **từ chối** ưu đãi rơi vào cùng nhánh với thành công nên màn chọn đóng lại
     * như đã áp xong.
     */
    internal fun applySelectedOffers(
        offers: List<EligibleOffer>,
        onSettled: ((outcome: EndowApplyOutcome) -> Unit)? = null,
    ) {
        val vm = viewModel
        if (vm == null) {
            // Widget đã detach (host `replace` màn thay vì `add`) → không có store để validate.
            // Báo LỖI chứ không phải `Applied`: `Applied` nghĩa là "áp xong", màn chọn sẽ đóng và
            // user tưởng đã áp trong khi widget không hề đổi.
            onSettled?.invoke(EndowApplyOutcome.Failed(ErrorCodes.GENERAL))
            return
        }
        // Store `suspend` và trả kết cục của đúng lượt này → không còn rình `isValidating` trên
        // dòng state chung, không còn phải giữ lỗi lại chờ nơi gọi đọc.
        val scope = viewScope
        if (scope == null) {
            onSettled?.invoke(EndowApplyOutcome.Failed(ErrorCodes.GENERAL))
            return
        }
        scope.launch { onSettled?.invoke(vm.validateAndApply(offers)) }
    }

    /** Đánh dấu ưu đãi hiện tại không còn khả dụng mà không thay đổi danh sách. */
    fun markAppliedVoucherUnavailable() {
        viewModel?.markUnavailable()
    }

    fun applyToken(token: DiscountBadgeToken?) {
        lastAppliedToken = token
        applyTokenInternal(token)
    }

    fun setTitle(title: String) {
        binding.txtTitleEndow.text = title
    }

    /** Trạng thái đang hiển thị của widget. */
    fun getCurrentState(): EndowWidgetState = currentState

    /**
     * Gọi khi user bấm nút thanh toán của **host**: tạo phiên redemption cho các ưu đãi đang áp.
     *
     * ```kotlin
     * btnConfirmPayment.setOnClickListener {
     *     binding.endowView.confirmRedemption(
     *         onSuccess = { proceedPayment() },
     *         onError = { error -> showError(error.message) },
     *     )
     * }
     * ```
     *
     * Không áp ưu đãi nào → [onSuccess] ngay, không gọi mạng. Hết ngân sách giữa chừng → SDK tự
     * validate lại, widget hiện giá mới, rồi [onError] `INSUFFICIENT_BUDGET`.
     *
     * Nghiệp vụ nằm ở `EndowStore.confirmRedemption` (dùng chung với iOS). Trước đây phải qua
     * `PromotionIntegrateManager` — một class riêng, một scope riêng, và host phải nhớ gọi `clear()`;
     * nay chạy trên chính scope của widget nên không còn nghĩa vụ nào.
     *
     * Widget chưa attach (chưa có ViewModel) → [onError] `PRM_ERROR_GENERAL`.
     */
    @JvmOverloads
    fun confirmRedemption(
        onSuccess: () -> Unit,
        onError: (error: PromotionSDKError) -> Unit = {},
    ) {
        val vm = viewModel
        val scope = viewScope
        if (vm == null || scope == null) {
            onError(PromotionSDKError.from(ErrorCodes.GENERAL))
            return
        }
        scope.launch {
            when (val result = vm.confirmRedemption()) {
                is EndowConfirmResult.Success -> onSuccess()
                is EndowConfirmResult.Failure -> {
                    // KHÔNG popup, kể cả `PRM_MOB_021` (cờ `VOUCHER_REDEEM` tắt).
                    //
                    // Khác hẳn các điểm gác khác (`openMyPromotion`, `openPromotionDetail`): ở đó user
                    // vừa bấm để MỞ một tính năng, không nói gì thì màn hình đứng im vô lý. Còn đây là
                    // giữa luồng THANH TOÁN của host — SDK chen một popup của mình vào là cướp quyền
                    // điều khiển, trong khi host mới là bên biết phải dừng hay đi tiếp và hiện gì.
                    //
                    // Cờ tắt vẫn KHÔNG gọi API (`EndowStore.confirmRedemption` chặn trước) và vẫn trả
                    // đúng mã lỗi ra đây — chỉ bỏ phần hiển thị. Đối ứng `PromotionSDKImpl.confirmRedemption` iOS.
                    onError(PromotionSDKError.from(result.errorCode))
                }
            }
        }
    }

    // ─── Private: render ──────────────────────────────────────────────────────

    private fun renderState(state: EndowState) {
        // Handle error
        state.errorCode?.let {
            onError?.invoke(PromotionSDKError.from(it))
            // Widget không đi qua `PRMStoreViewModel.effects` (đọc thẳng StateFlow) nên phải tự bắt
            // TOKEN_EXPIRED ở đây — 4 màn Fragment khác đã có `PRMStoreViewModel.effects` lo hộ.
            if (it == ErrorCodes.TOKEN_EXPIRED) {
                PromotionSDK.getCallback()?.onExpireToken()
            }
            // Xoá được ngay: nơi gọi `validateAndApply` nhận `EndowApplyOutcome` trả về trực tiếp,
            // không đọc nhờ dòng state này nữa nên không còn đua nhau (trước phải có
            // `consumeErrorUnlessSettling`).
            viewModel?.consumeError()
        }

        if (!state.hasLoadedInitial) return

        binding.shimmerEndow.stopShimmer()
        binding.shimmerEndow.visibility = GONE

        // Trạng thái widget do store quyết định (EndowStore.widgetState) — View chỉ render.
        when (state.widgetState) {
            EndowWidgetState.UNAVAILABLE -> {
                currentState = EndowWidgetState.UNAVAILABLE
                showUnavailableState(state.appliedDiscounts)
            }

            EndowWidgetState.APPLIED -> {
                currentState = EndowWidgetState.APPLIED
                showAppliedState(state.appliedDiscounts)
            }

            EndowWidgetState.NOT_APPLIED -> {
                currentState = EndowWidgetState.NOT_APPLIED
                showNotAppliedState(state.totalVoucherCount)
            }

            EndowWidgetState.EMPTY -> {
                currentState = EndowWidgetState.EMPTY
                showEmptyState()
            }
        }

        notifyHost(state)
    }

    /**
     * Phát sự kiện cho host qua callback đã set lúc [PromotionSDK.initialize]. **Quyết định** bắn
     * hay không nằm ở [EndowHostNotifier] (dùng chung iOS); ở đây chỉ map sự kiện → callback.
     * Chỉ còn `onVoucherApplied`: các callback đếm voucher / bật-tắt / đóng màn đã bỏ, host không cần biết.
     */
    private fun notifyHost(state: EndowState) = emit(hostNotifier.onState(state))

    /** Map [EndowHostEvent] dùng chung → callback public của Android. */
    private fun emit(events: List<EndowHostEvent>) {
        val callback = PromotionSDK.getCallback() ?: return
        events.forEach { event ->
            when (event) {
                is EndowHostEvent.VoucherApplied -> callback.onVoucherApplied(event.voucherId)
            }
        }
    }

    // ─── Private: UI helpers ──────────────────────────────────────────────────

    private fun setupRecyclerView() {
        binding.rcvEndow.apply {
            adapter = applyPromotionAdapter
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupClickListeners() {
        binding.txtStatusEndow.setOnClickListener {
            when (currentState) {
                EndowWidgetState.NOT_APPLIED, EndowWidgetState.UNAVAILABLE -> openChoosePromotionScreen()
                EndowWidgetState.APPLIED -> {
                    viewModel?.clearApplied()
                }
                EndowWidgetState.EMPTY -> Unit
            }
        }
    }

    /**
     * Widget tự điều hướng sang màn "Chọn ưu đãi" — host không cần wiring gì thêm, chỉ nhúng
     * [PRMEndowView] vào layout (xem [PromotionSDK.openChoosePromotion]).
     *
     * `context` không quy về được [androidx.fragment.app.FragmentActivity] (widget đặt ngoài
     * Activity/Fragment thật, hiếm nhưng có thể xảy ra) → báo lỗi qua [onError] thay vì crash.
     */
    private fun openChoosePromotionScreen() {
        val activity = context.findActivity()
        if (activity == null) {
            onError?.invoke(PromotionSDKError.from(ErrorCodes.GENERAL))
            return
        }
        PromotionSDK.openChoosePromotion(activity, this)
    }

    /** Chưa chọn ưu đãi, còn ưu đãi khả dụng → hiển thị nút "Sử dụng" */
    private fun showNotAppliedState(count: Int) {
        binding.apply {
            txtNumberEndow.text = when (count) {
                1 -> context.getString(R.string.prm_one_endow)
                else -> context.getString(R.string.prm_multiple_endow, count)
            }
            txtNumberEndow.visibility = VISIBLE
            rcvEndow.visibility = GONE
            txtStatusEndow.visibility = VISIBLE
            txtStatusEndow.text = context.getString(R.string.prm_use_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    /** Đã chọn ưu đãi → hiển thị danh sách + nút "Hủy" */
    private fun showAppliedState(details: List<AppliedDiscount>) {
        binding.apply {
            applyPromotionAdapter.submitList(details)
            txtNumberEndow.visibility = GONE
            rcvEndow.visibility = VISIBLE
            txtStatusEndow.visibility = VISIBLE
            txtStatusEndow.text = context.getString(R.string.prm_cancel_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    /** Ưu đãi đã chọn không còn khả dụng → hiển thị danh sách mờ + nút "Chọn lại" */
    private fun showUnavailableState(details: List<AppliedDiscount>) {
        binding.apply {
            applyPromotionAdapter.submitList(details)
            txtNumberEndow.visibility = GONE
            rcvEndow.visibility = VISIBLE
            txtStatusEndow.visibility = VISIBLE
            txtStatusEndow.text = context.getString(R.string.prm_change_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    /** Không có ưu đãi nào → ẩn nút hành động */
    private fun showEmptyState() {
        binding.apply {
            txtNumberEndow.text = context.getString(R.string.prm_no_endow)
            txtNumberEndow.visibility = VISIBLE
            rcvEndow.visibility = GONE
            txtStatusEndow.visibility = INVISIBLE
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    private fun applyTokenInternal(token: DiscountBadgeToken?) {
        // KHÔNG đụng tới nền ở đây. Trước đây có `binding.root.background = null` chạy lại mỗi lần
        // đổi trạng thái — không có gì đặt nền cho `viewContainer` nên nó vô tác dụng, nhưng để lại
        // là cái bẫy: nền mặc định vừa đặt ở init sẽ bị xoá ngay lần render đầu nếu ai đó chuyển nó
        // sang `this` mà quên dòng này.
        token?.actionTextColor?.let { binding.txtStatusEndow.applyTextColorIfSet(it) }
        applyPromotionAdapter.applyToken(token)
    }
}