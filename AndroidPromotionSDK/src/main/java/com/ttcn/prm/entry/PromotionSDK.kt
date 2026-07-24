package com.ttcn.prm.entry

// Extension ở androidMain của promotionLogic: nạp applicationContext + suy ra isDebug.
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.ttcn.prm.R
import com.ttcn.promotionsdk.core.di.PromotionContainer
import com.ttcn.promotionsdk.core.di.initialize
import com.ttcn.promotionsdk.core.domain.model.featureflag.PromotionFeatureFlag
import com.ttcn.promotionsdk.core.domain.usecase.PromotionFeatureGate
import com.ttcn.prm.entry.api.PromotionOrderItem
import com.ttcn.prm.entry.api.PromotionSDKApi
import com.ttcn.prm.ui.feature.promotion.mypromotion.MyPromotionFragment
import com.ttcn.prm.ui.feature.promotion.promotiondetail.PromotionDetailFragment
import com.ttcn.prm.ui.theme.PromotionSDKTheme
import com.ttcn.prm.ui.theme.PromotionThemeRegistry
import com.ttcn.prm.ui.theme.PromotionThemeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Điểm vào SDK — singleton `object`, đối ứng 1:1 `PromotionSDK` bên iOS (thứ tự thành viên khớp nhau:
 * xem docs/InitParity.md §1). Cấu hình một lần qua [initialize], cập nhật đơn hàng/dịch vụ qua [updateContext].
 */
object PromotionSDK {

    private const val TAG = "PromotionSDK"
    private const val TAG_MY_PROMOTION = "prm_my_promotion"
    private const val TAG_PROMOTION_DETAIL = "prm_promotion_detail"

    private var callback: PromotionSDKCallback? = null
    private var mutableContext: PromotionMutableContext? = null
    private var sdkScope: CoroutineScope? = null
    /** Application context giữ lại để dựng lại đồ thị DI mà không cần host truyền lại. */
    private var appContext: Context? = null

    /**
     * Cấu hình **cố định**, chốt ở lần [initialize] **đầu tiên**. Các lần [initialize] sau (host init lại
     * mỗi khi login) chỉ áp field **động** (customerId/accessToken/availableServices); nếu host lỡ truyền
     * field cố định khác đi → SDK cảnh báo và **bỏ qua**. Muốn đổi thật thì [release] rồi init lại.
     */
    private data class FixedConfig(
        val baseUrl: String,
        val language: String,
        val environment: PromotionEnvironment,
    )
    private var fixedConfig: FixedConfig? = null

    // ─── Init ────────────────────────────────────────────────────────────────

    /**
     * Khởi tạo **tối giản** — đủ cho phần lớn host: chỉ customerId + token + baseUrl.
     * `availableServices`/`theme`/`callback` là tuỳ chọn; cần cấu hình sâu hơn thì dùng overload
     * [initialize] nhận [PromotionSDKOptions]. Đối ứng `PromotionSDK.initialize(...)` phẳng bên iOS.
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        context: Context,
        customerId: String,
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
            session = PromotionSessionConfig(customerId, accessToken, baseUrl, language, environment),
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
     * truyền lại field **động** (`customerId` / `accessToken` / `availableServices`); SDK **bỏ qua** mọi
     * thay đổi ở field cố định (có cảnh báo log). Muốn đổi cấu hình cố định thật → [release] rồi init lại.
     */
    @JvmStatic
    fun initialize(context: Context, options: PromotionSDKOptions) {
        val incoming = options.session
        val locked = fixedConfig
        if (isInitialized() && locked != null) {
            // Login lại: field cố định đã khoá. Cảnh báo nếu host truyền khác, rồi giữ nguyên bản khoá.
            if (locked.baseUrl != incoming.baseUrl ||
                locked.environment != incoming.environment ||
                locked.language != incoming.language
            ) {
                Log.w(
                    TAG,
                    "initialize() được gọi lại với field cố định khác (baseUrl/environment/language). " +
                        "Các field này chốt ở lần initialize() đầu và bị bỏ qua. Gọi release() trước nếu muốn đổi.",
                )
            }
            if (options.callback != null) callback = options.callback
            // Chỉ áp field động; ép field cố định về bản đã khoá. Context đơn hàng reset (phiên mới).
            applySession(
                context,
                incoming.copy(
                    baseUrl = locked.baseUrl,
                    language = locked.language,
                    environment = locked.environment,
                ),
                options.availableServices,
                keepOrderContext = false,
            )
            return
        }

        // Lần đầu (hoặc sau release): chốt field cố định + dựng đồ thị DI đầy đủ.
        if (isInitialized()) release()
        appContext = context.applicationContext
        callback = options.callback
        fixedConfig = FixedConfig(incoming.baseUrl, incoming.language, incoming.environment)
        mutableContext = PromotionMutableContext(incoming, options.availableServices)
        PromotionContainer.initialize(context, options.toCoreConfig(mutableContext))
        // Host truyền theme → ghi đè và lưu. Không truyền → khôi phục theme đã lưu lần trước.
        // Nhờ vậy host cấu hình một lần; các lần mở app sau chỉ cần initialize(config), theme tự sống lại.
        val resolved = options.theme
        if (resolved != null) PromotionThemeStore.save(resolved)
        PromotionThemeRegistry.configure(resolved ?: PromotionThemeStore.load())
        sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        // Nạp cờ tính năng từ server. `refresh()` không ném lỗi: hỏng thì giữ cache (fail-open).
        sdkScope?.launch { PromotionFeatureGate.refresh() }
    }

    /**
     * **Đăng nhập user mới** sau khi đã [initialize] một lần — chỉ truyền field **động**
     * (`customerId` + `accessToken` + `availableServices`); SDK **giữ nguyên** field cố định đã khoá
     * (baseUrl / environment / language / theme) + callback. Đây là lối chính cho host: gọi [initialize]
     * **một lần** lúc mở app, mỗi lần login sau chỉ gọi [updateSession].
     *
     * @param availableServices Danh mục dịch vụ cho phiên mới; bỏ trống (`null`) = giữ danh mục hiện tại.
     *
     * Context đơn hàng/dịch vụ reset về rỗng vì đây là phiên mới. Đối ứng `updateSession(...)` bên iOS.
     *
     * @throws IllegalStateException nếu [initialize] chưa được gọi.
     */
    @JvmStatic
    @JvmOverloads
    fun updateSession(
        customerId: String,
        accessToken: String,
        availableServices: List<PromotionAvailableService>? = null,
    ) {
        val ctx = checkNotNull(mutableContext) {
            "PromotionSDK.initialize() must be called before updateSession()."
        }
        val context = checkNotNull(appContext) { "Application context missing — call initialize() first." }
        applySession(
            context,
            ctx.session.copy(customerId = customerId, accessToken = accessToken),
            availableServices ?: ctx.availableServices,
            keepOrderContext = false,
        )
    }

    /**
     * Refresh access token **giữa phiên** (cùng customer, không đổi login) — nhẹ hơn [updateSession]:
     * **giữ nguyên** cả context đơn hàng đang ghi (dùng khi token hết hạn giữa checkout).
     * Đối ứng `updateToken(_:)` bên iOS.
     *
     * @throws IllegalStateException nếu [initialize] chưa được gọi.
     */
    @JvmStatic
    fun updateToken(accessToken: String) {
        val ctx = checkNotNull(mutableContext) {
            "PromotionSDK.initialize() must be called before updateToken()."
        }
        val context = checkNotNull(appContext) { "Application context missing — call initialize() first." }
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
        sdkScope?.launch { PromotionFeatureGate.refresh() }
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
        // Reset registry trong bộ nhớ; **không** xoá theme đã lưu — nó sống qua release/init.
        PromotionThemeRegistry.configure(null)
        callback = null
        mutableContext = null
        appContext = null
        // Mở khoá cấu hình cố định: initialize() kế tiếp được coi là "lần đầu" và chốt lại từ đầu.
        fixedConfig = null
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
     * Thay cho `useCases: PromotionUseCases` trước đây — kiểu đó thuộc `promotionLogic`, mà host
     * chỉ tích hợp `AndroidPromotionSDK` nên không resolve được. Xem [PromotionSDKApi].
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

    /** Giá trị dynamic hiện tại được ghi qua [updateContext]. Null khi chưa [updateContext]. */
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
     * @param orderItems Dòng sản phẩm của đơn — cần khi muốn lấy campaign theo SKU; bỏ trống thì
     * chỉ nhận campaign cấp đơn. Đối ứng `PromotionSDK.updateContext(orderItems:)` bên iOS.
     *
     * @throws IllegalStateException nếu [initialize] chưa được gọi.
     */
    @JvmStatic
    @JvmOverloads
    fun updateContext(
        orderId: String? = null,
        orderValue: String? = null,
        serviceCode: String? = null,
        metaData: String? = null,
        orderItems: List<PromotionOrderItem> = emptyList(),
    ) {
        val ctx = checkNotNull(mutableContext) {
            "PromotionSDK.initialize() must be called before updateContext()."
        }
        ctx.orderId = orderId
        ctx.orderValue = orderValue
        ctx.serviceCode = serviceCode
        ctx.metaData = metaData
        ctx.orderItems = orderItems
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
    // SDK **không** phơi API hỏi feature flag ra host. Host không cần biết cờ nào đang bật: mọi
    // điểm vào đều tự gác qua `PromotionFeatureGate` của `promotionLogic` — `openMyPromotion`,
    // `PRMBaseFragment.openPromotionDetail`, `PRMEndowView` — và hiện thông báo PRM_MOB_021 khi bị
    // chặn. Trước đây có `PromotionSDK.featureFlags`, nhưng không nơi nào dùng.

    // ─── Screens ─────────────────────────────────────────────────────────────

    /**
     * Hiển thị màn "Ưu đãi của tôi" ([MyPromotionFragment]).
     *
     * Gác bởi cờ [PromotionFeatureFlag.VOUCHER_LIST]: TẮT → hiện thông báo PRM_MOB_021 và **không**
     * mở màn, y như `PromotionSDK.openMyPromotion` bên iOS.
     *
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param containerViewId Nếu khác null, dùng FragmentTransaction.replace trên container này;
     * nếu null, fragment được add lên android.R.id.content.
     *
     * @throws IllegalStateException khi chưa gọi [initialize].
     */
    @JvmStatic
    @JvmOverloads
    fun openMyPromotion(activity: FragmentActivity, containerViewId: Int? = null) {
        check(PromotionContainer.isInitialized()) {
            "PromotionSDK.initialize() must be called before openMyPromotion()."
        }
        if (!PromotionFeatureGate.canOpenVoucherList()) {
            Toast.makeText(activity, R.string.prm_feature_disabled, Toast.LENGTH_SHORT).show()
            callback?.onAvailabilityChanged(false)
            return
        }
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(TAG_MY_PROMOTION) != null) return
        val fragment = MyPromotionFragment()
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                if (containerViewId != null) {
                    replace(containerViewId, fragment, TAG_MY_PROMOTION)
                } else {
                    add(android.R.id.content, fragment, TAG_MY_PROMOTION)
                }
            }
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
     * @param voucherId Id voucher cần xem.
     * @param activity Activity host (FragmentActivity / AppCompatActivity).
     * @param containerViewId Khác null → `replace` trên container này; null → `add` lên
     * `android.R.id.content`. Cùng quy ước với [openMyPromotion].
     *
     * @throws IllegalStateException khi chưa gọi [initialize].
     */
    @JvmStatic
    @JvmOverloads
    fun openPromotionDetail(
        voucherId: String,
        activity: FragmentActivity,
        containerViewId: Int? = null,
    ) {
        check(PromotionContainer.isInitialized()) {
            "PromotionSDK.initialize() must be called before openPromotionDetail()."
        }
        if (!PromotionFeatureGate.canOpenVoucherDetail()) {
            Toast.makeText(activity, R.string.prm_feature_disabled, Toast.LENGTH_SHORT).show()
            callback?.onAvailabilityChanged(false)
            return
        }
        val fm = activity.supportFragmentManager
        if (fm.findFragmentByTag(TAG_PROMOTION_DETAIL) != null) return
        val fragment = PromotionDetailFragment.newInstance(voucherId)
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .apply {
                if (containerViewId != null) {
                    replace(containerViewId, fragment, TAG_PROMOTION_DETAIL)
                } else {
                    add(android.R.id.content, fragment, TAG_PROMOTION_DETAIL)
                }
            }
            .addToBackStack(TAG_PROMOTION_DETAIL)
            .commit()
    }
}
