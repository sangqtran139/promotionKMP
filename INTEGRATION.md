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

---

## 1. Cài đặt

Thêm AAR vào project:

```gradle
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://your-repo/promotion-sdk") }
    }
}

// build.gradle (app)
dependencies {
    implementation("com.ttcn:promotion-sdk:1.0.0")
}
```

---

## 2. Khởi tạo SDK

Gọi `init()` trong `Application.onCreate()` hoặc trước khi dùng bất kỳ tính năng nào.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        PromotionSDK.init(
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
PromotionSDK.init(
    context = this,
    options = PromotionSDKOptions(
        config = PromotionSDKConfig(/* ... */),
        theme  = PromotionSDKTheme(config = themeConfig),
    ),
)

// Cách 2: gọi SAU init()
PromotionSDK.init(context, PromotionSDKOptions(config = PromotionSDKConfig(/* ... */)))
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
        override fun onVoucherApplied(discountDetails: List<DiscountDetail>) {
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
VoucherSearchResult(
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

DiscountValidationResult(
    overallValid: Boolean,
    totalDiscountAmount: String,
    finalAmount: String,
    items: List<DiscountItemResult>,
    validItems: List<DiscountItemResult>,   // computed
    invalidItems: List<DiscountItemResult>, // computed
)

RedemptionSessionResult(
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

Tất cả use case đều throw exception khi API lỗi. Dùng `runCatching` để bắt:

```kotlin
runCatching {
    useCases.validateDiscounts(request)
}.onSuccess { result ->
    // xử lý kết quả
}.onFailure { throwable ->
    when (throwable) {
        is PromotionApiException -> handleApiError(throwable.errorCode)
        else                     -> showGenericError()
    }
}
```

### Error codes thường gặp

| Code | Ý nghĩa |
|------|---------|
| `missing_customer_id` | Chưa cung cấp customerId |
| `INSUFFICIENT_BUDGET` | Voucher hết ngân sách, cần validate lại |
| `no_result` | Không tìm thấy kết quả |
| `error_general` | Lỗi chung |
