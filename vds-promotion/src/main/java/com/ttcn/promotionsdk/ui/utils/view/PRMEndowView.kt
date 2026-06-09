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
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.DiscountDetail
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.DiscountRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableCustomerInfo
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableDiscountsRequest
import com.ttcn.promotionsdk.core.data.dto.stackablediscount.StackableOrderInfo
import com.ttcn.promotionsdk.core.data.remote.PromotionApiException
import com.ttcn.promotionsdk.core.di.inject
import com.ttcn.promotionsdk.core.domain.repository.PromotionRepository
import com.ttcn.promotionsdk.databinding.PrmViewEndowBinding
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.ApplyPromotionAdapter
import com.ttcn.promotionsdk.ui.feature.promotion.choosepromotion.adapter.EndowViewState
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.MyVoucherListItem
import com.ttcn.promotionsdk.ui.feature.promotion.mypromotion.toMyVoucherListItem
import com.ttcn.promotionsdk.ui.theme.DiscountBadgeToken
import com.ttcn.promotionsdk.ui.theme.PromotionThemeRegistry
import com.ttcn.promotionsdk.ui.utils.applyTextColorIfSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID

class PRMEndowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // ─── Binding & Adapter ────────────────────────────────────────────────────

    private val binding: PrmViewEndowBinding =
        PrmViewEndowBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * [rcvEndow] luôn render từ [DiscountDetail] — kết quả từ
     * validateStackableDiscounts, KHÔNG render từ MyVoucherListItem trực tiếp.
     */
    private val applyPromotionAdapter = ApplyPromotionAdapter()

    // ─── Injected dependencies ────────────────────────────────────────────────

    private val repository: PromotionRepository by inject()
    private val requestContextProvider: PromotionRequestContextProvider by inject()

    // ─── Internal state ───────────────────────────────────────────────────────

    private var currentState: EndowViewState = EndowViewState.NOT_APPLIED
    private var lastAppliedToken: DiscountBadgeToken? = null
    private var hasLoadedInitial = false

    /** Raw data từ getCustomerVouchers — dùng để pass sang ChoosePromotionFragment */
    var myVouchers: List<MyVoucherListItem> = emptyList()
        private set
    var otherVouchers: List<MyVoucherListItem> = emptyList()
        private set

    /**
     * Kết quả hiện tại từ validateStackableDiscounts.
     * [rcvEndow] render từ list này.
     */
    var discountDetails: List<DiscountDetail> = emptyList()
        private set

    // ─── Coroutine scope ──────────────────────────────────────────────────────

    private var viewScope: CoroutineScope? = null

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

        findViewTreeLifecycleOwner()?.lifecycle?.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                scope.cancel()
                viewScope = null
            }
        })

        if (!hasLoadedInitial) loadInitialVouchers()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.cancel()
        viewScope = null
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Nhận kết quả validateStackableDiscounts từ host (sau khi user apply ở
     * ChoosePromotionFragment hoặc sau auto-apply lúc load initial).
     *
     * [rcvEndow] luôn render từ list này.
     *
     * - [DiscountDetail.valid] = true  → hiển thị bình thường
     * - [DiscountDetail.valid] = false → hiển thị mờ/disabled
     */
    fun setDiscountDetails(details: List<DiscountDetail>) {
        discountDetails = details
        if (details.isEmpty()) {
            currentState = EndowViewState.NOT_APPLIED
            val total = myVouchers.size + otherVouchers.size
            showNotAppliedState(total)
        } else {
            currentState = EndowViewState.APPLIED
            showAppliedState(details)
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

    /**
     * Load lần đầu khi view attach.
     *
     * Sau khi load xong, nếu có voucher [isAutoApplied]=true thì tự gọi
     * validateStackableDiscounts nội bộ — KHÔNG cần host tham gia.
     * Kết quả validate → [setDiscountDetails] để [rcvEndow] render đúng.
     */
    private fun loadInitialVouchers() {
        val scope = viewScope ?: return
        scope.launch {
            val customerId = requestContextProvider.getCustomerId()
            if (customerId.isNullOrBlank()) {
                onError?.invoke("missing_customer_id")
                return@launch
            }

            runCatching {
                repository.searchCustomerVouchers(
                    customerId = customerId,
                    keyword = null,
                    serviceCode = "vay",
                    sectionCode = null,
                    tab = null,
                    myVouchersPage = 0,
                    myVouchersSize = 10,
                    otherVouchersPage = 0,
                    otherVouchersSize = 10,
                )
            }.onSuccess { response ->
                myVouchers = response?.myVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                otherVouchers = response?.otherVouchers?.content.orEmpty().map { it.toMyVoucherListItem() }
                val total = (response?.myVouchers?.totalElements ?: myVouchers.size).toInt() +
                        (response?.otherVouchers?.totalElements ?: otherVouchers.size).toInt()
                hasLoadedInitial = true

                if (discountDetails.isEmpty()) {
                    val autoApplied = (myVouchers + otherVouchers).firstOrNull { it.isAutoApplied }
                    if (autoApplied != null) {
                        validateAndAutoApply(customerId, listOf(autoApplied))
                    } else {
                        currentState = if (total > 0) EndowViewState.NOT_APPLIED else EndowViewState.EMPTY
                        showNotAppliedState(total)
                    }
                }
            }.onFailure { throwable ->
                hasLoadedInitial = true
                onError?.invoke(throwable.toErrorCode())
            }
        }
    }

    /**
     * Gọi validateStackableDiscounts nội bộ cho auto-apply lúc load initial.
     * Kết quả được push thẳng vào [setDiscountDetails].
     */
    private fun validateAndAutoApply(
        customerId: String,
        vouchers: List<MyVoucherListItem>,
    ) {
        val scope = viewScope ?: return
        scope.launch {
            val request = StackableDiscountsRequest(
                idempotencyKey = UUID.randomUUID().toString(),
                customerInfo = StackableCustomerInfo(customerId = customerId),
                orderInfo = StackableOrderInfo(
                    orderId = requestContextProvider.getOrderId().orEmpty(),
                    orderValue = requestContextProvider.getOrderValue().orEmpty(),
                ),
                discountRequests = vouchers.mapIndexed { index, voucher ->
                    DiscountRequest(
                        objectType = "CAMPAIGN",
                        objectId = voucher.voucherId,
                        priority = index + 1,
                    )
                },
            )

            runCatching { repository.validateStackableDiscounts(request) }
                .onSuccess { response ->
                    // ✅ Dùng thẳng discountDetails từ response, không map lại
                    setDiscountDetails(response?.discountDetails.orEmpty())
                }
                .onFailure { throwable ->
                    val total = myVouchers.size + otherVouchers.size
                    currentState = EndowViewState.NOT_APPLIED
                    showNotAppliedState(total)
                    onError?.invoke(throwable.toErrorCode())
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
                EndowViewState.NOT_APPLIED,
                EndowViewState.APPLIED -> onOpenVoucherSelection?.invoke()
                EndowViewState.EMPTY   -> Unit
            }
        }
    }

    private fun showNotAppliedState(count: Int) {
        binding.apply {
            txtNumberEndow.text = when (count) {
                0    -> context.getString(R.string.prm_no_endow)
                1    -> context.getString(R.string.prm_one_endow)
                else -> context.getString(R.string.prm_multiple_endow, count)
            }
            txtNumberEndow.visibility = VISIBLE
            rcvEndow.visibility = GONE
            txtStatusEndow.visibility = if (count > 0) VISIBLE else GONE
            txtStatusEndow.text = context.getString(R.string.prm_use_voucher)
        }
        applyTokenInternal(lastAppliedToken ?: PromotionThemeRegistry.discountBadgeToken())
    }

    private fun showAppliedState(details: List<DiscountDetail>) {
        binding.apply {
            applyPromotionAdapter.submitList(details)
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