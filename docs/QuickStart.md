# QuickStart — Tích hợp trong 15 phút

Đường ngắn nhất từ "chưa có gì" đến "mở được màn ưu đãi". Chi tiết đầy đủ ở
[AndroidIntegrationGuide](./AndroidIntegrationGuide.md) / [IosIntegrationGuide](./IosIntegrationGuide.md).

## Mục lục

<!-- toc -->
- [1. Bước 0 — Bạn cần có sẵn](#1-bước-0--bạn-cần-có-sẵn)
- [2. Android](#2-android)
  - [2.1. Khai dependency](#21-khai-dependency)
  - [2.2. Nối nguồn token](#22-nối-nguồn-token)
  - [2.3. Khởi tạo (một lần, sau khi đăng nhập)](#23-khởi-tạo-một-lần-sau-khi-đăng-nhập)
  - [2.4. Mở màn ưu đãi](#24-mở-màn-ưu-đãi)
  - [2.5. Widget ở màn thanh toán](#25-widget-ở-màn-thanh-toán)
  - [2.6. Đăng xuất](#26-đăng-xuất)
- [3. iOS](#3-ios)
  - [3.1. Nhúng framework](#31-nhúng-framework)
  - [3.2. Nối nguồn token](#32-nối-nguồn-token)
  - [3.3. Khởi tạo](#33-khởi-tạo)
  - [3.4. Mở màn & widget](#34-mở-màn--widget)
  - [3.5. Đăng xuất](#35-đăng-xuất)
- [4. Headless — tự dựng UI](#4-headless--tự-dựng-ui)
- [5. Bốn lỗi hay gặp nhất ở lần tích hợp đầu](#5-bốn-lỗi-hay-gặp-nhất-ở-lần-tích-hợp-đầu)
<!-- /toc -->

---

## 1. Bước 0 — Bạn cần có sẵn

- Tài khoản đọc Artifactory nội bộ (Android).
- `baseUrl` của môi trường BFF.
- Nguồn access token của app (SDK **không** tự đăng nhập).
- Android `minSdk ≥ 24` · iOS `≥ 13.0`.

---

## 2. Android

### 2.1. Khai dependency

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney")
            credentials {
                username = providers.gradleProperty("ttcnArtifactoryUser").orNull   // đặt ở ~/.gradle/gradle.properties
                password = providers.gradleProperty("ttcnArtifactoryToken").orNull  // KHÔNG commit
            }
            content { includeGroup("vn.viettelpay.library") }
        }
        google()
        mavenCentral()
    }
}

// build.gradle.kts (module app)
dependencies {
    implementation("vn.viettelpay.library:promotion:1.0.0")
}
```

Không cần khai tay Ktor/coroutines/… — Gradle kéo theo qua metadata.

### 2.2. Nối nguồn token

```kotlin
object AppPromotionTokenSource : PromotionTokenSource {
    // ⚠️ Gọi từ THREAD NỀN ở mỗi request → phải thread-safe
    override fun currentToken(): String? = tokenStore.accessToken

    // SDK gọi khi ăn 401. Ghi token mới vào kho của mình RỒI báo true.
    override fun refreshToken(onResult: (Boolean) -> Unit) {
        authRepository.refresh { ok -> onResult(ok) }
    }
}
```

### 2.3. Khởi tạo (một lần, sau khi đăng nhập)

```kotlin
PromotionSDK.initialize(
    context = applicationContext,
    tokenSource = AppPromotionTokenSource,
    baseUrl = BuildConfig.PROMOTION_BASE_URL,
    environment = PromotionEnvironment.PROD,
    language = "vi-VN",
    callback = object : PromotionSDKCallback {
        override fun onVoucherApplied(voucherId: String) { /* … */ }
        override fun onServiceSelected(selection: PromotionServiceSelection) { /* … */ }
        override fun onExpireToken() { goToLogin() }
    },
)
```

### 2.4. Mở màn ưu đãi

```kotlin
PromotionSDK.openMyPromotion(activity)                  // full screen
PromotionSDK.openMyPromotion(activity, R.id.container)  // nhúng vào container của host
```

### 2.5. Widget ở màn thanh toán

```kotlin
// Bơm thông tin đơn TRƯỚC khi vào màn có voucher
PromotionSDK.updateOrderInfo(orderId = orderId, productId = productId, orderValue = amount)
```

```xml
<com.ttcn.prm.ui.feature.offerwidget.PRMOfferWidget
    android:id="@+id/offerWidget"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

Bấm vào widget → SDK **tự** mở màn "Chọn ưu đãi". Host không cần wiring gì.

```kotlin
// khi user bấm nút thanh toán của host
binding.offerWidget.confirmRedemption(
    onSuccess = { result -> proceedPayment(result) },
    onError   = { error -> stopCheckout(error) },
)
```

### 2.6. Đăng xuất

```kotlin
PromotionSDK.release()
```

---

## 3. iOS

### 3.1. Nhúng framework

Kênh chính là **SPM `binaryTarget`** trỏ tới zip trên Artifactory nội bộ:

```swift
.binaryTarget(
    name: "Promotion",          // BẮT BUỘC trùng tên xcframework trong zip, không phải module `PromotionKit`
    url: "https://mobile-data.viettelmoney.vn/artifactory/vdo-ios-frameworks/Martech/Promotion/1.0.0/Promotion-1.0.0.xcframework.zip",
    checksum: "<sha256 lấy từ metadata.json cạnh zip>"
)
```

Thêm package đó vào project host → **General** → *Frameworks, Libraries, and Embedded Content* → đặt
**Embed & Sign**. Không cần `OTHER_LDFLAGS` hay search path.

⚠️ SwiftPM **không hỏi mật khẩu** — phải khai `~/.netrc`, nếu không Xcode chỉ báo "failed downloading"
mà không nói là 401:

```
machine mobile-data.viettelmoney.vn login <user> password <identity token>
```

Chưa được cấp tài khoản Artifactory thì kéo tay file `Promotion.xcframework` cũng chạy —
xem [IosIntegrationGuide §3.2](./IosIntegrationGuide.md).

### 3.2. Nối nguồn token

```swift
import PromotionKit

final class AppPromotionTokenSource: PromotionTokenSource {   // KHÔNG @MainActor
    func currentToken() -> String? { tokenStore.accessToken }
    func refreshToken(onResult: @escaping (Bool) -> Void) {
        authRepository.refresh { ok in onResult(ok) }
    }
}
```

### 3.3. Khởi tạo

```swift
PromotionSDK.initialize(
    options: PromotionSDKOptions(
        session: PromotionSessionConfig(
            tokenSource: AppPromotionTokenSource(),
            baseUrl: Config.promotionBaseURL,
            language: "vi-VN",
            environment: .prod
        ),
        callback: self       // PromotionSDKCallback
    )
)
```

### 3.4. Mở màn & widget

```swift
PromotionSDK.openMyPromotion(from: self)
PromotionSDK.updateOrderInfo(orderId: orderId, productId: productId, orderValue: amount)
PromotionSDK.confirmRedemption(onSuccess: { … }, onError: { … })
```

### 3.5. Đăng xuất

```swift
PromotionSDK.release()
```

---

## 4. Headless — tự dựng UI

```kotlin
when (val r = PromotionSDK.api.getVouchers(page = 0, size = 10)) {
    is PromotionApiResult.Success -> render(r.data.vouchers)
    is PromotionApiResult.Failure -> showError(r.error.message)
}
```

Năm hàm: `getVouchers` · `findEligible` · `getVoucherDetail` · `validateDiscounts` ·
`createRedemption`. Xem [PublicApi §3](./common/PublicApi.md).

---

## 5. Bốn lỗi hay gặp nhất ở lần tích hợp đầu

| Triệu chứng | Nguyên nhân |
|---|---|
| `Unresolved reference 'PromotionUseCases'` | Đang import `com.ttcn.promotionsdk.*` — đó là lõi nội bộ, host không thấy. Chỉ dùng `com.ttcn.prm.entry.*` |
| Mọi API trả 401 dù vừa đăng nhập | `currentToken()` trả token cũ, hoặc host chưa ghi token mới **trước khi** báo `true` |
| Màn ưu đãi rỗng ở màn thanh toán | Chưa gọi `updateOrderInfo(...)`, hoặc `productId` không khớp `applicableProducts.productId` của voucher |
| iOS: `dyld: Library not loaded` | Framework đang để "Do Not Embed" — phải **Embed & Sign** |

Danh sách đầy đủ: [Troubleshooting](./Troubleshooting.md).
