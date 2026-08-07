package com.ttcn.prm.entry

// Extension ở androidMain của promotionLogic: nạp applicationContext + suy ra isDebug.
import android.content.Context
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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
import com.ttcn.prm.ui.base.PromotionToastGate
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
import kotlinx.coroutines.withContext

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
     * mỗi khi login) chỉ áp field **động** (accessToken/availableServices); nếu host lỡ truyền
     * field cố định khác đi → SDK cảnh báo và **bỏ qua**. Muốn đổi thật thì [release] rồi init lại.
     */
    private data class FixedConfig(
        val baseUrl: String,
        val language: String,
        val environment: PromotionEnvironment,
    )
    private var fixedConfig: FixedConfig? = null

    /**
     * Gác "đã [initialize] chưa" cho các **điểm mở màn**: chưa init → ghi `Log.e` rồi trả `false` để
     * nơi gọi `return`, **không ném**. Đối ứng `requireImpl(_:)` bên iOS (cũng log + trả `nil`).
     */
    private fun requireInitialized(caller: String): Boolean {
        if (PromotionContainer.isInitialized()) return true
        Log.e(TAG, "$caller bị gọi trước initialize() — bỏ qua. Hãy gọi PromotionSDK.initialize(...) trước.")
        return false
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
        // Nạp xong mới báo host: đây là lúc đầu tiên biết chắc SDK có được bật hay không.
        sdkScope?.launch {
            PromotionFeatureGate.refresh()
            notifyAvailability()
        }
    }

    /**
     * **Đăng nhập user mới** sau khi đã [initialize] một lần — chỉ truyền field **động**
     * (`accessToken` + `availableServices` + `callback`); SDK **giữ nguyên** field cố định đã khoá
     * (baseUrl / environment / language / theme). Đây là lối chính cho host: gọi [initialize]
     * **một lần** lúc mở app, mỗi lần login sau chỉ gọi [updateSession].
     *
     * @param availableServices Danh mục dịch vụ cho phiên mới; bỏ trống (`null`) = giữ danh mục hiện tại.
     * @param callback Nơi nhận sự kiện cho phiên mới; bỏ trống (`null`) = **giữ nguyên** callback hiện tại
     *   (giống [availableServices]). Không có đường "gỡ callback" ở đây — muốn gỡ thì [release].
     *   Dùng khi host thay object nghe sự kiện theo user đang đăng nhập, thay vì phải gọi lại [initialize].
     *
     * Context đơn hàng/dịch vụ reset về rỗng vì đây là phiên mới. Đối ứng `updateSession(...)` bên iOS.
     *
     * @throws IllegalStateException nếu [initialize] chưa được gọi.
     */
    @JvmStatic
    @JvmOverloads
    fun updateSession(
        accessToken: String,
        availableServices: List<PromotionAvailableService>? = null,
        callback: PromotionSDKCallback? = null,
    ) {
        val ctx = checkNotNull(mutableContext) {
            "PromotionSDK.initialize() must be called before updateSession()."
        }
        val context = checkNotNull(appContext) { "Application context missing — call initialize() first." }
        // Gán TRƯỚC applySession: nạp lại cờ tính năng ở cuối applySession sẽ bắn
        // `onAvailabilityChanged` của phiên mới — phải về callback mới, không phải callback của user cũ.
        if (callback != null) this.callback = callback
        applySession(
            context,
            ctx.session.copy(accessToken = accessToken),
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
     * Sau mỗi lần nạp, SDK báo lại công tắc tổng qua [PromotionSDKCallback.onAvailabilityChanged].
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
            onComplete?.let { done ->
                CoroutineScope(Dispatchers.Main).launch { done(PromotionFeatureFlagsSnapshot.AllEnabled) }
            }
            return
        }
        scope.launch {
            PromotionFeatureGate.refresh()
            val flags = featureFlags()
            withContext(Dispatchers.Main) {
                callback?.onAvailabilityChanged(flags.all)
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
        withContext(Dispatchers.Main) { cb.onAvailabilityChanged(enabled) }
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
    @JvmStatic
    @JvmOverloads
    fun openMyPromotion(activity: FragmentActivity, containerViewId: Int? = null) {
        if (!requireInitialized("openMyPromotion()")) return
        if (!PromotionFeatureGate.canOpenVoucherList()) {
            // Toast PRM_MOB_021 LUÔN hiện, không qua cổng toast chung — user bấm mà màn không mở.
            PromotionToastGate.showFeatureDisabled(activity)
            callback?.onAvailabilityChanged(false)
            return
        }
        val fm = activity.supportFragmentManager
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
     *         activity.supportFragmentManager.popBackStack()   // host tự đóng
     *         goToCheckout(detail)
     *     },
     * )
     * ```
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
    ) {
        if (!requireInitialized("openPromotionDetail()")) return
        if (!PromotionFeatureGate.canOpenVoucherDetail()) {
            // Toast PRM_MOB_021 LUÔN hiện, không qua cổng toast chung — user bấm mà màn không mở.
            PromotionToastGate.showFeatureDisabled(activity)
            callback?.onAvailabilityChanged(false)
            return
        }
        val fm = activity.supportFragmentManager
        // TEMP DEBUG: log ngay giá trị containerViewId THẬT SỰ nhận được từ host, trước khi làm gì khác.
        val resolvedView = containerViewId?.let { activity.findViewById<android.view.View>(it) }
        Log.d(
            "PRMSystemBack",
            "[PromotionSDK.openPromotionDetail] voucherId=$voucherId " +
                "containerViewId=${containerViewId?.let { "0x" + it.toString(16) } ?: "null"} " +
                "resolvedView=${resolvedView?.let { it::class.java.name + "@" + System.identityHashCode(it).toString(16) } ?: "NOT FOUND"} " +
                "fm=${System.identityHashCode(fm).toString(16)} " +
                "existingTagFragment=${fm.findFragmentByTag(TAG_PROMOTION_DETAIL)}"
        )
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
        Log.d(
            "PRMSystemBack",
            "[PromotionSDK.openPromotionDetail] commit() called (async), fragment=${System.identityHashCode(fragment).toString(16)}"
        )
    }


    private fun FragmentTransaction.addOrHideThenAdd(
        fm: FragmentManager,
        containerViewId: Int?,
        fragment: Fragment,
        tag: String,
    ): FragmentTransaction = apply {
        if (containerViewId != null) {
            val existing = fm.findFragmentById(containerViewId)
            Log.d(
                "PRMSystemBack",
                "[addOrHideThenAdd] containerViewId=0x${containerViewId.toString(16)} != null -> " +
                    "existingFragmentInContainer=$existing -> add(0x${containerViewId.toString(16)}, ${tag})"
            )
            existing?.takeIf { it.isAdded && !it.isHidden }?.let { hide(it) }
            add(containerViewId, fragment, tag)
        } else {
            Log.d("PRMSystemBack", "[addOrHideThenAdd] containerViewId == null -> add(android.R.id.content, $tag)")
            add(android.R.id.content, fragment, tag)
        }
    }
}
