package com.ttcn.promotionsdk.ui.feature.promotion.endowview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.ui.entry.AppliedDiscount
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.api.PromotionVoucher
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.ChoosePromotionFragment
import com.ttcn.promotionsdk.databinding.PrmViewEndowBinding
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ApplyPromotionAdapter
import androidx.core.view.isVisible
import com.ttcn.promotionsdk.core.domain.model.eligible.EligibleOffer
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.theme.token.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.utils.applyTextColorIfSet
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

    private val binding: PrmViewEndowBinding =
        PrmViewEndowBinding.inflate(LayoutInflater.from(context), this, true)

    private val applyPromotionAdapter = ApplyPromotionAdapter()

    // ─── ViewModel — khởi tạo lazy để scope sẵn sàng khi onAttachedToWindow ──

    private var viewModel: PRMEndowViewModel? = null

    // ─── Internal UI state ────────────────────────────────────────────────────

    private var currentState: EndowViewState = EndowViewState.NOT_APPLIED
    private var lastAppliedToken: DiscountBadgeToken? = null
    private var viewScope: CoroutineScope? = null

    // Theo dõi transition để phát callback host đúng một lần mỗi lần đổi (không spam theo mỗi render).
    private var lastNotifiedState: EndowViewState? = null
    private var lastNotifiedCount: Int = -1

    // ─── Read-only accessors (delegate to ViewModel state) ────────────────────

    /**
     * Ưu đãi đã load sẵn, dùng lại cho màn "Chọn ưu đãi" để khỏi gọi `findEligible` hai lần.
     * `internal`: [EligibleOffer] thuộc `promotionLogic`, không được lọt ra API public.
     * Host lấy qua [ChoosePromotionFragment.forEndowView].
     */
    internal val myVouchers: List<EligibleOffer>
        get() = viewModel?.uiState?.value?.myVouchers ?: emptyList()

    internal val otherVouchers: List<EligibleOffer>
        get() = viewModel?.uiState?.value?.otherVouchers ?: emptyList()

    val discountDetails: List<AppliedDiscount>
        get() = viewModel?.uiState?.value?.discountDetails ?: emptyList()

    // ─── Public callbacks ─────────────────────────────────────────────────────

    var onOpenVoucherSelection: (() -> Unit)? = null

    /** Voucher user bấm vào trong widget. Trả DTO public — [MyVoucherListItem] là model nội bộ. */
    var onVoucherItemClick: ((PromotionVoucher) -> Unit)? = null
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
        val vm = PRMEndowViewModel.create(scope)
        viewModel = vm

        findViewTreeLifecycleOwner()?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
                viewScope = null
                viewModel = null
            }
        })

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

    /** Cờ TẮT → ẩn widget và không gọi API. Cờ BẬT → hiện và nạp ưu đãi (chỉ nạp một lần). */
    private fun applyFeatureFlag(enabled: Boolean, vm: PRMEndowViewModel) {
        isVisible = enabled
        if (enabled) vm.loadInitial()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
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
            onSettled?.invoke(null)
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

    fun getCurrentState(): EndowViewState = currentState

    // ─── Private: render ──────────────────────────────────────────────────────

    private fun renderState(state: PRMEndowUiState) {
        // Handle error
        state.error?.let {
            onError?.invoke(it)
            viewModel?.consumeError()
        }

        if (!state.hasLoadedInitial) return

        binding.shimmerEndow.stopShimmer()
        binding.shimmerEndow.visibility = GONE

        // Trạng thái widget do store quyết định (EndowStore.widgetState) — View chỉ render.
        when (state.widgetState) {
            EndowViewState.UNAVAILABLE -> {
                currentState = EndowViewState.UNAVAILABLE
                showUnavailableState(state.discountDetails)
            }

            EndowViewState.APPLIED -> {
                currentState = EndowViewState.APPLIED
                showAppliedState(state.discountDetails)
            }

            EndowViewState.NOT_APPLIED -> {
                currentState = EndowViewState.NOT_APPLIED
                showNotAppliedState(state.totalVoucherCount)
            }

            EndowViewState.EMPTY -> {
                currentState = EndowViewState.EMPTY
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
        if (currentState == EndowViewState.APPLIED && lastNotifiedState != EndowViewState.APPLIED) {
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
                EndowViewState.NOT_APPLIED -> onOpenVoucherSelection?.invoke()
                EndowViewState.APPLIED -> {
                    viewModel?.clearApplied()
                    PromotionSDK.getCallback()?.onVoucherCleared()
                }
                EndowViewState.UNAVAILABLE -> onOpenVoucherSelection?.invoke()
                EndowViewState.EMPTY -> Unit
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