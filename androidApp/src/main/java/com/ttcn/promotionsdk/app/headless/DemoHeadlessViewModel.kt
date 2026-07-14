package com.ttcn.promotionsdk.app.headless

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttcn.promotionsdk.ui.entry.PromotionSDK
import com.ttcn.promotionsdk.ui.entry.api.PromotionApiResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Chế độ headless: đối tác tự dựng UI, chỉ gọi [PromotionSDK.api].
 *
 * Không một import nào từ `promotionLogic` — đó là mục đích của lớp wrapper `PromotionSDKApi`:
 * host chỉ tích hợp AndroidPromotionUI, lõi nằm ngoài compile classpath.
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
            emit("   customerId  : ${s?.customerId ?: "(null)"}")
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
            when (val result = api.getVouchers(page = 0, size = 10)) {
                is PromotionApiResult.Success -> {
                    val vouchers = result.data.vouchers
                    firstVoucherId = vouchers.firstOrNull()?.id
                    emit("✅ getVouchers")
                    emit("   vouchers: ${vouchers.size} items (lastPage=${result.data.isLastPage})")
                    vouchers.take(3).forEach { emit("   - [${it.id}] ${it.title}") }
                    if (vouchers.size > 3) emit("   ... +${vouchers.size - 3} more")
                }

                is PromotionApiResult.Failure -> emit("❌ getVouchers: ${result.error.message}")
            }
        }
    }

    // ─── Step 2: Get voucher detail ───────────────────────────────────────────
    fun getVoucherDetail() {
        val voucherId = requireVoucherId() ?: return
        launchStep {
            when (val result = api.getVoucherDetail(voucherId)) {
                is PromotionApiResult.Success -> {
                    val detail = result.data
                    emit("✅ getVoucherDetail [${detail.id}]")
                    emit("   title      : ${detail.title}")
                    emit("   merchant   : ${detail.merchantName}")
                    emit("   expires    : ${detail.expireDate}")
                    emit("   status     : ${detail.status}")
                }

                is PromotionApiResult.Failure -> emit("❌ getVoucherDetail: ${result.error.message}")
            }
        }
    }

    // ─── Step 3: Validate discounts ───────────────────────────────────────────
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

                is PromotionApiResult.Failure -> emit("❌ validateDiscounts: ${result.error.message}")
            }
        }
    }

    // ─── Step 4: Create redemption ────────────────────────────────────────────
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

                is PromotionApiResult.Failure -> emit("❌ createRedemption: ${result.error.message}")
            }
        }
    }

    private fun requireVoucherId(): String? = firstVoucherId ?: run {
        viewModelScope.launch { emit("⚠️ Chưa có voucherId, hãy Search trước") }
        null
    }

    private fun launchStep(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            block()
            _isLoading.value = false
        }
    }

    private suspend fun emit(msg: String) = _log.emit(msg)
}
