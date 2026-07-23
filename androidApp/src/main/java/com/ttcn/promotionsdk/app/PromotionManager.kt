package com.ttcn.promotionsdk.app

//  MẪU THAM KHẢO — Wrapper / Anti-Corruption Layer bọc PromotionSDK SDK.
//
//  Đối xứng 1-1 với `PromotionManager.swift` bên iOS (xem docs/InitParity.md §6). Ý tưởng: TOÀN BỘ
//  phần app chỉ nói chuyện với `PromotionManager` (qua interface `PromotionServing`), KHÔNG gọi SDK
//  rải rác. Khi upgrade/đổi SDK, chỉ sửa đúng file này.
//
//  Gom vào 1 chỗ các ràng buộc dễ sai của SDK:
//   - SDK là singleton tĩnh (PromotionSDK.initialize / updateContext / release) — như iOS.
//   - Token bị "chụp" lúc init → refresh token = init lại với session mới.
//   - Order/dịch vụ cập nhật qua updateContext (không re-init).
//   - `callback` gói trong options → adapter riêng map type SDK sang model APP rồi phát ra.
//
//  Khác iOS ở vài điểm N1 (docs/InitParity.md): Android cần `Context` khi init; headless là `suspend`
//  (iOS dùng completion); widget checkout dùng `PRMEndowView` trực tiếp (không phơi ở wrapper).

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.ttcn.prm.entry.PromotionAvailableService
import com.ttcn.prm.entry.PromotionEnvironment
import com.ttcn.prm.entry.PromotionSDK
import com.ttcn.prm.entry.PromotionSDKCallback
import com.ttcn.prm.entry.PromotionSDKOptions
import com.ttcn.prm.entry.PromotionServiceSelection
import com.ttcn.prm.entry.PromotionSessionConfig
import com.ttcn.prm.ui.theme.PromotionSDKTheme
import com.ttcn.prm.entry.api.PromotionApiResult
import com.ttcn.prm.entry.api.PromotionEligibleOffer
import com.ttcn.prm.entry.api.PromotionVoucher
import com.ttcn.prm.entry.api.PromotionVoucherDetail

// ─── Model của APP (không dùng type SDK ở tầng app → anti-corruption) ─────────

/** Thông tin đơn hàng khi mở widget thanh toán. `value`: chuỗi số nguyên VNĐ, vd "500000". */
data class OrderContext(val id: String, val value: String)

/** Dịch vụ user chọn trong bottom sheet "Chọn dịch vụ". */
data class ServiceSelection(val voucherId: String, val code: String, val name: String, val iconUrl: String)

/** Dịch vụ khả dụng host cấu hình (dùng cho bottom sheet "Chọn dịch vụ"). */
data class AvailableService(
    val code: String,
    val name: String,
    val type: String = "",
    val iconUrl: String = "",
)

/** Voucher rút gọn (kết quả headless fetchVouchers). */
data class VoucherSummary(
    val id: String,
    val merchantName: String,
    val title: String,
    val imageURL: String?,
    val expireDate: String?,
    val isUsed: Boolean,
    val statusLabel: String?,
)

/** Danh sách voucher "của tôi" (Search Customer Vouchers chỉ trả voucher đã sở hữu). */
data class VoucherPage(val mine: List<VoucherSummary>, val mineIsLastPage: Boolean)

/** Kết quả validate voucher theo đơn. */
data class ValidationSummary(val isValid: Boolean, val totalDiscount: String, val finalAmount: String)

/** Ưu đãi đủ điều kiện áp cho đơn (kết quả headless findEligibleOffers). */
data class EligibleOffer(
    val id: String,
    val name: String,
    val objectType: String,
    val usable: Boolean,
    val estimatedDiscount: String?,
    val expireDate: String?,
    val ineligibleReason: String?,
)

/** Hai nhóm ưu đãi đủ điều kiện: "của tôi" (voucher đã sở hữu) và "khác" (campaign công khai). */
data class EligibleOffers(
    val mine: List<EligibleOffer>,
    val others: List<EligibleOffer>,
    val mineIsLastPage: Boolean,
    val othersIsLastPage: Boolean,
)

/** Chi tiết một voucher (kết quả headless fetchVoucherDetail). */
data class VoucherDetail(
    val id: String,
    val merchantName: String,
    val title: String,
    val description: String,
    val guideline: String,
    val startDate: String?,
    val expireDate: String?,
    val bannerURL: String?,
    val logoURL: String?,
    val statusLabel: String?,
)

// ─── Cổng app-facing (app phụ thuộc interface này, dễ mock/test, dễ thay SDK) ──

interface PromotionServing {
    /** Gọi sau khi login thành công. `availableServices` do host cung cấp (cho bottom sheet "Chọn dịch vụ"). */
    fun start(context: Context, customerId: String, token: String?, availableServices: List<AvailableService>)
    /** Gọi khi access token được refresh — manager tự init lại với token mới. */
    fun updateToken(token: String?)
    /** Cập nhật context đơn hàng / dịch vụ mỗi khi vào màn có voucher (không re-init). */
    fun updateContext(orderId: String?, orderValue: String?, serviceCode: String?, metaData: String?)
    /** Gọi khi logout. */
    fun stop()

    /** Mở màn "Ưu đãi của tôi". */
    fun openMyPromotions(activity: FragmentActivity, containerViewId: Int?)
    /** Mở thẳng màn chi tiết một ưu đãi, không qua danh sách (host đã biết `voucherId`). */
    fun openPromotionDetail(voucherId: String, activity: FragmentActivity, containerViewId: Int?)
    // Widget checkout: Android dùng `PRMEndowView` trực tiếp trong layout (+ PromotionIntegrateManager),
    // không phơi ở wrapper — điểm lệch N1 với iOS `makeCheckoutWidget` (docs/InitParity.md §5.3).

    // --- Headless API (không UI) — `suspend` thay cho completion bên iOS (N1) ---
    suspend fun fetchVouchers(keyword: String?, serviceCode: String?, tab: String?, page: Int): Result<VoucherPage>
    /** Lấy ưu đãi đủ điều kiện cho đơn (voucher đã sở hữu + campaign công khai). */
    suspend fun findEligibleOffers(order: OrderContext, tab: String?, myPage: Int, otherPage: Int): Result<EligibleOffers>
    /** Lấy chi tiết một voucher theo id (dùng khi host tự dựng màn chi tiết). */
    suspend fun fetchVoucherDetail(voucherId: String, serviceCode: String?): Result<VoucherDetail>
    suspend fun validate(order: OrderContext, voucherIds: List<String>): Result<ValidationSummary>
    suspend fun createRedemption(order: OrderContext, voucherId: String): Result<String>

    // Sự kiện (fan-out từ callback 1-1 của SDK ra nhiều listener của app).
    var onVoucherApplied: ((voucherId: String) -> Unit)?
    var onVoucherCleared: (() -> Unit)?
    var onVoucherCountChanged: ((count: Int) -> Unit)?
    var onServiceSelected: ((ServiceSelection) -> Unit)?
    var onAvailabilityChanged: ((enabled: Boolean) -> Unit)?
    var onClosed: (() -> Unit)?
}

// ─── Manager: chỗ DUY NHẤT chạm PromotionSDK SDK ──────────────────────────────

object PromotionManager : PromotionServing {

    // Sự kiện
    override var onVoucherApplied: ((String) -> Unit)? = null
    override var onVoucherCleared: (() -> Unit)? = null
    override var onVoucherCountChanged: ((Int) -> Unit)? = null
    override var onServiceSelected: ((ServiceSelection) -> Unit)? = null
    override var onAvailabilityChanged: ((Boolean) -> Unit)? = null
    override var onClosed: (() -> Unit)? = null

    private var appContext: Context? = null
    private var customerId: String? = null
    private var availableServices: List<AvailableService> = emptyList()

    /** SDK đã sẵn sàng chưa (đã init, chưa release). Công cụ demo dùng để gác. */
    val isReady: Boolean get() = PromotionSDK.isInitialized()

    // Theme cấu hình tập trung 1 chỗ (null = mặc định SDK).
    private val theme: PromotionSDKTheme? = null
    // Base URL Promotion BFF — host cấu hình. Đối ứng `PromotionSessionConfig.baseUrl` bên iOS.
    private const val BASE_URL = "http://125.235.38.229:8080"

    /** Adapter riêng conform `PromotionSDKCallback` — map type SDK → model app rồi phát ra ngoài. */
    private val sdkCallback = object : PromotionSDKCallback {
        override fun onVoucherApplied(voucherId: String) { this@PromotionManager.onVoucherApplied?.invoke(voucherId) }
        override fun onVoucherCleared() { this@PromotionManager.onVoucherCleared?.invoke() }
        override fun onVoucherCountChanged(count: Int) { this@PromotionManager.onVoucherCountChanged?.invoke(count) }
        override fun onServiceSelected(selection: PromotionServiceSelection) {
            this@PromotionManager.onServiceSelected?.invoke(
                ServiceSelection(selection.voucherId, selection.serviceCode, selection.serviceName, selection.iconUrl)
            )
        }
        override fun onAvailabilityChanged(enabled: Boolean) { this@PromotionManager.onAvailabilityChanged?.invoke(enabled) }
        override fun onClosed() { this@PromotionManager.onClosed?.invoke() }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun start(context: Context, customerId: String, token: String?, availableServices: List<AvailableService>) {
        this.appContext = context.applicationContext
        this.customerId = customerId
        this.availableServices = availableServices
        rebuild(token)
    }

    override fun updateToken(token: String?) {
        rebuild(token) // token bị chụp lúc init → refresh = init lại
    }

    override fun updateContext(orderId: String?, orderValue: String?, serviceCode: String?, metaData: String?) {
        PromotionSDK.updateContext(orderId, orderValue, serviceCode, metaData)
    }

    override fun stop() {
        PromotionSDK.release()
        customerId = null
        availableServices = emptyList()
    }

    private fun rebuild(token: String?) {
        val ctx = appContext ?: return
        val cid = customerId ?: return
        if (PromotionSDK.isInitialized()) PromotionSDK.release()
        // Đối ứng iOS: PromotionSDK.initialize(options) một lần; refresh token = init lại với session mới.
        PromotionSDK.initialize(
            ctx,
            PromotionSDKOptions(
                session = PromotionSessionConfig(
                    customerId = cid,
                    accessToken = token ?: "",
                    baseUrl = BASE_URL,
                    language = "vi-VN",
                    environment = PromotionEnvironment.PROD,
                ),
                availableServices = availableServices.map {
                    PromotionAvailableService(it.code, it.name, it.type, it.iconUrl)
                },
                theme = theme,
                callback = sdkCallback,
            ),
        )
    }

    // ─── Điều hướng / UI ────────────────────────────────────────────────────────

    override fun openMyPromotions(activity: FragmentActivity, containerViewId: Int?) {
        PromotionSDK.openMyPromotion(activity, containerViewId)
    }

    override fun openPromotionDetail(voucherId: String, activity: FragmentActivity, containerViewId: Int?) {
        PromotionSDK.openPromotionDetail(voucherId, activity, containerViewId)
    }

    // ─── Headless ─────────────────────────────────────────────────────────────

    override suspend fun fetchVouchers(
        keyword: String?,
        serviceCode: String?,
        tab: String?,
        page: Int,
    ): Result<VoucherPage> {
        if (!PromotionSDK.isInitialized()) return Result.failure(notReady())
        return when (val r = PromotionSDK.api.getVouchers(keyword, serviceCode, tab, page)) {
            is PromotionApiResult.Success -> Result.success(
                VoucherPage(mine = r.data.vouchers.map(::map), mineIsLastPage = r.data.isLastPage)
            )
            is PromotionApiResult.Failure -> Result.failure(r.error)
        }
    }

    override suspend fun findEligibleOffers(
        order: OrderContext,
        tab: String?,
        myPage: Int,
        otherPage: Int,
    ): Result<EligibleOffers> {
        if (!PromotionSDK.isInitialized()) return Result.failure(notReady())
        return when (
            val r = PromotionSDK.api.findEligible(
                orderId = order.id,
                orderValue = order.value,
                tabCode = tab,
                myPage = myPage,
                otherPage = otherPage,
            )
        ) {
            is PromotionApiResult.Success -> Result.success(
                EligibleOffers(
                    mine = r.data.myOffers.map(::map),
                    others = r.data.otherOffers.map(::map),
                    mineIsLastPage = r.data.myIsLastPage,
                    othersIsLastPage = r.data.otherIsLastPage,
                )
            )
            is PromotionApiResult.Failure -> Result.failure(r.error)
        }
    }

    override suspend fun fetchVoucherDetail(voucherId: String, serviceCode: String?): Result<VoucherDetail> {
        if (!PromotionSDK.isInitialized()) return Result.failure(notReady())
        return when (val r = PromotionSDK.api.getVoucherDetail(voucherId, serviceCode)) {
            is PromotionApiResult.Success -> Result.success(map(r.data))
            is PromotionApiResult.Failure -> Result.failure(r.error)
        }
    }

    override suspend fun validate(order: OrderContext, voucherIds: List<String>): Result<ValidationSummary> {
        if (!PromotionSDK.isInitialized()) return Result.failure(notReady())
        return when (val r = PromotionSDK.api.validateDiscounts(order.id, order.value, voucherIds)) {
            is PromotionApiResult.Success -> Result.success(
                ValidationSummary(r.data.overallValid, r.data.totalDiscountAmount, r.data.finalAmount)
            )
            is PromotionApiResult.Failure -> Result.failure(r.error)
        }
    }

    override suspend fun createRedemption(order: OrderContext, voucherId: String): Result<String> {
        if (!PromotionSDK.isInitialized()) return Result.failure(notReady())
        return when (val r = PromotionSDK.api.createRedemption(order.id, order.value, listOf(voucherId))) {
            is PromotionApiResult.Success -> Result.success(r.data.sessionId) // map type SDK → String cho app
            is PromotionApiResult.Failure -> Result.failure(r.error)
        }
    }

    // ─── Mapping SDK → app model (anti-corruption) ────────────────────────────

    private fun notReady() = IllegalStateException("SDK chưa khởi tạo (chưa login?)")

    private fun map(v: PromotionVoucher) = VoucherSummary(
        id = v.id,
        merchantName = v.merchantName,
        title = v.title,
        imageURL = v.imageURL,
        expireDate = v.expireDate,
        isUsed = v.isUsed,
        statusLabel = v.displayStatusLabel,
    )

    private fun map(o: PromotionEligibleOffer) = EligibleOffer(
        id = o.id,
        name = o.name,
        objectType = o.objectType,
        usable = o.usable,
        estimatedDiscount = o.estimatedDiscount,
        expireDate = o.expireDate,
        ineligibleReason = o.ineligibleReason,
    )

    private fun map(d: PromotionVoucherDetail) = VoucherDetail(
        id = d.id,
        merchantName = d.merchantName,
        title = d.title,
        description = d.description,
        guideline = d.guideline,
        startDate = d.startDate,
        expireDate = d.expireDate,
        bannerURL = d.bannerURL,
        logoURL = d.logoURL,
        statusLabel = d.displayStatusLabel,
    )
}
