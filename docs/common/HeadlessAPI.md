# HeadlessAPI — API của `:promotionLogic`

Bề mặt API không-UI của lõi. Mọi hàm nghiệp vụ trả `PromotionResult` — **không ném exception ra ngoài**.

> **Đây KHÔNG phải bề mặt cho app host.** Chỉ `AndroidPromotionSDK` và `PromotionSDKUI` gọi vào đây.
> Host không với tới được `com.ttcn.promotionsdk.core.*`: Android khai
> `implementation(projects.promotionLogic)`, iOS khai `@_implementationOnly import PRMKotlinBridge`.
>
> Thứ host gọi là `PRMSDK.api` (`PRMSDKApi`), nó uỷ quyền xuống đây rồi map sang DTO.
> Xem [PublicApi.md](./PublicApi.md).

---

## 1. Khởi tạo

```kotlin
// Android — BẮT BUỘC dùng overload có Context.
// Nó nạp applicationContext cho SharedPreferences và suy ra isDebug từ FLAG_DEBUGGABLE.
// Host không gọi trực tiếp: PRMSDK.initialize(context, options) làm việc này.
PromotionContainer.init(context, PromotionSDKConfig(apiKey = "...", baseUrl = "..."))

// Common / iOS
PromotionContainer.init(PromotionSDKConfig(apiKey = "...", baseUrl = "...", isDebug = false))
```

```kotlin
data class PromotionSDKConfig(
    val apiKey: String,
    val baseUrl: String,
    val requestContextProvider: PromotionRequestContextProvider? = null,
    val environment: SdkEnvironment = SdkEnvironment.PROD,
    val availableServices: List<AvailableService> = emptyList(),
    val isDebug: Boolean = false,   // bật log body HTTP
)
```

`PromotionRequestContextProvider` là cách host cấp token / customerId / ngôn ngữ cho SDK:

```kotlin
interface PromotionRequestContextProvider {
    fun getCustomerId(): String? = null
    fun getService(): String? = null
    fun getAccessToken(): String? = null
    fun getLanguage(): String? = null      // mặc định "vi-VN"
    fun getOrderId(): String? = null
    fun getOrderValue(): String? = null
}
```

Dọn dẹp: `PromotionContainer.clear()` — đóng `HttpClient` (nếu đã dựng), xoá registry DI,
nhả `applicationContext` trên Android.

---

## 2. `PromotionResult`

```kotlin
sealed interface PromotionResult<out T> {
    data class Success<out T>(val data: T) : PromotionResult<T>
    data class Failure(
        val errorCode: String,
        val message: String? = null,
        val httpStatus: Int? = null,
    ) : PromotionResult<Nothing>
}
```

```kotlin
when (val r = PromotionUseCases().searchVouchers(request)) {
    is PromotionResult.Success -> render(r.data)
    is PromotionResult.Failure -> showError(r.errorCode)
}
```

---

## 3. Năm use case nghiệp vụ — `PromotionUseCases()`

`PromotionContainer` chỉ lo `initialize` / config; nó **không** phơi ra use case. Dựng thẳng facade
sau khi đã `initialize(...)`: `PromotionUseCases()`.

| Hàm | Endpoint | Cờ gác | Dùng khi |
|---|---|---|---|
| `searchVouchers` | `GET .../customer-vouchers` | `VOUCHER_LIST` | Danh sách voucher khách **đã sở hữu** |
| `getVoucherDetail` | `GET .../customer-vouchers/{voucherId}` | `VOUCHER_DETAIL` | Chi tiết một voucher |
| `findEligible` | `POST .../redemption/eligible` | `VOUCHER_SELECTION` | Ưu đãi **đủ điều kiện cho một đơn hàng** (checkout) |
| `validateDiscounts` | `POST .../redemptions/validate/stackable-discounts` | `VOUCHER_APPLY` | Validate trước khi áp dụng |
| `createRedemption` | `POST .../redemptions/sessions` | `VOUCHER_REDEEM` | Tạo phiên thanh toán với voucher đã chọn |

> **Rule 2 API — `searchVouchers` vs `findEligible`.**
> `searchVouchers` chỉ trả voucher khách đã sở hữu, **không xét đơn hàng**.
> `findEligible` xét đơn hàng và trả **hai nhóm**: `myOffers` (đã sở hữu) và `otherOffers`
> (campaign công khai chưa sở hữu), phân trang độc lập.
> Màn checkout (`ChoosePromotion`, widget `PRMEndowView`) dùng `findEligible` trên **cả hai** nền tảng.
> `findEligible` **không nhận `keyword`** — ô tìm kiếm của màn "Chọn ưu đãi" vì thế chưa chạy.

> **Rule kill-switch.** Mỗi hàm bị gác bởi một feature flag. Cờ TẮT → trả ngay
> `PromotionResult.Failure(PromotionErrorCodes.FEATURE_DISABLED)` (= `PRM_MOB_021`), **không gọi mạng**.
> `PROMOTION.ENABLE_ALL` là công tắc tổng: tắt nó thì mọi cờ con đều tắt.
> **Fail-open**: chưa `initialize()` hoặc chưa có cache cờ → coi như bật hết.

### 3.1. `searchVouchers`

```kotlin
suspend fun searchVouchers(
    request: SearchCustomerVouchersRequest,
): PromotionResult<SearchCustomerVouchersResult>
```

`SearchCustomerVouchersRequest(customerId, keyword?, serviceCode?, tab?, page?, size?)`.
Trả `SearchCustomerVouchersResult { tabs, content, defaultTab, selectedTab, last, totalElements, … }`.
Kèm `resolveActiveTab(requestedTab?)` — quy tắc chọn tab active **dùng chung 2 nền tảng**
(`selectedTab` → `defaultTab` → tab client yêu cầu → tab đầu theo `order`); UI chỉ đọc, không tự resolve.

### 3.2. `getVoucherDetail`

```kotlin
suspend fun getVoucherDetail(
    voucherId: String,
    customerId: String,
    service: String? = null,
): PromotionResult<VoucherDetail>
```

### 3.3. `findEligible`

```kotlin
suspend fun findEligible(
    request: FindEligibleCampaignsRequest,
): PromotionResult<EligibleOffersResult>
```

```kotlin
val result = PromotionUseCases().findEligible(
    FindEligibleCampaignsRequest(
        customerId = "C-1",
        orderId = "ORDER-123",
        orderValue = "500000",
        items = listOf(EligibleOrderItem(skuId = "SKU-01", quantity = 1, unitPrice = "500000")),
        tabCode = null,                     // null → tab mặc định của server
        section = null,                     // null → cả hai nhóm; có giá trị → chỉ load-more nhóm đó
        myPage = 0, otherPage = 0,
    )
)
```

`EligibleOffersResult { myOffers, otherOffers, tabs, activeTab, myIsLastPage, otherIsLastPage, … }`.

Phân trang độc lập 2 nhóm dùng `request.forSectionPage(section, nextPage, currentMyPage, currentOtherPage)`
— rule **dùng chung Android & iOS**: chỉ nhóm được yêu cầu tiến sang `nextPage`, nhóm kia giữ trang hiện tại;
`section = null` (load đầu) → cả hai về `nextPage`. UI chỉ truyền trang hiện tại, không tự map `myPage`/`otherPage`.

Mỗi `EligibleOffer`:

- `id` — ưu tiên `voucherId` (nhóm của tôi), fallback `campaignId` (nhóm khác).
  Dùng `id` này cho `validateDiscounts` / `createRedemption`.
- `isOwnedVoucher` — true khi server trả kèm `voucherId`.
- `usable = false` nghĩa là server trả `displayMode = "DISABLED"` → **hiển thị mờ, không cho chọn**.
- Lý do chưa đủ điều kiện: đọc `minOrderValue` và `unmatchedRules`. **SDK không dựng sẵn câu tiếng Việt**
  — UI tự ghép, ví dụ `"Đơn tối thiểu ${format(minOrderValue)}đ để áp dụng"`.
- Bỏ `items` → chỉ nhận campaign cấp đơn, không có campaign yêu cầu SKU.
- Offer nào không có cả `voucherId` lẫn `campaignId` sẽ bị **loại bỏ** (không định danh được để redeem).

### 3.4. `validateDiscounts`

```kotlin
suspend fun validateDiscounts(
    request: ValidateDiscountsRequest,
): PromotionResult<ValidateDiscountsResult>
```

`ValidateDiscountsResult` có sẵn `validItems` / `invalidItems`; và diễn giải **per-offer** (lặp theo offer
đã chọn): `itemFor(objectId)`, `isValidFor(objectId)` (chỉ chặn khi server nói rõ `valid=false`),
`discountFor(objectId)` (lấy `calculatedDiscount` của dòng, thiếu thì fallback `totalDiscountAmount`).
**Cả Android lẫn iOS đều đi qua đúng bộ helper này** — Android qua ext `appliedDiscountFor`, iOS gọi trực tiếp.

> ⚠️ Fallback `discountFor → totalDiscountAmount` mang ngữ nghĩa **một offer** (total = giảm của offer đó).
> Hiện cả hai nền tảng gate chọn đơn nên đúng; khi bật multi-select cần xem lại fallback (tránh cộng nhầm total cho từng offer).

### 3.5. `createRedemption`

```kotlin
suspend fun createRedemption(
    request: CreateRedemptionRequest,
): PromotionResult<CreateRedemptionResult>
```

`CreateRedemptionResult` có sẵn `hasErrors` và `hasBudgetError` (mã `INSUFFICIENT_BUDGET`).
`idempotencyKey` được SDK sinh tự động (`kotlin.uuid.Uuid`) cho mỗi request.

---

## 4. Feature flag — `PromotionFeatureFlagUseCases()`

```kotlin
class PromotionFeatureFlagUseCases {
    suspend fun refresh()                                  // POST .../feature-flag/list
    fun isEnabled(featureName: String): Boolean
    fun flagsOf(featureNames: List<String>): List<FeatureFlag>
    fun all(): PromotionFeatureFlags
}
```

Các cờ (`PromotionFeatureFlag`): `ENABLE_ALL`, `VOUCHER_APPLY`, `VOUCHER_REDEEM`,
`VOUCHER_SELECTION`, `VOUCHER_DETAIL`, `VOUCHER_LIST`.

Ba hành vi cần nhớ:

1. **`ENABLE_ALL` là công tắc tổng.** Tắt nó thì mọi cờ con đều tắt, bất kể giá trị riêng.
2. **Chưa có cache → bật hết.** SDK không tự khoá tính năng khi chưa gọi được API lần nào.
3. **`refresh()` không ném lỗi.** Gọi API thất bại thì giữ nguyên cờ đang cache.

Cờ được ghi xuống `KeyValueStorage` nên lần mở app sau không phải chờ API.
Xem [StorageGuide.md](./StorageGuide.md).

### 4.1. Gác ở đâu

Nguồn sự thật là **một object Kotlin duy nhất**, `PromotionFeatureGate`, dùng chung cho cả hai nền tảng:

```kotlin
object PromotionFeatureGate {
    fun isEnabled(flagName: String): Boolean
    fun canOpenVoucherList(): Boolean
    fun canOpenVoucherDetail(): Boolean
    fun canShowVoucherSelection(): Boolean
    fun canApplyVoucher(): Boolean
    fun canRedeemVoucher(): Boolean
    suspend fun refresh()
}
```

Nó được hỏi ở **hai tầng**, vì có hai đường vào khác nhau:

| Tầng | Ai hỏi gate | Gác cái gì |
|---|---|---|
| Facade headless | `PromotionUseCases` | Năm hàm nghiệp vụ → `Failure(FEATURE_DISABLED)` |
| UI native | Điểm điều hướng | Mở màn, mở chi tiết, hiện widget |

ViewModel của UI native dựng thẳng use case đơn lẻ (`SearchCustomerVouchersUseCase()`) nên **không**
đi qua facade — đó là lý do tầng UI phải hỏi gate lần nữa, chứ không phải gác hai lần thừa.
Phần riêng của mỗi nền tảng chỉ còn cách **hiển thị** thông báo: Toast (Android) vs popup (iOS).

> Cạm bẫy đã gặp: Android nạp cờ lúc `PRMSDK.initialize()` rồi **không đọc lại ở đâu cả**.
> Tắt `VOUCHER_DETAIL` trên server thì iOS chặn màn chi tiết, Android vẫn vào bình thường.
> Nếu thêm một màn mới, thêm một hàm `canOpen…` vào gate — facade không thấy được điều hướng.

---

## 5. Ánh xạ với SDK cũ

Lõi này là **hợp** của hai SDK gốc — cả hai bên đều không mất tính năng khi chuyển sang.

| Use case | `ttcn-promotion-android-sdk` | `ttcn-promotion-ios-sdk` |
|---|---|---|
| `searchVouchers` | `searchVouchers` | `getVouchers` |
| `getVoucherDetail` | `getVoucherDetail` | `getVoucherDetail` |
| `validateDiscounts` | `validateDiscounts` | `validateDiscounts` |
| `createRedemption` | `createRedemption` | `createRedemption` |
| `findEligible` | *(không có)* | `findEligible` |
| Feature flag | 4 use case | `PromotionFeatureFlag` trong `CoreNetwork` |

---

## 6. Quy tắc khi mở rộng API

1. Thêm endpoint → `PromotionApiService` + `KtorPromotionApiService` + `PromotionRemoteDataSource`.
2. Thêm DTO ở `core/data/dto/<nhóm>/` kèm mapper `toXxx()`; **không** để DTO lọt lên Domain.
3. Thêm domain model ở `core/domain/model/<nhóm>/`, use case ở `core/domain/usecase/`.
4. Đăng ký use case trong `UseCaseModule`, phơi ra qua `PromotionUseCases`.
5. Viết test `commonTest` bằng `MockEngine` — chạy trên **cả** Android lẫn iOS.
6. Cập nhật file này + `NetworkingGuide.md` (AI_AGENT_RULES điều 8).
