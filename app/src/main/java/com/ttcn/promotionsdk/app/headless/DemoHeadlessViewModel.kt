package com.ttcn.promotionsdk.app.headless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.domain.model.redemption.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.DiscountItemRequest
import com.ttcn.promotionsdk.core.domain.model.redemption.RedemptionItemRequest
import com.ttcn.promotionsdk.core.domain.model.voucher.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.stackablediscount.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.PromotionResult
import com.ttcn.promotionsdk.core.domain.model.voucher.VoucherItem
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DemoHeadlessViewModel : ViewModel() {

    // ─── Lấy use cases từ SDK — đây là tất cả những gì partner cần ───────────
    private val useCases = PromotionSDK.useCases

    // ─── State ────────────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _log = MutableSharedFlow<String>()
    val log = _log.asSharedFlow()

    // Giữ voucherId lấy từ search để các bước sau dùng
    private var firstVoucherId: String? = null
    private var validatedItems: List<Pair<String, String>> = emptyList() // objectId, objectType

    // ─── Step 1: Search vouchers ──────────────────────────────────────────────
    fun searchVouchers() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = useCases.searchVouchers(
                SearchCustomerVouchersRequest(
                    customerId = CUSTOMER_ID,
                    serviceCode = SERVICE_CODE,
                    keyword = null,
                    tab = null,
                    page = 0,
                    size = 10,
                )
            )
            when (result) {
                is PromotionResult.Success -> {
                    val data = result.data
                    val all: List<VoucherItem> = data.content
                    firstVoucherId = all.firstOrNull()?.voucherId
                    emit("✅ searchVouchers")
                    emit("   vouchers: ${all.size} items (total=${data.totalElements})")
                    all.take(3).forEach { emit("   - [${it.voucherId}] ${it.title}") }
                    if (all.size > 3) emit("   ... +${all.size - 3} more")
                }
                is PromotionResult.Failure -> emit("❌ searchVouchers: ${result.errorCode}")
            }
            _isLoading.value = false
        }
    }

    // ─── Step 2: Get voucher detail ───────────────────────────────────────────
    fun getVoucherDetail() {
        val voucherId = firstVoucherId ?: run {
            viewModelScope.launch { emit("⚠️ Chưa có voucherId, hãy Search trước") }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = useCases.getVoucherDetail(voucherId = voucherId, customerId = CUSTOMER_ID)) {
                is PromotionResult.Success -> {
                    val detail = result.data
                    emit("✅ getVoucherDetail [${detail.voucherId}]")
                    emit("   title      : ${detail.title}")
                    emit("   merchant   : ${detail.merchantName}")
                    emit("   expires    : ${detail.expirationDate}")
                    emit("   status     : ${detail.status}")
                }
                is PromotionResult.Failure -> emit("❌ getVoucherDetail: ${result.errorCode}")
            }
            _isLoading.value = false
        }
    }

    // ─── Step 3: Validate discounts ───────────────────────────────────────────
    fun validateDiscounts() {
        val voucherId = firstVoucherId ?: run {
            viewModelScope.launch { emit("⚠️ Chưa có voucherId, hãy Search trước") }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = useCases.validateDiscounts(
                ValidateDiscountsRequest(
                    customerId = CUSTOMER_ID,
                    orderId = ORDER_ID,
                    orderValue = ORDER_VALUE,
                    items = listOf(DiscountItemRequest(objectId = voucherId)),
                )
            )
            when (result) {
                is PromotionResult.Success -> {
                    val data = result.data
                    validatedItems = data.validItems.map { it.objectId to it.objectType }
                    emit("✅ validateDiscounts")
                    emit("   overallValid       : ${data.overallValid}")
                    emit("   totalDiscountAmount: ${data.totalDiscountAmount}")
                    emit("   finalAmount        : ${data.finalAmount}")
                    data.items.forEach { item ->
                        emit("   [${item.objectId}] valid=${item.valid} discount=${item.calculatedDiscount}")
                    }
                }
                is PromotionResult.Failure -> emit("❌ validateDiscounts: ${result.errorCode}")
            }
            _isLoading.value = false
        }
    }

    // ─── Step 4: Create redemption ────────────────────────────────────────────
    fun createRedemption() {
        if (validatedItems.isEmpty()) {
            viewModelScope.launch { emit("⚠️ Chưa validate, hãy Validate trước") }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val result = useCases.createRedemption(
                CreateRedemptionRequest(
                    customerId = CUSTOMER_ID,
                    orderId = ORDER_ID,
                    orderValue = ORDER_VALUE,
                    items = validatedItems.map { (objectId, objectType) ->
                        RedemptionItemRequest(objectId = objectId, objectType = objectType)
                    },
                )
            )
            when (result) {
                is PromotionResult.Success -> {
                    val data = result.data
                    emit("✅ createRedemption")
                    emit("   sessionId   : ${data.sessionId}")
                    emit("   totalDiscount: ${data.totalDiscount}")
                    emit("   finalAmount : ${data.finalAmount}")
                    emit("   hasErrors   : ${data.hasErrors}")
                    data.validationErrors.forEach { err ->
                        emit("   ⚠️ ${err.code}: ${err.message}")
                    }
                }
                is PromotionResult.Failure -> emit("❌ createRedemption: ${result.errorCode}")
            }
            _isLoading.value = false
        }
    }

    private suspend fun emit(msg: String) = _log.emit(msg)

    companion object {
        private const val CUSTOMER_ID = "CUS-001"
        private const val SERVICE_CODE = "vay"
        private const val ORDER_ID = "ORD-001"
        private const val ORDER_VALUE = "500000"
    }
}
