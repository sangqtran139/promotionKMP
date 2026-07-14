# PublicApi — Bề mặt SDK cho app host

Đây là **toàn bộ** những gì đối tác nhìn thấy. Mọi thứ khác là nội bộ.

SDK có **hai bề mặt**, đừng lẫn:

| | Ai gọi | Ở đâu | Type |
|---|---|---|---|
| **Bề mặt host** | App đối tác | `AndroidPromotionUI` / `PromotionSDKUI` | DTO riêng của UI SDK |
| **Bề mặt lõi** | UI của chính SDK | `promotionLogic` | `PromotionUseCases`, `PromotionResult`, domain model |

Bề mặt lõi được tài liệu ở [HeadlessAPI.md](./HeadlessAPI.md). **Host không với tới được nó** — đó là
chủ đích, không phải quy ước lỏng lẻo:

- **Android** — `AndroidPromotionUI` khai `implementation(projects.promotionLogic)`, nên
  `com.ttcn.promotionsdk.core.*` nằm ngoài compile classpath của host. Thử import là lỗi compile:
  `Unresolved reference 'PromotionUseCases'`.
- **iOS** — `PromotionSDKUI` khai `@_implementationOnly import PromotionKit`. Type Kotlin lọt vào chữ
  ký public sẽ bị ghi vào `.swiftinterface` và app host không build được:
  `error: Unable to find module dependency: 'PromotionKit'`.

Hệ quả: **mọi model của lõi phải được map sang DTO** trước khi ra tới host. Nơi làm việc đó là
`PromotionSDKApi` — xem §3.

---

## 1. Vòng đời

### Android

```kotlin
object PromotionSDK {
    fun init(context: Context, options: PromotionSDKOptions)
    fun isInitialized(): Boolean
    fun release()

    val api: PromotionSDKApi                        // bề mặt headless; ném IllegalStateException nếu chưa init
    val requestContext: PromotionContextProvider?   // đúng object host đã truyền vào config

    fun getTheme(): PromotionSDKTheme
    fun getCallback(): PromotionSDKCallback?
    fun openMyPromotion(activity: FragmentActivity, containerViewId: Int? = null)
    fun openPromotionDetail(activity: FragmentActivity, voucherId: String, containerViewId: Int? = null)
}
```

`openPromotionDetail` mở **thẳng** màn chi tiết theo `voucherId`, không qua danh sách — dùng khi host
đã biết id (bấm push notification, deeplink từ banner ngoài SDK). Đối ứng
`sdk.openPromotionDetail(voucherId:from:)` bên iOS. Cả hai đều gác bởi cờ `VOUCHER_DETAIL` y như
đường vào nội bộ: kill-switch không có cửa sau chỉ vì host gọi thẳng entry.

```kotlin
PromotionSDK.init(
    context,
    PromotionSDKOptions(
        config = PromotionConfig(
            apiKey = "...",
            baseUrl = "https://...",
            contextProvider = object : PromotionContextProvider {
                override fun getCustomerId() = "CUST-001"
                override fun getAccessToken() = token
                override fun getOrderId() = orderId
                override fun getOrderValue() = orderValue
            },
            availableServices = listOf(PromotionAvailableService("P-FOOD-001", "Mua đồ ăn")),
        ),
    ),
)
```

`PromotionConfig` là bản sao public của `PromotionSDKConfig` ở lõi. Nó **không** có `isDebug`:
`PromotionSDK.init` suy ra từ `ApplicationInfo.FLAG_DEBUGGABLE` của host.

`contextProvider` được **uỷ quyền, không sao chép** — host refresh token là SDK thấy ngay ở request
kế tiếp, khỏi `init` lại.

### iOS

```swift
let sdk = PromotionSDK(customerId: "CUST-001", token: token,
                       orderId: orderId, orderValue: orderValue,
                       availableServices: [PromotionAvailableService(serviceCode: "P-FOOD-001", serviceName: "Mua đồ ăn")])
sdk.delegate = self          // PromotionSDKCallback
let api = sdk.api            // PromotionSDKApi
```

> **Mô hình vòng đời hai bên KHÁC nhau, và đây là chỗ đối tác dễ vấp.**
> Android đọc `customerId`/`token`/`orderId` từ `PromotionContextProvider` ở **mỗi** lời gọi.
> iOS chốt chúng vào instance lúc khởi tạo — đổi token thì phải tạo lại `PromotionSDK`.
> Hai bề mặt API giống hệt nhau; vòng đời thì không.

---

## 2. Feature flag — SDK **không** phơi ra

Host không cần biết cờ nào đang bật. Mọi điểm vào tự gác qua `PromotionFeatureGate` của lõi
(`openMyPromotion`, mở chi tiết, widget) và báo lại khi bị chặn: Toast + `PRM_MOB_021` (Android),
popup + `vdsPromotion(_:didUpdateAvailability:)` (iOS). Xem [HeadlessAPI.md §4](./HeadlessAPI.md).

---

## 3. `PromotionSDKApi` — ranh giới, không phải use case

Nó **không** chứa nghiệp vụ. Gác cờ, bắt lỗi, chuẩn hoá `errorCode` đều nằm trong `PromotionUseCases`
của lõi, dùng chung hai nền tảng. Lớp này làm đúng một việc: **uỷ quyền rồi map** — model lõi → DTO,
`PromotionResult` → `PromotionApiResult`.

Hai file dưới đây là **song ánh**. Cùng tên type, cùng tên field, cùng thứ tự khai báo, cùng tên
tham số, cùng cách xử lý `NO_RESULT`. **Sửa một bên thì sửa cả hai.**

| Android `ui/entry/api/` | iOS `PromotionSDK/Entry/API/` |
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
| `AppliedDiscount` | Ưu đãi đã validate. Đi qua `PromotionSDKCallback.onVoucherApplied` và `PRMEndowView.setDiscountDetails`. |
| `PromotionSDKCallback` | `onVoucherApplied` / `onError` / `onSDKClosed`. |
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
