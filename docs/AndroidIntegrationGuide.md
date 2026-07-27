# Android Integration Guide — Promotion SDK cho app host

> Hướng dẫn **tích hợp** dành cho đội app host (bên tiêu thụ SDK). Không phải tài liệu phát triển nội
> bộ SDK — cái đó xem [`AndroidGuide.md`](./AndroidGuide.md) / [`AndroidUIGuide.md`](./AndroidUIGuide.md).
> Bề mặt API song ánh Android↔iOS: [`PublicApi.md`](./PublicApi.md). Phân phối Maven: [`Distribution.md`](./Distribution.md).
> Tích hợp trực tiếp — gọi thẳng `PromotionSDK`, không cần wrapper (xem [`InitParity.md`](./InitParity.md) §6).
> Bản iOS đối xứng: [`IosIntegrationGuide.md`](./IosIntegrationGuide.md).

---

## 0. TL;DR

- Thêm **một** dòng dependency Maven `com.ttcn.promotion:promotionSDK` — Gradle tự kéo `promotionLogic`,
  Ktor, coroutines, AppCompat, Glide… Không cần khai tay.
- Mọi thứ host chạm đều nằm ở package `com.ttcn.prm.entry.*` (`PromotionSDK`, `PromotionSDKApi`,
  `PromotionSDKTheme`, `PromotionSDKCallback`…).
- Cấu hình một lần bằng `PromotionSDK.initialize(context, accessToken, baseUrl)`, bơm đơn hàng bằng `updateContext(...)`,
  nhận sự kiện qua `PromotionSDKCallback`.

---

## 1. SDK đóng gói thế nào (vì sao host "sạch")

SDK phát hành dạng **Android library (AAR)** qua Maven. Lõi Kotlin (`:promotionLogic`) được khai
`implementation(projects.promotionLogic)` bên trong SDK → trong Gradle Module Metadata nó nằm ở scope
**runtime**, không phải compile. Hệ quả: host **kéo được** `promotionLogic` để chạy nhưng **không thấy**
`com.ttcn.promotionsdk.core.*` trên compile classpath. Type của lõi không lọt ra API public — cùng ranh
giới mà [`PromotionSDKApi`](#8-headless-api-tự-dựng-ui--promotionsdkapi) cố tình map DTO thay vì trả thẳng model lõi.

Hệ quả cho host:

- ✅ Host chỉ thấy các type `com.ttcn.prm.entry.*` — không chạm được model nội bộ của lõi.
- ⚠️ Ktor, coroutines, AppCompat, Glide, Gson **có** trên compile classpath của host (SDK dùng
  `implementation` các lib này, và metadata Maven đưa chúng xuống runtime + compile transitively). Đây là
  điểm khác iOS (nơi mọi thứ giấu tuyệt đối trong một dynamic framework).

> ⚠️ **Đừng** import `com.ttcn.promotionsdk.core.*` ở host — đó là lõi nội bộ, không nằm trên compile
> classpath; code sẽ không resolve.

---

## 2. Yêu cầu & phân phối

| Mục | Giá trị |
|---|---|
| Artifact | Maven: `com.ttcn.promotion:promotionSDK:1.0.0` (AAR + POM/`.module`) |
| Package public | `com.ttcn.prm.entry.*` |
| minSdk | **24** |
| Namespace SDK | `com.ttcn.promotionsdk` (dùng chung `R` / databinding) |
| UI | XML View + View/DataBinding (**không** Compose) — trả `Fragment` / custom `View` |
| Repo | `mavenLocal()` (đang dùng) hoặc Nexus/Artifactory nội bộ |

---

## 3. Thêm vào project

### 3.1. Khai dependency (Gradle Kotlin DSL)

```kotlin
// settings.gradle.kts hoặc build.gradle.kts (project)
repositories {
    mavenLocal()          // hoặc maven("https://nexus.noi-bo/...") — repo nội bộ
    google()
    mavenCentral()
}

// build.gradle.kts (app module host)
dependencies {
    implementation("com.ttcn.promotion:promotionSDK:1.0.0")
}
```

Gradle đọc metadata → tự kéo `promotionLogic`, Ktor, coroutines, AppCompat, Glide, Gson… đúng version SDK
đã compile. **Không** cần khai tay từng lib. (Cách cũ dùng file-AAR thì host phải tự `implementation(libs.ktor…)`
— đã bỏ; xem [`Distribution.md`](./Distribution.md) §2.)

### 3.2. Kiểm tra nhanh

```kotlin
import com.ttcn.prm.entry.PromotionSDK

Log.d("PRM", PromotionSDK.isInitialized().toString()) // false — resolve OK là được
```

---

## 4. Vòng đời SDK

`PromotionSDK` là **singleton `object`** — mọi điểm vào là static. SDK giữ **một** phiên sống tại một thời điểm.
Điểm lệch với iOS (N1): Android cần `context`.

**Cách tối giản** — đủ cho phần lớn host, chỉ 3 tham số bắt buộc:

```kotlin
import com.ttcn.prm.entry.*

// Sau khi login thành công:
PromotionSDK.initialize(
    context = applicationContext,
    accessToken = auth.accessToken,
    baseUrl = "http://125.235.38.229:8080",
    // các tham số dưới đây là TUỲ CHỌN:
    environment = PromotionEnvironment.PROD,          // mặc định PROD
    availableServices = listOf(                        // cho bottom sheet "Chọn dịch vụ"
        PromotionAvailableService("TOPUP", "Nạp tiền", iconUrl = iconUrl),
    ),
    callback = myCallback,                             // implement PromotionSDKCallback (xem §7)
)
```

Cần cấu hình sâu hơn (theme, ...) thì dùng overload nhận `PromotionSDKOptions`:

```kotlin
PromotionSDK.initialize(applicationContext, PromotionSDKOptions(
    session = PromotionSessionConfig(user.id, auth.accessToken, baseUrl, environment = PromotionEnvironment.PROD),
    availableServices = services, theme = myTheme, callback = myCallback,
))
```

| Việc | API |
|---|---|
| Khởi tạo (tối giản) | `PromotionSDK.initialize(context, accessToken, baseUrl)` |
| Khởi tạo (đầy đủ) | `PromotionSDK.initialize(context, options)` |
| **Login lại** (session mới) | `PromotionSDK.updateSession(accessToken, availableServices?)` |
| Refresh token giữa phiên | `PromotionSDK.updateToken(newToken)` (tuỳ chọn) |
| Kiểm tra đã init | `PromotionSDK.isInitialized(): Boolean` |
| Giải phóng (logout) | `PromotionSDK.release()` |
| Lấy callback đã set | `PromotionSDK.getCallback(): PromotionSDKCallback?` |

**Host gọi `initialize` MỘT LẦN, mỗi login sau chỉ gọi `updateSession`.** SDK tách hai loại field:

| Cố định (khoá ở lần init **đầu**) | Đặc trưng session (đổi mỗi login) |
|---|---|
| `baseUrl`, `environment`, `language`, `theme` | `accessToken`, `availableServices` |

- **Login lần đầu (mở app):** `initialize(...)` với đầy đủ config → SDK **chốt** field cố định.
- **Login lại (user khác / phiên mới):** `PromotionSDK.updateSession(accessToken, availableServices)`
  — chỉ field động; SDK **giữ** field cố định đã khoá + callback. `availableServices` bỏ trống = giữ danh
  mục hiện tại. Context đơn hàng reset về rỗng vì là phiên mới.

  ```kotlin
  if (PromotionSDK.isInitialized()) {
      PromotionSDK.updateSession(accessToken = token, availableServices = services)
  } else {
      PromotionSDK.initialize(applicationContext, token, baseUrl, availableServices = services, callback = cb)
  }
  ```

- **Gọi lại `initialize(...)` cũng an toàn** (guard): SDK khoá field cố định, chỉ áp field động; host lỡ
  truyền field cố định khác đi thì **bỏ qua** kèm cảnh báo log.
- **Đổi field cố định thật** (vd chuyển environment): `release()` rồi `initialize(...)` lại.
- **Refresh token giữa phiên (cùng customer, đang checkout):** `PromotionSDK.updateToken(newToken)` —
  tuỳ chọn, nhẹ hơn; **giữ nguyên** cả context đơn hàng đang ghi.

> `release()` khi chưa init là vô hại; không xoá theme đã lưu.

---

## 5. Bơm context đơn hàng

Giữ **một** phiên từ lúc login, tới màn có voucher mới bơm đơn hàng — **không** re-init:

```kotlin
PromotionSDK.updateContext(
    orderId = order.id,
    orderValue = "500000",   // chuỗi số nguyên VNĐ
    serviceCode = "TOPUP",
    metaData = null,
    // Có dòng sản phẩm → lấy được campaign theo SKU; bỏ trống thì chỉ campaign cấp đơn.
    orderItems = listOf(PromotionOrderItem(skuId = "SKU1", quantity = 1, unitPrice = "500000")),
)
```

SDK đọc lại các giá trị này ở **mỗi** request, nên chỉ cần gọi trước khi mở màn / gọi API. Đọc ngược lại qua
`PromotionSDK.currentOrderId / currentOrderValue / currentServiceCode / currentMetaData` và `PromotionSDK.session`.

> ⚠️ `updateContext` / `api` / `openMyPromotion` / `openPromotionDetail` gọi trước `initialize` sẽ ném
> **`IllegalStateException`** — luôn init trước.

---

## 6. Màn hình UI có sẵn

Android dùng **Fragment** cho màn, và **custom View** cho widget checkout (khác iOS dùng factory
`createEndowView` — N1, xem [`InitParity.md`](./InitParity.md) §5.3).

### 6.1. Màn "Ưu đãi của tôi" / "Chi tiết"

```kotlin
// "Ưu đãi của tôi" — containerViewId null → add lên android.R.id.content; khác null → replace trong container đó
PromotionSDK.openMyPromotion(activity, containerViewId = R.id.promotion_container)

// Chi tiết một ưu đãi (đã biết voucherId, vd từ push notification / deeplink)
PromotionSDK.openPromotionDetail(voucherId = "V123", activity = activity, containerViewId = null)
```

`activity` phải là `FragmentActivity` (AppCompatActivity là con của nó).

### 6.2. Widget checkout — `PRMEndowView` + `PromotionIntegrateManager`

Đặt `PRMEndowView` vào layout XML, rồi tạo `PromotionIntegrateManager` để lo createRedemption/revalidate:

```xml
<com.ttcn.prm.ui.feature.promotion.endowview.PRMEndowView
    android:id="@+id/endowView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
// Trong Fragment.setupUI — nhớ updateContext(orderId, orderValue) trước khi màn dựng widget
val promotionManager = PromotionIntegrateManager.create(binding.endowView)

btnConfirmPayment.setOnClickListener {
    promotionManager.confirmRedemption(
        onSuccess = { proceedPayment() },
        onError = { errorCode -> showError(errorCode) },
    )
}

override fun onDestroyView() {
    super.onDestroyView()
    promotionManager.clear()   // giải phóng coroutine scope
}
```

### 6.3. Feature flag tự gác

Nếu cờ tương ứng TẮT, `openMyPromotion` / `openPromotionDetail` tự hiện Toast `PRM_MOB_021` và **không** mở
màn, rồi báo host qua `onAvailabilityChanged(false)`. Host **không** cần hỏi cờ — chỉ cần nghe callback để ẩn
điểm vào (xem §7).

---

## 7. Nhận sự kiện — `PromotionSDKCallback`

Là `interface` với mọi method có default rỗng → chỉ override cái cần.

```kotlin
val myCallback = object : PromotionSDKCallback {
    override fun onVoucherApplied(voucherId: String) { /* user áp voucher thành công */ }
    override fun onVoucherCleared() { /* user bỏ chọn voucher */ }
    override fun onVoucherCountChanged(count: Int) { /* widget load xong, biết số voucher khả dụng */ }
    override fun onServiceSelected(selection: PromotionServiceSelection) { /* điều hướng tới dịch vụ đã chọn */ }
    override fun onAvailabilityChanged(enabled: Boolean) { /* enabled == false → ẩn điểm vào ưu đãi */ }
    override fun onClosed() { /* màn SDK đóng (user back / release) */ }
}
```

| Sự kiện | Khi nào bắn |
|---|---|
| `onVoucherApplied(voucherId)` | User bấm "Áp dụng" thành công |
| `onVoucherCleared()` | User bỏ chọn voucher trên widget |
| `onVoucherCountChanged(count)` | Widget load xong, biết tổng voucher khả dụng |
| `onServiceSelected(selection)` | User chọn dịch vụ trong bottom sheet |
| `onAvailabilityChanged(enabled)` | Feature flag báo bật/tắt SDK |
| `onClosed()` | Màn SDK bị đóng |

> Callback của SDK là kênh **1-1** (một object nhận sự kiện, truyền qua `initialize(callback = ...)`).
> Muốn nhiều nơi cùng nghe → host tự bọc một object fan-out nhỏ (tuỳ chọn; demo có `DemoPromotionCallback` ~30 dòng).

---

## 8. Headless API (tự dựng UI) — `PromotionSDK.api`

Không dùng UI có sẵn thì gọi `PromotionSDK.api` (kiểu `PromotionSDKApi`). Các hàm là **`suspend`** (khác iOS
dùng closure — N1), trả `PromotionApiResult<T>` (sealed: `Success` / `Failure`). **Phải `initialize` trước.**

```kotlin
lifecycleScope.launch {
    when (val r = PromotionSDK.api.getVouchers(keyword = "grab", page = 0)) {
        is PromotionApiResult.Success -> render(r.data.vouchers) // PromotionVoucherPage
        is PromotionApiResult.Failure -> showError(r.error)      // PromotionSDKError
    }
}
```

Năm hàm (song ánh iOS):

| Hàm | Dùng cho |
|---|---|
| `getVouchers(keyword, serviceCode, tab, page, size)` | Voucher **của khách** (đã sở hữu) |
| `findEligible(orderId, orderValue, items, tabCode, myPage, mySize, otherPage, otherSize)` | Ưu đãi đủ điều kiện cho đơn — 2 nhóm "của tôi"/"khác", phân trang độc lập |
| `getVoucherDetail(voucherId, serviceCode)` | Chi tiết một voucher |
| `validateDiscounts(orderId, orderValue, voucherIds, objectType)` | Validate voucher với đơn trước khi áp |
| `createRedemption(orderId, orderValue, voucherIds, objectType)` | Tạo redemption session để thanh toán |

> Chỉ `CancellationException` mới thoát ra (giữ structured concurrency); mọi lỗi khác về `Failure`.

---

## 9. Xử lý lỗi — `PromotionSDKError`

API **không ném lỗi nghiệp vụ**; mọi thất bại về `PromotionApiResult.Failure(error)`. `PromotionSDKError`
là `sealed class`:

```kotlin
when (val e = result.error) {
    is PromotionSDKError.NetworkFailure -> handle(e.code, e.message) // code = HTTP status; token hết hạn về đây với code == 401
    PromotionSDKError.Timeout           -> { /* timeout */ }
    PromotionSDKError.ParseFailed       -> { /* server trả data:null nơi bắt buộc có */ }
    PromotionSDKError.FeatureDisabled   -> { /* tính năng TẮT qua feature flag (PRM_MOB_021) */ }
    is PromotionSDKError.Unknown        -> { /* e.error: Throwable gốc */ }
    PromotionSDKError.SessionExpired    -> { /* hiện SDK KHÔNG tự phát case này (xem ghi chú) */ }
}
```

Mỗi case có sẵn `message` tiếng Việt để hiển thị. `NetworkFailure` còn có `.serverCode`.

> ⚠️ **Token hết hạn:** SDK hiện **không** map ra `SessionExpired` — lỗi 401 về dưới dạng
> `NetworkFailure(code = 401, …)`. Muốn bắt phiên hết hạn, host kiểm `error.serverCode == 401` rồi refresh
> token và `initialize` lại. (`SessionExpired` là case dành sẵn cho tương lai, đối xứng 2 nền tảng.)

---

## 10. Theming

```kotlin
PromotionSDK.configure(
    PromotionSDKTheme(
        buttonToken = ButtonToken(/* ... */),
        // 6 token: buttonToken, searchBarToken, listItemToken, tabChipToken, tabUnderlineToken, discountBadgeToken
    )
)
```

- `configure(theme)` áp **và lưu lại** → sống qua các lần mở app. Truyền `null` = xoá, về mặc định SDK.
- Truyền `theme` trong `PromotionSDKOptions` lúc init cũng được; để `null` = SDK tự khôi phục theme đã lưu.
- Nhóm token để `null` = giữ mặc định SDK cho nhóm đó.
- Nên cấu hình **một lần** lúc khởi tạo — view đã render chỉ đổi khi được dựng lại (rebind / đẩy màn mới).
- Đọc theme đang áp: `PromotionSDK.currentTheme(): PromotionSDKTheme?`.

---

## 11. Sai lầm thường gặp

| ❌ Sai | ✅ Đúng |
|---|---|
| Gọi `api` / `updateContext` / mở màn trước `initialize` | Luôn `initialize` sau login trước tiên |
| Login lại nhưng đổi luôn baseUrl/environment | Gọi lại `initialize(...)` — field cố định giữ nguyên; đổi thật thì `release()` trước |
| Import `com.ttcn.promotionsdk.core.*` | Chỉ dùng `com.ttcn.prm.entry.*` |
| Truyền `Activity` thường vào `openMyPromotion` | Phải là `FragmentActivity` / `AppCompatActivity` |
| Quên `PromotionIntegrateManager.clear()` trong `onDestroyView` | Luôn `clear()` để huỷ coroutine scope |
| Tự hỏi feature flag để ẩn UI | Lắng nghe `onAvailabilityChanged(enabled)` |
| Tưởng phải tự viết wrapper `PromotionManager` | Gọi thẳng `PromotionSDK` — SDK đã tự lo token/context/callback |

---

## 12. Vòng đời gợi ý (khớp host thật)

```
login thành công        → PromotionSDK.initialize(context, accessToken, baseUrl)
vào màn có voucher       → PromotionSDK.updateContext(orderId, orderValue, ...)
mở UI                    → openMyPromotion / openPromotionDetail / PRMEndowView + PromotionIntegrateManager
login lại (phiên mới)    → PromotionSDK.initialize(...)   (SDK khoá field cố định)
refresh token giữa phiên → PromotionSDK.updateToken(newToken)
logout                   → PromotionSDK.release()
```

---

## 13. Điểm lệch Android ↔ iOS (N1)

| Điểm | Android | iOS |
|---|---|---|
| Phân phối | AAR qua Maven | dynamic `PRM.xcframework` |
| `initialize` | cần `context` | không cần |
| Headless async | `suspend` + `PromotionApiResult` (sealed) | closure + `Result` |
| Widget checkout | `PRMEndowView` (View trong layout) + `PromotionIntegrateManager` | `createEndowView(from:)` factory |
| Mở màn | `Fragment` + `containerViewId?` | push/present `UIViewController` |
| Ẩn deps | `core.*` giấu; **nhưng** Ktor/coroutines lọt classpath host | giấu tuyệt đối trong 1 framework |
| Enum môi trường | `PROD` / `STAGING` | `.prod` / `.staging` |

Xem [`InitParity.md`](./InitParity.md) là nguồn sự thật cho mọi điểm lệch.
