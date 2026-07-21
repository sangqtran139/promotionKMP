# Init Parity Spec — Android ↔ iOS

> **Nguồn sự thật duy nhất** cho tầng khởi tạo (init) của Promotion SDK trên cả hai nền tảng.
> Mọi thay đổi liên quan `PromotionSDK` / `PromotionSDKOptions` / `PromotionSessionConfig` /
> `PromotionSDKCallback` **bắt buộc** cập nhật file này trước khi/đi kèm khi sửa code.

## 0. Nguyên tắc

1. **Ép trùng chữ tuyệt đối** tên hàm, tên tham số, thứ tự tham số, comment và vị trí file — *nơi ngôn
   ngữ cho phép*.
2. Điểm **không thể** trùng do ràng buộc ngôn ngữ/nền tảng → liệt kê ở [§4 Ngoại lệ N1](#4-ngoại-lệ-n1--buộc-lệch)
   kèm lý do. Không được phát sinh ngoại lệ mới nếu không ghi vào đây.
3. Host **không** import SDK trực tiếp — chỉ đi qua wrapper `PromotionServing` ([§6](#6-wrapper-host--hợp-đồng-chung)).
4. Ký hiệu: ✅ đã khớp · 🔧 phải nắn · ⚠️ cần bạn xác nhận · N1 ngoại lệ nền tảng.

---

## 1. Entry `PromotionSDK` — bảng ánh xạ canonical

| Khái niệm | Canonical (đích) | Android hiện tại | iOS hiện tại | TT |
|---|---|---|---|---|
| Khởi tạo | **`initialize`** | `fun initialize(context, options)` | `initialize(options:)` | ✅ tên trùng; N1 nhỏ: Android cần `context` (iOS không) |
| Giải phóng | `release()` | ✅ | ✅ | ✅ |
| Trạng thái | `isInitialized()` | ✅ | ✅ | ✅ |
| Headless | `api` | ✅ `val api` | ✅ `var api` | ✅ |
| Session | `session` | ✅ | ✅ | ✅ |
| Order động | `currentOrderId` / `currentOrderValue` / `currentServiceCode` / `currentMetaData` | ✅ | ✅ | ✅ |
| Cập nhật context | `updateContext(orderId, orderValue, serviceCode, metaData)` | ✅ | ✅ | ✅ |
| Đặt theme | `configure(theme)` | ✅ | ✅ | ✅ |
| Đọc theme | **`currentTheme()`** (hàm, cả 2) | `fun currentTheme()` | `var currentTheme` 🔧 | 🔧 iOS đổi property → hàm |
| Đọc callback | **`getCallback()`** (cả 2) | `fun getCallback()` | *(thiếu)* 🔧 | 🔧 iOS bổ sung |
| Mở "Ưu đãi của tôi" | `openMyPromotion(host[, containerViewId])` | `openMyPromotion(activity, containerViewId?)` | `openMyPromotion(from:)` | N1 (Fragment/containerViewId Android-only) |
| Mở chi tiết | `openPromotionDetail(voucherId, host[, containerViewId])` | `openPromotionDetail(activity, voucherId, containerViewId?)` 🔧 | `openPromotionDetail(voucherId:, from:)` | 🔧 **Android đổi thứ tự → voucherId đứng trước** |
| Widget checkout | *(không nằm trên `PromotionSDK`)* | `PRMEndowView` (View) | `createEndowView(from:)` ×3 | N1 (xem [§5.3](#53-widget)) |

**Callback identity:** bỏ tham số `sdk` ở **mọi** method callback trên cả 2 nền tảng (SDK là singleton →
không cần truyền identity). iOS gỡ luôn hack `callbackToken`.

---

## 2. Config types — bảng ánh xạ

| Type | Field / thứ tự (canonical) | Android | iOS | TT |
|---|---|---|---|---|
| `PromotionSDKOptions` | `session, availableServices, theme, callback` | ✅ | ✅ | ✅ |
| `PromotionSessionConfig` | `customerId, accessToken, baseUrl, language = "vi-VN", environment` | ✅ (`baseUrl`) | `baseURL` 🔧 | 🔧 **iOS đổi `baseURL` → `baseUrl`** |
| `PromotionEnvironment` | `PROD, STAGING` ⚠️ hoặc `prod, staging` ⚠️ | `PROD, STAGING` | `prod, staging` | ⚠️ **cần chốt spelling** (xem ghi chú) |
| `PromotionAvailableService` | `serviceCode, serviceName, serviceType = "", iconUrl = ""` | ✅ | ✅ | ✅ |
| `PromotionMutableContext` (internal) | `session` + `orderId/orderValue/serviceCode/metaData` + 7 getter | ✅ | ✅ | ✅ nội bộ, vị trí xem [§5](#5-bố-cục-file-target-đối-xứng) |

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
| `onServiceSelected(selection)` | `PromotionServiceSelection` | Đã đổi tên type `PromotionSDKServiceSelection` → **`PromotionServiceSelection`** (trùng cả 2). |
| `onAvailabilityChanged(enabled)` | `Bool` | Từ iOS `didUpdateAvailability`. |
| `onClosed()` | — | Từ iOS `didClose` / Android `onSDKClosed`. |

**Đã loại:** Android `onError(errorCode)` — iOS không có, không nằm trong 6 sự kiện chuẩn. Đã bỏ khỏi
`PromotionSDKCallback` theo lựa chọn "hợp nhất theo iOS".

Tất cả method đều `default {}` (Kotlin default method / Swift protocol extension) → host chỉ implement cái cần.

`PromotionServiceSelection` nằm **cùng file** `PromotionSDKCallback` ở cả hai bên. Sự kiện chọn dịch vụ
được các màn nội bộ phát **trực tiếp** qua `PromotionSDK.getCallback()?.onServiceSelected(...)` (iOS:
`MyPromotion`/`Search`/`PromotionDetail` ViewController; Android: `MyPromotionFragment`/`PromotionDetailFragment`)
— không còn indirection qua holder runtime.

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

> Ánh xạ mapping options→core: Android có `PromotionSDKOptions.toCoreConfig(provider)` (extension trong
> `PromotionConfig.kt`). iOS đang map inline trong `PromotionSDKImpl.init` → 🔧 tách thành hàm
> `PromotionSDKOptions.toCoreConfig(context:)` trong `PromotionConfig.swift` cho khớp vị trí.

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

## 6. Wrapper host — hợp đồng chung (`PromotionServing`)

Mỗi nền tảng **một file duy nhất** chạm SDK. Chữ ký đối xứng 1-1 (điều chỉnh theo kiểu nền tảng ở host param).

```text
interface/protocol PromotionServing:
    // Vòng đời
    start(customerId, token, availableServices)     // → PromotionSDK.initialize(options)
    updateToken(token)                               // → initialize lại session mới
    updateContext(orderId, orderValue, serviceCode, metaData)
    stop()                                           // → PromotionSDK.release()

    // UI
    openMyPromotions(from host)
    openPromotionDetail(voucherId, from host)
    makeCheckoutWidget(from host, order) -> View     // che PRMEndowView / createEndowView

    // Headless (bọc đủ 5 hàm của PromotionSDKApi)
    fetchVouchers(...) ; findEligibleOffers(...) ; fetchVoucherDetail(...) ; validate(...) ; createRedemption(...)

    // Sự kiện (fan-out từ callback 1-1 của SDK → nhiều listener của app)
    onVoucherApplied ; onVoucherCleared ; onVoucherCountChanged
    onServiceSelected ; onAvailabilityChanged ; onClosed

class PromotionManager : PromotionServing   // singleton, CHỖ DUY NHẤT import SDK
    - map model APP ⇄ model SDK (anti-corruption)
    - nuốt các ràng buộc: token chụp lúc init, updateContext trước khi mở màn có voucher
```

- iOS: đã có mẫu `iosApp/.../PromotionManager.swift` → tinh chỉnh tên cho khớp bảng này.
- Android: **tạo mới** `androidApp/.../PromotionManager.kt` gương y hệt (hiện chưa có).

---

## 7. Việc thực hiện — checklist theo thứ tự

- [x] **B1.** Duyệt spec. Chốt 3 điểm ⚠️: (a) **enum case giữ convention** mỗi bên (`prod`↔`PROD`) — N1 duy nhất được miễn; (b) **bỏ `onError`**; (c) đổi `PromotionSDKServiceSelection → PromotionServiceSelection`.
- [x] **B2.** Nắn 🔧: `baseURL→baseUrl`, `currentTheme` property→hàm, thêm `getCallback()` (iOS); đổi thứ tự tham số `openPromotionDetail` + đổi `init→initialize` (Android); gộp file config iOS về `PromotionConfig.swift`; `availableServices` đi qua core config 2 bên (xoá `PromotionSessionRuntime`).
- [x] **B3.** Hợp nhất callback 2 bên theo [§3], bỏ tham số `sdk`, `PromotionSDKServiceSelection→PromotionServiceSelection`, bỏ `onError`. iOS emit đủ 6; Android nối `onAvailabilityChanged` tại 2 gate.
- [x] **B4.** Wrapper `PromotionManager` đối xứng 2 nền tảng theo [§6] (adapter callback), rewire điểm init chính của demo.
- [x] **B5.** Cập nhật `docs/PublicApi.md` + demo. Spec này **đã đồng bộ** với code (2026-07-16).

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
