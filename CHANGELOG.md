# Changelog

Toàn bộ thay đổi đáng chú ý của **TTCN Promotion SDK** ghi ở đây. Định dạng theo
[Keep a Changelog](https://keepachangelog.com/), version theo [SemVer](https://semver.org/).

Nguồn version tập trung: `gradle.properties` (`SDK_VERSION`) cho Android/KMP; `MARKETING_VERSION`
trong `PRM.xcodeproj` cho iOS — **giữ trùng số**.

## [Unreleased]

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
