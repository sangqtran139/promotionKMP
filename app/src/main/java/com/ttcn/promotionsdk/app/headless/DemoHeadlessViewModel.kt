package com.ttcn.promotionsdk.app.headless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.core.domain.model.CreateRedemptionRequest
import com.ttcn.promotionsdk.core.domain.model.DiscountItemRequest
import com.ttcn.promotionsdk.core.domain.model.RedemptionItemRequest
import com.ttcn.promotionsdk.core.domain.model.SearchCustomerVouchersRequest
import com.ttcn.promotionsdk.core.domain.model.ValidateDiscountsRequest
import com.ttcn.promotionsdk.core.domain.model.VoucherItem
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
            runCatching {
                useCases.searchVouchers(
                    SearchCustomerVouchersRequest(
                        customerId = CUSTOMER_ID,
                        serviceCode = SERVICE_CODE,
                        keyword = null,
                        tab = null,
                        sectionCode = null,
                        myVouchersPage = 0,
                        myVouchersSize = 10,
                        otherVouchersPage = 0,
                        otherVouchersSize = 10,
                    )
                )
            }.onSuccess { result ->
                val all: List<VoucherItem> = (result?.myVouchers?.content.orEmpty()) +
                        (result?.otherVouchers?.content.orEmpty())
                firstVoucherId = all.firstOrNull()?.voucherId
                emit("✅ searchVouchers")
                emit("   myVouchers  : ${result?.myVouchers?.content?.size ?: 0} items")
                emit("   otherVouchers: ${result?.otherVouchers?.content?.size ?: 0} items")
                all.take(3).forEach { emit("   - [${it.voucherId}] ${it.title}") }
                if (all.size > 3) emit("   ... +${all.size - 3} more")
            }.onFailure { e ->
                emit("❌ searchVouchers: ${e.message}")
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
            runCatching {
                useCases.getVoucherDetail(
                    voucherId = voucherId,
                    customerId = CUSTOMER_ID,
                )
            }.onSuccess { detail ->
                emit("✅ getVoucherDetail [${detail?.voucherId}]")
                emit("   title      : ${detail?.title}")
                emit("   merchant   : ${detail?.merchantName}")
                emit("   expires    : ${detail?.expirationDate}")
                emit("   status     : ${detail?.status}")
            }.onFailure { e ->
                emit("❌ getVoucherDetail: ${e.message}")
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
            runCatching {
                useCases.validateDiscounts(
                    ValidateDiscountsRequest(
                        customerId = CUSTOMER_ID,
                        orderId = ORDER_ID,
                        orderValue = ORDER_VALUE,
                        items = listOf(DiscountItemRequest(objectId = voucherId)),
                    )
                )
            }.onSuccess { result ->
                validatedItems = result?.validItems
                    ?.map { it.objectId to it.objectType }
                    .orEmpty()
                emit("✅ validateDiscounts")
                emit("   overallValid       : ${result?.overallValid}")
                emit("   totalDiscountAmount: ${result?.totalDiscountAmount}")
                emit("   finalAmount        : ${result?.finalAmount}")
                result?.items?.forEach { item ->
                    emit("   [${item.objectId}] valid=${item.valid} discount=${item.calculatedDiscount}")
                }
            }.onFailure { e ->
                emit("❌ validateDiscounts: ${e.message}")
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
            runCatching {
                useCases.createRedemption(
                    CreateRedemptionRequest(
                        customerId = CUSTOMER_ID,
                        orderId = ORDER_ID,
                        orderValue = ORDER_VALUE,
                        items = validatedItems.map { (objectId, objectType) ->
                            RedemptionItemRequest(objectId = objectId, objectType = objectType)
                        },
                    )
                )
            }.onSuccess { result ->
                emit("✅ createRedemption")
                emit("   sessionId   : ${result?.sessionId}")
                emit("   totalDiscount: ${result?.totalDiscount}")
                emit("   finalAmount : ${result?.finalAmount}")
                emit("   hasErrors   : ${result?.hasErrors}")
                result?.validationErrors?.forEach { err ->
                    emit("   ⚠️ ${err.code}: ${err.message}")
                }
            }.onFailure { e ->
                emit("❌ createRedemption: ${e.message}")
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
