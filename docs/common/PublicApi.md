# PublicApi — Bề mặt SDK cho app host

Đây là **toàn bộ** những gì đối tác nhìn thấy. Mọi thứ khác là nội bộ.

SDK có **hai bề mặt**, đừng lẫn:

| | Ai gọi | Ở đâu | Type |
|---|---|---|---|
| **Bề mặt host** | App đối tác | `AndroidPromotionSDK` / `PromotionSDKUI` | DTO riêng của UI SDK |
| **Bề mặt lõi** | UI của chính SDK | `promotionLogic` | `PromotionUseCases`, `PromotionResult`, domain model |

Bề mặt lõi được tài liệu ở [HeadlessAPI.md](./HeadlessAPI.md). **Host không với tới được nó** — đó là
chủ đích, không phải quy ước lỏng lẻo:

- **Android** — `AndroidPromotionSDK` khai `implementation(projects.promotionLogic)`, nên
  `com.ttcn.promotionsdk.core.*` nằm ngoài compile classpath của host. Thử import là lỗi compile:
  `Unresolved reference 'PromotionUseCases'`.
- **iOS** — `PromotionSDKUI` khai `@_implementationOnly import PRMKotlinBridge`. Type Kotlin lọt vào chữ
  ký public sẽ bị ghi vào `.swiftinterface` và app host không build được:
  `error: Unable to find module dependency: 'PRMKotlinBridge'`.

Hệ quả: **mọi model của lõi phải được map sang DTO** trước khi ra tới host. Nơi làm việc đó là
`PromotionSDKApi` — xem §3.

---

## 1. Vòng đời

### Android

```kotlin
object PromotionSDK {
    // Khởi tạo tối giản (đủ cho phần lớn host — chỉ 2 tham số bắt buộc):
    fun initialize(context: Context, accessToken: String, baseUrl: String,
                   environment: PromotionEnvironment = PROD, language: String = "vi-VN",
                   availableServices: List<PromotionAvailableService> = emptyList(),
                   theme: PromotionSDKTheme? = null, callback: PromotionSDKCallback? = null)
    // Khởi tạo đầy đủ:
    fun initialize(context: Context, options: PromotionSDKOptions)
    // Login lại (lối chính): chỉ field động; giữ field cố định đã khoá (baseUrl/env/language/theme).
    // availableServices null = giữ danh mục hiện tại; callback null = giữ callback hiện tại.
    // Gọi lại initialize() cũng được (guard cùng cơ chế).
    fun updateSession(accessToken: String,
                      availableServices: List<PromotionAvailableService>? = null,
                      callback: PromotionSDKCallback? = null)
    fun updateToken(accessToken: String)                        // (tuỳ chọn) refresh token giữa phiên, giữ context đơn hàng
    fun release()
    fun isInitialized(): Boolean
    fun getCallback(): PromotionSDKCallback?

    val api: PromotionSDKApi                        // bề mặt headless; ném IllegalStateException nếu chưa initialize

    val session: PromotionSessionConfig?            // session đã truyền lúc initialize
    val currentOrderId: String?
    val currentOrderValue: String?
    val currentServiceCode: String?
    val currentMetaData: String?
    fun updateContext(orderId: String? = null, orderValue: String? = null,
                      serviceCode: String? = null, metaData: String? = null,
                      orderItems: List<PromotionOrderItem> = emptyList())

    fun configure(theme: PromotionSDKTheme?)
    fun currentTheme(): PromotionSDKTheme?

    // Feature flag — fail-open, không hàm nào ném lỗi (xem §2)
    fun featureFlags(): PromotionFeatureFlagsSnapshot          // cache đồng bộ, không gọi mạng
    fun isFeatureEnabled(feature: PromotionFeature): Boolean
    fun isSdkEnabled(): Boolean                                // công tắc tổng ENABLE_ALL
    fun refreshFeatureFlags(onComplete: ((PromotionFeatureFlagsSnapshot) -> Unit)? = null)

    fun openMyPromotion(activity: FragmentActivity, containerViewId: Int? = null)
    fun openPromotionDetail(
        voucherId: String,
        activity: FragmentActivity,
        containerViewId: Int? = null,
        returnVoucherOnApply: Boolean = true,
        hostHandlesDismiss: Boolean = false,
        onVoucherApplied: ((detail: PromotionVoucherDetail) -> Unit)? = null,
    )
}
```

> Thứ tự thành viên + tên hàm **khớp 1:1** với `PromotionSDK` bên iOS. Xem [InitParity.md](./InitParity.md).

`openPromotionDetail` mở **thẳng** màn chi tiết theo `voucherId`, không qua danh sách — dùng khi host
đã biết id (bấm push notification, deeplink từ banner ngoài SDK). Đối ứng
`sdk.openPromotionDetail(voucherId:from:returnVoucherOnApply:onVoucherApplied:)` bên iOS. Cả hai đều
gác bởi cờ `VOUCHER_DETAIL` y như đường vào nội bộ: kill-switch không có cửa sau chỉ vì host gọi
thẳng entry.

`returnVoucherOnApply` quyết định nhãn nút + hành vi khi bấm (TLNV MOB_002 control #5):

| Giá trị | Nút | Bấm thì |
|---|---|---|
| `true` (**mặc định**) | "Áp dụng" | trả **object `PromotionVoucherDetail`** về `onVoucherApplied` rồi SDK tự đóng màn |
| `false` | "Dùng ngay" | SDK mở bottom sheet chọn dịch vụ → `PromotionSDKCallback.onServiceSelected` |

Đây là **kênh trả duy nhất gắn với lời gọi**, khác `PromotionSDKCallback` là singleton set một lần
lúc `initialize` và không mang thông tin màn nào đã mở SDK. Nhờ vậy màn host bất kỳ — không cần là
màn thanh toán — đều mở được chi tiết rồi nhận voucher về đúng chỗ.

Callback trả **cả object `PromotionVoucherDetail`** (id, merchantName, title, description, guideline,
startDate/expireDate, bannerURL/logoURL, status, displayStatusLabel, codes, usageGuideUrl) — đúng type
`api.getVoucherDetail` trả, nên host không phải gọi API lần nữa. Phép map domain → DTO dùng chung với
headless: `VoucherDetail.toPublicDetail()` (Android) / `PromotionSDKApi.toVoucherDetail(_:)` (iOS),
đặt ở **ranh giới public**; tầng UI nội bộ vẫn chuyền `VoucherDetail` của lõi.

> Object chỉ có sau khi API chi tiết trả về. Nút "Áp dụng" bị khoá trước đó nên bình thường luôn có;
> trường hợp bất thường SDK **bỏ qua callback** thay vì trả object rỗng.

`hostHandlesDismiss` (mặc định `false`) quyết định **ai đóng màn chi tiết** sau khi bấm "Áp dụng":

| Giá trị | SDK làm gì |
|---|---|
| `false` (**mặc định**) | Gọi `onVoucherApplied` rồi **tự pop** màn chi tiết |
| `true` | Gọi `onVoucherApplied` rồi **để nguyên màn** — host tự đóng |

Callback luôn chạy **trước** khi pop, nên ở chế độ `true` host vẫn còn màn để tự xử lý: hỏi xác nhận,
chạy animation riêng, hoặc đẩy thẳng sang màn khác thay vì quay lại màn cũ.

```kotlin
// Android — host tự đóng
PromotionSDK.openPromotionDetail(voucherId, activity, hostHandlesDismiss = true) { detail ->
    activity.supportFragmentManager.popBackStack()
    goToCheckout(detail)
}
```
```swift
// iOS
PromotionSDK.openPromotionDetail(voucherId: id, from: self, hostHandlesDismiss: true) { [weak self] detail in
    self?.navigationController?.popViewController(animated: true)
    self?.goToCheckout(detail)
}
```

Chỉ có nghĩa khi `returnVoucherOnApply == true` — nhánh "Dùng ngay" không đóng màn bao giờ.

> ⚠️ **Mặc định là `true`**, tức đổi hành vi so với bản trước (trước đây luôn là "Dùng ngay" + chọn
> dịch vụ). Host đang gọi `openPromotionDetail` mà muốn giữ hành vi cũ phải truyền
> `returnVoucherOnApply = false`.

```kotlin
PromotionSDK.initialize(
    context,
    PromotionSDKOptions(
        session = PromotionSessionConfig(
            accessToken = token,
            baseUrl = "https://...",
            language = "vi-VN",
            environment = PromotionEnvironment.PROD,
        ),
        availableServices = listOf(PromotionAvailableService("P-FOOD-001", "Mua đồ ăn")),
        callback = myCallback,
    ),
)
PromotionSDK.updateContext(orderId = orderId, orderValue = orderValue)   // cập nhật khi vào màn có voucher
val api = PromotionSDK.api
```

Order/dịch vụ **động** đi qua `updateContext` (ghi vào `PromotionMutableContext`, lõi đọc lại ở **mỗi**
request) — không cần `initialize` lại. Refresh token = `initialize` lại với session mới.

### iOS

```swift
PromotionSDK.initialize(
    options: PromotionSDKOptions(
        session: PromotionSessionConfig(
            accessToken: token,
            baseUrl: "https://...",
            language: "vi-VN",
            environment: .prod
        ),
        availableServices: [PromotionAvailableService(serviceCode: "P-FOOD-001", serviceName: "Mua đồ ăn")],
        callback: self          // PromotionSDKCallback
    )
)
PromotionSDK.updateContext(orderId: orderId, orderValue: orderValue)   // cập nhật khi vào màn có voucher
// cần campaign theo SKU → thêm orderItems: [PromotionOrderItem(...)]
let api = PromotionSDK.api      // PromotionSDKApi
```

> **Mô hình vòng đời hai bên giờ ĐỐI ỨNG nhau.** Cả hai đều là singleton tĩnh: cấu hình một lần qua
> `PromotionSDK.initialize(options:)` (iOS) / `PromotionSDK.initialize(context, options)` (Android), rồi cập
> nhật đơn hàng/dịch vụ qua `updateContext(...)` mà **không** init lại. Giá trị động nằm ở
> `PromotionMutableContext` — lõi đọc lại ở **mỗi** request, nên refresh token = `initialize`/`init`
> lại với session mới, còn order/service chỉ cần `updateContext`.

**`orderItems` (dòng sản phẩm / SKU).** Truyền qua `updateContext(orderItems = …)` ở **cả hai** nền
tảng khi cần campaign theo SKU; bỏ trống → chỉ nhận campaign cấp đơn. `ChoosePromotionStore` và
`EndowStore` đọc lại qua `PromotionRequestContextProvider.getOrderItems()`. iOS còn giữ thêm overload
tiện tay `createEndowView(from:orderId:orderValue:orderItems:)` (N1 — widget iOS là factory, xem
[InitParity §5.3](./InitParity.md#53-widget)).

---

## 2. Feature flag — SDK tự gác, host hỏi thêm được

**Tầng bắt buộc (SDK tự làm).** Mọi điểm vào tự gác qua `PromotionFeatureGate` của lõi
(`openMyPromotion`, mở chi tiết, widget) và báo lại khi bị chặn: toast `PRM_MOB_021` ở **cả hai nền
tảng**, kèm `onAvailabilityChanged(enabled:)`. Host không làm gì thì kill-switch vẫn chạy đủ.

**Tầng tuỳ chọn (host hỏi trước).** Bốn hàm ở bảng dưới cho host ẩn entry point của chính mình thay
vì để user bấm rồi ăn toast:

| Android | iOS |
|---|---|
| `PromotionSDK.featureFlags()` | `PromotionSDK.featureFlags()` |
| `PromotionSDK.isFeatureEnabled(PromotionFeature.VOUCHER_LIST)` | `PromotionSDK.isFeatureEnabled(.voucherList)` |
| `PromotionSDK.isSdkEnabled()` | `PromotionSDK.isSdkEnabled()` |
| `PromotionSDK.refreshFeatureFlags { flags -> … }` | `PromotionSDK.refreshFeatureFlags { flags in … }` |

Ba hàm đầu đọc **cache đồng bộ** (không gọi mạng); `refreshFeatureFlags` gọi server rồi trả snapshot
mới trên **main thread**. Tất cả **fail-open**: chưa `initialize` / chưa có cache → bật hết, và
không hàm nào ném lỗi.

DTO public: `PromotionFeature` (enum) + `PromotionFeatureFlagsSnapshot` — song ánh
`PromotionFeatureModels.kt` ↔ `PromotionFeatureModels.swift`, cùng lý do phải map như §3. Hằng chuỗi
`PromotionFeatureFlag` và data class `PromotionFeatureFlags` của lõi **không** ra tới host.

Chi tiết + bảng "khi nào `onAvailabilityChanged` bắn": [features/FeatureFlag.md §3](../features/FeatureFlag.md).
Xem thêm [HeadlessAPI.md §4](./HeadlessAPI.md).

---

## 3. `PromotionSDKApi` — ranh giới, không phải use case

Nó **không** chứa nghiệp vụ. Gác cờ, bắt lỗi, chuẩn hoá `errorCode` đều nằm trong `PromotionUseCases`
của lõi, dùng chung hai nền tảng. Lớp này làm đúng một việc: **uỷ quyền rồi map** — model lõi → DTO,
`PromotionResult` → `PromotionApiResult`.

Hai file dưới đây là **song ánh**. Cùng tên type, cùng tên field, cùng thứ tự khai báo, cùng tên
tham số, cùng cách xử lý `NO_RESULT`. **Sửa một bên thì sửa cả hai.**

| Android `ui/entry/api/` | iOS `PromotionSDKUI/Entry/API/` |
|---|---|
| `PromotionSDKApi.kt` | `PromotionSDKApi.swift` |
| `PromotionApiModels.kt` | `PromotionApiModels.swift` |
| `PromotionApiResult.kt` | `PromotionApiResult.swift` |

### 3.1. Năm hàm

```kotlin
suspend fun getVouchers(keyword: String? = null, serviceCode: String? = null, tab: String? = null,
                        page: Int = 0, size: Int = 10): PromotionApiResult<PromotionVoucherPage>

suspend fun findEligible(orderId: String, orderValue: String, items: List<PromotionOrderItem> = emptyList(),
                         tabCode: String? = null, myPage: Int = 0, mySize: Int = 10,
                         otherPage: Int = 0, otherSize: Int = 10): PromotionApiResult<PromotionEligibleResult>

suspend fun getVoucherDetail(voucherId: String, serviceCode: String? = null): PromotionApiResult<PromotionVoucherDetail>

suspend fun validateDiscounts(orderId: String, orderValue: String, voucherIds: List<String>,
                              objectType: String = "CAMPAIGN"): PromotionApiResult<PromotionValidationResult>

suspend fun createRedemption(orderId: String, orderValue: String, voucherIds: List<String>,
                             objectType: String = "CAMPAIGN"): PromotionApiResult<PromotionRedemptionResult>
```

Bên iOS y hệt, chỉ thay `suspend` bằng `completion: @escaping (PromotionApiResult<T>) -> Void`. Đó là
**khác biệt duy nhất được phép** giữa hai file.

`getVouchers` phân trang bằng `page`/`size`. `findEligible` dùng `myPage`/`otherPage` vì nó trả **hai
nhóm phân trang độc lập** — `myOffers` (voucher đã sở hữu) và `otherOffers` (campaign công khai).
Chọn hàm nào: xem [HeadlessAPI.md §3](./HeadlessAPI.md).

### 3.2. Kết quả và lỗi

```kotlin
sealed interface PromotionApiResult<out T> {
    data class Success<out T>(val data: T) : PromotionApiResult<T>
    data class Failure(val error: PromotionSDKError) : PromotionApiResult<Nothing>
}
```

iOS: `typealias PromotionApiResult<T> = Result<T, PromotionSDKError>` — Swift đã có `Result` sẵn,
đặt cùng tên để tài liệu hai bên đọc như một.

```kotlin
when (val r = PromotionSDK.api.getVouchers()) {
    is PromotionApiResult.Success -> render(r.data.vouchers)
    is PromotionApiResult.Failure -> when (r.error) {
        is PromotionSDKError.FeatureDisabled -> showUnavailable()
        is PromotionSDKError.NetworkFailure  -> showError(r.error.message)
        else                                 -> showError(r.error.message)
    }
}
```

Sáu nhánh lỗi, giống nhau hai bên: `NetworkFailure(code, message)`, `SessionExpired`, `Timeout`,
`ParseFailed`, `FeatureDisabled`, `Unknown(error)`.

- Kotlin: `sealed class PromotionSDKError : Exception()`; `message` là câu hiển thị được.
- Swift: `enum PromotionSDKError: Error, LocalizedError`; `errorDescription` ≡ `message` bên Kotlin.

**API không ném lỗi nghiệp vụ.** Chỉ `CancellationException` thoát ra, để structured concurrency của
host còn hoạt động.

`NO_RESULT` (server trả `data: null`) được xử lý theo ngữ cảnh, **không** đồng nhất:
danh sách → thành công với list rỗng; chi tiết / validate / redemption → `ParseFailed`.

### 3.3. DTO

Mười type, thứ tự khai báo trong file đúng như bảng này:

| Type | Ghi chú |
|---|---|
| `PromotionVoucher` | `id`, `merchantName`, `title`, `imageURL`, `expireDate`, `isUsed`, `status`, `displayStatusLabel` |
| `PromotionVoucherPage` | `vouchers`, `isLastPage` |
| `PromotionVoucherDetail` | thêm `description`, `guideline`, `startDate`, `bannerURL`, `logoURL` |
| `PromotionEligibleOffer` | `id` = `voucherId` nếu đã sở hữu, ngược lại `campaignId`; `usable = false` → hiển thị mờ |
| `PromotionEligibleResult` | `myOffers`, `otherOffers`, `myIsLastPage`, `otherIsLastPage` |
| `PromotionOrderItem` | `skuId`, `productId`, `productName`, `productCategory`, `quantity`, `unitPrice` |
| `PromotionAvailableService` | `serviceCode`, `serviceName`, `serviceType`, `iconUrl`. ⚠️ `serviceCode` phải khớp **`applicableProducts.productId`** của voucher (không phải `sku`) thì dịch vụ mới hiện ở bottom sheet "Chọn dịch vụ"; danh sách bị lọc trùng theo `serviceCode` nên mỗi `productId` chỉ khai **một** dòng, kể cả khi nó gắn nhiều SKU |
| `PromotionValidationResult` | `overallValid`, `totalDiscountAmount`, `finalAmount`, `items` |
| `PromotionDiscountItem` | `objectId`, `discountAmount`, `isValid`, `eligibilityStatus` |
| `PromotionRedemptionResult` | `sessionId`, `totalDiscount`, `finalAmount`, `validationErrors` |
| `PromotionRedemptionError` | `code`, `message` |

Hai quy ước đã chốt, đừng đảo lại:

1. **Ngày tháng là chuỗi thô của server**, cả hai nền tảng. Parse ở tầng này thì định dạng lạ sẽ trả
   `null`, và host không phân biệt được "voucher vô thời hạn" với "server trả sai định dạng". Định
   dạng ngày là việc của tầng hiển thị.
2. **Viết tắt để hoa**: `imageURL`, `bannerURL`, `logoURL` — theo Swift API Design Guidelines. Kotlin
   nhường ở đây, đổi lại iOS nhường ở `vouchers`/`isLastPage`/`description`/`page`/`size`.

---

## 4. Thành phần UI công khai (Android)

| Type | Dùng để |
|---|---|
| `PRMEndowView` | Widget ưu đãi ở màn thanh toán. Đặt thẳng vào XML của host. |
| `ChoosePromotionFragment.forEndowView(endowView)` | Mở màn "Chọn ưu đãi", nối sẵn với widget. |
| `PromotionIntegrateManager.create(endowView)` | Gọi `confirmRedemption(onSuccess, onError)` khi bấm thanh toán. |
| `AppliedDiscount` | Ưu đãi đã validate. Đi qua callback của `PRMEndowView` và `PRMEndowView.setDiscountDetails` (chi tiết giảm giá **không** qua `PromotionSDKCallback`). **Android-only, N1:** iOS không phơi type này — host iOS nhận `onVoucherApplied(voucherId)` rồi gọi `api.validateDiscounts(...)` nếu cần breakdown. Xem [InitParity.md §5.3](./InitParity.md#53-widget). |
| `PromotionSDKCallback` | Thống nhất với iOS (6 sự kiện): `onVoucherApplied(voucherId)` / `onVoucherCleared` / `onVoucherCountChanged` / `onServiceSelected` / `onAvailabilityChanged` / `onClosed`. Xem [InitParity.md §3](./InitParity.md). |
| `PromotionTheme` | Đổi theme sau `init`. Xem [Theming.md](./Theming.md). |

```kotlin
binding.endowView.onOpenVoucherSelection = {
    addFragment(ChoosePromotionFragment.forEndowView(binding.endowView))
}
```

`forEndowView` lấy lại ưu đãi widget đã tải (khỏi gọi `findEligible` lần hai), pre-select voucher
đang áp, và đẩy kết quả ngược về widget. Trước đây host phải tự làm bốn việc đó bằng tay — và phải
chạm vào `EligibleOffer`, một type của lõi.

> **Đang hỏng:** `PRMEndowView.onVoucherItemClick` khai báo rồi nhưng **không được gọi ở đâu cả** trong
> SDK. Wire vào cũng không bao giờ bắn. Bug có sẵn, chưa sửa.

---

## 5. Quy tắc khi mở rộng

1. **Không để type của `promotionLogic` xuất hiện trong chữ ký public.** Trên Android nó không gây
   lỗi lúc build SDK — lỗi chỉ nổ ở app host. Cách kiểm tra: thêm tạm một file vào `androidApp`
   import `com.ttcn.promotionsdk.core.*` và đổi `androidApp` sang `implementation(projects.androidPromotionUI)`;
   phải thấy `Unresolved reference`.
2. **Model MVI, adapter, ViewModel đều `internal`.** Chúng mang `EligibleOffer`, `VoucherItem`,
   `VoucherStatus` — type của lõi.
3. Thêm/đổi DTO hoặc hàm ở `PromotionSDKApi` → **sửa cả Kotlin lẫn Swift trong cùng một commit**, giữ
   nguyên tên và thứ tự. Cập nhật file này.
4. Đổi chữ ký public của iOS → app host phải `⇧⌘K` (Clean Build Folder). `build-xcframework.sh` đã tự
   dọn `SwiftExplicitPrecompiledModules`, nhưng cache của Xcode vẫn có thể nói dối nếu bạn build tay.
