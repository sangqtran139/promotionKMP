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
- Mọi thứ host chạm đều nằm ở package `com.ttcn.prm.entry.**` — và **chỉ** ở đó:
  `entry` (`PromotionSDK`, `PromotionSDKCallback`, `PromotionSDKOptions`, `PromotionSessionConfig`…),
  `entry.api` (headless), `entry.theme` (+ `entry.theme.token`), `entry.endowview` (widget checkout:
  `PRMEndowView`, `AppliedDiscount`).
  Mọi class khác của SDK là `internal` — IDE không gợi ý, và import vào là lỗi compile.
- Cấu hình một lần bằng `PromotionSDK.initialize(context, accessToken, baseUrl)`, bơm đơn hàng bằng `updateContext(...)`,
  nhận sự kiện qua `PromotionSDKCallback`.

---

## 1. SDK đóng gói thế nào (vì sao host "sạch")

SDK phát hành dạng **Android library (AAR)** qua Maven. Lõi Kotlin (`:promotionLogic`) được khai
`implementation(projects.promotionLogic)` bên trong SDK → trong Gradle Module Metadata nó nằm ở scope
**runtime**, không phải compile. Hệ quả: host **kéo được** `promotionLogic` để chạy nhưng **không thấy**
`com.ttcn.promotionsdk.*` trên compile classpath. Type của lõi không lọt ra API public — cùng ranh
giới mà [`PromotionSDKApi`](#8-headless-api-tự-dựng-ui--promotionsdkapi) cố tình map DTO thay vì trả thẳng model lõi.

Hệ quả cho host:

- ✅ Host chỉ thấy các type `com.ttcn.prm.entry.*` — không chạm được model nội bộ của lõi.
- ⚠️ Ktor, coroutines, AppCompat, Glide, Gson **có** trên compile classpath của host (SDK dùng
  `implementation` các lib này, và metadata Maven đưa chúng xuống runtime + compile transitively). Đây là
  điểm khác iOS (nơi mọi thứ giấu tuyệt đối trong một dynamic framework).

> ⚠️ **Đừng** import `com.ttcn.promotionsdk.*` ở host — đó là lõi nội bộ, không nằm trên compile
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
| Repo | **JFrog Artifactory nội bộ** (cần tài khoản đọc); `mavenLocal()` khi dev trên máy team |

---

## 3. Thêm vào project

### 3.1. Khai dependency (Gradle Kotlin DSL)

SDK nằm trên **Artifactory nội bộ**, không phải Maven Central — host phải khai repo đó kèm tài
khoản đọc (xin identity token của bên cấp SDK):

```kotlin
// settings.gradle.kts (host)
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://<artifactory-host>/artifactory/libs-release-local")
            credentials {
                // Để credentials ở ~/.gradle/gradle.properties — KHÔNG commit.
                username = providers.gradleProperty("ttcnArtifactoryUser").orNull
                password = providers.gradleProperty("ttcnArtifactoryToken").orNull
            }
            // Chỉ mở cho group của SDK: repo nội bộ không nên tranh resolve androidx/kotlin
            // với google()/mavenCentral().
            content { includeGroup("com.ttcn.promotion") }
        }
        google()
        mavenCentral()
    }
}

// build.gradle.kts (app module host)
dependencies {
    implementation("com.ttcn.promotion:promotionSDK:1.0.0")
}
```

Gradle đọc metadata → tự kéo `promotionLogic`, Ktor, coroutines, AppCompat, Glide, Gson… đúng version SDK
đã compile. **Không** cần khai tay từng lib. (Cách cũ dùng file-AAR thì host phải tự `implementation(libs.ktor…)`
— đã bỏ; xem [`android/Distribution.md`](./android/Distribution.md) §2.)

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
    session = PromotionSessionConfig(auth.accessToken, baseUrl, environment = PromotionEnvironment.PROD),
    availableServices = services, theme = myTheme, callback = myCallback,
))
```

| Việc | API |
|---|---|
| Khởi tạo (tối giản) | `PromotionSDK.initialize(context, accessToken, baseUrl)` |
| Khởi tạo (đầy đủ) | `PromotionSDK.initialize(context, options)` |
| **Login lại** (session mới) | `PromotionSDK.updateSession(accessToken, availableServices?, callback?)` |
| Refresh token giữa phiên | `PromotionSDK.updateToken(newToken)` (tuỳ chọn) |
| Kiểm tra đã init | `PromotionSDK.isInitialized(): Boolean` |
| Giải phóng (logout) | `PromotionSDK.release()` |
| Lấy callback đã set | `PromotionSDK.getCallback(): PromotionSDKCallback?` |

**Host gọi `initialize` MỘT LẦN, mỗi login sau chỉ gọi `updateSession`.** SDK tách hai loại field:

| Cố định (khoá ở lần init **đầu**) | Đặc trưng session (đổi mỗi login) |
|---|---|
| `baseUrl`, `environment`, `language`, `theme` | `accessToken`, `availableServices` |

- **Login lần đầu (mở app):** `initialize(...)` với đầy đủ config → SDK **chốt** field cố định.
- **Login lại (user khác / phiên mới):** `PromotionSDK.updateSession(accessToken, availableServices, callback)`
  — chỉ field động; SDK **giữ** field cố định đã khoá. `availableServices` / `callback` bỏ trống = giữ danh
  mục / callback hiện tại (muốn **gỡ** callback thì dùng `release()`). Context đơn hàng reset về rỗng vì là
  phiên mới.

  ```kotlin
  if (PromotionSDK.isInitialized()) {
      // callback = ... chỉ cần truyền khi host đổi object nghe sự kiện theo user; bỏ trống = giữ cái cũ.
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

> ⚠️ Gọi trước `initialize` thì:
> - `updateContext` / `api` → ném **`IllegalStateException`**.
> - `openMyPromotion` / `openPromotionDetail` → **không ném**, chỉ `Log.e` rồi bỏ qua (đối xứng
>   `requireImpl` bên iOS). Lý do: `isFeatureEnabled`/`featureFlags` fail-open trả "bật hết" khi chưa
>   init, host hỏi trước rồi hiện nút thì cú bấm của user không được phép giết app.
>
> Luôn init trước — hai điểm mở màn không ném không có nghĩa là bỏ qua được `initialize`, màn sẽ
> không mở và log báo lỗi.

---

## 6. Màn hình UI có sẵn

Android dùng **Fragment** cho màn, và **custom View** cho widget checkout (khác iOS dùng factory
`createEndowView` — N1, xem [`InitParity.md`](./InitParity.md) §5.3).

### 6.1. Màn "Ưu đãi của tôi" / "Chi tiết"

```kotlin
// "Ưu đãi của tôi" — containerViewId null → add lên android.R.id.content; khác null → replace trong container đó
PromotionSDK.openMyPromotion(activity, containerViewId = R.id.promotion_container)

// Chi tiết một ưu đãi (đã biết voucherId, vd từ push notification / deeplink)
// Mặc định returnVoucherOnApply = true → nút "Áp dụng", trả voucher về đúng lời gọi này.
PromotionSDK.openPromotionDetail(
    voucherId = "V123",
    activity = activity,
    containerViewId = null,
    onVoucherApplied = { id -> applyToMyScreen(id) },   // SDK đã tự đóng màn chi tiết
)

// Muốn hành vi cũ (nút "Dùng ngay" → SDK tự mở bottom sheet chọn dịch vụ):
PromotionSDK.openPromotionDetail("V123", activity, returnVoucherOnApply = false)

// Muốn TỰ đóng màn chi tiết (hỏi xác nhận / animation riêng / đi thẳng sang màn khác):
PromotionSDK.openPromotionDetail(
    voucherId = "V123",
    activity = activity,
    hostHandlesDismiss = true,          // SDK báo xong ĐỂ NGUYÊN màn
    onVoucherApplied = { detail ->
        activity.supportFragmentManager.popBackStack()   // host tự đóng
        goToCheckout(detail)
    },
)
```

`activity` phải là `FragmentActivity` (AppCompatActivity là con của nó).

`onVoucherApplied` là **kênh trả gắn với lời gọi**, nên màn nào của host mở cũng nhận đúng chỗ —
khác `PromotionSDKCallback` (singleton, set một lần lúc `initialize`, không biết màn nào đã gọi).
Màn gọi **không cần** là màn thanh toán.

### 6.2. Widget checkout — `PRMEndowView`

Đặt `PRMEndowView` vào layout XML; nút thanh toán của host gọi `confirmRedemption` trên chính widget:

```xml
<com.ttcn.prm.ui.feature.endowview.PRMEndowView
    android:id="@+id/endowView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content" />
```

```kotlin
// Trong Fragment.setupUI — nhớ updateContext(orderId, orderValue) trước khi màn dựng widget

// User bấm vào widget → mở màn "Chọn ưu đãi". SDK dựng sẵn fragment đã nối với widget;
// host chỉ add vào container của mình. Kiểu trả về là `androidx.fragment.app.Fragment` trần —
// class thật là UI nội bộ của SDK, host không cần biết tên.
binding.endowView.onOpenVoucherSelection = {
    parentFragmentManager.beginTransaction()
        .add(R.id.container, PromotionSDK.createChoosePromotionFragment(binding.endowView))
        .addToBackStack(null)
        .commit()
}

btnConfirmPayment.setOnClickListener {
    binding.endowView.confirmRedemption(
        onSuccess = { proceedPayment() },
        onError = { errorCode -> showError(errorCode) },   // có thể là PRM_MOB_021, xem dưới
    )
}

```

> **`confirmRedemption` cũng bị feature flag gác.** Cờ `VOUCHER_REDEEM` (hoặc công tắc tổng
> `ENABLE_ALL`) TẮT → `onError("PRM_MOB_021")`, **không gọi mạng**, và **không** gọi `onSuccess`.
> Host phải xử lý mã này như một lỗi chặn thanh toán, đừng cho đi tiếp: giá đang hiển thị ở
> `PRMEndowView` là giá đã giảm, mà server không hề ghi nhận redemption.
>
> Ngoại lệ có chủ đích: user **chưa chọn voucher nào** thì `confirmRedemption` gọi `onSuccess` ngay,
> bất kể cờ. Kill-switch tắt ưu đãi, không được tắt thanh toán của host.

### 6.3. Feature flag — SDK tự gác, host hỏi thêm được

Nếu cờ tương ứng TẮT, `openMyPromotion` / `openPromotionDetail` tự hiện Toast `PRM_MOB_021` và **không** mở
màn, rồi báo host qua `onAvailabilityChanged(false)`. **Không làm gì thêm thì kill-switch vẫn chạy đủ.**

Muốn mượt hơn — ẩn hẳn nút trước khi user kịp bấm — thì hỏi SDK:

```kotlin
// Đọc cache, đồng bộ, không gọi mạng
binding.btnMyVoucher.isVisible = PromotionSDK.isFeatureEnabled(PromotionFeature.VOUCHER_LIST)

// Hoặc nạp lại từ server rồi dựng UI (onComplete chạy trên main thread)
PromotionSDK.refreshFeatureFlags { flags ->
    binding.groupPromotion.isVisible = flags.all
    binding.btnMyVoucher.isVisible = flags.voucherList
}
```

| Hàm | Trả gì |
|---|---|
| `PromotionSDK.featureFlags()` | `PromotionFeatureFlagsSnapshot` — toàn bộ cờ, đọc cache |
| `PromotionSDK.isFeatureEnabled(feature)` | `Boolean` cho một `PromotionFeature` |
| `PromotionSDK.isSdkEnabled()` | Công tắc tổng — `false` thì ẩn **toàn bộ** điểm vào ưu đãi |
| `PromotionSDK.refreshFeatureFlags { … }` | Nạp lại từ server, trả snapshot mới trên main thread |

Ba điều cần nhớ:

- **Snapshot đã áp sẵn công tắc tổng**: `flags.all == false` → mọi field còn lại đều `false`.
- **Fail-open**: chưa `initialize()` hoặc chưa gọi được API lần nào → trả bật hết. Không hàm nào ném lỗi.
- Cờ có thể đổi giữa phiên → **đừng cache lại** snapshot, hỏi lại mỗi khi dựng UI.

Không muốn hỏi chủ động thì chỉ cần nghe `onAvailabilityChanged` (§7).

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
| `onAvailabilityChanged(enabled)` | Feature flag báo bật/tắt SDK — bắn **cả `true` lẫn `false`**: nạp cờ xong sau `initialize`, mỗi lần `refreshFeatureFlags`, widget checkout đổi trạng thái, và khi user bấm mà bị chặn. Nhớ đọc tham số `enabled`, đừng coi mọi lần gọi là "tắt". |
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
import com.ttcn.prm.ui.theme.PromotionSDKTheme
import com.ttcn.prm.ui.theme.token.ButtonToken

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
| Import `com.ttcn.promotionsdk.*` | Chỉ dùng `com.ttcn.prm.entry.*` |
| Truyền `Activity` thường vào `openMyPromotion` | Phải là `FragmentActivity` / `AppCompatActivity` |
| Tự hỏi feature flag để ẩn UI | Lắng nghe `onAvailabilityChanged(enabled)` |
| Tưởng phải tự viết wrapper `PromotionManager` | Gọi thẳng `PromotionSDK` — SDK đã tự lo token/context/callback |

---

## 12. Vòng đời gợi ý (khớp host thật)

```
login thành công        → PromotionSDK.initialize(context, accessToken, baseUrl)
vào màn có voucher       → PromotionSDK.updateContext(orderId, orderValue, ...)
mở UI                    → openMyPromotion / openPromotionDetail / PRMEndowView
login lại (phiên mới)    → PromotionSDK.initialize(...)   (SDK khoá field cố định)
refresh token giữa phiên → PromotionSDK.updateToken(newToken)
logout                   → PromotionSDK.release()
```

---

## 13. Điểm lệch Android ↔ iOS (N1)

| Điểm | Android | iOS |
|---|---|---|
| Phân phối | AAR qua Maven | dynamic `Promotion.xcframework` |
| `initialize` | cần `context` | không cần |
| Headless async | `suspend` + `PromotionApiResult` (sealed) | closure + `Result` |
| Widget checkout | `PRMEndowView` (View trong layout) + `confirmRedemption` | `createEndowView(from:)` + `PromotionSDK.confirmRedemption` |
| Mở màn | `Fragment` + `containerViewId?` | push/present `UIViewController` |
| Ẩn deps | `core.*` giấu; **nhưng** Ktor/coroutines lọt classpath host | giấu tuyệt đối trong 1 framework |
| Enum môi trường | `PROD` / `STAGING` | `.prod` / `.staging` |

Xem [`InitParity.md`](./InitParity.md) là nguồn sự thật cho mọi điểm lệch.
