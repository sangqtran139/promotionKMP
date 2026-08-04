# Changelog

Toàn bộ thay đổi đáng chú ý của **TTCN Promotion SDK** ghi ở đây. Định dạng theo
[Keep a Changelog](https://keepachangelog.com/), version theo [SemVer](https://semver.org/).

Nguồn version tập trung: `gradle.properties` (`SDK_VERSION`) cho Android/KMP; `MARKETING_VERSION`
trong `PRM.xcodeproj` cho iOS — **giữ trùng số**.

## [Unreleased]

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
