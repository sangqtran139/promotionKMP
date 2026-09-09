# PublicApi — Bề mặt SDK cho app host

Đây là **toàn bộ** những gì đối tác nhìn thấy. Mọi thứ khác là nội bộ.

> **Luật một dòng: chỉ `Entry` mới public.**
>
> | | Bề mặt public | Mọi thứ khác |
> |---|---|---|
> | Android | `com.ttcn.prm.entry.**` (gồm `entry.api`) + `ui.theme.**` + `ui.feature.endowview` | `internal` |
> | iOS | `iosPromotionSDK/Entry/**` **+ `PromotionSDKUI/Theme/**`** trong module `PRM` | không có `public` |
>
> Không có ngoại lệ. Cần host dùng được cái gì thì **dời nó vào `entry`**, đừng nới `public` tại chỗ —
> xem §5. Kiểm tra bằng một lệnh:
>
> ```bash
> ./scripts/check-public-api.sh
> ```
>
> ⚠️ **Hai lệnh `grep` chép tay trước đây ở đây là SAI** — chúng chỉ loại trừ `/entry/` (Android) và
> không loại trừ gì (iOS), trong khi allowlist do chính bảng trên khai gồm ba nhánh Android và hai
> nhánh iOS. Chạy thật thì chúng **luôn đỏ 126 dòng** (Android 17 + iOS 109) dù code hoàn toàn đúng.
>
> Một gate luôn đỏ còn tệ hơn không có gate: người làm theo checklist chỉ có hai đường, và cả hai đều
> xấu — hoặc "sửa cho xanh" bằng cách đổi `public` → `internal` ở `ui/theme` (tức **xoá theme API
> khỏi bề mặt host**, breaking, mà trông y như một hành động tuân thủ rule), hoặc học được rằng gate
> này luôn đỏ nên bỏ qua, và từ đó mọi vi phạm **thật** cũng bị bỏ qua cùng.
>
> Bảng trên cũng thiếu `PromotionSDKUI/Theme/**` ở dòng iOS — đã bổ sung.
>
> Script trên chỉ soi **source**. `internal` của Kotlin không đi tới bytecode: nó biên dịch thành
> `public final class`, nên host viết Java hoặc dùng reflection vẫn với tới được. Phần bịt thêm ở
> tầng AAR — sources.jar và resource private — nằm ở §6.

SDK có **hai bề mặt**, đừng lẫn:

| | Ai gọi | Ở đâu | Type |
|---|---|---|---|
| **Bề mặt host** | App đối tác | `AndroidPromotionSDK` / `PromotionSDKUI` | DTO riêng của UI SDK |
| **Bề mặt lõi** | UI của chính SDK | `promotionLogic` | `PromotionUseCases`, `PromotionResult`, domain model |

Bề mặt lõi được tài liệu ở [HeadlessAPI.md](./HeadlessAPI.md). **Host không với tới được nó** — đó là
chủ đích, không phải quy ước lỏng lẻo:

- **Android** — `AndroidPromotionSDK` khai `implementation(projects.promotionLogic)`, nên
  `com.ttcn.promotionsdk.*` nằm ngoài compile classpath của host. Thử import là lỗi compile:
  `Unresolved reference 'PromotionUseCases'`.
- **iOS** — `PromotionSDKUI` khai `@_implementationOnly import PRMKotlinBridge`. Type Kotlin lọt vào chữ
  ký public sẽ bị ghi vào `.swiftinterface` và app host không build được:
  `error: Unable to find module dependency: 'PRMKotlinBridge'`.

Hệ quả: **mọi model của lõi phải được map sang DTO** trước khi ra tới host. Nơi làm việc đó là
`PromotionSDKApi` — xem §3.

## Mục lục

<!-- toc -->
- [1. Vòng đời](#1-vòng-đời)
  - [1.1. Android](#11-android)
  - [1.2. iOS](#12-ios)
- [2. Feature flag — SDK tự gác, host hỏi thêm được](#2-feature-flag--sdk-tự-gác-host-hỏi-thêm-được)
- [3. `PromotionSDKApi` — ranh giới, không phải use case](#3-promotionsdkapi--ranh-giới-không-phải-use-case)
  - [3.1. Năm hàm](#31-năm-hàm)
  - [3.2. Kết quả và lỗi](#32-kết-quả-và-lỗi)
  - [3.2.1. Ba gate giữ bề mặt public](#321-ba-gate-giữ-bề-mặt-public)
  - [3.3. DTO](#33-dto)
- [4. Thành phần UI công khai (Android)](#4-thành-phần-ui-công-khai-android)
- [5. Quy tắc khi mở rộng](#5-quy-tắc-khi-mở-rộng)
- [5b. Cái gì là "breaking" — danh sách chốt](#5b-cái-gì-là-breaking--danh-sách-chốt)
  - [5b.0. Bề mặt iOS là `@MainActor`](#5b0-bề-mặt-ios-là-mainactor)
- [6. Bịt kín ở tầng AAR (Android)](#6-bịt-kín-ở-tầng-aar-android)
  - [6.1. Không phát hành sources.jar](#61-không-phát-hành-sourcesjar)
  - [6.2. Resource private toàn bộ](#62-resource-private-toàn-bộ)
  - [6.3. Dư địa đã biết](#63-dư-địa-đã-biết)
<!-- /toc -->

---

## 1. Vòng đời

### 1.1. Android

```kotlin
object PromotionSDK {
    // Khởi tạo tối giản (đủ cho phần lớn host — chỉ 2 tham số bắt buộc):
    fun initialize(context: Context, tokenSource: PromotionTokenSource, baseUrl: String,
                   environment: PromotionEnvironment = PROD, language: String = "vi-VN",
                   availableServices: List<PromotionAvailableService> = emptyList(),
                   theme: PromotionSDKTheme? = null, callback: PromotionSDKCallback? = null)
    // Khởi tạo đầy đủ:
    fun initialize(context: Context, options: PromotionSDKOptions)
    // Login lại (lối chính): gọi lại initialize(). `updateSession`/`updateToken` ĐÃ BỎ.
    // baseUrl/environment/language/theme là cấu hình TĨNH: đặt lần đầu rồi dùng lại.
    // Token hết hạn giữa phiên: KHÔNG có API nào — SDK đọc lại tokenSource.currentToken() mỗi request.
    fun release()                                   // xoá dữ liệu phiên; GIỮ cấu hình tĩnh + theme đã lưu
    fun isInitialized(): Boolean
    fun getCallback(): PromotionSDKCallback?

    val api: PromotionSDKApi                        // bề mặt headless; chưa initialize → mọi hàm trả NotInitialized
    val sdkVersion: String                          // "1.0.0" — đọc được trước initialize

    val session: PromotionSessionConfig?            // session đã truyền lúc initialize
    val currentOrderId: String?
    val currentOrderValue: String?
    val currentServiceCode: String?
    val currentMetaData: String?
    fun updateOrderInfo(orderId: String, productId: String, orderValue: String? = null,
                        metaData: String? = null,
                        skuSourceId: String? = null,
                        productName: String? = null, productCategory: String? = null,
                        quantity: Int? = null, unitPrice: String? = null)

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
    // Màn "Chọn ưu đãi" nối sẵn với widget. `PRMEndowView` tự gọi hàm này khi user bấm — host không
    // cần wiring. Public để host tự kích hoạt từ nơi khác nếu cần. Xem §4. (Android-only, N1: chưa có
    // widget tương ứng bên iOS.)
    fun openChoosePromotion(activity: FragmentActivity, endowView: PRMEndowView, containerViewId: Int? = null)
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
            tokenSource = myTokenSource,
            baseUrl = "https://...",
            language = "vi-VN",
            environment = PromotionEnvironment.PROD,
        ),
        availableServices = listOf(PromotionAvailableService("P-FOOD-001", "Mua đồ ăn")),
        callback = myCallback,
    ),
)
PromotionSDK.updateOrderInfo(orderId = orderId, productId = productId, orderValue = orderValue)   // cập nhật khi vào màn có voucher
val api = PromotionSDK.api
```

Order/dịch vụ **động** đi qua `updateOrderInfo` (ghi vào `PromotionMutableContext`, lõi đọc lại ở **mỗi**
request) — không cần `initialize` lại. Token cũng vậy: SDK đọc lại `tokenSource.currentToken()` ở mỗi
request, host không phải gọi gì khi token đổi.

### 1.2. iOS

```swift
PromotionSDK.initialize(
    options: PromotionSDKOptions(
        session: PromotionSessionConfig(
            tokenSource: myTokenSource,
            baseUrl: "https://...",
            language: "vi-VN",
            environment: .prod
        ),
        availableServices: [PromotionAvailableService(productId: "P-FOOD-001", productName: "Mua đồ ăn")],
        callback: self          // PromotionSDKCallback
    )
)
PromotionSDK.updateOrderInfo(orderId: orderId, productId: productId, orderValue: orderValue)   // cập nhật khi vào màn có voucher
// cần campaign theo SKU chi tiết hơn → thêm skuSourceId:/quantity:/unitPrice: (và productName:/productCategory: nếu có)
let api = PromotionSDK.api      // PromotionSDKApi
```

> **Mô hình vòng đời hai bên giờ ĐỐI ỨNG nhau.** Cả hai đều là singleton tĩnh: cấu hình một lần qua
> `PromotionSDK.initialize(options:)` (iOS) / `PromotionSDK.initialize(context, options)` (Android), rồi cập
> nhật đơn hàng/dịch vụ qua `updateOrderInfo(...)` mà **không** init lại. Giá trị động nằm ở
> `PromotionMutableContext` — lõi đọc lại ở **mỗi** request, nên refresh token = `initialize`/`init`
> lại với session mới, còn order/service chỉ cần `updateOrderInfo`.

**Dòng sản phẩm / SKU.** Đơn chỉ hỗ trợ **một** dòng sản phẩm nên `updateOrderInfo` nhận field phẳng
(`skuSourceId`/`productId`/`productName`/`productCategory`/`quantity`/`unitPrice`) thay vì `List<PromotionOrderItem>`
ở **cả hai** nền tảng; SDK tự bọc lại thành `List<PromotionOrderItem>` 1 phần tử trước khi ghi vào
`PromotionMutableContext`. `orderId`/`productId` **bắt buộc** (không default, không nullable) — `skuSourceId`
để trống thì request `findEligible` **không gửi field này lên server** (bỏ hẳn khỏi JSON, không gửi
chuỗi rỗng), các field còn lại vẫn tuỳ chọn. `ChoosePromotionStore` và `EndowStore` đọc lại qua
`PromotionRequestContextProvider.getOrderItems()`. iOS còn giữ thêm overload tiện tay
`createEndowView(from:orderId:orderValue:orderItems:)` (N1 — widget iOS là factory, xem
[InitParity §5.1](./InitParity.md#51-widget)).

---

## 2. Feature flag — SDK tự gác, host hỏi thêm được

**Tầng bắt buộc (SDK tự làm).** Mọi điểm vào tự gác qua `PromotionFeatureGate` của lõi
(`openMyPromotion`, mở chi tiết, widget) và báo lại khi bị chặn: toast `PRM_MOB_021` ở **cả hai nền
tảng** — SDK tự hiện popup ở điểm mở màn. Host không làm gì thì kill-switch vẫn chạy đủ.

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

Chi tiết: [features/FeatureFlag.md](../features/FeatureFlag.md). Muốn tự xử lý thay vì để SDK hiện popup → truyền `onFeatureDisabled` cho hàm `open…`.
Xem thêm [HeadlessAPI.md §4](./HeadlessAPI.md).

---

## 3. `PromotionSDKApi` — ranh giới, không phải use case

Nó **không** chứa nghiệp vụ. Gác cờ, bắt lỗi, chuẩn hoá `errorCode` đều nằm trong `PromotionUseCases`
của lõi, dùng chung hai nền tảng. Lớp này làm đúng một việc: **uỷ quyền rồi map** — model lõi → DTO,
`PromotionResult` → `PromotionApiResult`.

Hai file dưới đây là **song ánh**. Cùng tên type, cùng tên field, cùng thứ tự khai báo, cùng tên
tham số, cùng cách xử lý `NO_RESULT`. **Sửa một bên thì sửa cả hai.**

| Android `entry/api/` | iOS `Entry/API/` |
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

**Tám** nhánh lỗi, giống nhau hai bên:

| Nhánh | Khi nào |
|---|---|
| `NetworkFailure(code, message)` | mạng/HTTP hỏng. `message` **không bao giờ rỗng** — thiếu câu của server thì lùi về "Không có kết nối mạng…" |
| `BusinessRule(code, serverMessage)` | server từ chối theo **rule nghiệp vụ** (vd `VOUCHER_EXPIRED`). `code` là mã thô, `serverMessage` là câu cho người dùng (`null` khi server không kèm) |
| `SessionExpired` | token hết hạn |
| `Timeout` | |
| `ParseFailed` | server trả `data: null` ở nơi bắt buộc có dữ liệu |
| `FeatureDisabled` | cờ tính năng TẮT (`PRM_MOB_021`) |
| `NotInitialized` | gọi `api` trước `initialize` |
| `Unknown(error)` | còn lại |

- Kotlin: `sealed class PromotionSDKError : Exception()`; `message` là câu hiển thị được.
- Swift: `enum PromotionSDKError: Error, LocalizedError`; `errorDescription` ≡ `message` bên Kotlin.

> ⚠️ **Thêm nhánh = major bump.** Host `when`/`switch` trên type này; Kotlin đòi `when` trên `sealed`
> phải đủ nhánh, và enum Swift ở module thường không `@frozen` được. Mỗi nhánh mới làm hỏng biên dịch
> của mọi host đã liệt kê đủ. Danh sách trên là bản đã chốt — muốn thêm thì phải là major.

**API không ném lỗi nghiệp vụ, và cũng không ném vì chưa khởi tạo.** Chỉ `CancellationException`
thoát ra, để structured concurrency của host còn hoạt động. Đọc `PromotionSDK.api` trước
`initialize` **không** crash và **không** ném: nó trả một bề mặt mà mọi hàm cho `NotInitialized`.

**Một đường map lỗi duy nhất.** `PromotionSDKError.from(errorCode, serverMessage, httpStatus)` là nơi
duy nhất quy mã lỗi thô sang type công khai — cả bề mặt headless (`PromotionSDKApi`) lẫn callback của
widget (`PRMEndowView.onError`, `confirmRedemption`) đều gọi nó. Trước đây mỗi bên map một bản riêng,
nên cùng một lỗi ra hai kết quả khác nhau tuỳ host đi vào đường nào.

`NO_RESULT` (server trả `data: null`) được xử lý theo ngữ cảnh, **không** đồng nhất:
danh sách → thành công với list rỗng; chi tiết / validate / redemption → `ParseFailed`.

### 3.2.1. Ba gate giữ bề mặt public

| Gate | Lệnh | Trả lời câu hỏi |
|---|---|---|
| Allowlist | `./scripts/check-public-api.sh` | Có khai báo `public` nào lọt ra ngoài allowlist không? |
| Baseline lõi | `./scripts/api-baseline.sh check` | Bề mặt Kotlin→ObjC của lõi có đổi so với bản đã review không? |
| `explicitApiWarning()` | `./gradlew :promotionLogic:compileKotlinIosSimulatorArm64` | Khai báo nào của lõi chưa ghi visibility tường minh? |

**Baseline lõi** (`docs/api/PromotionLogic.baseline.h`) đáng có vì repo dùng **SKIE**: nó sinh phần
Swift API **từ** Kotlin, nên chỉ cần nâng version SKIE là bề mặt Swift đổi mà **không commit nào chạm
source**. Không có baseline thì không cơ chế nào phát hiện. Đổi bề mặt có chủ đích → review diff rồi
`./scripts/api-baseline.sh update`, commit **cùng** commit đổi API.

> ⚠️ Baseline hiện tại chụp **trước** nhóm đổi tên API-1/API-4/API-5 (tên module, prefix token,
> `Endow` → `Offer`). Làm xong nhóm đó thì `update` lại — diff sẽ lớn, và đó là điều đúng.

**`explicitApiWarning()`** đang ở mức **cảnh báo**, chưa `explicitApi()` (strict). Lý do: bật strict
là build đỏ ngay với ~200 khai báo thiếu visibility/kiểu trả về tường minh. Danh sách cảnh báo đó
chính là việc cần rà; hạ về 0 rồi mới nâng lên strict. Mọi thứ `public` ở lõi đều lọt ra
`PromotionLogic.h`, mà Kotlin mặc định là `public` — quên gõ `internal` là đã phát hành API mới.

### 3.3. DTO

Mười type, thứ tự khai báo trong file đúng như bảng này:

| Type | Ghi chú |
|---|---|
| `PromotionVoucher` | `id`, `merchantName`, `title`, `imageURL`, `expireDate`, `isUsed`, `status`, `displayStatusLabel` |
| `PromotionVoucherPage` | `vouchers`, `isLastPage` |
| `PromotionVoucherDetail` | thêm `description`, `guideline`, `startDate`, `bannerURL`, `logoURL` |
| `PromotionEligibleOffer` | `id` = `voucherId` nếu đã sở hữu, ngược lại `campaignId`; `usable = false` → hiển thị mờ |
| `PromotionEligibleResult` | `myOffers`, `otherOffers`, `myIsLastPage`, `otherIsLastPage` |
| `PromotionOrderItem` | `skuSourceId`, `productId`, `productName`, `productCategory`, `quantity`, `unitPrice` |
| `PromotionAvailableService` | `productId`, `productName`, `skuSourceId`, `iconUrl`. ⚠️ `productId` phải khớp **`applicableProducts.productId`** của voucher (không phải `sku`) thì dịch vụ mới hiện ở bottom sheet "Chọn dịch vụ"; danh sách bị lọc trùng theo `productId` nên mỗi `productId` chỉ khai **một** dòng, kể cả khi nó gắn nhiều SKU |
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
| `com.ttcn.prm.ui.feature.endowview.PRMEndowView` | Widget ưu đãi ở màn thanh toán. Đặt thẳng vào XML của host. Bấm vào widget → **tự** mở màn "Chọn ưu đãi", host không cần wiring gì. |
| `PromotionSDK.openChoosePromotion(activity, endowView, containerViewId)` | Widget tự gọi hàm này khi user bấm. Public để host tự kích hoạt màn "Chọn ưu đãi" từ nơi khác nếu cần (vd nút riêng ngoài widget) — cùng khuôn `openMyPromotion`/`openPromotionDetail`. |
| `PRMEndowView.confirmRedemption(onSuccess, onError)` | Gọi khi bấm nút thanh toán của host. iOS: `PromotionSDK.confirmRedemption(onSuccess:onError:)`. |
| `com.ttcn.promotionsdk.presentation.endow.EndowWidgetState` | Trạng thái widget, đọc qua `PRMEndowView.getCurrentState()`. |
| `com.ttcn.prm.ui.feature.endowview.AppliedDiscount` | Ưu đãi đã validate. Đi qua callback của `PRMEndowView` và `PRMEndowView.setDiscountDetails` (chi tiết giảm giá **không** qua `PromotionSDKCallback`). Nay là **`typealias` → `com.ttcn.promotionsdk.presentation.endow.EndowAppliedDiscount`** (kiểu thật ở `promotionLogic`, dùng chung với iOS): host Kotlin **không phải đổi gì**, host **Java** phải dùng tên đầy đủ `EndowAppliedDiscount` vì Java không thấy typealias. **Android-only, N1:** iOS không phơi type này — host iOS nhận `onVoucherApplied(voucherId)` rồi gọi `api.validateDiscounts(...)` nếu cần breakdown. Xem [InitParity.md §5.1](./InitParity.md#51-widget). |
| `PromotionSDKCallback` | Thống nhất với iOS, còn **3 sự kiện**: `onVoucherApplied(voucherId)` / `onServiceSelected` / `onExpireToken()`. Bốn cái cũ (`onVoucherCleared` / `onVoucherCountChanged` / `onAvailabilityChanged` / `onClosed`) đã bỏ — host không cần biết. `onExpireToken()` bắn khi 1 API bên trong màn SDK trả HTTP 401 (chưa áp dụng cho headless `PromotionSDKApi`). Xem [InitParity.md §3](./InitParity.md). |
| `PromotionTheme` | Đổi theme sau `init`. Xem [Theming.md](./Theming.md). |

```kotlin
// Chỉ cần nhúng widget vào layout — không cần dòng nào để mở màn "Chọn ưu đãi".
<com.ttcn.prm.ui.feature.endowview.PRMEndowView
    android:id="@+id/endowView"
    ... />
```

Bấm vào widget (trạng thái `NOT_APPLIED`/`UNAVAILABLE`) → `PRMEndowView` tự resolve
`FragmentActivity` từ `context`, gọi `PromotionSDK.openChoosePromotion(activity, this)`. Hàm này lấy
lại ưu đãi widget đã tải (khỏi gọi `findEligible` lần hai), pre-select voucher đang áp, và đẩy kết
quả ngược về widget khi user bấm "Áp dụng" — host không phải chạm `EligibleOffer`, một type của lõi,
và không cần biết `ChoosePromotionFragment` (nội bộ) tồn tại.

Host muốn tự kích hoạt màn này từ nơi khác (vd nút "Xem ưu đãi" riêng, ngoài cú bấm mặc định của
widget) thì gọi thẳng `PromotionSDK.openChoosePromotion(activity, endowView)`.


---

## 5. Quy tắc khi mở rộng

1. **Không để type của `promotionLogic` xuất hiện trong chữ ký public.** Trên Android nó không gây
   lỗi lúc build SDK — lỗi chỉ nổ ở app host. Cách kiểm tra: thêm tạm một file vào `androidApp`
   import `com.ttcn.promotionsdk.*` và đổi `androidApp` sang `implementation(projects.androidPromotionUI)`;
   phải thấy `Unresolved reference`.
2. **Mặc định là `internal`.** Không chỉ model MVI / adapter / ViewModel — mọi khai báo top-level
   ngoài `entry` đều `internal`: widget (`PRMButton`, `PRMSearchField`…), base class
   (`PRMBaseFragment`, `PRMBaseActivity`, `PRMStoreViewModel`), extension trong `ui/utils/`, cả năm
   Fragment nghiệp vụ, và nhóm theme nội bộ (`PromotionThemeRegistry` / `Store` / `Defaults` /
   `ThemeHex` / `applier` / `applytoken`). Bên iOS là "không viết `public`" ngoài `Entry/`.
2b. **Host cần thêm thứ gì thì DỜI vào `entry`, đừng nới `public` tại chỗ.** Nới tại chỗ là cách
   `ui/theme`, `ui/widget`, `ui/base` phình thành bề mặt công khai ngoài ý muốn trước đây. Dời file
   thì `grep` ở đầu tài liệu này vẫn xanh, và người đọc code biết ngay ranh giới nằm ở đâu.
3. Thêm/đổi DTO hoặc hàm ở `PromotionSDKApi` → **sửa cả Kotlin lẫn Swift trong cùng một commit**, giữ
   nguyên tên và thứ tự. Cập nhật file này.
4. Đổi chữ ký public của iOS → app host phải `⇧⌘K` (Clean Build Folder). `build-xcframework.sh` đã tự
   dọn `SwiftExplicitPrecompiledModules`, nhưng cache của Xcode vẫn có thể nói dối nếu bạn build tay.

---

## 5b. Cái gì là "breaking" — danh sách chốt

SemVer nói *khi nào* bump major nhưng không nói *cái gì* tính là breaking cho **SDK này**. Không có
danh sách thì mỗi lần lại tranh luận lại, và loại breaking nguy hiểm nhất — **breaking hành vi** —
gần như luôn bị bỏ sót vì nó không đổi một chữ ký nào.

### 5b.0. Bề mặt iOS là `@MainActor`

`PromotionSDK` (và cả cây UI bên dưới: `PromotionSDKImpl`, `PRMStoreViewModel`, `PRMBaseBuilder`,
`PRMBaseRouter`) đánh `@MainActor`. Hợp đồng này **vốn đã** là "gọi trên main thread" — nó là API UI
(`openMyPromotion(from: UIViewController)`, `createEndowView(from:)`, `configure(theme:)`) — nhưng
trước đây chỉ nằm trong doc comment, nên host gọi từ thread nền sẽ **crash lúc chạy** ở tầng UIKit
thay vì được compiler chỉ đúng chỗ sai.

Host gọi từ ngữ cảnh không phải main thì bọc `await MainActor.run { … }`.

> **Thêm `@MainActor` SAU go-live là source-breaking với mọi host**, nên nó phải nằm ở đây từ đầu.
> Nó cũng đóng luôn phần global mutable state (bốn `private static var` của `PromotionSDK`) — thứ
> Swift 6 sẽ chặn.

Ba chỗ trong chính SDK từng chỉ được bảo đảm bằng comment, nay compiler giữ:
`PRMStoreViewModel.emitErrorIfNeeded`, `PromotionSDKImpl.render(_:on:)`, và closure `onState`/`onEffect`
(kiểu nay là `@MainActor (State) -> Void`). Riêng `bindStore` giữ `DispatchQueue.main.async` +
`MainActor.assumeIsolated` chứ **không** đổi sang `Task { @MainActor in }`: `Task` không bảo đảm thứ
tự giữa nhiều lần phát, mà `StateFlow` là conflated — hai state tới gần nhau mà chạy đảo thứ tự là
bản cũ ghi đè bản mới.

### 5b.1. Breaking chữ ký (rõ ràng)

| Thay đổi | Vì sao vỡ |
|---|---|
| Xoá / đổi tên hàm, property, type public | host không compile |
| Đổi kiểu tham số hoặc kiểu trả về | như trên |
| **Thêm tham số không có giá trị mặc định** | như trên. Thêm kèm default thì **không** breaking |
| **Thêm hàm vào `PromotionSDKCallback` mà không có default implementation** | host cài đặt protocol/interface đó sẽ không compile. Kotlin: `fun x() {}`; Swift: `extension` với bản rỗng |
| Đổi module name / prefix type (API-1, API-4) | vỡ dòng `import` và mọi chỗ dùng tên cũ |
| Đổi `implementation` → không còn `api` ở artifact nằm trong chữ ký public | host mất type khỏi compile classpath |
| **Thêm `@MainActor`** vào type/hàm public (iOS) | mọi chỗ host gọi từ ngữ cảnh không phải main thành lỗi compile — xem 5b.0 |

### 5b.2. Breaking hành vi (không đổi chữ ký nào — nguy hơn)

| Thay đổi | Vì sao vỡ | Đã xảy ra thật |
|---|---|---|
| **Đổi thứ tự callback** phát về host | host xử lý theo thứ tự cũ sẽ sai; không có gì báo | ✅ `PromotionSDKImpl.emit` từng đổi applied-trước → count-trước-applied. Nay có golden test khoá lại (`EndowHostNotifierEmitTests`, `EndowHostNotifierTest`) |
| **Thêm nhánh vào `PromotionSDKError`** | Kotlin `when` trên `sealed` và Swift `switch` trên enum không `@frozen` đòi liệt kê đủ → host không compile | ✅ thêm `BusinessRule` + `NotInitialized` (2026-09-10) |
| **Đổi mã lỗi thô** (`ErrorCodes`) | host so chuỗi mã sẽ rơi vào nhánh sai | — |
| **Đổi thời điểm phát** callback (sớm/muộn hơn) | host dựa vào "đã có dữ liệu lúc callback chạy" | — |
| **Đổi ngữ nghĩa một trạng thái** (vd `EndowWidgetState.UNAVAILABLE` nay chỉ đến từ 2 đường thay vì 3) | host render theo trạng thái sẽ hiện sai | ✅ 2026-09-10 |
| **Bỏ/đổi resource public Android** (`prm_*` mà host tham chiếu) | host không build được resource | — |

### 5b.3. KHÔNG breaking

Thêm hàm/property/type mới · thêm case vào enum **chỉ SDK sinh ra và host không `switch`** · thêm
tham số **có** default · đổi phần `internal` · đổi chuỗi hiển thị · sửa bug mà hợp đồng không đổi.

### 5b.4. Kiểm bằng gì

`./scripts/api-baseline.sh check` bắt được nhóm **5b.1** (diff header là bằng chứng). Nhóm **5b.2**
thì baseline **không** thấy — đó là lý do phải có test khoá thứ tự và số lần gọi callback (§3.2.1),
và lý do bảng này liệt kê chúng ra thành văn bản.

> Danh sách này phải **đóng băng trước commit go-live** — sau đó mỗi dòng ở 5b.1/5b.2 là một lần
> major bump, và với SDK nội bộ thì "major bump" nghĩa là đi bắt từng đối tác sửa code.

---

## 6. Bịt kín ở tầng AAR (Android)

`internal` là hàng rào của **compiler Kotlin**, không phải của artifact. Trong bytecode nó vẫn là
`public final class`; host viết Java gọi thẳng được, reflection gọi được, và Android Studio vẫn liệt
kê đủ trong External Libraries. Hai lớp dưới đây bịt thêm phần còn lại.

> **AAR phát hành KHÔNG obfuscate.** Từng thử bật R8 trên chính module SDK (`isMinifyEnabled = true`
> + `proguard-rules.pro`) và đã gỡ bỏ (2026-08-08): nó rút gọn cả mapper Data Binding nên host crash
> `AbstractMethodError` ngay khi mở màn SDK đầu tiên. Đổi lấy một AAR khó debug mà chẳng thêm được
> hàng rào thật nào — `internal` + hai mục dưới đây đã đủ. Host nào muốn thu gọn thì bật R8 ở app
> của họ; `consumer-rules.pro` (đóng gói sẵn trong AAR) đã lo phần giữ API.

### 6.1. Không phát hành sources.jar

`AndroidPromotionSDK/build.gradle.kts` → `singleVariant("release")` (bỏ `withSourcesJar()`).
`promotionLogic/build.gradle.kts` → `withSourcesJar(publish = false)` (KMP mặc định publish sources).

Trước đây bản phát hành kèm nguyên văn 98 file `.kt` của tầng UI cộng toàn bộ `commonMain` của lõi.
Đổi lại: host **không step-into vào SDK khi debug** được nữa. Bù lại, vì AAR không obfuscate nên
stacktrace đối tác gửi về vẫn đọc thẳng được tên class/hàm, không cần `mapping.txt`.

### 6.2. Resource private toàn bộ

`AndroidPromotionSDK/src/main/res/values/public.xml` chứa một `<public />` rỗng. AAPT2 hiểu là:
không resource nào public → `public.txt` trong AAR rỗng → host tham chiếu `@dimen/prm_view_size_24`
sẽ ăn lint `PrivateResource`.

`androidApp` trước đây mượn 38 resource của SDK; chúng đã được tách sang bản riêng của app
(`androidApp/src/main/res/values/demo_tokens.xml`, tiền tố `demo_`). App demo đóng vai host, nó phải
dựng được UI của mình mà không cần biết bên trong SDK có resource gì.

### 6.3. Dư địa đã biết

Vì AAR không obfuscate, **mọi tên class trong `com.ttcn.prm.**` và `com.ttcn.promotionsdk.**` đều đọc
được** nếu host decompile AAR. Đó là lựa chọn có ý thức, không phải sơ suất — hàng rào ở đây là
*hợp đồng* (`internal` + không sources.jar + resource private), không phải chống dịch ngược:

1. **`internal` chỉ chặn lúc compile.** Host viết Kotlin `import com.ttcn.prm.ui...` là lỗi compile;
   host viết Java hoặc reflection thì vẫn gọi được. Không có cách nào bịt hẳn ở JVM.
2. **`com.ttcn.prm.R` / `com.ttcn.prm.BR`** do AGP sinh ở phía consumer, luôn public. Với §6.2 thì
   mọi entry trong đó đều private nên host tham chiếu sẽ ăn lint `PrivateResource`.
3. **`promotionLogic` là artifact Maven riêng** (`vn.viettelpay.library:promotionLogic`). Nó ở scope
   `runtime` nên **không** nằm trên compile classpath của host (import là lỗi compile), và sources.jar
   đã gỡ ở §6.1 — nhưng tên class thì vẫn còn nguyên trong APK.

Cần giấu thật sự (yêu cầu bảo mật, không phải yêu cầu hợp đồng API) thì phải tính lại từ đầu: gộp
`promotionLogic` vào AAR (fat AAR) rồi R8 một lượt, kèm bộ keep list cho Data Binding / Gson /
kotlinx.serialization. Đó là thay đổi **hợp đồng phát hành**: POM + `module.json` do Gradle sinh sẽ
đổi theo, host nhận bộ dependency khác — không làm lặng lẽ được.
