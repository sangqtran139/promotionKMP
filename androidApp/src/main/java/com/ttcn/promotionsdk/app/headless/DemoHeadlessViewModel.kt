package com.ttcn.promotionsdk.app.headless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.api.PromotionApiResult
import com.ttcn.prm.entry.api.PromotionOrderItem
import com.ttcn.prm.entry.api.PromotionSDKError
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Chế độ headless: đối tác tự dựng UI, chỉ gọi [PromotionSDK.api].
 *
 * Không một import nào từ `promotionLogic` — đó là mục đích của lớp wrapper `PromotionSDKApi`:
 * host chỉ tích hợp AndroidPromotionSDK, lõi nằm ngoài compile classpath.
 *
 * **Soi gương `DemoHeadlessViewController.swift` bên iOS**: cùng 5 bước, cùng thứ tự, cùng chuỗi log.
 * Sửa một bên thì sửa cả hai.
 */
class DemoHeadlessViewModel : ViewModel() {

    private val api = PromotionSDK.api

    // ─── Đọc lại giá trị đã set qua PromotionSDK.updateContext() ─────────────
    private val orderId get() = PromotionSDK.currentOrderId.orEmpty()
    private val orderValue get() = PromotionSDK.currentOrderValue.orEmpty()

    // ─── State ────────────────────────────────────────────────────────────────
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _log = MutableSharedFlow<String>()
    val log = _log.asSharedFlow()

    // Giữ voucherId lấy từ search để các bước sau dùng
    private var firstVoucherId: String? = null
    private var validatedVoucherIds: List<String> = emptyList()

    init {
        viewModelScope.launch {
            val s = PromotionSDK.session
            emit("── SDK context ──────────────────────")
            emit("   language    : ${s?.language ?: "(null)"}")
            emit("   orderId     : ${PromotionSDK.currentOrderId ?: "(null)"}")
            emit("   orderValue  : ${PromotionSDK.currentOrderValue ?: "(null)"}")
            emit("   serviceCode : ${PromotionSDK.currentServiceCode ?: "(null)"}")
            emit("─────────────────────────────────────")
        }
    }

    // ─── Step 1: Search vouchers ──────────────────────────────────────────────
    fun searchVouchers() {
        launchStep {
            when (val result = api.getVouchers(page = 0, size = PAGE_SIZE)) {
                is PromotionApiResult.Success -> {
                    val vouchers = result.data.vouchers
                    firstVoucherId = vouchers.firstOrNull()?.id
                    emit("✅ getVouchers")
                    emit("   vouchers: ${vouchers.size} items (lastPage=${result.data.isLastPage})")
                    vouchers.take(MAX_LOG_ITEMS).forEach { emit("   - [${it.id}] ${it.title}") }
                    if (vouchers.size > MAX_LOG_ITEMS) emit("   ... +${vouchers.size - MAX_LOG_ITEMS} more")
                }

                is PromotionApiResult.Failure -> emit(formatError("getVouchers", result.error))
            }
        }
    }

    // ─── Step 2: Find eligible ────────────────────────────────────────────────
    fun findEligible() {
        launchStep {
            val result = api.findEligible(
                orderId = orderId,
                orderValue = orderValue,
                items = demoOrderItems(),
                myPage = 0,
                mySize = PAGE_SIZE,
                otherPage = 0,
                otherSize = PAGE_SIZE,
            )
            when (result) {
                is PromotionApiResult.Success -> {
                    val data = result.data
                    emit("✅ findEligible")
                    emit("   myOffers   : ${data.myOffers.size} items (lastPage=${data.myIsLastPage})")
                    data.myOffers.take(MAX_LOG_ITEMS).forEach { emit("   - [${it.id}] ${it.name} usable=${it.usable}") }
                    emit("   otherOffers: ${data.otherOffers.size} items (lastPage=${data.otherIsLastPage})")
                    data.otherOffers.take(MAX_LOG_ITEMS).forEach { emit("   - [${it.id}] ${it.name} usable=${it.usable}") }
                }

                is PromotionApiResult.Failure -> emit(formatError("findEligible", result.error))
            }
        }
    }

    // ─── Step 3: Get voucher detail ───────────────────────────────────────────
    fun getVoucherDetail() {
        val voucherId = requireVoucherId() ?: return
        launchStep {
            when (val result = api.getVoucherDetail(voucherId)) {
                is PromotionApiResult.Success -> {
                    val detail = result.data
                    emit("✅ getVoucherDetail [${detail.id}]")
                    emit("   title      : ${detail.title}")
                    emit("   merchant   : ${detail.merchantName}")
                    emit("   expires    : ${detail.expireDate ?: "(null)"}")
                    emit("   status     : ${detail.status}")
                }

                is PromotionApiResult.Failure -> emit(formatError("getVoucherDetail", result.error))
            }
        }
    }

    // ─── Step 4: Validate discounts ───────────────────────────────────────────
    fun validateDiscounts() {
        val voucherId = requireVoucherId() ?: return
        launchStep {
            val result = api.validateDiscounts(
                orderId = orderId,
                orderValue = orderValue,
                voucherIds = listOf(voucherId),
            )
            when (result) {
                is PromotionApiResult.Success -> {
                    val data = result.data
                    validatedVoucherIds = data.items.filter { it.isValid }.map { it.objectId }
                    emit("✅ validateDiscounts")
                    emit("   overallValid       : ${data.overallValid}")
                    emit("   totalDiscountAmount: ${data.totalDiscountAmount}")
                    emit("   finalAmount        : ${data.finalAmount}")
                    data.items.forEach { item ->
                        emit("   [${item.objectId}] valid=${item.isValid} discount=${item.discountAmount}")
                    }
                }

                is PromotionApiResult.Failure -> emit(formatError("validateDiscounts", result.error))
            }
        }
    }

    // ─── Step 5: Create redemption ────────────────────────────────────────────
    fun createRedemption() {
        if (validatedVoucherIds.isEmpty()) {
            viewModelScope.launch { emit("⚠️ Chưa validate, hãy Validate trước") }
            return
        }
        launchStep {
            val result = api.createRedemption(
                orderId = orderId,
                orderValue = orderValue,
                voucherIds = validatedVoucherIds,
            )
            when (result) {
                is PromotionApiResult.Success -> {
                    val data = result.data
                    emit("✅ createRedemption")
                    emit("   sessionId    : ${data.sessionId}")
                    emit("   totalDiscount: ${data.totalDiscount}")
                    emit("   finalAmount  : ${data.finalAmount}")
                    emit("   hasErrors    : ${data.validationErrors.isNotEmpty()}")
                    data.validationErrors.forEach { err ->
                        emit("   ⚠️ ${err.code}: ${err.message}")
                    }
                }

                is PromotionApiResult.Failure -> emit(formatError("createRedemption", result.error))
            }
        }
    }

    private fun requireVoucherId(): String? = firstVoucherId ?: run {
        viewModelScope.launch { emit("⚠️ Chưa có voucherId, hãy Search trước") }
        null
    }

    /**
     * Dòng đơn hàng giả lập — `findEligible` cần items để lấy campaign theo SKU (rỗng thì chỉ nhận
     * campaign cấp đơn). Context của SDK không đọc ngược ra được `orderItems`, nên demo tự dựng.
     */
    private fun demoOrderItems() = listOf(
        PromotionOrderItem(
            skuId = DEMO_SKU_ID,
            productId = DEMO_PRODUCT_ID,
            quantity = 1,
            unitPrice = orderValue,
        ),
    )

    private fun launchStep(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            block()
            _isLoading.value = false
        }
    }

    private suspend fun emit(msg: String) = _log.emit(msg)

    /** `❌ tên: [type] message (serverCode=…)` — cùng định dạng với bên iOS. */
    private fun formatError(name: String, error: PromotionSDKError): String {
        val serverCode = error.serverCode?.let { " (serverCode=$it)" }.orEmpty()
        return "❌ $name: [${errorType(error)}] ${error.message}$serverCode"
    }

    private fun errorType(error: PromotionSDKError): String = when (error) {
        is PromotionSDKError.NetworkFailure -> "networkFailure"
        PromotionSDKError.SessionExpired -> "sessionExpired"
        PromotionSDKError.Timeout -> "timeout"
        PromotionSDKError.ParseFailed -> "parseFailed"
        PromotionSDKError.FeatureDisabled -> "featureDisabled"
        is PromotionSDKError.Unknown -> "unknown"
    }

    private companion object {
        const val PAGE_SIZE = 10
        const val MAX_LOG_ITEMS = 3
        const val DEMO_SKU_ID = "SKU-01"
        const val DEMO_PRODUCT_ID = "P-01"
    }
}
