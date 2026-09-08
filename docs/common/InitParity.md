# Init Parity Spec — Android ↔ iOS

> **Nguồn sự thật duy nhất** cho tầng khởi tạo (init) của Promotion SDK trên cả hai nền tảng.
> Mọi thay đổi liên quan `PromotionSDK` / `PromotionSDKOptions` / `PromotionSessionConfig` /
> `PromotionSDKCallback` **bắt buộc** cập nhật file này trước khi/đi kèm khi sửa code.

## Mục lục

<!-- toc -->
- [0. Nguyên tắc](#0-nguyên-tắc)
- [1. Entry `PromotionSDK` — bảng ánh xạ canonical](#1-entry-promotionsdk--bảng-ánh-xạ-canonical)
- [2. Config types — bảng ánh xạ](#2-config-types--bảng-ánh-xạ)
  - [2.1. Token — `PromotionTokenSource`, một khái niệm duy nhất](#21-token--promotiontokensource-một-khái-niệm-duy-nhất)
- [3. `PromotionSDKCallback` — hợp nhất theo iOS (6 sự kiện), tên trùng cả 2 bên](#3-promotionsdkcallback--hợp-nhất-theo-ios-6-sự-kiện-tên-trùng-cả-2-bên)
  - [3.1. Cờ tính năng chặn điểm mở màn — `onFeatureDisabled`](#31-cờ-tính-năng-chặn-điểm-mở-màn--onfeaturedisabled)
- [4. Ngoại lệ N1 — buộc lệch (đã duyệt)](#4-ngoại-lệ-n1--buộc-lệch-đã-duyệt)
- [5. Bố cục file (target, đối xứng)](#5-bố-cục-file-target-đối-xứng)
  - [5.1. Widget](#51-widget)
- [6. Tích hợp trực tiếp — host không cần wrapper](#6-tích-hợp-trực-tiếp--host-không-cần-wrapper)
- [7. Việc thực hiện — checklist theo thứ tự](#7-việc-thực-hiện--checklist-theo-thứ-tự)
<!-- /toc -->

---

## 0. Nguyên tắc

1. **Ép trùng chữ tuyệt đối** tên hàm, tên tham số, thứ tự tham số, comment và vị trí file — *nơi ngôn
   ngữ cho phép*.
2. Điểm **không thể** trùng do ràng buộc ngôn ngữ/nền tảng → liệt kê ở [§4 Ngoại lệ N1](#4-ngoại-lệ-n1--buộc-lệch-đã-duyệt)
   kèm lý do. Không được phát sinh ngoại lệ mới nếu không ghi vào đây.
3. Host gọi **thẳng** `PromotionSDK` — không cần wrapper (đã bỏ khuyến nghị `PromotionServing`, xem [§6](#6-tích-hợp-trực-tiếp--host-không-cần-wrapper)).
4. Ký hiệu: ✅ đã khớp · 🔧 phải nắn · ⚠️ cần bạn xác nhận · N1 ngoại lệ nền tảng.

---

## 1. Entry `PromotionSDK` — bảng ánh xạ canonical

| Khái niệm | Canonical (đích) | Android hiện tại | iOS hiện tại | TT |
|---|---|---|---|---|
| Khởi tạo (options) | **`initialize`** | `fun initialize(context, options)` | `initialize(options:)` | ✅ tên trùng; N1 nhỏ: Android cần `context` (iOS không) |
| Khởi tạo (phẳng) | **`initialize`** overload | `initialize(context, tokenSource, baseUrl, environment=, language=, availableServices=, theme=, callback=)` | `initialize(tokenSource:baseUrl:environment:language:availableServices:theme:callback:)` | ✅ đủ cho phần lớn host — chỉ 2 tham số bắt buộc; uỷ thẳng cho overload options |
| Login lại / mở lại app | **`initialize`** | gọi lại `initialize(context, options)` | gọi lại `initialize(options:)` | ✅ **lối duy nhất**, `updateSession`/`updateToken` đã bỏ. `baseUrl`/`environment`/`language`/`theme` là cấu hình **tĩnh**: đặt ở lần đầu (hoặc lần đầu sau `release()`) rồi **dùng lại** — host chỉ đưa `tokenSource`. `release()` **giữ** cấu hình tĩnh + theme, chỉ xoá dữ liệu phiên |
| Token hết hạn giữa phiên | — | không có API | không có API | ✅ **đã bỏ `updateToken`**: SDK đọc lại `tokenSource.currentToken()` ở mỗi request, không có gì để đẩy vào |
| Giải phóng | `release()` | ✅ | ✅ | ✅ |
| Trạng thái | `isInitialized()` | ✅ | ✅ | ✅ |
| Headless | `api` | ✅ `val api` | ✅ `var api` | ✅ |
| Session | `session` | ✅ | ✅ | ✅ |
| Order động | `currentOrderId` / `currentOrderValue` / `currentServiceCode` / `currentMetaData` | ✅ | ✅ | ✅ |
| Cập nhật context | `updateOrderInfo(orderId, productId, orderValue, metaData, skuSourceId, productName, productCategory, quantity, unitPrice)` | ✅ | ✅ | ✅ `orderId`/`productId` **bắt buộc** (B10); đơn chỉ 1 dòng sản phẩm → field phẳng thay vì `List<PromotionOrderItem>`, SDK tự bọc lại thành list nội bộ; `serviceCode` bỏ khỏi tham số ở cả 2 bên (B8); `skuId` đổi tên `skuSourceId`, để trống thì **không** gửi field này lên `findEligible` (B11) |
| Đặt theme | `configure(theme)` | ✅ | ✅ | ✅ |
| Đọc theme | **`currentTheme()`** (hàm, cả 2) | `fun currentTheme()` | `var currentTheme` 🔧 | 🔧 iOS đổi property → hàm |
| Đọc callback | **`getCallback()`** (cả 2) | `fun getCallback()` | *(thiếu)* 🔧 | 🔧 iOS bổ sung |
| Cờ — chụp tất cả | `featureFlags()` | `fun featureFlags(): PromotionFeatureFlagsSnapshot` | `featureFlags() -> PromotionFeatureFlagsSnapshot` | ✅ cache đồng bộ, fail-open |
| Cờ — tra một | `isFeatureEnabled(feature)` | `fun isFeatureEnabled(feature: PromotionFeature)` | `isFeatureEnabled(_ feature: PromotionFeature)` | ✅ enum public mỗi bên; hằng chuỗi lõi không ra tới host |
| Cờ — công tắc tổng | `isSdkEnabled()` | `fun isSdkEnabled()` | `isSdkEnabled()` | ✅ uỷ cho `PromotionFeatureGate.isSdkEnabled()` |
| Cờ — nạp lại | `refreshFeatureFlags(…)` | `fun refreshFeatureFlags(onComplete? = null)` | `refreshFeatureFlags(completion:)` | ✅ callback về **main thread** |
| Mở "Ưu đãi của tôi" | `openMyPromotion(host[, containerViewId])` | `openMyPromotion(activity, containerViewId?)` | `openMyPromotion(from:)` | N1 (Fragment/containerViewId Android-only) |
| *Gác chưa-init của 2 hàm mở màn* | log rồi bỏ qua, **không ném** | `requireInitialized(caller)` | `requireImpl(_:)` | ✅ |
| Mở chi tiết | `openPromotionDetail(voucherId, host[, containerViewId], returnVoucherOnApply, onVoucherApplied)` | `openPromotionDetail(voucherId, activity, containerViewId?, returnVoucherOnApply = true, hostHandlesDismiss = false, onVoucherApplied: ((PromotionVoucherDetail) -> Unit)? = null)` | `openPromotionDetail(voucherId:from:returnVoucherOnApply:hostHandlesDismiss:onVoucherApplied:)` | ✅ boolean + closure đối xứng, **không enum ở nền tảng nào**; cờ xuống thẳng `arguments` (Android) / `DataModel` (iOS). Callback trả object `PromotionVoucherDetail`; UI nội bộ chuyền `VoucherDetail` domain, map sang DTO ở ranh giới public. `hostHandlesDismiss` = ai pop màn chi tiết sau khi "Áp dụng" (mặc định SDK tự pop) |
| Widget checkout | *(không nằm trên `PromotionSDK`)* | `PRMEndowView` (View) | `createEndowView(from:)` ×3 | N1 (xem [§5.1](#51-widget)) |

**Callback identity:** bỏ tham số `sdk` ở **mọi** method callback trên cả 2 nền tảng (SDK là singleton →
không cần truyền identity). iOS gỡ luôn hack `callbackToken`.

---

## 2. Config types — bảng ánh xạ

| Type | Field / thứ tự (canonical) | Android | iOS | TT |
|---|---|---|---|---|
| `PromotionSDKOptions` | `session, availableServices, theme, callback` | ✅ | ✅ | ✅ |
| `PromotionSessionConfig` | `tokenSource, baseUrl, language = "vi-VN", environment` | ✅ (`baseUrl`) | `baseURL` 🔧 | 🔧 **iOS đổi `baseURL` → `baseUrl`**. Không còn `accessToken` |
| `PromotionTokenSource` | `currentToken()`, `refreshToken(onResult)` | `interface`, `refreshToken` có default `= onResult(false)` | `protocol`, default ở `public extension` | ✅ **nguồn token duy nhất** — xem [§Token](#21-token--promotiontokensource-một-khái-niệm-duy-nhất) |
| `PromotionEnvironment` | `PROD, STAGING` ⚠️ hoặc `prod, staging` ⚠️ | `PROD, STAGING` | `prod, staging` | ⚠️ **cần chốt spelling** (xem ghi chú) |
| `PromotionAvailableService` | `productId, productName, skuSourceId = "", iconUrl = ""` | ✅ | ✅ | ✅ |
| `PromotionMutableContext` (internal) | `session` + `orderId/orderValue/serviceCode/metaData/orderItems` + 7 getter + `refreshAccessToken` | ✅ | ✅ | ✅ nội bộ, vị trí xem [§5](#5-bố-cục-file-target-đối-xứng). **Không có field token nào** — chỉ chuyển tiếp sang `tokenSource` |
| `PromotionOrderItem` | `skuSourceId, productId, productName, productCategory, quantity, unitPrice` | ✅ | ✅ | ✅ `getOrderItems()` map sang `EligibleOrderItem` của lõi ở **cả hai** bên |

### 2.1. Token — `PromotionTokenSource`, một khái niệm duy nhất

Token vào SDK qua **một** đường: `PromotionSessionConfig.tokenSource`. Không có `accessToken`, không
có `updateToken`, không có bản sao nào bên trong SDK. `PromotionMutableContext` chỉ **chuyển tiếp**:

```
getAccessToken()     → session.tokenSource.currentToken()
refreshAccessToken() → session.tokenSource.refreshToken(onResult)
```

Vì sao: token của host sống ngắn (≈15 phút) và host tự lấy token mới theo cơ chế riêng. Mọi trạng
thái token do SDK giữ đều là một bản sao có thể lệch với host — đó chính là con bug gốc. Không giữ
gì thì không lệch được.

Lõi hỏi `getAccessToken()` ở **mỗi** request (`defaultRequest { }` trong `PromotionHttpClient`), nên
host không phải báo gì cho SDK khi token đổi.

Ăn 401 → `PromotionRemoteDataSource.apiCall` gọi `refreshAccessToken` qua `TokenRefreshGate` rồi chạy
lại request **đúng một lần**; `false` thì để `TOKEN_EXPIRED` nổi lên và bắn `onExpireToken()`. Cổng
gộp mọi request 401 cùng lúc thành **một** lần hỏi host (`generation` + `Mutex`), bỏ cuộc sau 15 giây
nếu host không gọi lại callback.

**Ràng buộc phải giữ đối xứng khi sửa:**

1. Hai hàm **cùng tên, cùng thứ tự** hai bên: `currentToken()` rồi `refreshToken(_:)`.
2. `refreshToken` trả **`Boolean`/`Bool`**, không phải token mới. Đổi nó thành `String?` là mở đường
   thứ hai cho token vào SDK, kèm câu hỏi "SDK dùng chuỗi host trả hay đọc lại kho của host?" — đúng
   thứ mơ hồ mà thiết kế này loại bỏ. Hợp đồng: host ghi vào kho của mình **rồi** báo `true`.
3. Mặc định `refreshToken` = `false` (chịu ngay). Kotlin khai bằng default method trên interface;
   Swift **không** thừa hưởng default của protocol Kotlin lẫn của protocol Swift khai trong protocol
   body, nên bản mặc định iOS phải nằm ở `public extension PromotionTokenSource`.
4. `PromotionMutableContext` **không được** cache token. Thêm bất kỳ field token nào ở đó là tái tạo
   con bug gốc. Test khoá điều này: `PromotionMutableContextTokenTest`.
5. `currentToken()` chạy trên **thread nền** (`ioDispatcher` bọc `apiCall` ở lõi) và nằm trên đường
   dựng header của mọi request → phải non-blocking, thread-safe, không `@MainActor` ở Swift.
6. `PromotionSessionConfig` chỉ còn `tokenSource, baseUrl, language, environment`. Chỗ duy nhất dựng
   tay đủ field ở iOS là `PromotionSDK.initialize(options:)` (nhánh dùng lại `staticConfig`) —
   Android có `copy()` nên tự mang.

> **Enum case (đã chốt):** giữ convention mỗi bên (`PROD`↔`prod`) — N1 *duy nhất được miễn* vì ánh
> xạ 1-1 hiển nhiên. Mọi tên hàm/tham số khác đã ép trùng chữ tuyệt đối.

---

## 3. `PromotionSDKCallback` — hợp nhất theo iOS (6 sự kiện), tên trùng cả 2 bên

Bỏ phong cách `vdsPromotion(_:didX:)` (ObjC-delegate) để tên **trùng chữ** được với Kotlin. Bỏ tham số `sdk`.

| Sự kiện (canonical) | Payload | Ghi chú |
|---|---|---|
| `onVoucherApplied(voucherId)` | `String` | Theo iOS (voucherId). Android đã **rút về voucherId** (bỏ `List<AppliedDiscount>` ở callback); `AppliedDiscount` vẫn dùng ở luồng widget, không ở callback. |
| `onServiceSelected(selection)` | `PromotionServiceSelection` (`voucherId, productId, productName, skuSourceId = "", iconUrl`) | Đã đổi tên type `PromotionSDKServiceSelection` → **`PromotionServiceSelection`** (trùng cả 2). Từ 2026-08-04: thêm field `skuSourceId` (lấy từ `PromotionAvailableService.skuSourceId` host cấu hình), map xuyên suốt `AvailableService`/`ServiceSelectorUiItem` (Android) và `AvailableService`/`ServiceSelectorItem` (iOS). |
| `onExpireToken()` | *(không tham số)* | Từ 2026-08-18: bắn khi 1 API bên trong màn SDK (Ưu đãi của tôi / Tìm kiếm / Chi tiết / Chọn ưu đãi / widget Endow) trả HTTP 401. `toErrorCode()` map `httpStatus` 401 → `PromotionErrorCodes.TOKEN_EXPIRED` (`promotionLogic`, dùng chung 5 store, cả 2 nền tảng); từ 2026-08-19 **403 không còn map sang mã này** (không đủ quyền ≠ hết hạn token) — chỉ 401. Android: `PRMStoreViewModel.effects` (4 màn Fragment) và `PRMEndowView.renderState` (widget) tự bắn `PromotionSDK.getCallback()?.onExpireToken()` khi thấy mã này. iOS (từ 2026-08-18): `PRMStoreViewModel.emitErrorIfNeeded` (4 màn Store-based) và `PromotionSDKImpl.render(_:on:)` (widget Endow) làm y hệt — effect/state vẫn chảy tiếp xuống UI như cũ (không nuốt lỗi). Headless (`PromotionSDKApi`) **không** đi qua callback này, xem [HeadlessAPI.md](./HeadlessAPI.md)/[PublicApi.md](./PublicApi.md). |

**Đã loại:**
- Android `onError(errorCode)` — iOS không có, bỏ theo lựa chọn "hợp nhất theo iOS".
- `onVoucherCleared` / `onVoucherCountChanged` / `onAvailabilityChanged` / `onClosed` — **host không
  cần biết** mấy thứ này. Widget tự quản trạng thái của nó; việc bật/tắt theo cờ do SDK tự xử lý.
  Cờ tính năng chặn một điểm mở màn thì báo qua tham số `onFeatureDisabled` của chính hàm `open…`
  (xem dưới), không qua callback toàn cục.

`PromotionSDKCallback` nay còn **ba** sự kiện: `onVoucherApplied`, `onServiceSelected`, `onExpireToken`.

### 3.1. Cờ tính năng chặn điểm mở màn — `onFeatureDisabled`

Cả ba hàm mở màn nhận thêm tham số **tuỳ chọn**:

| | Android | iOS |
|---|---|---|
| | `openMyPromotion(activity, containerViewId?, onFeatureDisabled?)` | `openMyPromotion(from:onFeatureDisabled:)` |
| | `openPromotionDetail(…, onFeatureDisabled?)` | `openPromotionDetail(…, onFeatureDisabled:)` |
| | `openChoosePromotion(…, onFeatureDisabled?)` | — (chưa có bản public) |

Truyền → host tự xử lý. **Không truyền → SDK tự hiện popup** PRM_MOB_021. Màn không mở trong cả hai
trường hợp.

Nhận closure ngay ở hàm `open…` chứ không dùng callback toàn cục là **có lý do kỹ thuật**:
`PromotionSDKCallback` là interface/protocol có default method — gọi vào thì luôn trúng thân mặc
định, SDK **không phân biệt được** "host có implement" với "host mặc kệ", nên không thể dựa vào nó
để quyết định có tự hiện popup hay không.

Tất cả method đều `default {}` (Kotlin default method / Swift protocol extension) → host chỉ implement cái cần.

`PromotionServiceSelection` nằm **cùng file** `PromotionSDKCallback` ở cả hai bên. Sự kiện chọn dịch vụ
được các màn nội bộ phát **trực tiếp** qua `PromotionSDK.getCallback()?.onServiceSelected(...)` từ **đủ
ba màn ở cả hai nền tảng**: `MyPromotion` / `SearchMyPromotion` / `PromotionDetail`
(iOS: ViewController; Android: Fragment).

---

## 4. Ngoại lệ N1 — buộc lệch (đã duyệt)

| Điểm | Android | iOS | Vì sao không trùng được |
|---|---|---|---|
| Tham số môi trường app | `initialize(context, …)` | `initialize(…)` | Android cần `Context`; iOS không. (Tên hàm đã trùng `initialize`.) |
| Kiểu host khi mở màn | `activity: FragmentActivity` | `from: UIViewController` | Kiểu nền tảng khác nhau. |
| Tham số `containerViewId` | có | *(không)* | Chỉ Android có FragmentContainer. |
| Facade/Impl | 1 `object` gộp | `PromotionSDK` + `PromotionSDKImpl` | iOS cần box giấu type để tránh cross-module deserialization (binary-interface trick). |
| Widget | `PRMEndowView` (View) | `createEndowView` (factory) | Idiom nền tảng (XML View vs factory UIView). Wrapper chuẩn hoá — [§5.1](#51-widget). |
| Enum case (nếu chọn giữ) | `PROD/STAGING` | `prod/staging` | Convention enum mỗi ngôn ngữ (đang chờ ⚠️ §2). |
| Ràng buộc View↔ViewModel | `StateFlow` + `collectFlow` | closure `onState`/`onEffect` | Không có `Flow` trong Swift. Hình dạng đã **ép trùng**: cùng `handleAction`, cùng `UiState`/`Effect`, `onState` replay state hiện tại khi gán (mô phỏng `StateFlow`). Từ 2026-07-23 iOS **không** còn Combine. |
| Cách hiện lỗi / thông báo | popup | popup | **Đã đồng nhất: toast bỏ hẳn ở cả 2 bên.** Cần báo user → `PRMBaseFragment.showErrorDialog` (Android, `PRMBaseConfirmDialog`) ↔ `PRMBaseViewController.showErrorDialog` (iOS, `PRMConfirmationDialog`). Màn đã có shimmer/empty-view nói thay thì **không hiện gì**. Chuỗi lỗi trùng nhau: `mapPromotionError` ↔ `PromotionUIStrings.errorMessage`. Xem [ErrorHandling.md](./ErrorHandling.md). |

---

## 5. Bố cục file (target, đối xứng)

Mục tiêu: **cùng số file, cùng tên khái niệm, cùng thứ tự khai báo** trong mỗi file.

| Vai trò | Android (`.../entry/`) | iOS (`.../iosPromotionSDK/Entry/`) |
|---|---|---|
| Entry object/facade | `PromotionSDK.kt` | `PromotionSDK.swift` |
| (impl box — N1) | — | `PromotionSDKImpl.swift` |
| Options | `PromotionSDKOptions.kt` | `PromotionSDKOptions.swift` |
| Config (session + env + availableService + map + mutableContext) | `PromotionConfig.kt` | `PromotionConfig.swift` |
| Callback (+ service selection type) | `PromotionSDKCallback.kt` | `PromotionSDKCallback.swift` |

`PromotionConfig` (cả 2 bên) gộp: `PromotionSessionConfig`, `PromotionEnvironment`, `PromotionAvailableService`,
hàm map options→core, và `PromotionMutableContext` (nội bộ). `availableServices` đi vào **core config**
(`toCoreConfig`) ở **cả hai** nền tảng → `ServiceSelectorBuilder`/`MyPromotionViewModel` đọc lại từ lõi
qua **một** hàm dùng chung `configuredServicesFor(applicableProducts)` ở `promotionLogic`
(nó tự gọi `PromotionContainer.requireConfig().availableServices`). Không còn holder Swift riêng
(`PromotionSessionRuntime` đã xoá). File chỉ-iOS còn lại: `PromotionSDKImpl.swift` (box binary-interface, N1).

> Ánh xạ mapping options→core: Android `PromotionSDKOptions.toCoreConfig(contextProvider)`, iOS
> `PromotionSDKOptions.toCoreConfig(context:isDebug:)` — cùng là extension trong `PromotionConfig.{kt,swift}`.
> Lệch duy nhất là `isDebug` (N1: iOS không có `ApplicationInfo.FLAG_DEBUGGABLE` nên host/impl truyền vào).
> `baseUrl` đi thẳng từ session ở **cả hai** bên — không bên nào có giá trị fallback.

### 5.1. Widget

Giữ lệch có chủ đích: Android `PRMEndowView` (View đặt trong layout), iOS `createEndowView(from:)`.
Wrapper phơi **một** API chung `makeCheckoutWidget(...)` để host không thấy khác biệt.

**Chi tiết giảm giá (`AppliedDiscount`) — N1, đã duyệt:** chỉ **Android** phơi `AppliedDiscount` +
`PRMEndowView.setDiscountDetails(...)` để host đọc breakdown giảm giá **trực tiếp** từ widget. **iOS
cố tình KHÔNG phơi** type này: `createEndowView(from:)` trả `UIView` đục (che type nội bộ theo box
binary-interface — [§4](#4-ngoại-lệ-n1--buộc-lệch-đã-duyệt)), nên host iOS chỉ nhận **id** voucher đã áp qua
`onVoucherApplied(voucherId)`; muốn biết số tiền giảm thì gọi headless `PromotionSDK.api.validateDiscounts(...)`
với `voucherId` đó (trả `PromotionValidationResult` — cùng dữ liệu, đi qua ranh giới DTO hợp lệ). Đây
**không** phải thiếu sót cần "sửa": phơi `AppliedDiscount` bên iOS sẽ kéo type lõi vào `.swiftinterface`
(xem [PublicApi.md §3](./PublicApi.md)).

---

## 6. Tích hợp trực tiếp — host **không** cần wrapper

Host gọi thẳng `PromotionSDK`, **không** cần wrapper anti-corruption — mọi ràng buộc đã nằm trong
chính SDK. Tích hợp tối thiểu:

```text
// Sau khi login:
PromotionSDK.initialize(context, tokenSource, baseUrl)   // overload phẳng, 3 tham số

// Token hết hạn: KHÔNG cần gọi gì — SDK đọc lại tokenSource.currentToken() ở mỗi request.

// Vào màn có voucher:
PromotionSDK.updateOrderInfo(orderId, productId, orderValue)

// Mở UI có sẵn:
PromotionSDK.openMyPromotion(activity[, containerViewId])   // Android
PromotionSDK.openMyPromotion(from: viewController)           // iOS

// Logout:
PromotionSDK.release()
```

Vì sao không còn cần wrapper:
- **`initialize` phẳng** — 3 tham số bắt buộc, không phải lồng 3 constructor.
- **`tokenSource`** — host khai nguồn token một lần; SDK tự đọc lại, không ai phải đồng bộ gì.
- **Callback** là interface/protocol có default method → host chỉ implement sự kiện mình cần, truyền
  thẳng qua `initialize(callback:)`. Không cần fan-out.
- **Headless** trả DTO công khai (`PromotionApiResult`/`PromotionVoucher`…) — không rò type lõi, host
  dùng trực tiếp không cần lớp map.

Host **vẫn có thể** tự bọc một lớp mỏng nếu muốn anti-corruption trong kiến trúc của họ — đó là lựa
chọn của host, **không** phải yêu cầu của SDK. Demo (`androidApp`/`iosApp`) gọi thẳng `PromotionSDK`
làm bằng chứng SDK đủ đơn giản để dùng không cần wrapper.

---

## 7. Việc thực hiện — checklist theo thứ tự

- [x] **B1.** Duyệt spec. Chốt 3 điểm ⚠️: (a) **enum case giữ convention** mỗi bên (`prod`↔`PROD`) — N1 duy nhất được miễn; (b) **bỏ `onError`**; (c) đổi `PromotionSDKServiceSelection → PromotionServiceSelection`.
- [x] **B2.** Nắn 🔧: `baseURL→baseUrl`, `currentTheme` property→hàm, thêm `getCallback()` (iOS); đổi thứ tự tham số `openPromotionDetail` + đổi `init→initialize` (Android); gộp file config iOS về `PromotionConfig.swift`; `availableServices` đi qua core config 2 bên (xoá `PromotionSessionRuntime`).
- [x] **B3.** Hợp nhất callback 2 bên theo [§3], bỏ tham số `sdk`, `PromotionSDKServiceSelection→PromotionServiceSelection`, bỏ `onError`. (Sau đó rút tiếp còn 2 sự kiện — xem §3.)
- [x] **B4.** Wrapper `PromotionManager` đối xứng 2 nền tảng theo [§6] (adapter callback), rewire điểm init chính của demo.
- [x] **B5.** Cập nhật `docs/PublicApi.md` + demo. Spec này **đã đồng bộ** với code (2026-07-16).
- [x] **B6.** (2026-07-23) Rà soát lại sau khi rollout shared store, nắn nốt các điểm lệch:
  `orderItems` vào `updateContext` 2 bên (Android trước đó **không** override `getOrderItems()` → luôn
  rỗng); Android bổ sung bottom sheet "Chọn dịch vụ" ở màn Tìm kiếm; màn "Chọn ưu đãi" của Android
  **chờ validate xong** rồi mới đóng (lỗi → ở lại + báo, như iOS); màn chi tiết **bỏ hẳn seed từ ngoài**
  ở cả 2 bên — chỉ hiển thị khi API detail trả về (xem [features/PromotionDetail.md](../features/PromotionDetail.md));
  `PRMEndowViewModel`
  Android đổi tên hàm khớp iOS (`loadInitial`/`setApplied`/`markUnavailable`/`clearApplied`/`consumeError`);
  iOS bỏ `baseUrl` fallback hardcode, dùng rule "Xem thêm" dùng chung (`mySeeMoreState`/`visibleMyOffers`)
  và nhận `isEnabled` từ store thay vì tự suy lại.
- [x] **B7.** (2026-08-14) Đổi tên `updateContext` → `updateOrderInfo` ở cả 2 nền tảng. Đơn chỉ hỗ trợ
  **một** dòng sản phẩm nên bỏ tham số `orderItems: List<PromotionOrderItem>` (Android) /
  `[PromotionOrderItem]` (iOS), thay bằng field phẳng `skuId`/`productId`/`productName`/
  `productCategory`/`quantity`/`unitPrice` (đều tuỳ chọn, mặc định `nil`/`null`); SDK tự bọc lại
  thành `List<PromotionOrderItem>` 1 phần tử hoặc rỗng trước khi ghi vào
  `PromotionMutableContext.orderItems` — tầng dưới (`getOrderItems()`, `ChoosePromotionStore`/
  `EndowStore`) không đổi. Điều kiện có/không item ban đầu khoá theo `skuId`, sau đổi sang `productId`
  ở B9.
- [x] **B8.** (2026-08-14) Bỏ tham số `serviceCode` khỏi `updateOrderInfo` ở **cả 2** nền tảng — không
  còn cách nào để host set `PromotionMutableContext.serviceCode` qua public API. Field nội bộ
  `serviceCode` + getter `getService()`/`currentServiceCode` **vẫn còn** (không xoá, tránh động vào
  DI/parity chỗ khác) nhưng nay luôn `null` — hệ quả: `eligibleOrderItems()` (`PromotionSDKConfig.kt`,
  cơ chế đổ `serviceCode` vào `orderInfo.items[].productId` cho `findEligible`) không còn nhánh nào
  kích hoạt được nữa, coi như no-op ở cả 2 bên. Không nhầm với `PromotionAvailableService.productId`
  (danh mục dịch vụ cho bottom sheet) hay `getVouchers/getVoucherDetail(serviceCode:)` (lọc theo dịch
  vụ) — hai khái niệm khác, **không** đổi.
- [x] **B9.** (2026-08-14) Điều kiện dựng `PromotionOrderItem` trong `updateOrderInfo` đổi từ khoá theo
  `skuId` sang khoá theo `productId` ở cả 2 nền tảng — có `productId` thì dựng 1 phần tử (`skuId` để
  trống → rơi về chuỗi rỗng `""`, không còn là điều kiện bật/tắt); không có `productId` → `orderItems`
  rỗng. Hợp lý sau B8: `productId` giờ là field host chắc chắn có sẵn (địa chỉ dịch vụ đang thanh
  toán, xem N-serviceCode ở B8) trong khi `skuId` có thể chưa biết ở một số luồng.
- [x] **B10.** (2026-08-17) `orderId`/`productId` trong `updateOrderInfo` đổi từ tuỳ chọn (`String?` /
  `String?`, default `nil`/`null`) sang **bắt buộc** (`String`, không default) ở cả 2 nền tảng — host
  phải luôn truyền cả hai. Hệ quả: nhánh `orderItems = emptyList()`/`[]` (thiếu `productId`) không còn
  đường vào nữa — mọi lời gọi `updateOrderInfo` giờ luôn dựng đúng 1 `PromotionOrderItem` (thay thế
  điều kiện của B9). Thứ tự tham số đổi theo: `productId` dời từ vị trí 5 lên vị trí 2 (ngay sau
  `orderId`) để hai tham số bắt buộc đứng đầu chữ ký ở cả 2 bên. `PromotionOrderItem.productId` (model)
  **không đổi** — vẫn `String?`, vì type này còn dùng ở luồng widget/`findEligible` nơi `productId` vẫn
  tuỳ chọn.
- [x] **B11.** (2026-08-19) `PromotionOrderItem.skuId` / tham số `skuId` của `updateOrderInfo` đổi tên
  thành `skuSourceId` ở cả 2 nền tảng — khớp tên field thật trên wire
  (`EligibleOrderItemDto.skuSourceId`), khỏi phải tự quy đổi `skuId` (host) ↔ `skuSourceId` (server).
  Kèm sửa lỗi: `EligibleOrderItemDto.skuSourceId` đổi `String` → `String?`; mapper
  (`EligibleCampaignsMapper.toDto()`) chỉ set khi giá trị khác rỗng — trước đây `skuId` để trống thì
  `updateOrderInfo` vẫn dựng item với `skuId = ""` (theo B9/B10), server nhận `"skuSourceId":""`; nay
  field bị bỏ hẳn khỏi JSON nhờ `explicitNulls = false` (`PromotionHttpClient`), các field khác của
  item (`productId`/`productName`/`quantity`/`unitPrice`) không đổi.

> ### Emission — đã cân cả 2 nền tảng ✅
> Contract callback đối xứng tuyệt đối **và** đủ 6 sự kiện đều được phát ở cả hai bên:
> - **iOS:** SDK callback là kênh duy nhất — phát đủ 6 (qua `PromotionSDKImpl` + facade).
>
> Điểm lệch emission còn lại (nhỏ): iOS phát `onVoucherApplied` sau khi validate xong ở luồng "Chọn
> ưu đãi"; Android phát khi widget vào trạng thái APPLIED hoặc UNAVAILABLE (`discountDetails` không
> rỗng, kể cả `valid = false`). Ngữ nghĩa tương đương, thời điểm hơi khác do kiến trúc widget khác
> nhau (N1).
```
