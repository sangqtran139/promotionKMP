package com.ttcn.prm.entry

import com.ttcn.promotionsdk.config.AvailableService
import com.ttcn.promotionsdk.config.PromotionRequestContextProvider
import com.ttcn.promotionsdk.config.PromotionSDKConfig
import com.ttcn.promotionsdk.config.SdkEnvironment
import com.ttcn.promotionsdk.domain.model.eligible.EligibleOrderItem
import com.ttcn.prm.entry.api.PromotionOrderItem

/**
 * Nguồn access token của host — **cách duy nhất** token đi vào SDK.
 *
 * SDK không giữ bản sao token nào. Mọi request đều hỏi lại [currentToken]; ăn 401 thì hỏi
 * [refreshToken] đúng một lần rồi thử lại; vẫn không được thì hỏng luôn.
 *
 * ```kotlin
 * PromotionSDK.initialize(
 *     context, baseUrl = BASE_URL,
 *     tokenSource = object : PromotionTokenSource {
 *         override fun currentToken() = auth.accessToken
 *     },
 * )
 * ```
 *
 * **Vòng đời — bẫy dễ dính nhất:** [PromotionSDK] là singleton `object`, nó giữ object này tới tận
 * [PromotionSDK.release]. Cài đặt nó bằng một anonymous object bên trong Fragment/Activity là bắt
 * luôn màn hình đó vào SDK → **rò rỉ vĩnh viễn**, và token đóng băng ở giá trị cuối. Trỏ vào kho
 * token **cấp app** (singleton / repository), đừng trỏ vào màn hình.
 */
interface PromotionTokenSource {

    /**
     * Token hiện hành của host. SDK gọi ở **mỗi** request — chính vì vậy host không cần báo gì cho
     * SDK khi token đổi: cơ chế nào của app ghi vào kho lúc nào cũng được, lượt gọi kế tiếp tự dùng
     * giá trị mới.
     *
     * **Hợp đồng — bắt buộc đọc:** hàm này nằm trên đường dựng header của mọi request, nên nó phải
     * là một phép **đọc biến trong bộ nhớ**: không I/O, không chặn, không gọi mạng, gọi được từ
     * thread bất kỳ (SDK gọi từ thread nền). Field nguồn nên là `@Volatile` / `AtomicReference` /
     * `StateFlow.value`. Đặt việc giải mã Keychain / `EncryptedSharedPreferences` /
     * `runBlocking { dataStore… }` vào đây là tự cắm một lần chặn vào từng lượt gọi API.
     *
     * Trả `null`/rỗng → SDK gửi request **không kèm** header `Authorization`.
     */
    fun currentToken(): String?

    /**
     * SDK ăn **HTTP 401** → xin host lấy token mới. Host gọi [onResult] với `true` khi kho token đã
     * có token mới, `false` khi chịu. SDK thử lại request hỏng **đúng một lần** nếu `true`; `false`
     * thì để lỗi `TOKEN_EXPIRED` nổi lên và bắn [PromotionSDKCallback.onExpireToken].
     *
     * **Mặc định là `false`** — host không cài đặt thì 401 hỏng ngay, không chờ. Đúng hành vi cho
     * app không có cách lấy token mới theo yêu cầu.
     *
     * `Boolean` chứ không phải token mới là cố ý: token vào SDK theo **một** đường duy nhất là
     * [currentToken]. Host ghi token mới vào kho của mình **rồi** báo `true` — không có đường thứ hai
     * để nhầm.
     *
     * **Ràng buộc:**
     * - Gọi [onResult] **đúng một lần**, kể cả khi hỏng. Không gọi thì SDK chờ tối đa 15 giây rồi
     *   coi như `false`.
     * - Được gọi từ thread nền; trả kết quả từ thread nào cũng được.
     * - Một thời điểm SDK chỉ gọi **một** lượt dù bao nhiêu request cùng ăn 401. Nhưng cơ chế của
     *   host vẫn nên single-flight: hai màn mở cách nhau vài giây vẫn có thể chạm vào hai lần.
     *
     * ```kotlin
     * override fun refreshToken(onResult: (Boolean) -> Unit) {
     *     auth.refresh { newToken ->
     *         if (newToken != null) auth.accessToken = newToken   // ghi vào kho TRƯỚC
     *         onResult(newToken != null)
     *     }
     * }
     * ```
     */
    fun refreshToken(onResult: (Boolean) -> Unit) = onResult(false)
}

/**
 * Thông tin phiên và cấu hình kết nối — truyền 1 lần lúc [PromotionSDK.initialize].
 * Để cập nhật đơn hàng / dịch vụ mỗi khi vào màn, dùng [PromotionSDK.updateOrderInfo].
 *
 * Không có field `accessToken`: token là thứ **thay đổi theo thời gian**, nên nó vào SDK dưới dạng
 * một nguồn ([tokenSource]) chứ không phải một chuỗi chụp sẵn.
 */
data class PromotionSessionConfig(
    val tokenSource: PromotionTokenSource,
    val baseUrl: String,
    val language: String = "vi-VN",
    val environment: PromotionEnvironment = PromotionEnvironment.PROD,
)

/**
 * Một dịch vụ khả dụng mà voucher có thể áp dụng.
 *
 * [productId] phải khớp `productId` trong `applicableProducts` của voucher thì dịch vụ mới hiện
 * ở bottom sheet "Chọn dịch vụ".
 */
data class PromotionAvailableService(
    val productId: String,
    val productName: String,
    val skuSourceId: String = "",
    val iconUrl: String = "",
)

enum class PromotionEnvironment { PROD, STAGING }

// ─── Public → core ──────────────────────────────────────────────────────────

internal fun PromotionSDKOptions.toCoreConfig(
    contextProvider: PromotionRequestContextProvider?,
): PromotionSDKConfig = buildCoreConfig(session, availableServices, contextProvider)

/** Dùng lại khi cần dựng core config mà không có [PromotionSDKOptions] trong tay. */
internal fun PromotionMutableContext.toCoreConfig(): PromotionSDKConfig =
    buildCoreConfig(session, availableServices, this)

private fun buildCoreConfig(
    session: PromotionSessionConfig,
    availableServices: List<PromotionAvailableService>,
    contextProvider: PromotionRequestContextProvider?,
): PromotionSDKConfig = PromotionSDKConfig(
    baseUrl = session.baseUrl,
    requestContextProvider = contextProvider,
    environment = when (session.environment) {
        PromotionEnvironment.PROD -> SdkEnvironment.PROD
        PromotionEnvironment.STAGING -> SdkEnvironment.STAGING
    },
    availableServices = availableServices.map {
        AvailableService(
            productId = it.productId,
            productName = it.productName,
            skuSourceId = it.skuSourceId,
            iconUrl = it.iconUrl,
        )
    },
)

/**
 * Giữ toàn bộ context mà SDK cần — tĩnh (session + danh mục dịch vụ) + động (đơn hàng/dịch vụ).
 * [PromotionSDK.updateOrderInfo] ghi trực tiếp vào đây; instance được tạo mới mỗi [PromotionSDK.initialize].
 * [availableServices] giữ ở đây để dựng lại core config không cần host truyền lại.
 */
internal class PromotionMutableContext(
    val session: PromotionSessionConfig,
    val availableServices: List<PromotionAvailableService> = emptyList(),
) : PromotionRequestContextProvider {

    @JvmField @Volatile var orderId: String? = null
    @JvmField @Volatile var orderValue: String? = null
    @JvmField @Volatile var serviceCode: String? = null
    @JvmField @Volatile var metaData: String? = null

    /** Dòng sản phẩm (SKU) của đơn hiện tại — lõi đọc qua [getOrderItems] cho `findEligible`. */
    @JvmField @Volatile var orderItems: List<PromotionOrderItem> = emptyList()

    /**
     * Hai hàm dưới đây chỉ **chuyển tiếp** sang [PromotionTokenSource] của host — không cache, không
     * fallback, không nhánh. Đó là điểm của thiết kế: token có đúng một nguồn, nên không tồn tại
     * trạng thái nào của SDK để lệch với host.
     *
     * Lõi gọi [getAccessToken] ở **mỗi** request qua `defaultRequest { }`.
     * Đối ứng `PromotionMutableContext.getAccessToken()` bên iOS.
     */
    override fun getAccessToken(): String? = session.tokenSource.currentToken()

    /** Đối ứng `PromotionMutableContext.refreshAccessToken(onResult:)` bên iOS. */
    override fun refreshAccessToken(onResult: (Boolean) -> Unit) =
        session.tokenSource.refreshToken(onResult)

    override fun getLanguage() = session.language
    override fun getOrderId() = orderId
    override fun getOrderValue() = orderValue
    override fun getService() = serviceCode
    override fun getMetaData() = metaData

    /**
     * Map order items (public) → model lõi cho Find Eligible Campaigns — `ChoosePromotionStore` /
     * `EndowStore` đọc chung 2 nền tảng. Đối ứng `PromotionMutableContext.getOrderItems()` bên iOS.
     */
    override fun getOrderItems(): List<EligibleOrderItem> = orderItems.map {
        EligibleOrderItem(
            skuSourceId = it.skuSourceId,
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            orderItemId = null,
            productId = it.productId,
            productName = it.productName,
            productCategory = it.productCategory,
        )
    }
}
