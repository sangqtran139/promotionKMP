package com.ttcn.promotionsdk.ui.utils.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.ttcn.promotionsdk.R
import com.ttcn.promotionsdk.core.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.core.data.remote.PromotionApiException
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.databinding.PrmViewEndowBinding
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ApplyPromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.EndowViewState
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.FakeVoucherData
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeToken
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

    // ─── Injected dependencies ────────────────────────────────────────────────

    private val repository: PromotionRepository by inject()
    private val requestContextProvider: PromotionRequestContextProvider by inject()

    // ─── Internal state ───────────────────────────────────────────────────────

    private var currentState: EndowViewState = EndowViewState.NOT_APPLIED
    private var lastAppliedToken: DiscountBadgeToken? = null
    private var hasLoadedInitial = false

    var myVouchers: List<MyVoucherListItem> = emptyList()
        private set
    var otherVouchers: List<MyVoucherListItem> = emptyList()
        private set
    var appliedVouchers: List<MyVoucherListItem> = emptyList()
        private set

    // ─── Coroutine scope ──────────────────────────────────────────────────────

    private var viewScope: CoroutineScope? = null

    // ─── Public callbacks ─────────────────────────────────────────────────────

    var onOpenVoucherSelection: (() -> Unit)? = null
    var onVoucherItemClick: ((MyVoucherListItem) -> Unit)? = null
    var onError: ((errorCode: String) -> Unit)? = null

    /**
     * Fired once after the initial load when ≥1 voucher has isAutoApplied = true.
     * The host can use this to sync its own state (e.g. update order total).
     * Not fired when the host calls [setAppliedVouchers] manually.
     */
    var onAutoApplied: ((autoApplied: List<MyVoucherListItem>) -> Unit)? = null

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

        val lifecycleOwner = findViewTreeLifecycleOwner()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        viewScope = scope

        lifecycleOwner?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
                viewScope = null
            }
        })

        if (!hasLoadedInitial) {
            loadInitialVouchers()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.cancel()
        viewScope = null
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Push the user's manual selection back into the view.
     * Clears any previous auto-apply state.
     */
    fun setAppliedVouchers(selected: List<MyVoucherListItem>) {
        appliedVouchers = selected
        if (selected.isEmpty()) {
            currentState = EndowViewState.NOT_APPLIED
            val total = myVouchers.size + otherVouchers.size
            showNotAppliedState(total)
        } else {
            currentState = EndowViewState.APPLIED
            showAppliedState(selected)
        }
    }

    fun applyToken(token: DiscountBadgeToken?) {
        lastAppliedToken = token
        applyTokenInternal(token)
    }

    fun setTitle(title: String) {
        binding.txtTitleEndow.text = title
    }

    fun getCurrentState(): EndowViewState = currentState

    // ─── Private: load vouchers ───────────────────────────────────────────────

    private fun loadInitialVouchers() {
        val scope = viewScope ?: return
        scope.launch {
            // ── Fake data ─────────────────────────────────────────────────────
            val myList = FakeVoucherData.getMyVouchers(page = 0)
            val otherList = FakeVoucherData.getOtherVouchers(page = 0)
            val total = FakeVoucherData.getTotalMyVoucherCount() +
                    FakeVoucherData.getTotalOtherVoucherCount()

            myVouchers = myList
            otherVouchers = otherList
            hasLoadedInitial = true

            // ── Auto-apply logic ──────────────────────────────────────────────
            // Only auto-apply if the host has not already pushed a manual selection
            if (appliedVouchers.isEmpty()) {
                val autoApplied = (myList + otherList).firstOrNull { it.isAutoApplied }
                    ?.let { listOf(it) }
                    ?: emptyList()
                if (autoApplied.isNotEmpty()) {
                    appliedVouchers = autoApplied
                    currentState = EndowViewState.APPLIED
                    showAppliedState(autoApplied)
                    onAutoApplied?.invoke(autoApplied)
                } else {
                    currentState = if (total > 0) EndowViewState.NOT_APPLIED else EndowViewState.EMPTY
                    showNotAppliedState(total)
                }
            }
            // If appliedVouchers was already set before load finished, keep current APPLIED state

            // ── API thật (uncomment khi sẵn sàng) ────────────────────────────
            // val customerId = requestContextProvider.getCustomerId()
            // if (customerId.isNullOrBlank()) {
            //     onError?.invoke("missing_customer_id")
            //     return@launch
            // }
            // runCatching {
            //     repository.searchCustomerVouchers(
            //         customerId = customerId,
            //         keyword = null,
            //         serviceCode = "vay",
            //         sectionCode = null,
            //         tab = null,
            //         myVouchersPage = 0,
            //         myVouchersSize = 10,
            //         otherVouchersPage = 0,
            //         otherVouchersSize = 10,
            //     )
            // }.onSuccess { response ->
            //     myVouchers = response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
            //     otherVouchers = response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
            //     val total = (response?.myVouchers?.totalElements ?: myVouchers.size) +
            //                 (response?.otherVouchers?.totalElements ?: otherVouchers.size)
            //     hasLoadedInitial = true
            //     if (appliedVouchers.isEmpty()) {
            //         val autoApplied = (myVouchers + otherVouchers).firstOrNull { it.isAutoApplied }
            //             ?.let { listOf(it) }
            //             ?: emptyList()
            //         if (autoApplied.isNotEmpty()) {
            //             appliedVouchers = autoApplied
            //             currentState = EndowViewState.APPLIED
            //             showAppliedState(autoApplied)
            //             onAutoApplied?.invoke(autoApplied)
            //         } else {
            //             currentState = if (total > 0) EndowViewState.NOT_APPLIED else EndowViewState.EMPTY
            //             showNotAppliedState(total)
            //         }
            //     }
            // }.onFailure { throwable ->
            //     hasLoadedInitial = true
            //     onError?.invoke(throwable.toErrorCode())
            // }
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
                EndowViewState.NOT_APPLIED,
                EndowViewState.APPLIED -> onOpenVoucherSelection?.invoke()
                EndowViewState.EMPTY -> Unit
            }
        }
    }

    private fun showNotAppliedState(count: Int) {
        binding.apply {
            txtNumberEndow.text = when (count) {
                0 -> context.getString(R.string.prm_no_endow)
                1 -> context.getString(R.string.prm_one_endow)
                else -> context.getString(R.string.prm_multiple_endow, count)
            }
            txtNumberEndow.visibility = VISIBLE
            rcvEndow.visibility = GONE
            txtStatusEndow.visibility = if (count > 0) VISIBLE else GONE
            txtStatusEndow.text = context.getString(R.string.prm_use_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    private fun showAppliedState(appliedVouchers: List<MyVoucherListItem>) {
        binding.apply {
            applyPromotionAdapter.submitList(appliedVouchers)
            txtNumberEndow.visibility = GONE
            rcvEndow.visibility = VISIBLE
            txtStatusEndow.visibility = VISIBLE
            txtStatusEndow.text = context.getString(R.string.prm_change_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    private fun applyTokenInternal(token: DiscountBadgeToken?) {
        binding.root.background = null
        token?.actionTextColor?.let { binding.txtStatusEndow.applyTextColorIfSet(it) }
        applyPromotionAdapter.applyToken(token)
    }

    private fun Throwable.toErrorCode(): String =
        (this as? PromotionApiException)?.errorCode ?: message ?: "error_general"
}