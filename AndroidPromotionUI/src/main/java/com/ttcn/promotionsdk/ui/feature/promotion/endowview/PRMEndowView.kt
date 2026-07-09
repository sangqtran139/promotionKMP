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
import com.ttcn.promotionsdk.databinding.PrmViewEndowBinding
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ApplyPromotionAdapter
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

    // ─── Public read-only accessors (delegate to ViewModel state) ─────────────

    val myVouchers: List<MyVoucherListItem>
        get() = viewModel?.uiState?.value?.myVouchers ?: emptyList()

    val otherVouchers: List<MyVoucherListItem>
        get() = viewModel?.uiState?.value?.otherVouchers ?: emptyList()

    val discountDetails: List<AppliedDiscount>
        get() = viewModel?.uiState?.value?.discountDetails ?: emptyList()

    // ─── Public callbacks ─────────────────────────────────────────────────────

    var onOpenVoucherSelection: (() -> Unit)? = null
    var onVoucherItemClick: ((MyVoucherListItem) -> Unit)? = null
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

        vm.loadInitialVouchers()
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
        viewModel?.applyDiscountDetails(details, unavailable = hasInvalid)
    }

    /** Đánh dấu ưu đãi hiện tại không còn khả dụng mà không thay đổi danh sách. */
    fun markAppliedVoucherUnavailable() {
        viewModel?.markDiscountUnavailable()
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
            viewModel?.clearError()
        }

        if (!state.hasLoadedInitial) return

        binding.shimmerEndow.stopShimmer()
        binding.shimmerEndow.visibility = GONE

        when {
            // Ưu đãi đã áp dụng nhưng không còn khả dụng → UNAVAILABLE
            state.discountUnavailable && state.discountDetails.isNotEmpty() -> {
                currentState = EndowViewState.UNAVAILABLE
                showUnavailableState(state.discountDetails)
            }

            // Đã áp dụng ưu đãi → APPLIED
            state.discountDetails.isNotEmpty() -> {
                currentState = EndowViewState.APPLIED
                showAppliedState(state.discountDetails)
            }

            // Có ưu đãi nhưng chưa chọn → NOT_APPLIED
            state.totalVoucherCount > 0 -> {
                currentState = EndowViewState.NOT_APPLIED
                showNotAppliedState(state.totalVoucherCount)
            }

            // Không có ưu đãi nào → EMPTY (ẩn nút hành động)
            else -> {
                currentState = EndowViewState.EMPTY
                showEmptyState()
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
                EndowViewState.NOT_APPLIED -> onOpenVoucherSelection?.invoke()
                EndowViewState.APPLIED -> viewModel?.clearDiscountDetails()
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
            txtStatusEndow.visibility = GONE
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    private fun applyTokenInternal(token: DiscountBadgeToken?) {
        binding.root.background = null
        token?.actionTextColor?.let { binding.txtStatusEndow.applyTextColorIfSet(it) }
        applyPromotionAdapter.applyToken(token)
    }
}