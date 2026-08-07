package com.ttcn.prm.ui.feature.endowview

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.prm.R
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.databinding.PrmViewEndowBinding
import com.ttcn.prm.ui.feature.choosepromotion.adapter.ApplyPromotionAdapter
import androidx.core.view.isVisible
import com.ttcn.promotionsdk.domain.exception.ErrorCodes
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.presentation.endow.EndowConfirmResult
import com.ttcn.promotionsdk.presentation.endow.EndowWidgetState
import com.ttcn.prm.ui.theme.token.DiscountBadgeToken
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.utils.applyTextColorIfSet
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

    // Theo dõi transition để phát callback host đúng một lần mỗi lần đổi (không spam theo mỗi render).
    private var lastNotifiedState: EndowWidgetState? = null
    private var lastNotifiedCount: Int = -1
    /** `null` = chưa báo lần nào, nên lần áp cờ đầu tiên luôn bắn callback. */
    private var lastNotifiedAvailability: Boolean? = null

    // ─── Read-only accessors (delegate to ViewModel state) ────────────────────

    /**
     * Ưu đãi đã load sẵn, dùng lại cho màn "Chọn ưu đãi" để khỏi gọi `findEligible` hai lần.
     * `internal`: [EligibleOffer] thuộc `promotionLogic`, không được lọt ra API public.
     * Host lấy qua [PromotionSDK.createChoosePromotionFragment].
     */
    internal val myVouchers: List<EligibleOffer>
        get() = viewModel?.uiState?.value?.myVouchers ?: emptyList()

    internal val otherVouchers: List<EligibleOffer>
        get() = viewModel?.uiState?.value?.otherVouchers ?: emptyList()

    /** Cờ phân trang đi kèm [myVouchers] / [otherVouchers] — màn "Chọn ưu đãi" cần để biết còn trang không. */
    internal val myIsLastPage: Boolean
        get() = viewModel?.uiState?.value?.myIsLastPage ?: true

    internal val otherIsLastPage: Boolean
        get() = viewModel?.uiState?.value?.otherIsLastPage ?: true

    val discountDetails: List<AppliedDiscount>
        get() = viewModel?.uiState?.value?.discountDetails ?: emptyList()

    // ─── Public callbacks ─────────────────────────────────────────────────────

    var onOpenVoucherSelection: (() -> Unit)? = null

    var onError: ((errorCode: String) -> Unit)? = null

    // ─── Init ─────────────────────────────────────────────────────────────────

    init {
        setupRecyclerView()
        setupClickListeners()
        applyToken(PromotionThemeRegistry.discountBadgeToken())
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        viewScope = scope

        // ViewModel tạo lúc scope đã sẵn sàng — scope gắn với View lifecycle
        val vm = EndowViewModel.create(scope)
        viewModel = vm

        // Lưới an toàn cho trường hợp view bị huỷ mà không qua onDetachedFromWindow. Phải GỠ ở
        // [onDetachedFromWindow]: observer là inner class nên nó giữ luôn `this`, mà lifecycle của
        // host thì sống lâu hơn view rất nhiều — attach/detach vài vòng (RecyclerView tái dụng,
        // fragment show/hide) là chồng một đống observer, mỗi cái ghim một view chết lại tới khi
        // host destroy.
        val owner = findViewTreeLifecycleOwner()
        val observer = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                releaseScope()
            }
        }
        lifecycleOwnerRef = owner
        lifecycleObserver = observer
        owner?.lifecycle?.addObserver(observer)

        // Observe state changes
        scope.launch {
            vm.uiState.collect { state ->
                renderState(state)
            }
        }

        binding.shimmerEndow.startShimmer()

        // Gác bởi cờ VOUCHER_SELECTION, y như `PromotionSDKImpl.applyFlag` bên iOS: áp cache hiện
        // có ngay lập tức (chưa có cache → bật lạc quan), rồi làm mới từ server và áp lại nếu đổi.
        applyFeatureFlag(PromotionFeatureGate.canShowVoucherSelection(), vm)
        scope.launch {
            PromotionFeatureGate.refresh()
            applyFeatureFlag(PromotionFeatureGate.canShowVoucherSelection(), vm)
        }
    }

    /**
     * Cờ TẮT → ẩn widget và không gọi API. Cờ BẬT → hiện và nạp ưu đãi (chỉ nạp một lần).
     *
     * Báo host mỗi lần đổi trạng thái, y như `PromotionSDKImpl.applyFlag` bên iOS: widget bị rút đi
     * là lúc host cần biết để thu gọn layout của mình. Chỉ bắn khi **đổi** để `applyFlag` gọi hai lần
     * (cache rồi server) không sinh callback trùng.
     */
    private fun applyFeatureFlag(enabled: Boolean, vm: EndowViewModel) {
        if (lastNotifiedAvailability != enabled) {
            lastNotifiedAvailability = enabled
            PromotionSDK.getCallback()?.onAvailabilityChanged(enabled)
        }
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

    private fun releaseScope() {
        viewScope?.cancel()
        viewScope = null
        viewModel = null
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
     * [onSettled] gọi một lần khi validate xong, kèm **mã lỗi** (`null` = thành công) — màn chọn dùng
     * để quyết định đóng hay báo lỗi. Đối ứng completion của `endowVM.validateAndApply` bên iOS.
     */
    internal fun applySelectedOffers(
        offers: List<EligibleOffer>,
        onSettled: ((errorCode: String?) -> Unit)? = null,
    ) {
        val vm = viewModel
        if (vm == null) {
            // Widget đã detach (host `replace` màn thay vì `add`) → không có store để validate.
            // Báo LỖI chứ không phải `null`: `null` nghĩa là "áp xong", màn chọn sẽ đóng và user
            // tưởng đã áp trong khi widget không hề đổi.
            onSettled?.invoke(ErrorCodes.GENERAL)
            return
        }
        vm.validateAndApply(offers) { state -> onSettled?.invoke(state.errorCode) }
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
     *         onError = { code -> showError(code) },
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
        onError: (errorCode: String) -> Unit = {},
    ) {
        val vm = viewModel
        val scope = viewScope
        if (vm == null || scope == null) {
            onError(ErrorCodes.GENERAL)
            return
        }
        scope.launch {
            when (val result = vm.confirmRedemption()) {
                is EndowConfirmResult.Success -> onSuccess()
                is EndowConfirmResult.Failure -> onError(result.errorCode)
            }
        }
    }

    // ─── Private: render ──────────────────────────────────────────────────────

    private fun renderState(state: PRMEndowUiState) {
        // Handle error
        state.error?.let {
            onError?.invoke(it)
            // KHÔNG xoá khi đang chờ kết quả validate — xem `EndowViewModel.consumeErrorUnlessSettling`.
            viewModel?.consumeErrorUnlessSettling()
        }

        if (!state.hasLoadedInitial) return

        binding.shimmerEndow.stopShimmer()
        binding.shimmerEndow.visibility = GONE

        // Trạng thái widget do store quyết định (EndowStore.widgetState) — View chỉ render.
        when (state.widgetState) {
            EndowWidgetState.UNAVAILABLE -> {
                currentState = EndowWidgetState.UNAVAILABLE
                showUnavailableState(state.discountDetails)
            }

            EndowWidgetState.APPLIED -> {
                currentState = EndowWidgetState.APPLIED
                showAppliedState(state.discountDetails)
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
     * Phát sự kiện cho host qua callback đã set lúc [PromotionSDK.initialize] — đối ứng iOS
     * (`onVoucherCountChanged` / `onVoucherApplied`). Chỉ phát khi thật sự đổi để không spam theo
     * mỗi lần render. `onVoucherCleared` phát trực tiếp ở click "Hủy" (xem [setupClickListeners]).
     */
    private fun notifyHost(state: PRMEndowUiState) {
        val callback = PromotionSDK.getCallback()
        if (state.totalVoucherCount != lastNotifiedCount) {
            lastNotifiedCount = state.totalVoucherCount
            callback?.onVoucherCountChanged(state.totalVoucherCount)
        }
        if (currentState == EndowWidgetState.APPLIED && lastNotifiedState != EndowWidgetState.APPLIED) {
            state.discountDetails.firstOrNull()?.objectId?.let { callback?.onVoucherApplied(it) }
        }
        lastNotifiedState = currentState
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
                EndowWidgetState.NOT_APPLIED -> onOpenVoucherSelection?.invoke()
                EndowWidgetState.APPLIED -> {
                    viewModel?.clearApplied()
                    PromotionSDK.getCallback()?.onVoucherCleared()
                }
                EndowWidgetState.UNAVAILABLE -> onOpenVoucherSelection?.invoke()
                EndowWidgetState.EMPTY -> Unit
            }
        }
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
        binding.root.background = null
        token?.actionTextColor?.let { binding.txtStatusEndow.applyTextColorIfSet(it) }
        applyPromotionAdapter.applyToken(token)
    }
}