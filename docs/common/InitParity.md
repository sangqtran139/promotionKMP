# Init Parity Spec — Android ↔ iOS

> **Nguồn sự thật duy nhất** cho tầng khởi tạo (init) của Promotion SDK trên cả hai nền tảng.
> Mọi thay đổi liên quan `PromotionSDK` / `PromotionSDKOptions` / `PromotionSessionConfig` /
> `PromotionSDKCallback` **bắt buộc** cập nhật file này trước khi/đi kèm khi sửa code.

## 0. Nguyên tắc

1. **Ép trùng chữ tuyệt đối** tên hàm, tên tham số, thứ tự tham số, comment và vị trí file — *nơi ngôn
   ngữ cho phép*.
2. Điểm **không thể** trùng do ràng buộc ngôn ngữ/nền tảng → liệt kê ở [§4 Ngoại lệ N1](#4-ngoại-lệ-n1--buộc-lệch)
   kèm lý do. Không được phát sinh ngoại lệ mới nếu không ghi vào đây.
3. Host gọi **thẳng** `PromotionSDK` — không cần wrapper (đã bỏ khuyến nghị `PromotionServing`, xem [§6](#6-tích-hợp-trực-tiếp--host-không-cần-wrapper)).
4. Ký hiệu: ✅ đã khớp · 🔧 phải nắn · ⚠️ cần bạn xác nhận · N1 ngoại lệ nền tảng.

---

## 1. Entry `PromotionSDK` — bảng ánh xạ canonical

| Khái niệm | Canonical (đích) | Android hiện tại | iOS hiện tại | TT |
|---|---|---|---|---|
| Khởi tạo (options) | **`initialize`** | `fun initialize(context, options)` | `initialize(options:)` | ✅ tên trùng; N1 nhỏ: Android cần `context` (iOS không) |
| Khởi tạo (phẳng) | **`initialize`** overload | `initialize(context, accessToken, baseUrl, environment=, language=, availableServices=, theme=, callback=)` | `initialize(accessToken:baseUrl:environment:language:availableServices:theme:callback:)` | ✅ đủ cho phần lớn host — chỉ 2 tham số bắt buộc; uỷ thẳng cho overload options |
| Login lại (session mới) | **`updateSession`** | `fun updateSession(accessToken, availableServices? = null, callback? = null)` | `updateSession(accessToken:availableServices:callback:)` | ✅ lối chính re-login: chỉ field động; **giữ** field cố định đã khoá. `availableServices` null = giữ danh mục hiện tại. Từ 2026-08-06 thêm `callback` (null = giữ callback hiện tại; gỡ callback vẫn chỉ có `release()`), gán **trước** khi dựng lại session để `onAvailabilityChanged` của phiên mới về đúng callback mới. Context động reset |
| Login lại (fallback) | **`initialize`** (idempotent) | gọi lại `initialize(context, options)` | gọi lại `initialize(options:)` | ✅ guard: SDK **khoá** field cố định (baseUrl/env/language/theme) ở lần init đầu; gọi lại chỉ áp field động, field cố định khác đi → cảnh báo log + bỏ qua |
| Refresh token (giữa phiên) | **`updateToken`** | `fun updateToken(accessToken)` | `updateToken(_:)` | ✅ tuỳ chọn — cùng customer + **giữ cả** context đơn hàng đang ghi (dùng khi token hết hạn giữa checkout) |
| Giải phóng | `release()` | ✅ | ✅ | ✅ |
| Trạng thái | `isInitialized()` | ✅ | ✅ | ✅ |
| Headless | `api` | ✅ `val api` | ✅ `var api` | ✅ |
| Session | `session` | ✅ | ✅ | ✅ |
| Order động | `currentOrderId` / `currentOrderValue` / `currentServiceCode` / `currentMetaData` | ✅ | ✅ | ✅ |
| Cập nhật context | `updateContext(orderId, orderValue, serviceCode, metaData, orderItems)` | ✅ | ✅ | ✅ |
| Đặt theme | `configure(theme)` | ✅ | ✅ | ✅ |
| Đọc theme | **`currentTheme()`** (hàm, cả 2) | `fun currentTheme()` | `var currentTheme` 🔧 | 🔧 iOS đổi property → hàm |
| Đọc callback | **`getCallback()`** (cả 2) | `fun getCallback()` | *(thiếu)* 🔧 | 🔧 iOS bổ sung |
| Cờ — chụp tất cả | `featureFlags()` | `fun featureFlags(): PromotionFeatureFlagsSnapshot` | `featureFlags() -> PromotionFeatureFlagsSnapshot` | ✅ cache đồng bộ, fail-open |
| Cờ — tra một | `isFeatureEnabled(feature)` | `fun isFeatureEnabled(feature: PromotionFeature)` | `isFeatureEnabled(_ feature: PromotionFeature)` | ✅ enum public mỗi bên; hằng chuỗi lõi không ra tới host |
| Cờ — công tắc tổng | `isSdkEnabled()` | `fun isSdkEnabled()` | `isSdkEnabled()` | ✅ uỷ cho `PromotionFeatureGate.isSdkEnabled()` |
| Cờ — nạp lại | `refreshFeatureFlags(…)` | `fun refreshFeatureFlags(onComplete? = null)` | `refreshFeatureFlags(completion:)` | ✅ callback về **main thread**; kèm bắn `onAvailabilityChanged` |
| Mở "Ưu đãi của tôi" | `openMyPromotion(host[, containerViewId])` | `openMyPromotion(activity, containerViewId?)` | `openMyPromotion(from:)` | N1 (Fragment/containerViewId Android-only) |
| *Gác chưa-init của 2 hàm mở màn* | log rồi bỏ qua, **không ném** | `requireInitialized(caller)` | `requireImpl(_:)` | ✅ Android trước đây `check(...)` ném `IllegalStateException` — lệch iOS, đã sửa 2026-08-06 |
| Mở chi tiết | `openPromotionDetail(voucherId, host[, containerViewId], returnVoucherOnApply, onVoucherApplied)` | `openPromotionDetail(voucherId, activity, containerViewId?, returnVoucherOnApply = true, onVoucherApplied: ((PromotionVoucherDetail) -> Unit)? = null)` | `openPromotionDetail(voucherId:from:returnVoucherOnApply:onVoucherApplied:)` | ✅ boolean + closure đối xứng, **không enum ở nền tảng nào** (`PromotionDetailEntry` / `PromotionDetailBuilder.Entry` đã xoá 2026-08-06); cờ xuống thẳng `arguments` (Android) / `DataModel` (iOS). Callback trả object `PromotionVoucherDetail`; UI nội bộ chuyền `VoucherDetail` domain, map sang DTO ở ranh giới public |
| Widget checkout | *(không nằm trên `PromotionSDK`)* | `PRMEndowView` (View) | `createEndowView(from:)` ×3 | N1 (xem [§5.3](#53-widget)) |

**Callback identity:** bỏ tham số `sdk` ở **mọi** method callback trên cả 2 nền tảng (SDK là singleton →
không cần truyền identity). iOS gỡ luôn hack `callbackToken`.

---

## 2. Config types — bảng ánh xạ

| Type | Field / thứ tự (canonical) | Android | iOS | TT |
|---|---|---|---|---|
| `PromotionSDKOptions` | `session, availableServices, theme, callback` | ✅ | ✅ | ✅ |
| `PromotionSessionConfig` | `accessToken, baseUrl, language = "vi-VN", environment` | ✅ (`baseUrl`) | `baseURL` 🔧 | 🔧 **iOS đổi `baseURL` → `baseUrl`** |
| `PromotionEnvironment` | `PROD, STAGING` ⚠️ hoặc `prod, staging` ⚠️ | `PROD, STAGING` | `prod, staging` | ⚠️ **cần chốt spelling** (xem ghi chú) |
| `PromotionAvailableService` | `serviceCode, serviceName, serviceType = "", iconUrl = ""` | ✅ | ✅ | ✅ |
| `PromotionMutableContext` (internal) | `session` + `orderId/orderValue/serviceCode/metaData/orderItems` + 7 getter | ✅ | ✅ | ✅ nội bộ, vị trí xem [§5](#5-bố-cục-file-target-đối-xứng) |
| `PromotionOrderItem` | `skuId, productId, productName, productCategory, quantity, unitPrice` | ✅ | ✅ | ✅ `getOrderItems()` map sang `EligibleOrderItem` của lõi ở **cả hai** bên |

> **Enum case (đã chốt):** giữ convention mỗi bên (`PROD`↔`prod`) — N1 *duy nhất được miễn* vì ánh
> xạ 1-1 hiển nhiên. Mọi tên hàm/tham số khác đã ép trùng chữ tuyệt đối.

---

## 3. `PromotionSDKCallback` — hợp nhất theo iOS (6 sự kiện), tên trùng cả 2 bên

Bỏ phong cách `vdsPromotion(_:didX:)` (ObjC-delegate) để tên **trùng chữ** được với Kotlin. Bỏ tham số `sdk`.

| Sự kiện (canonical) | Payload | Ghi chú |
|---|---|---|
| `onVoucherApplied(voucherId)` | `String` | Theo iOS (voucherId). Android đã **rút về voucherId** (bỏ `List<AppliedDiscount>` ở callback); `AppliedDiscount` vẫn dùng ở luồng widget, không ở callback. |
| `onVoucherCleared()` | — | |
| `onVoucherCountChanged(count)` | `Int` | |
| `onServiceSelected(selection)` | `PromotionServiceSelection` | Đã đổi tên type `PromotionSDKServiceSelection` → **`PromotionServiceSelection`** (trùng cả 2). Từ 2026-08-04: thêm field `serviceType` (lấy từ `PromotionAvailableService.serviceType` host cấu hình), map xuyên suốt `AvailableService`/`ServiceSelectorUiItem` (Android) và `AvailableService`/`ServiceSelectorItem` (iOS). |
| `onAvailabilityChanged(enabled)` | `Bool` | Từ iOS `didUpdateAvailability`. **Bắn cả `true` lẫn `false` ở cả 2 nền tảng** (trước 2026-08-04 Android chỉ bắn `false` lúc user bị chặn, nên host ẩn rồi không hiện lại được): nạp cờ xong sau `initialize`/login lại, mỗi lần `refreshFeatureFlags`, widget checkout đổi trạng thái, và khi user bấm mà bị chặn. Bảng đầy đủ: [features/FeatureFlag.md §3](../features/FeatureFlag.md). |
| `onClosed()` | — | Từ iOS `didClose` / Android `onSDKClosed`. |

**Đã loại:** Android `onError(errorCode)` — iOS không có, không nằm trong 6 sự kiện chuẩn. Đã bỏ khỏi
`PromotionSDKCallback` theo lựa chọn "hợp nhất theo iOS".

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
| Widget | `PRMEndowView` (View) | `createEndowView` (factory) | Idiom nền tảng (XML View vs factory UIView). Wrapper chuẩn hoá — [§5.3](#53-widget). |
| Enum case (nếu chọn giữ) | `PROD/STAGING` | `prod/staging` | Convention enum mỗi ngôn ngữ (đang chờ ⚠️ §2). |
| Ràng buộc View↔ViewModel | `StateFlow` + `collectFlow` | closure `onState`/`onEffect` | Không có `Flow` trong Swift. Hình dạng đã **ép trùng**: cùng `handleAction`, cùng `UiState`/`Effect`, `onState` replay state hiện tại khi gán (mô phỏng `StateFlow`). Từ 2026-07-23 iOS **không** còn Combine. |
| Cách hiện lỗi / thông báo | `Toast` | `PRMToast` | **Đã đồng nhất: toast ở cả 2 bên** (từ 2026-07-24; trước đây iOS dùng `PRMConfirmationDialog`). Áp dụng cho **mọi lỗi**, kể cả PRM_MOB_021 khi cờ tính năng TẮT. Nội dung chuỗi cũng trùng: `PRMBaseFragment.mapPromotionError` ↔ `PromotionUIStrings.errorMessage` (cùng bộ mã lỗi). **Gom sau một cờ bật/tắt, mặc định TẮT** — `PromotionToastGate.isEnabled` (Android) / `PromotionToast.isEnabled` (iOS); lỗi vẫn được bắt, chỉ không hiện. **Trừ PRM_MOB_021 (cờ tính năng TẮT): luôn hiện, bỏ qua cổng** — `PromotionToastGate.showFeatureDisabled()` / `PromotionToast.showAlways()`. Xem [ErrorHandling.md](./ErrorHandling.md). |

---

## 5. Bố cục file (target, đối xứng)

Mục tiêu: **cùng số file, cùng tên khái niệm, cùng thứ tự khai báo** trong mỗi file.

| Vai trò | Android (`.../ui/entry/`) | iOS (`.../PromotionSDKUI/Entry/`) |
|---|---|---|
| Entry object/facade | `PromotionSDK.kt` | `PromotionSDK.swift` |
| (impl box — N1) | — | `PromotionSDKImpl.swift` |
| Options | `PromotionSDKOptions.kt` | `PromotionSDKOptions.swift` |
| Config (session + env + availableService + map + mutableContext) | `PromotionConfig.kt` | `PromotionConfig.swift` |
| Callback (+ service selection type) | `PromotionSDKCallback.kt` | `PromotionSDKCallback.swift` |

`PromotionConfig` (cả 2 bên) gộp: `PromotionSessionConfig`, `PromotionEnvironment`, `PromotionAvailableService`,
hàm map options→core, và `PromotionMutableContext` (nội bộ). `availableServices` đi vào **core config**
(`toCoreConfig`) ở **cả hai** nền tảng → `ServiceSelectorBuilder`/`MyPromotionViewModel` đọc lại từ lõi
(`PromotionContainer.requireConfig().availableServices`). Không còn holder Swift riêng
(`PromotionSessionRuntime` đã xoá). File chỉ-iOS còn lại: `PromotionSDKImpl.swift` (box binary-interface, N1).

> Ánh xạ mapping options→core: Android `PromotionSDKOptions.toCoreConfig(contextProvider)`, iOS
> `PromotionSDKOptions.toCoreConfig(context:isDebug:)` — cùng là extension trong `PromotionConfig.{kt,swift}`.
> Lệch duy nhất là `isDebug` (N1: iOS không có `ApplicationInfo.FLAG_DEBUGGABLE` nên host/impl truyền vào).
> `baseUrl` đi thẳng từ session ở **cả hai** bên — không bên nào có giá trị fallback.

### 5.3 Widget

Giữ lệch có chủ đích: Android `PRMEndowView` (View đặt trong layout), iOS `createEndowView(from:)`.
Wrapper phơi **một** API chung `makeCheckoutWidget(...)` để host không thấy khác biệt.

**Chi tiết giảm giá (`AppliedDiscount`) — N1, đã duyệt:** chỉ **Android** phơi `AppliedDiscount` +
`PRMEndowView.setDiscountDetails(...)` để host đọc breakdown giảm giá **trực tiếp** từ widget. **iOS
cố tình KHÔNG phơi** type này: `createEndowView(from:)` trả `UIView` đục (che type nội bộ theo box
binary-interface — [§4](#4-ngoại-lệ-n1--buộc-lệch)), nên host iOS chỉ nhận **id** voucher đã áp qua
`onVoucherApplied(voucherId)`; muốn biết số tiền giảm thì gọi headless `PromotionSDK.api.validateDiscounts(...)`
với `voucherId` đó (trả `PromotionValidationResult` — cùng dữ liệu, đi qua ranh giới DTO hợp lệ). Đây
**không** phải thiếu sót cần "sửa": phơi `AppliedDiscount` bên iOS sẽ kéo type lõi vào `.swiftinterface`
(xem [PublicApi.md §3](./PublicApi.md)).

---

## 6. Tích hợp trực tiếp — host **không** cần wrapper

**Bỏ khuyến nghị `PromotionManager`/`PromotionServing` (2026-07-24).** Trước đây spec bắt mỗi host tự
viết một wrapper anti-corruption (~340 dòng Android / ~420 dòng iOS) để nuốt các ràng buộc của SDK.
Nay các ràng buộc đó đã đưa **vào chính SDK**, nên host gọi thẳng `PromotionSDK` — tích hợp tối thiểu:

```text
// Sau khi login:
PromotionSDK.initialize(context, accessToken, baseUrl)   // overload phẳng, 3 tham số

// Refresh token — KHÔNG cần tự init lại, SDK giữ nguyên context/services/theme/callback:
PromotionSDK.updateToken(newToken)

// Vào màn có voucher:
PromotionSDK.updateContext(orderId, orderValue)

// Mở UI có sẵn:
PromotionSDK.openMyPromotion(activity[, containerViewId])   // Android
PromotionSDK.openMyPromotion(from: viewController)           // iOS

// Logout:
PromotionSDK.release()
```

Vì sao không còn cần wrapper:
- **`initialize` phẳng** — 3 tham số bắt buộc, không phải lồng 3 constructor.
- **`updateToken`** — SDK tự dựng lại session mới, host không phải biết "refresh = init lại".
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
- [x] **B3.** Hợp nhất callback 2 bên theo [§3], bỏ tham số `sdk`, `PromotionSDKServiceSelection→PromotionServiceSelection`, bỏ `onError`. iOS emit đủ 6; Android nối `onAvailabilityChanged` tại 2 gate.
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

> ### Emission — đã cân cả 2 nền tảng ✅
> Contract callback đối xứng tuyệt đối **và** đủ 6 sự kiện đều được phát ở cả hai bên:
> - **iOS:** SDK callback là kênh duy nhất — phát đủ 6 (qua `PromotionSDKImpl` + facade).
> - **Android:** `onAvailabilityChanged` tại 2 gate (`openMyPromotion` / `openPromotionDetail`);
>   `onVoucherApplied` / `onVoucherCleared` / `onVoucherCountChanged` từ widget `PRMEndowView`
>   (transition state → `PromotionSDK.getCallback()`); `onServiceSelected` từ bottom sheet ở
>   `MyPromotionFragment` + `PromotionDetailFragment`; `onClosed` từ `MyPromotionFragment.onDestroyView`
>   khi `isRemoving`. `getCallback()` null (host chưa set) → no-op an toàn.
>
> Điểm lệch emission còn lại (nhỏ): iOS phát `onVoucherApplied` sau khi validate xong ở luồng "Chọn
> ưu đãi"; Android phát khi widget vào trạng thái APPLIED (`discountDetails` không rỗng). Ngữ nghĩa
> tương đương, thời điểm hơi khác do kiến trúc widget khác nhau (N1).
```
