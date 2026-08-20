package com.ttcn.prm.entry

// Extension ở androidMain của promotionLogic: nạp applicationContext + suy ra isDebug.
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.findFragment
import com.ttcn.promotionsdk.di.PromotionContainer
import com.ttcn.promotionsdk.di.initialize
import com.ttcn.promotionsdk.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureFlagUseCases
import com.ttcn.promotionsdk.domain.usecase.PromotionFeatureGate
import com.ttcn.prm.entry.api.PromotionFeature
import com.ttcn.prm.entry.api.PromotionFeatureFlagsSnapshot
import com.ttcn.prm.entry.api.PromotionOrderItem
import com.ttcn.prm.entry.api.PromotionSDKApi
import com.ttcn.prm.entry.api.PromotionVoucherDetail
import com.ttcn.prm.entry.api.toPublicDetail
import com.ttcn.prm.entry.api.flagName
import com.ttcn.prm.entry.api.toSnapshot
import com.ttcn.prm.ui.feature.endowview.PRMEndowView
import com.ttcn.prm.ui.base.PRMBaseConfirmDialog
import com.ttcn.prm.ui.feature.choosepromotion.ChoosePromotionFragment
import com.ttcn.prm.ui.feature.mypromotion.MyPromotionFragment
import com.ttcn.prm.ui.feature.promotiondetail.PromotionDetailFragment
import com.ttcn.prm.ui.theme.PromotionSDKTheme
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.theme.PromotionThemeStore
import com.ttcn.prm.ui.utils.isPromotionSdkDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Điểm vào SDK — singleton `object`, đối ứng 1:1 `PromotionSDK` bên iOS (thứ tự thành viên khớp nhau:
 * xem docs/InitParity.md §1). Cấu hình một lần qua [initialize], cập nhật đơn hàng/dịch vụ qua [updateOrderInfo].
 */
object PromotionSDK {

    private const val TAG = "PromotionSDK"
    /** Tag riêng cho log chẩn đoán điều hướng/phím back — xem [debugLog]. */
    private const val TAG_SYSTEM_BACK = "PRMSystemBack"
    private const val TAG_MY_PROMOTION = "prm_my_promotion"
    private const val TAG_PROMOTION_DETAIL = "prm_promotion_detail"
    private const val TAG_CHOOSE_PROMOTION = "prm_choose_promotion"

    private var callback: PromotionSDKCallback? = null
    private var mutableContext: PromotionMutableContext? = null
    private var sdkScope: CoroutineScope? = null
    /** Application context giữ lại để dựng lại đồ thị DI mà không cần host truyền lại. */
    private var appContext: Context? = null

    /**
     * Cấu hình **tĩnh** — đặt ở lần [initialize] đầu (hoặc lần đầu sau [release]) rồi dùng lại cho
     * mọi lần init sau. [release] **không** xoá: đây là cấu hình tích hợp của host, không phải dữ
     * liệu phiên.
     */
    private data class StaticConfig(
        val baseUrl: String,
        val language: String,
        val environment: PromotionEnvironment,
    )
    private var staticConfig: StaticConfig? = null


    /**
     * Gác "đã [initialize] chưa" cho các **điểm mở màn**: chưa init → ghi `Log.e` rồi trả `false` để
     * nơi gọi `return`, **không ném**. Đối ứng `requireImpl(_:)` bên iOS (cũng log + trả `nil`).
     */
    private fun requireInitialized(caller: String): Boolean {
        if (PromotionContainer.isInitialized()) return true
        Log.e(TAG, "$caller bị gọi trước initialize() — bỏ qua. Hãy gọi PromotionSDK.initialize(...) trước.")
        return false
    }

    /**
     * Log chẩn đoán, **chỉ in khi host bật `isDebug`** — cùng cờ gác cURL/`LogLevel.BODY` ở
     * `PromotionHttpClient` và ảnh ở `PRMImageExt`. Đối ứng `#if DEBUG` bên iOS.
     *
     * Khác [Log.e] của [requireInitialized]: cái đó báo host dùng SAI API nên phải luôn in; những
     * dòng dưới đây chỉ mô tả SDK chọn nhánh nào ở luồng chạy đúng, ra logcat bản release là rác —
     * kèm theo lộ cả tên class fragment/view của host.
     */
    private fun debugLog(message: String) {
        if (isPromotionSdkDebug()) Log.d(TAG_SYSTEM_BACK, message)
    }

    // ─── Init ────────────────────────────────────────────────────────────────

    /**
     * Khởi tạo **tối giản** — đủ cho phần lớn host: chỉ token + baseUrl.
     * `availableServices`/`theme`/`callback` là tuỳ chọn; cần cấu hình sâu hơn thì dùng overload
     * [initialize] nhận [PromotionSDKOptions]. Đối ứng `PromotionSDK.initialize(...)` phẳng bên iOS.
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        context: Context,
        accessToken: String,
        baseUrl: String,
        environment: PromotionEnvironment = PromotionEnvironment.PROD,
        language: String = "vi-VN",
        availableServices: List<PromotionAvailableService> = emptyList(),
        theme: PromotionSDKTheme? = null,
        callback: PromotionSDKCallback? = null,
    ) = initialize(
        context,
        PromotionSDKOptions(
            session = PromotionSessionConfig(accessToken, baseUrl, language, environment),
            availableServices = availableServices,
            theme = theme,
            callback = callback,
        ),
    )

    /**
     * Khởi tạo SDK. Đối ứng `PromotionSDK.initialize(options:)` bên iOS (iOS không cần `context`).
     *
     * **Host chỉ cần gọi [initialize] — kể cả khi login lại.** Lần đầu chốt phần **cố định**
     * (`baseUrl` / `environment` / `language` / `theme`). Các lần sau (đăng nhập user mới) chỉ cần
     * truyền lại field **động** (`accessToken` / `availableServices`); SDK **bỏ qua** mọi
     * thay đổi ở field cố định (có cảnh báo log). Muốn đổi cấu hình cố định thật → [release] rồi init lại.
     */
    @JvmStatic
    fun initialize(context: Context, options: PromotionSDKOptions) {
        // `baseUrl` / `environment` / `language` là cấu hình **tĩnh**: đã có thì DÙNG LẠI, không
        // khởi tạo lần nữa. Host gọi `initialize` mỗi lần vào app chỉ để đưa **token mới**, không
        // phải để đổi endpoint — bắt truyền lại đủ mỗi lần chỉ tạo cơ hội truyền thiếu.
        //
        // Khác cơ chế "khoá" cũ ở chỗ **không cảnh báo, không bỏ qua trong im lặng**: host truyền gì
        // ở lần đầu (hoặc sau `release()`) thì đó là cấu hình; các lần sau field tĩnh đơn giản là
        // không cần nữa. Muốn đổi thật → `release()` rồi init lại.
        val staticCfg = staticConfig
        val incoming = if (staticCfg == null) options.session else options.session.copy(
            baseUrl = staticCfg.baseUrl,
            language = staticCfg.language,
            environment = staticCfg.environment,
        )
        if (isInitialized()) release()
        staticConfig = StaticConfig(incoming.baseUrl, incoming.language, incoming.environment)
        appContext = context.applicationContext
        callback = options.callback
        mutableContext = PromotionMutableContext(incoming, options.availableServices)
        PromotionContainer.initialize(context, options.toCoreConfig(mutableContext))
        // Theme cũng là cấu hình tĩnh: host truyền → ghi đè + lưu; không truyền → khôi phục bản đã
        // lưu. Nhờ vậy host cấu hình một lần, các lần init sau theme tự sống lại.
        val resolved = options.theme
        if (resolved != null) PromotionThemeStore.save(resolved)
        PromotionThemeRegistry.configure(resolved ?: PromotionThemeStore.load())
        sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        // Nạp cờ tính năng từ server. `refresh()` không ném lỗi: hỏng thì giữ cache (fail-open).
        // Nạp xong mới báo host: đây là lúc đầu tiên biết chắc SDK có được bật hay không.
        sdkScope?.launch {
            PromotionFeatureGate.refresh()
            notifyAvailability()
        }
    }


    /**
     * Refresh access token **giữa phiên** (cùng customer, không đổi login) — nhẹ hơn [initialize]:
     * **giữ nguyên** cả context đơn hàng đang ghi (dùng khi token hết hạn giữa checkout).
     * Đối ứng `updateToken(_:)` bên iOS.
     *
     * [accessToken] trùng token hiện có → **no-op**, không re-init đồ thị DI.
     *
     * @throws IllegalStateException nếu [initialize] chưa được gọi.
     */
    @JvmStatic
    fun updateToken(accessToken: String) {
        val ctx = checkNotNull(mutableContext) {
            "PromotionSDK.initialize() must be called before updateToken()."
        }
        val context = checkNotNull(appContext) { "Application context missing — call initialize() first." }
        // Token trùng token hiện có → bỏ qua. `applySession` là thao tác NẶNG (huỷ `sdkScope`, `clear()`
        // + `initialize()` lại `PromotionContainer`, refresh feature flag) — host gọi `updateToken()`
        // lặp lại (ví dụ mỗi lần vào màn checkout) với cùng token không nên trả giá đó mỗi lần.
        if (ctx.session.accessToken == accessToken) return
        applySession(context, ctx.session.copy(accessToken = accessToken), ctx.availableServices, keepOrderContext = true)
    }

    /**
     * Dựng lại đồ thị DI với [newSession] + [availableServices]. [keepOrderContext] = true (refresh
     * token giữa phiên) thì bơm lại order/dịch vụ đang ghi; false (login mới) thì để rỗng.
     *
     * **BẮT BUỘC clear trước:** SdkDi là singleton bền, re-init không clear thì HttpClient (token cũ)
     * vẫn nằm trong cache singleton → token mới không có tác dụng. `clear()` đóng client cũ + reset
     * registry; **không** đụng theme (PromotionThemeRegistry riêng, sống qua clear).
     */
    private fun applySession(
        context: Context,
        newSession: PromotionSessionConfig,
        availableServices: List<PromotionAvailableService>,
        keepOrderContext: Boolean,
    ) {
        val prev = mutableContext
        val newMutable = PromotionMutableContext(newSession, availableServices)
        if (keepOrderContext && prev != null) {
            newMutable.orderId = prev.orderId; newMutable.orderValue = prev.orderValue
            newMutable.serviceCode = prev.serviceCode; newMutable.metaData = prev.metaData
            newMutable.orderItems = prev.orderItems
        }
        mutableContext = newMutable

        sdkScope?.cancel()
        PromotionContainer.clear()
        PromotionContainer.initialize(context, newMutable.toCoreConfig())
        sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        sdkScope?.launch {
            PromotionFeatureGate.refresh()
            notifyAvailability()
        }
        // callback + theme giữ nguyên trong bộ nhớ — không đụng.
    }

    /**
     * Giải phóng SDK. Đối ứng `PromotionSDK.release()` bên iOS. Gọi khi chưa init là vô hại.
     * **Không** xoá theme đã lưu — nó sống qua release/init.
     */
    @JvmStatic
    fun release() {
        sdkScope?.cancel()
        sdkScope = null
        PromotionContainer.clear()
        // Xoá sạch **dữ liệu phiên**: session, context đơn hàng, callback, đồ thị DI.
        //
        // GIỮ lại cấu hình tĩnh (`baseUrl`/`environment`/`language` trong [staticConfig], và theme đã
        // lưu): đó là cấu hình tích hợp của host, không phải dữ liệu người dùng. Xoá đi thì lần
        // `initialize` sau host buộc phải truyền lại đủ, đúng thứ cơ chế này sinh ra để tránh.
        PromotionThemeRegistry.configure(null)
        callback = null
        mutableContext = null
        appContext = null
    }

    /** `true` sau [initialize] và trước [release]. Gọi [release] khi chưa init là vô hại. */
    @JvmStatic
    fun isInitialized(): Boolean = PromotionContainer.isInitialized()

    /** Callback đã truyền lúc [initialize] (`null` nếu chưa init / không truyền). Đối ứng `getCallback()` iOS. */
    @JvmStatic
    fun getCallback(): PromotionSDKCallback? = callback

    // ─── Headless API ────────────────────────────────────────────────────────

    /**
     * Bề mặt headless cho host tự dựng UI. Phải gọi [initialize] trước.
     *
     * ```kotlin
     * when (val r = PromotionSDK.api.getVouchers()) {
     *     is PromotionApiResult.Success -> render(r.data.vouchers)
     *     is PromotionApiResult.Failure -> showError(r.error)
     * }
     * ```
     *
     * Trả DTO public; type của `promotionLogic` không lọt ra chữ ký. Xem [PromotionSDKApi].
     *
     * Dựng mới mỗi lần đọc: sau [release] + [initialize] lại, instance cũ vẫn giữ use case của đồ thị DI đã bị huỷ.
     *
     * @throws IllegalStateException khi chưa gọi [initialize].
     */
    @JvmStatic
    val api: PromotionSDKApi
        get() {
            check(PromotionContainer.isInitialized()) {
                "PromotionSDK.initialize() must be called before api."
            }
            return PromotionSDKApi()
        }

    // ─── Context ─────────────────────────────────────────────────────────────

    /** Session đã truyền lúc [initialize]. Null khi chưa [initialize] hoặc không truyền [PromotionSessionConfig]. */
    @JvmStatic
    val session: PromotionSessionConfig? get() = mutableContext?.session

    /** Giá trị dynamic hiện tại được ghi qua [updateOrderInfo]. Null khi chưa [updateOrderInfo]. */
    @JvmStatic
    val currentOrderId: String? get() = mutableContext?.orderId
    @JvmStatic
    val currentOrderValue: String? get() = mutableContext?.orderValue
    @JvmStatic
    val currentServiceCode: String? get() = mutableContext?.serviceCode
    @JvmStatic
    val currentMetaData: String? get() = mutableContext?.metaData

    /**
     * Cập nhật context đơn hàng / dịch vụ — gọi mỗi khi host vào màn có voucher (checkout, dịch vụ…).
     *
     * Ghi vào [PromotionMutableContext] đang sống; không cần [initialize] lại. SDK đọc lại các giá trị này
     * ở **mỗi** request, nên gọi trước khi mở màn hoặc gọi API là đủ.
     *
     * Đơn hiện chỉ hỗ trợ **một** dòng sản phẩm nên [skuSourceId]/[productName]/[productCategory]/
     * [quantity]/[unitPrice] được truyền phẳng thay vì `List<PromotionOrderItem>`; SDK tự bọc lại
     * thành `List<PromotionOrderItem>` 1 phần tử trước khi ghi vào context.
     *
     * @param orderId Mã đơn hàng — bắt buộc.
     * @param productId Mã dịch vụ/sản phẩm — bắt buộc, dùng để lấy campaign theo SKU. Đối ứng
     * `PromotionSDK.updateOrderInfo(productId:...)` bên iOS.
     * @param skuSourceId Mã SKU đối tác — tuỳ chọn. Bỏ trống thì SDK **không** gửi field này lên
     * server (không gửi chuỗi rỗng), server chỉ áp rule cấp sản phẩm/đơn.
     * @param quantity Số lượng (> 0) — mặc định `1` nếu không truyền.
     * @param unitPrice Đơn giá — mặc định `"0"` nếu không truyền.
     *
     * @throws IllegalStateException nếu [initialize] chưa được gọi.
     */
    @JvmStatic
    @JvmOverloads
    fun updateOrderInfo(
        orderId: String,
        productId: String,
        orderValue: String? = null,
        metaData: String? = null,
        skuSourceId: String? = null,
        productName: String? = null,
        productCategory: String? = null,
        quantity: Int? = null,
        unitPrice: String? = null,
    ) {
        val ctx = checkNotNull(mutableContext) {
            "PromotionSDK.initialize() must be called before updateOrderInfo()."
        }
        ctx.orderId = orderId
        ctx.orderValue = orderValue
        ctx.metaData = metaData
        ctx.orderItems = listOf(
            PromotionOrderItem(
                skuSourceId = skuSourceId.orEmpty(),
                productId = productId,
                productName = productName,
                productCategory = productCategory,
                quantity = quantity ?: 1,
                unitPrice = unitPrice ?: "0",
            ),
        )
    }

    // ─── Theming ─────────────────────────────────────────────────────────────

    /**
     * Đổi theme **và lưu lại** sau khi đã [initialize]. `null` = xoá theme đã lưu, về mặc định SDK.
     * Đối ứng `PromotionSDK.configure(theme:)` bên iOS.
     *
     * View đã render có thể chỉ cập nhật khi được dựng lại (rebind/đẩy màn mới) — nên cấu hình theme
     * **một lần** lúc khởi tạo là tốt nhất.
     */
    @JvmStatic
    fun configure(theme: PromotionSDKTheme?) {
        PromotionThemeRegistry.configure(theme)
        if (theme != null) PromotionThemeStore.save(theme) else PromotionThemeStore.clear()
    }

    /** Theme đang áp (`null` nếu đang dùng mặc định). Đối ứng `PromotionSDK.currentTheme()` bên iOS. */
    @JvmStatic
    fun currentTheme(): PromotionSDKTheme? = PromotionThemeRegistry.currentConfig()

    // ─── Feature flag ────────────────────────────────────────────────────────
    //
    // SDK **vẫn tự gác** mọi điểm vào (`openMyPromotion`, `PRMBaseFragment.openPromotionDetail`,
    // `PRMEndowView`) qua `PromotionFeatureGate` — bốn hàm dưới đây **không** thay thế việc đó, chúng
    // chỉ cho host *hỏi trước* để ẩn entry point của mình thay vì để user bấm rồi ăn toast PRM_MOB_021.
    //
    // Tất cả đều **fail-open**: chưa [initialize] hoặc chưa có cache → trả "bật hết". Không hàm nào
    // ném lỗi, vì cờ hỏng không được phép làm chết màn hình của host.

    /**
     * Ảnh chụp toàn bộ cờ, đọc **cache đồng bộ** — không gọi mạng, gọi được từ main thread.
     * Đã áp sẵn công tắc tổng: `all == false` thì mọi field còn lại đều `false`.
     *
     * Cache được nạp ở [initialize] và mỗi lần [refreshFeatureFlags]; muốn chắc chắn mới nhất thì
     * gọi [refreshFeatureFlags] rồi đọc trong `onComplete`.
     *
     * Đối ứng `PromotionSDK.featureFlags()` bên iOS.
     */
    @JvmStatic
    fun featureFlags(): PromotionFeatureFlagsSnapshot =
        runCatching { PromotionFeatureFlagUseCases().all().toSnapshot() }
            .getOrDefault(PromotionFeatureFlagsSnapshot.AllEnabled)

    /**
     * Tra **một** tính năng. Tương đương `featureFlags().isEnabled(feature)` nhưng khỏi dựng snapshot.
     *
     * Hai điều kiện phải **song song đúng**: công tắc tổng [isSdkEnabled] bật **và** cờ riêng của
     * tính năng bật. SDK tắt ⇒ mọi tính năng tắt, không có ngoại lệ.
     *
     * `&&` này không đổi kết quả — `PromotionFeatureFlags.isEnabled` đã tự áp `ENABLE_ALL` bằng
     * `if (!enableAll) return false`. Viết ra để luật hiện lên ngay tại bề mặt public: đây là hàm
     * host đọc để quyết định ẩn/hiện entry point, không ai phải lần vào lõi mới biết công tắc tổng
     * có được xét hay không. Fail-open giữ nguyên: chưa `initialize()` thì cả hai vế đều `true`.
     *
     * ```kotlin
     * binding.btnMyVoucher.isVisible = PromotionSDK.isFeatureEnabled(PromotionFeature.VOUCHER_LIST)
     * ```
     *
     * Đối ứng `PromotionSDK.isFeatureEnabled(_:)` bên iOS.
     */
    @JvmStatic
    fun isFeatureEnabled(feature: PromotionFeature): Boolean =
        isSdkEnabled() && PromotionFeatureGate.isEnabled(feature.flagName())

    /**
     * Công tắc tổng `PROMOTION.ENABLE_ALL` — `false` thì host nên ẩn **toàn bộ** điểm vào ưu đãi.
     * Tương đương `isFeatureEnabled(PromotionFeature.ALL)`.
     *
     * Đối ứng `PromotionSDK.isSdkEnabled()` bên iOS.
     */
    @JvmStatic
    fun isSdkEnabled(): Boolean = PromotionFeatureGate.isSdkEnabled()

    /**
     * Nạp lại cờ từ server rồi trả snapshot mới. **Không ném**: gọi API hỏng thì giữ nguyên cache
     * và vẫn gọi [onComplete] với giá trị đang có (fail-open).
     *
     * [onComplete] chạy trên **main thread** để host set UI được ngay. Chưa [initialize] thì gọi
     * luôn với [PromotionFeatureFlagsSnapshot.AllEnabled].
     *
     *
     * ```kotlin
     * PromotionSDK.refreshFeatureFlags { flags ->
     *     binding.groupPromotion.isVisible = flags.all
     * }
     * ```
     *
     * Đối ứng `PromotionSDK.refreshFeatureFlags(completion:)` bên iOS.
     */
    @JvmStatic
    @JvmOverloads
    fun refreshFeatureFlags(onComplete: ((PromotionFeatureFlagsSnapshot) -> Unit)? = null) {
        val scope = sdkScope
        if (scope == null) {
            // Chưa init → không có gì để nạp. Trả mặc định fail-open ngay, vẫn trên main thread.
            // Post thẳng lên main looper thay vì dựng một CoroutineScope rời: scope đó không ai giữ,
            // không ai huỷ, và ở đây chỉ để chạy đúng một lambda.
            onComplete?.let { done ->
                Handler(Looper.getMainLooper()).post { done(PromotionFeatureFlagsSnapshot.AllEnabled) }
            }
            return
        }
        scope.launch {
            PromotionFeatureGate.refresh()
            val flags = featureFlags()
            withContext(Dispatchers.Main) {
                onComplete?.invoke(flags)
            }
        }
    }

    /**
     * Báo host trạng thái công tắc tổng sau khi cờ đã được nạp xong ở [initialize] / [applySession].
     * Chạy sẵn trên coroutine nền nên phải chuyển về main thread trước khi gọi callback của host.
     */
    private suspend fun notifyAvailability() {
        val cb = callback ?: return
        val enabled = isSdkEnabled()
    }

    // ─── Screens ─────────────────────────────────────────────────────────────

    /**
     * Hiển thị màn "Ưu đãi của tôi" ([MyPromotionFragment]).
     *
     * Gác bởi cờ [PromotionFeatureFlag.VOUCHER_LIST]: TẮT → hiện thông báo PRM_MOB_021 và **không**
     * mở màn, y như `PromotionSDK.openMyPromotion` bên iOS.
     *
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param containerViewId Nếu khác null, fragment được **add** lên container này — fragment host
     * đang add trong cùng container (nếu có và đang hiện) chỉ bị `hide()`, **không** bị remove/destroy
     * view (xem [addOrHideThenAdd]); nếu null, fragment được add lên android.R.id.content.
     *
     * Chưa [initialize] → log `Log.e` rồi **không làm gì** (không ném). Xem [requireInitialized].
     */
    /**
     * Cờ tính năng TẮT ở một điểm mở màn: **ưu tiên trả cho host**, host không nhận thì SDK tự lo.
     *
     * Không dùng callback toàn cục mà nhận lambda ngay ở hàm `open…`: chỉ có cách đó SDK mới biết
     * chắc host **có đăng ký hay không**. `PromotionSDKCallback` là interface có default method —
     * gọi vào thì luôn trúng thân mặc định, không phân biệt được "host implement" với "host mặc kệ",
     * nên không thể dựa vào nó để quyết định có tự hiện popup hay không.
     *
     * Màn **không** mở trong cả hai nhánh — đây chỉ là chuyện ai báo cho user.
     */
    private fun notifyFeatureDisabled(activity: FragmentActivity, onFeatureDisabled: (() -> Unit)?) {
        if (onFeatureDisabled != null) {
            onFeatureDisabled()
            return
        }
        PRMBaseConfirmDialog.showFeatureDisabled(activity, activity.supportFragmentManager)
    }

    @JvmStatic
    @JvmOverloads
    fun openMyPromotion(
        activity: FragmentActivity,
        containerViewId: Int? = null,
        onFeatureDisabled: (() -> Unit)? = null,
    ) {
        if (!requireInitialized("openMyPromotion()")) return
        if (!PromotionFeatureGate.canOpenVoucherList()) {
            notifyFeatureDisabled(activity, onFeatureDisabled)
            return
        }
        val fm = resolveFragmentManager(activity, containerViewId)
        if (fm.findFragmentByTag(TAG_MY_PROMOTION) != null) return
        val fragment = MyPromotionFragment()
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .addOrHideThenAdd(fm, containerViewId, fragment, TAG_MY_PROMOTION)
            .addToBackStack(TAG_MY_PROMOTION)
            .commit()
    }

    /**
     * Mở thẳng màn "Chi tiết ưu đãi" theo [voucherId], **không** qua danh sách.
     *
     * Dùng khi host đã biết id — vd bấm vào push notification, hoặc deeplink từ banner ngoài SDK.
     * Đối ứng `PromotionSDK.openPromotionDetail(voucherId:from:)` bên iOS.
     *
     * Gác bởi cờ [PromotionFeatureFlag.VOUCHER_DETAIL] y như đường vào nội bộ
     * (`PRMBaseFragment.openPromotionDetail`): TẮT → thông báo PRM_MOB_021 và **không** mở màn.
     * Gác ở đây là bắt buộc — kill-switch không được có cửa sau chỉ vì host gọi thẳng entry.
     *
     * Màn tự fetch chi tiết theo [voucherId]; trong lúc chờ hiện shimmer.
     *
     * [returnVoucherOnApply] quyết định **nhãn nút và hành vi khi bấm** (TLNV MOB_002 control #5):
     *
     * | Giá trị | Nút | Bấm thì |
     * |---|---|---|
     * | `true` (mặc định) | "Áp dụng" | trả [PromotionVoucherDetail] về [onVoucherApplied], SDK tự đóng màn |
     * | `false` | "Dùng ngay" | SDK mở bottom sheet chọn dịch vụ, kết quả về [PromotionSDKCallback.onServiceSelected] |
     *
     * Bật là để **màn host nào cũng mang đi tích hợp được**: [onVoucherApplied] gắn với chính lời gọi
     * này nên data về đúng màn vừa mở, khác [PromotionSDKCallback] là kênh singleton không biết ai gọi.
     *
     * @param voucherId Id voucher cần xem.
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param containerViewId Khác null → **add** lên container này, chỉ `hide()` fragment host đang
     * add trong cùng container (nếu có); null → `add` lên `android.R.id.content`. Cùng quy ước với
     * [openMyPromotion] (xem [addOrHideThenAdd]).
     * @param returnVoucherOnApply `true` → trả voucher về [onVoucherApplied]; `false` → SDK tự điều
     * hướng sang chọn dịch vụ.
     * @param hostHandlesDismiss `false` (mặc định) → SDK tự đóng màn chi tiết sau khi bấm "Áp dụng".
     * `true` → SDK **để màn đó lại**, host tự pop trong [onVoucherApplied] — dùng khi cần hỏi xác
     * nhận, chạy animation riêng, hoặc điều hướng thẳng sang màn khác thay vì quay lại màn cũ:
     *
     * ```kotlin
     * PromotionSDK.openPromotionDetail(
     *     voucherId, activity, hostHandlesDismiss = true,
     *     onVoucherApplied = { detail ->
     *         PromotionSDK.closePromotionDetail(activity)   // host tự đóng
     *         goToCheckout(detail)
     *     },
     * )
     * ```
     *
     * ⚠️ **Dùng [closePromotionDetail], KHÔNG tự gọi `activity.supportFragmentManager.popBackStack()`.**
     * SDK chọn FragmentManager theo container (xem [resolveFragmentManager]) nên màn chi tiết có thể
     * nằm ở FM của Activity **hoặc** ở `childFragmentManager` của fragment host. Pop nhầm FM sẽ pop
     * entry của chính host — màn tụt xuống một nấc trong khi màn chi tiết vẫn còn.
     *
     * Chỉ có nghĩa khi [returnVoucherOnApply] bật — nhánh "Dùng ngay" không đóng màn bao giờ.
     * @param onVoucherApplied Chỉ dùng khi [returnVoucherOnApply] `true`. Nhận **cả object
     * [PromotionVoucherDetail]** — cùng thứ [PromotionSDKApi.getVoucherDetail] trả, nên host không
     * phải gọi API lần nữa để lấy tên/mô tả/HSD/ảnh/mã code. Gọi trên main thread, **trước** khi màn
     * chi tiết pop — nhờ vậy [hostHandlesDismiss] mới chạy được (host còn màn để tự đóng). Bỏ trống
     * thì màn vẫn đóng nhưng không ai nhận data.
     *
     * Chưa [initialize] → log `Log.e` rồi **không làm gì** (không ném). Xem [requireInitialized].
     */
    @JvmStatic
    @JvmOverloads
    fun openPromotionDetail(
        voucherId: String,
        activity: FragmentActivity,
        containerViewId: Int? = null,
        returnVoucherOnApply: Boolean = true,
        hostHandlesDismiss: Boolean = false,
        onVoucherApplied: ((detail: PromotionVoucherDetail) -> Unit)? = null,
        onFeatureDisabled: (() -> Unit)? = null,
    ) {
        if (!requireInitialized("openPromotionDetail()")) return
        if (!PromotionFeatureGate.canOpenVoucherDetail()) {
            notifyFeatureDisabled(activity, onFeatureDisabled)
            return
        }
        val fm = resolveFragmentManager(activity, containerViewId)
        if (fm.findFragmentByTag(TAG_PROMOTION_DETAIL) != null) return
        val fragment = PromotionDetailFragment
            .newInstance(voucherId, returnVoucherOnApply, hostHandlesDismiss)
            .apply {
                // Map domain -> DTO **ở đây**, ranh giới public. Fragment là tầng UI nội bộ, không
                // được biết tới type public nào — cùng lý do `PromotionSDKApi` map trước khi trả.
                this.onVoucherApplied = onVoucherApplied?.let { host -> { host(it.toPublicDetail()) } }
            }
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .addOrHideThenAdd(fm, containerViewId, fragment, TAG_PROMOTION_DETAIL)
            .addToBackStack(TAG_PROMOTION_DETAIL)
            .commit()
    }

    /**
     * Hiển thị màn "Chọn ưu đãi" nối sẵn với widget [endowView] ở màn thanh toán.
     *
     * `PRMEndowView` tự gọi hàm này khi user bấm widget (xem `PRMEndowView.setupClickListeners`) —
     * host **không cần wiring gì thêm**. Public vì đôi khi host muốn tự kích hoạt màn này từ nơi khác
     * ngoài cú bấm mặc định của widget (vd nút "Xem ưu đãi" riêng).
     *
     * Cùng khuôn [openMyPromotion]/[openPromotionDetail]: gác bởi cờ
     * [PromotionFeatureFlag.VOUCHER_SELECTION] (TẮT → thông báo PRM_MOB_021, không mở màn), tự chọn
     * [FragmentManager] theo [containerViewId] (xem [resolveFragmentManager]), dedup theo tag.
     *
     * Lấy lại ưu đãi widget đã tải (khỏi gọi `findEligible` lần hai), pre-select voucher đang áp, và
     * đẩy kết quả ngược về widget khi user bấm "Áp dụng" — xem [ChoosePromotionFragment.forEndowView].
     *
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param endowView Instance widget đang hiển thị — dùng để lấy lại data đã tải + đẩy kết quả chọn.
     * @param containerViewId Xem [openMyPromotion].
     *
     * Chưa [initialize] → log `Log.e` rồi **không làm gì**. Xem [requireInitialized].
     */
    @JvmStatic
    @JvmOverloads
    fun openChoosePromotion(
        activity: FragmentActivity,
        endowView: PRMEndowView,
        containerViewId: Int? = null,
        onFeatureDisabled: (() -> Unit)? = null,
    ) {
        if (!requireInitialized("openChoosePromotion()")) return
        if (!PromotionFeatureGate.canShowVoucherSelection()) {
            notifyFeatureDisabled(activity, onFeatureDisabled)
            return
        }
        val fm = resolveFragmentManager(activity, containerViewId)
        if (fm.findFragmentByTag(TAG_CHOOSE_PROMOTION) != null) return
        val fragment = ChoosePromotionFragment.forEndowView(endowView)
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .addOrHideThenAdd(fm, containerViewId, fragment, TAG_CHOOSE_PROMOTION)
            .addToBackStack(TAG_CHOOSE_PROMOTION)
            .commit()
    }


    /**
     * Đóng màn "Chi tiết ưu đãi" do SDK mở. Trả `true` nếu có màn để đóng.
     *
     * Cặp đôi với `hostHandlesDismiss = true`: host nhận voucher ở `onVoucherApplied`, xử lý xong thì
     * gọi hàm này. **Host không cần biết SDK dùng FragmentManager nào** — đó chính là lý do hàm này
     * tồn tại: [resolveFragmentManager] chọn FM theo container, nên màn chi tiết có thể nằm ở FM của
     * Activity hoặc ở `childFragmentManager` của một fragment host. Hàm này dò cả cây FM theo tag rồi
     * pop đúng chỗ.
     *
     * Dùng `POP_BACK_STACK_INCLUSIVE` với **tên entry** chứ không pop mù entry trên cùng: nếu host đã
     * chồng màn của họ lên trên màn chi tiết, pop mù sẽ ăn nhầm màn host.
     */
    @JvmStatic
    fun closePromotionDetail(activity: FragmentActivity): Boolean =
        popSdkScreen(activity, TAG_PROMOTION_DETAIL)

    /** Đối ứng [closePromotionDetail] cho màn "Ưu đãi của tôi". */
    @JvmStatic
    fun closeMyPromotion(activity: FragmentActivity): Boolean =
        popSdkScreen(activity, TAG_MY_PROMOTION)

    private fun popSdkScreen(activity: FragmentActivity, tag: String): Boolean {
        val fm = findManagerHolding(activity.supportFragmentManager, tag) ?: run {
            debugLog("[popSdkScreen] không tìm thấy fragment tag=$tag")
            return false
        }
        if (fm.isStateSaved) {
            debugLog("[popSdkScreen] tag=$tag nhưng FM đã lưu state -> bỏ qua")
            return false
        }
        return fm.popBackStackImmediate(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    /** Dò theo chiều sâu cây FragmentManager để tìm FM đang giữ fragment mang [tag]. */
    private fun findManagerHolding(root: FragmentManager, tag: String): FragmentManager? {
        if (root.findFragmentByTag(tag) != null) return root
        for (child in root.fragments) {
            if (!child.isAdded) continue
            findManagerHolding(child.childFragmentManager, tag)?.let { return it }
        }
        return null
    }

    /**
     * FragmentManager **sở hữu container** — không mặc định là của Activity.
     *
     * Host dùng Navigation: container nằm trong layout của một destination, mà destination lại là
     * Fragment con của `NavHostFragment`. Add vào `activity.supportFragmentManager` thì fragment SDK
     * và view container thuộc **hai vòng đời khác nhau** — NavController huỷ destination, view chết,
     * nhưng fragment SDK vẫn nằm trong back stack của Activity. Nó thành **mồ côi**: vẫn `RESUMED`
     * (nên `Window.Callback` wrapper vẫn gắn, vẫn nuốt phím back) và mỗi entry mồ côi ăn mất **một**
     * lần bấm back. Mở 2 màn SDK rồi điều hướng đi = phải bấm back 3 lần mới lùi được 1 màn.
     *
     * Cách chữa: hỏi thẳng container xem Fragment nào sở hữu nó.
     * - Có → dùng `childFragmentManager` của Fragment đó. Màn SDK thành con của destination, chết
     *   theo destination, back stack không còn rác.
     * - Không có (`containerViewId` null, hoặc container nằm thẳng trong layout Activity) → FM của
     *   Activity, đúng y hành vi cũ.
     *
     * Một đường code chạy đúng cho **cả host dùng lẫn không dùng Navigation**, và host không phải
     * sửa một dòng nào — chữ ký `openMyPromotion` / `openPromotionDetail` giữ nguyên.
     */
    private fun resolveFragmentManager(
        activity: FragmentActivity,
        containerViewId: Int?,
    ): FragmentManager {
        val container = containerViewId?.let { activity.findViewById<View>(it) }
            ?: return activity.supportFragmentManager
        // findFragment() ném IllegalStateException khi view không thuộc fragment nào — đó là trường
        // hợp container cấp Activity, hoàn toàn hợp lệ, nên nuốt và rơi về FM của Activity.
        val owner = runCatching { container.findFragment<Fragment>() }.getOrNull()
        return owner?.childFragmentManager ?: activity.supportFragmentManager
    }

    private fun FragmentTransaction.addOrHideThenAdd(
        fm: FragmentManager,
        containerViewId: Int?,
        fragment: Fragment,
        tag: String,
    ): FragmentTransaction = apply {
        if (containerViewId != null) {
            val existing = fm.findFragmentById(containerViewId)
            existing?.takeIf { it.isAdded && !it.isHidden }?.let { hide(it) }
            add(containerViewId, fragment, tag)
        } else {
            add(android.R.id.content, fragment, tag)
        }
    }
}
