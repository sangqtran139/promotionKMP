# Changelog

Toàn bộ thay đổi đáng chú ý của **TTCN Promotion SDK** ghi ở đây. Định dạng theo
[Keep a Changelog](https://keepachangelog.com/), version theo [SemVer](https://semver.org/).

Nguồn version tập trung: `gradle.properties` (`SDK_VERSION`) cho Android/KMP; `MARKETING_VERSION`
trong `PRM.xcodeproj` cho iOS — **giữ trùng số**.

## [Unreleased]

### Changed — **BREAKING**: bỏ `PromotionIntegrateManager`, luồng thanh toán về lõi dùng chung

Luồng `createRedemption → gặp INSUFFICIENT_BUDGET → validate lại → cập nhật widget` là **nghiệp vụ
đụng tiền**, nhưng nó nằm trong một class public **chỉ có ở Android**. iOS không hề có
`confirmRedemption` nào — host iOS phải tự gọi `PromotionSDKApi.createRedemption`. Kèm theo là hai
mapper thân giống hệt nhau (`appliedDiscountFor` bên Android ↔ `toEndowAppliedDiscount` ở lõi), một
CoroutineScope thứ ba cho một widget, và nghĩa vụ `clear()` mà host quên là rò.

Nay luồng này ở `EndowStore.confirmRedemption()` (`promotionLogic`), hai nền tảng chạy một đường và
có **6 test** ở `EndowStoreTest` (body-error, HTTP 422, lỗi khác, revalidate rỗng, đơn không voucher,
request mang đúng `expectedDiscount`).

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `PromotionIntegrateManager.create(endowView)` + `confirmRedemption(...)` + `clear()` | `endowView.confirmRedemption(onSuccess, onError)` — không còn class, không còn `clear()` |
| (iOS: không có) | `PromotionSDK.confirmRedemption(onSuccess:onError:)` |
| `com.ttcn.prm.ui.feature.endowview.EndowViewState` | `com.ttcn.promotionsdk.presentation.endow.EndowWidgetState` |
| `PRMEndowView.onVoucherItemClick` | Bỏ — nó khai báo rồi nhưng **SDK chưa từng gọi** (đã ghi "Đang hỏng" trong docs) |

`PRMEndowView.getCurrentState()` giữ nguyên, chỉ đổi kiểu trả về sang `EndowWidgetState`.


### Fixed — màn "Ưu đãi của tôi": tab nhảy ngược & danh sách bị xoá trắng (cả 2 nền tảng)

Hai lỗi trong `MyPromotionStore` (`promotionLogic`, dùng chung Android + iOS), cùng đến từ commit
thêm cache tab. Cả hai đều có test bắt đúng từ đầu nhưng bị commit trong tình trạng đỏ.

- **Tab sáng nhảy ngược dưới ngón tay user.** Store truyền `requestTabCode` vào
  `resolveActiveTab()`, mà rule đó xếp `selectedTab` của response **trên** tab client yêu cầu. Server
  echo lệch — nhận `tab=used` nhưng trả `selectedTab=all` — là tab sáng nhảy về "Tất cả" trong khi
  danh sách hiện ra lại là của "Đã dùng". Nay tab user vừa bấm thắng thẳng; `resolveActiveTab()` chỉ
  còn được hỏi ở lần load đầu, đúng vai trò "đáp xuống tab nào". Rule domain giữ nguyên, không đổi.
- **Kéo làm mới rớt mạng là mất sạch danh sách.** Nhánh `onFailure` dùng "tab này có cache không" để
  quyết định giữ hay xoá list. Nhưng cache chỉ được ghi khi có tab code, nên với host mà server
  **không trả `tabs[]`**, `tabCaches` rỗng suốt vòng đời màn → mọi lần load lại thất bại đều rơi vào
  nhánh xoá. Nay điều kiện là `keepCurrentListWhileLoading` — chính cái cờ gây ra chuyện "list đang
  hiện thuộc về tab khác", tức đúng câu hỏi cần trả lời.

Hành vi cố ý vẫn giữ: đổi sang tab **chưa có cache** mà API hỏng thì vẫn xoá list, để user không thấy
danh sách tab bên cạnh nằm dưới tab vừa bấm.

### Changed — **BREAKING**: chỉ `Entry` mới public, mọi thứ khác `internal` (cả 2 nền tảng)

Trước đây bề mặt public rò ra ngoài `entry` lúc nào không hay: 93 khai báo top-level của
`AndroidPromotionSDK` nằm ngoài package `entry` vẫn `public` — toàn bộ `ui/widget/**`
(`PRMButton`, `PRMSearchField`, `PRMTextView`…), `ui/base/**` (`PRMBaseFragment`, `PRMBaseActivity`,
`PRMBaseViewModel`), ~40 extension trong `ui/utils/**`, cả năm Fragment nghiệp vụ, và nhóm theme.
Host thấy hết trong autocomplete, dùng nhầm rồi vỡ ở bản sau. Bên iOS là 10 file `PromotionSDKUI/Theme/`.

Từ nay **`com.ttcn.prm.entry.**` (Android) / `iosPromotionSDK/Entry/**` (iOS) là bề mặt public duy
nhất**; mọi khai báo khác là `internal` / không `public`. Xem [PublicApi.md](./docs/common/PublicApi.md)
(có sẵn lệnh `grep` để CI hoặc người review kiểm lại).

**Host phải sửa:**

| Cũ | Mới |
|---|---|
| `com.ttcn.prm.ui.feature.endowview.PRMEndowView` (kể cả tag trong layout XML) | `com.ttcn.prm.ui.feature.endowview.PRMEndowView` |
| `com.ttcn.prm.ui.feature.PromotionIntegrateManager` | **Đã bỏ hẳn** — xem mục BREAKING ở trên (`endowView.confirmRedemption`) |
| `ChoosePromotionFragment.forEndowView(endowView)` | `PromotionSDK.createChoosePromotionFragment(endowView)` — trả `Fragment` trần |
| `com.ttcn.prm.ui.theme.PromotionSDKTheme` / `PromotionThemeJson` / `PromotionThemeDisplay` | `com.ttcn.prm.ui.theme.*` |
| `com.ttcn.prm.ui.theme.token.*` (6 token) | `com.ttcn.prm.ui.theme.token.*` |
| `com.ttcn.prm.entry.AppliedDiscount` | `com.ttcn.prm.ui.feature.endowview.AppliedDiscount` (về ở cạnh widget dùng nó) |
| Kế thừa `PRMBaseFragment` / `PRMBaseActivity` | Không còn — host tự viết base của mình (xem `androidApp/.../base/`) |
| `PromotionThemeDefaults` (iOS) | Không còn public — dùng `PromotionThemeDisplay.load()` |

Widget/extension/base/applier nội bộ **không có đường thay thế** và đó là chủ đích: chúng chưa bao
giờ là hợp đồng, chỉ tình cờ với tới được.

Kèm theo:
- `consumer-rules.pro` giữ `com.ttcn.prm.entry.**` — trước đó nó vẫn trỏ `com.ttcn.promotionsdk.ui.entry.**`,
  một package **không còn tồn tại**, nên bề mặt public thật ra đang **không** được R8 giữ.
- Layout demo `prm_fragment_payment_demo.xml` bị **gỡ khỏi SDK** (nó chỉ phục vụ app demo) và chuyển
  sang `androidApp` thành `fragment_payment_demo.xml`.
- App demo `androidApp` được sửa để chỉ dùng bề mặt host: có base class riêng
  (`app/base/AppBaseFragment` + `AppBaseActivity`), codec hex riêng (`DemoHex`), và màn "Theme
  preview" bỏ phần preview widget nội bộ (chỉ còn `PRMEndowView`).
- Hai chỗ **không khoá được** trên Android: `com.ttcn.prm.R` và `com.ttcn.prm.databinding.*` là class
  Java do AGP sinh nên luôn public. Chúng không nằm trong hợp đồng.

### Added — feature flag phơi ra host (cả 2 nền tảng)
- `PromotionSDK` có thêm 4 hàm để host **hỏi trước** trạng thái cờ, thay vì để user bấm rồi ăn toast
  PRM_MOB_021: `featureFlags()`, `isFeatureEnabled(feature)`, `isSdkEnabled()`,
  `refreshFeatureFlags(onComplete/completion:)`. Cùng tên và **cùng thứ tự** ở Android ↔ iOS.
  - Ba hàm đầu đọc **cache đồng bộ** (không gọi mạng); `refreshFeatureFlags` gọi server rồi trả
    snapshot mới trên **main thread**.
  - **Fail-open, không ném lỗi**: chưa `initialize()` hoặc chưa có cache → trả bật hết.
  - DTO public mới: `PromotionFeature` (enum) + `PromotionFeatureFlagsSnapshot` —
    `entry/api/PromotionFeatureModels.kt` ↔ `Entry/API/PromotionFeatureModels.swift`. Hằng chuỗi
    `PromotionFeatureFlag` và data class `PromotionFeatureFlags` của lõi **không** ra tới host
    (cùng lý do đã có `PromotionApiModels`).
  - **Tầng gác của SDK không đổi.** Đây là tầng tuỳ chọn; host bỏ qua thì kill-switch vẫn chạy đủ.
  - Xem [features/FeatureFlag.md §3](./docs/features/FeatureFlag.md) và
    [common/PublicApi.md §2](./docs/common/PublicApi.md).

### Changed — `onAvailabilityChanged` nay bắn cả `true`
- Trước đây callback này **chỉ** bắn `false`, và chỉ khi user đã bấm vào một điểm vào bị chặn — host
  ẩn entry point rồi thì không có đường hiện lại. Nay nó bắn ở 4 thời điểm với cả hai giá trị: nạp cờ
  xong sau `initialize`/login lại, mỗi lần `refreshFeatureFlags`, widget checkout đổi trạng thái, và
  khi user bấm mà bị chặn.
- Android còn thiếu so với iOS ở luồng widget — `PRMEndowView.applyFeatureFlag()` nay cũng báo host
  mỗi lần đổi trạng thái, đối xứng `PromotionSDKImpl.applyFlag`.
- **Tương thích:** thuần bổ sung, không đổi chữ ký. Host nào đang coi mọi lần gọi là "tắt SDK" thì
  phải đọc tham số `enabled` — hành vi cũ tương đương `if (!enabled)`.

### Changed — phát hành (Android)
- Thêm đích publish **JFrog Artifactory** bên cạnh `~/.m2`: repo khai một lần ở `build.gradle.kts`
  gốc cho cả `:promotionLogic` và `:AndroidPromotionSDK`, tự chọn repo release/snapshot theo hậu tố
  `SDK_VERSION`. Chạy `./scripts/build-android.sh --remote`. Xem
  [docs/android/Distribution.md](./docs/android/Distribution.md) §3.4.
  - Cấu hình đặt ở `~/.gradle/gradle.properties` (`artifactoryUrl` / `artifactoryUser` /
    `artifactoryPassword`) hoặc env `ARTIFACTORY_*` — thiếu thì repo không đăng ký, build vẫn chạy.
  - `groupId` tách thành property tập trung `SDK_GROUP` trong `gradle.properties` (giá trị **không
    đổi**: `com.ttcn.promotion`). Đổi giá trị này là breaking với host.
  - Host giờ phải khai repo Artifactory + credentials — [AndroidIntegrationGuide](./docs/AndroidIntegrationGuide.md) §3.1.

### Removed — ⚠️ BREAKING (bề mặt public)
- Bỏ hoàn toàn `customerId` khỏi SDK. Định danh khách BFF đã lấy từ JWT `sub`, không API nào
  còn gửi tham số này; consumer cuối cùng là feature flag (`userId` → Unleash) cũng đã gỡ.
  - `PromotionSessionConfig(customerId, …)` → `PromotionSessionConfig(accessToken, baseUrl, …)`
  - `initialize(context, customerId, accessToken, baseUrl, …)` → `initialize(context, accessToken, baseUrl, …)`
  - `initialize(customerId:accessToken:baseUrl:…)` → `initialize(accessToken:baseUrl:…)` (iOS)
  - `updateSession(customerId, accessToken, …)` → `updateSession(accessToken, …)` (cả 2 nền tảng)
  - `PromotionRequestContextProvider.getCustomerId()` bị xoá (còn 7 getter).
  - `FeatureFlagRequest` bỏ cả `userId` lẫn `sessionId` (hai trường luôn được gửi rỗng);
    `FeatureFlagRemoteDataSource.getFeatureFlags()` không còn tham số nào.
    ⚠️ **Đổi wire format** — body `POST feature-flag/list` giờ chỉ còn `{"properties":{}}`.
- **Hệ quả:** request feature flag không mang định danh khách → Unleash không rollout theo % user
  được, chỉ bật/tắt toàn bộ. Cần nối lại nguồn định danh khi vertical FeatureFlag triển khai.

## [1.0.0] — 2026-07-20

Bản phát hành ổn định đầu tiên. Từ đây bề mặt public tuân theo SemVer (thay đổi breaking → tăng major).

### Kiến trúc
- Kotlin Multiplatform: lõi Data + Domain dùng chung (`:promotionLogic`); UI native mỗi nền tảng
  (Android XML/MVI, iOS UIKit/MVVM).
- Bề mặt SDK đối xứng 1:1 Android ↔ iOS — xem [docs/InitParity.md](./docs/InitParity.md).

### Bề mặt công khai
- Entry `PromotionSDK`: `initialize` / `release` / `isInitialized` / `updateContext` /
  `configure(theme)` / `currentTheme` / `getCallback`.
- Màn hình: `openMyPromotion`, `openPromotionDetail`, widget checkout (`PRMEndowView` /
  `createEndowView`).
- Headless `PromotionSDKApi` (5 hàm): `getVouchers`, `findEligible`, `getVoucherDetail`,
  `validateDiscounts`, `createRedemption` — trả DTO + `PromotionApiResult`.
- Callback thống nhất 6 sự kiện (`PromotionSDKCallback`); theming qua `PromotionSDKTheme`.
- Feature flag **không** phơi ra host — mọi điểm vào tự gác qua `PromotionFeatureGate`.

### Phân phối
- Android: Maven (`com.ttcn.promotion:promotionSDK:1.0.0`) — xem [docs/Distribution.md](./docs/Distribution.md).
- iOS: `PRM.xcframework`.
