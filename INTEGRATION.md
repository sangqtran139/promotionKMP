# Promotion SDK — Hướng dẫn tích hợp

## Mục lục
1. [Cài đặt](#1-cài-đặt)
2. [Khởi tạo SDK](#2-khởi-tạo-sdk)
   - [2.1 Tùy biến giao diện (Theme)](#21-tùy-biến-giao-diện-theme)
3. [Chế độ Full UI](#3-chế-độ-full-ui)
4. [Chế độ Headless](#4-chế-độ-headless)
5. [API Reference](#5-api-reference)
6. [Models](#6-models)
7. [Xử lý lỗi](#7-xử-lý-lỗi)
8. [Public API surface](#8-public-api-surface)

---

## 1. Cài đặt

SDK được phân phối dưới dạng **file AAR**. File AAR **không tự kéo theo dependency**,
nên đối tác phải khai báo đầy đủ các thư viện bên dưới — nếu thiếu, app sẽ crash
`NoClassDefFoundError` lúc runtime.

### 1.1 Thêm file AAR

Copy `vds-promotion-1.0.0-release.aar` vào thư mục `app/libs/` của project.

### 1.2 Khai báo dependency

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation(files("libs/vds-promotion-1.0.0-release.aar"))

    // BẮT BUỘC — dependency của SDK (AAR không tự kéo về):
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("io.github.florent37:shapeofview:1.4.7")
}
```

> Các version trên là version SDK được build & kiểm thử. Nếu host app dùng version
> khác, ưu tiên đồng bộ để tránh xung đột. `room-compiler` là annotation processor
> (compile-time) nên **không** cần khai báo ở app.

### 1.3 Yêu cầu

- `minSdk >= 24`.
- ProGuard/R8: SDK đã kèm sẵn `consumer-rules.pro` (tự áp dụng), đối tác **không** cần
  thêm rule cho SDK.

### 1.4 Build file AAR (dành cho team phát hành SDK)

```bash
./gradlew :vds-promotion:assembleRelease
# Output: vds-promotion/build/outputs/aar/vds-promotion-1.0.0-release.aar
```

Đổi version tại `gradle.properties` (`SDK_VERSION=...`) trước khi build.

---

## 2. Khởi tạo SDK

Gọi `init()` trong `Application.onCreate()` hoặc trước khi dùng bất kỳ tính năng nào.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        PromotionSDK.initialize(
            context = this,
            options = PromotionSDKOptions(
                config = PromotionSDKConfig(
                    baseUrl       = "https://api.example.com",
                    customerId    = session.customerId,
                    accessToken   = session.accessToken,
                    serviceCode   = "vay",              // mã dịch vụ của đối tác
                    orderId       = currentOrder.id,
                    orderValue    = currentOrder.amount,
                ),
            )
        )
    }
}
```

Giải phóng khi không cần nữa (logout, kết thúc luồng thanh toán):

```kotlin
PromotionSDK.release()
```

### 2.1 Tùy biến giao diện (Theme)

SDK cho phép tùy biến màu sắc/bo góc qua hệ thống **token**. Mọi field token đều tùy chọn —
bỏ trống sẽ giữ giao diện mặc định của SDK.

```kotlin
val themeConfig = PromotionThemeConfig(
    buttonToken       = ButtonToken(backgroundColor = Color.parseColor("#EE0033"), cornerRadius = 8f),
    tabChipToken      = TabChipToken(activeBackgroundColor = ..., activeTextColor = ...),
    discountBadgeToken = DiscountBadgeToken(availableTextColor = ..., availableBackgroundColor = ...),
)
```

Có **2 cách** cấu hình theme:

```kotlin
// Cách 1 (khuyến nghị): truyền vào options khi init
PromotionSDK.initialize(
    context = this,
    options = PromotionSDKOptions(
        config = PromotionSDKConfig(/* ... */),
        theme  = PromotionSDKTheme(config = themeConfig),
    ),
)

// Cách 2: gọi SAU init()
PromotionSDK.initialize(context, PromotionSDKOptions(config = PromotionSDKConfig(/* ... */)))
PromotionTheme.configure(themeConfig)
```

Cả hai cách đều hợp lệ — `init()` chỉ ghi đè theme khi bạn **thực sự truyền** `options.theme`; nếu để mặc định
(rỗng), theme đã cấu hình trước đó bằng `PromotionTheme.configure()` vẫn được giữ. `options.theme` luôn là
đường có ưu tiên cao nhất khi bạn muốn ghi đè.

Bạn cũng có thể **đổi theme lúc runtime**: gọi `PromotionTheme.configure(newConfig)` bất kỳ lúc nào — các view
đang hiển thị (nút, ô tìm kiếm, `PRMEndowView`) sẽ tự áp lại ngay; danh sách voucher cập nhật ở lần cuộn/refresh kế tiếp.

Token hỗ trợ: `ButtonToken`, `SearchBarToken`, `ListItemToken`, `TabChipToken`, `TabUnderlineToken`,
`DiscountBadgeToken`. Chi tiết từng field và cơ chế áp dụng: xem `docs/Theming.md`.

---

## 3. Chế độ Full UI

SDK cung cấp sẵn màn hình "Ưu đãi của tôi". Đối tác chỉ cần gọi 1 dòng:

```kotlin
// Mở màn ưu đãi trên toàn màn hình
PromotionSDK.openMyPromotion(activity)

// Hoặc mở trong container cụ thể
PromotionSDK.openMyPromotion(activity, containerViewId = R.id.fragment_container)
```

Nhận callback khi user áp dụng voucher:

```kotlin
PromotionSDKOptions(
    config = ...,
    callback = object : PromotionSDKCallback {
        override fun onVoucherApplied(discountDetails: List<AppliedDiscount>) {
            // Cập nhật UI đơn hàng với discount
        }
        override fun onError(errorCode: String) {
            showError(errorCode)
        }
        override fun onSDKClosed() {
            // User đóng màn ưu đãi
        }
    }
)
```

---

## 4. Chế độ Headless

Đối tác tự build UI, dùng `PromotionSDK.useCases` để gọi API trực tiếp.

### 4.1 Tìm kiếm voucher

```kotlin
val useCases = PromotionSDK.useCases

val result = useCases.searchVouchers(
    SearchCustomerVouchersRequest(
        customerId      = "CUS_001",
        serviceCode     = "vay",
        keyword         = null,         // null = lấy tất cả
        tab             = null,
        sectionCode     = null,
        myVouchersPage  = 0,
        myVouchersSize  = 10,
        otherVouchersPage = 0,
        otherVouchersSize = 10,
    )
)

result?.myVouchers?.content?.forEach { voucher ->
    println("${voucher.title} - ${voucher.expirationDate}")
}
```

### 4.2 Chi tiết voucher

```kotlin
val detail = useCases.getVoucherDetail(
    voucherId  = "VCH_123",
    customerId = "CUS_001",
    service    = "vay",
)

println(detail?.description)
println(detail?.guideline)
```

### 4.3 Validate voucher trước khi thanh toán

```kotlin
val validation = useCases.validateDiscounts(
    ValidateDiscountsRequest(
        customerId  = "CUS_001",
        orderId     = "ORD_456",
        orderValue  = "500000",
        items = listOf(
            DiscountItemRequest(objectId = "VCH_123"),
            DiscountItemRequest(objectId = "VCH_456"),
        )
    )
)

validation?.let {
    println("Tổng giảm: ${it.totalDiscountAmount}")
    println("Thành tiền: ${it.finalAmount}")

    it.invalidItems.forEach { item ->
        println("Voucher ${item.objectId} không hợp lệ: ${item.eligibilityStatus}")
    }
}
```

### 4.4 Tạo redemption session (xác nhận thanh toán)

```kotlin
val redemption = useCases.createRedemption(
    CreateRedemptionRequest(
        customerId = "CUS_001",
        orderId    = "ORD_456",
        orderValue = "500000",
        items = listOf(
            RedemptionItemRequest(
                objectId         = "VCH_123",
                objectType       = "CAMPAIGN",
                expectedDiscount = "50000",
            )
        )
    )
)

if (redemption?.hasErrors == true) {
    if (redemption.hasBudgetError) {
        // Voucher hết ngân sách — cần validate lại
        revalidateAndRetry()
    } else {
        showError(redemption.validationErrors.first().message)
    }
} else {
    proceedPayment()
}
```

### 4.5 Luồng hoàn chỉnh (Coroutine)

```kotlin
viewModelScope.launch {
    val useCases = PromotionSDK.useCases

    // 1. Load voucher
    val searchResult = useCases.searchVouchers(searchRequest) ?: return@launch

    // 2. User chọn voucher → validate
    val validation = useCases.validateDiscounts(
        ValidateDiscountsRequest(
            customerId = customerId,
            orderId    = orderId,
            orderValue = orderValue,
            items      = selectedVouchers.map { DiscountItemRequest(it.voucherId) }
        )
    )

    if (validation?.overallValid == false) {
        showInvalidVoucherWarning(validation.invalidItems)
        return@launch
    }

    // 3. Confirm thanh toán
    val redemption = useCases.createRedemption(redemptionRequest)

    if (redemption?.hasErrors == true) {
        handleRedemptionError(redemption)
    } else {
        onPaymentSuccess()
    }
}
```

---

## 5. API Reference

### `PromotionUseCases`

| Method | Mô tả |
|--------|-------|
| `searchVouchers(request)` | Tìm kiếm voucher, hỗ trợ phân trang |
| `getVoucherDetail(voucherId, customerId, service?)` | Chi tiết một voucher |
| `validateDiscounts(request)` | Validate danh sách voucher với đơn hàng |
| `createRedemption(request)` | Tạo session xác nhận redemption |

---

## 6. Models

> **Package (từ bản này):** domain model được chia theo feature trong `com.ttcn.promotionsdk.core.domain.model.<feature>`:
> `…model.voucher.*` (voucher), `…model.redemption.*`, `…model.stackablediscount.*`.
> Nếu nâng cấp từ bản cũ (model phẳng ở `…core.domain.model.*`), cập nhật lại `import` tương ứng.

### Request models

```kotlin
SearchCustomerVouchersRequest(
    customerId: String,
    serviceCode: String?,
    keyword: String?,
    tab: String?,
    sectionCode: String?,
    myVouchersPage: Int?,
    myVouchersSize: Int?,
    otherVouchersPage: Int?,
    otherVouchersSize: Int?,
)

ValidateDiscountsRequest(
    customerId: String,
    orderId: String,
    orderValue: String,       // số tiền dạng String, VD: "500000"
    items: List<DiscountItemRequest>,
)

DiscountItemRequest(
    objectId: String,         // voucherId
    objectType: String,       // mặc định "CAMPAIGN"
)

CreateRedemptionRequest(
    customerId: String,
    orderId: String,
    orderValue: String,
    items: List<RedemptionItemRequest>,
)

RedemptionItemRequest(
    objectId: String,
    objectType: String,
    expectedDiscount: String?,  // giá trị giảm dự kiến từ bước validate
)
```

### Response models

```kotlin
SearchCustomerVouchersResult(
    tabs: List<VoucherTabItem>,
    selectedTab: String?,
    defaultTab: String?,
    myVouchers: VoucherListPage?,    // voucher của tôi
    otherVouchers: VoucherListPage?, // voucher khác
)

VoucherListPage(
    content: List<VoucherItem>,
    number: Int?,         // trang hiện tại (0-based)
    size: Int?,
    last: Boolean?,       // true = trang cuối
    totalElements: Long?,
)

VoucherItem(
    voucherId: String,
    merchantName: String?,
    title: String?,
    description: String?,
    logo: String?,
    expirationDate: String?,
    status: String?,
    displayStatusLabel: String?,
    objectType: String,
)

ValidateDiscountsResult(
    overallValid: Boolean,
    totalDiscountAmount: String,
    finalAmount: String,
    items: List<DiscountItemResult>,
    validItems: List<DiscountItemResult>,   // computed
    invalidItems: List<DiscountItemResult>, // computed
)

CreateRedemptionResult(
    sessionId: String,
    totalDiscount: String,
    finalAmount: String,
    validationErrors: List<RedemptionValidationError>,
    hasErrors: Boolean,      // computed
    hasBudgetError: Boolean, // computed — cần validate lại
)
```

---

## 7. Xử lý lỗi

Mọi hàm của `useCases` trả `PromotionResult<T>` (`com.ttcn.promotionsdk.core.domain.model.PromotionResult`) —
**không ném exception**. Xử lý bằng `when`:

```kotlin
when (val result = useCases.validateDiscounts(request)) {
    is PromotionResult.Success -> handle(result.data)
    is PromotionResult.Failure -> handleApiError(result.errorCode) // + result.message, result.httpStatus
}
```

### Error codes thường gặp

| Code | Ý nghĩa |
|------|---------|
| `missing_customer_id` | Chưa cung cấp customerId |
| `INSUFFICIENT_BUDGET` | Voucher hết ngân sách, cần validate lại |
| `no_result` | Không tìm thấy kết quả |
| `network_error` | Mất mạng / không kết nối được (nên gợi ý kiểm tra kết nối) |
| `timeout` | Hết thời gian chờ (nên cho thử lại) |
| `error_general` | Lỗi chung |

---

## 8. Public API surface

Host **chỉ** nên phụ thuộc các kiểu dưới đây. Mọi kiểu khác (DTO `core/data/*`, mapper, datasource,
class `internal`) là **nội bộ SDK**, có thể đổi bất kỳ lúc nào — đừng import.

| Nhóm | Kiểu công khai |
|------|----------------|
| Entry | `PromotionSDK`, `PromotionSDKOptions`, `PromotionSDKCallback`, `PromotionTheme` |
| Headless | `PromotionSDK.useCases` (`PromotionUseCases`) |
| Kết quả headless | `PromotionResult<T>` (`Success`/`Failure`) — `core/domain/model/PromotionResult` |
| Model nghiệp vụ | `core/domain/model/<feature>/*` — request + result (vd `SearchCustomerVouchersRequest`/`...Result`, `ValidateDiscountsRequest`/`...Result`, `CreateRedemptionRequest`/`...Result`, `VoucherDetail`) |
| Discount áp dụng | `AppliedDiscount` (`ui/entry`) — dùng ở callback & `PRMEndowView` |
| UI nhúng | `PRMEndowView`, `ChoosePromotionFragment`, `PromotionIntegrateManager`, các `PRM*` view/base |

> **Ổn định:** chỉ các kiểu trong bảng này được giữ ổn định giữa các phiên bản. SDK chưa release
> chính thức (1.0.0) nên các bản trước đó có thể còn breaking; từ bản phát hành đầu tiên sẽ theo semver.
