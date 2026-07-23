# Changelog

Toàn bộ thay đổi đáng chú ý của **TTCN Promotion SDK** ghi ở đây. Định dạng theo
[Keep a Changelog](https://keepachangelog.com/), version theo [SemVer](https://semver.org/).

Nguồn version tập trung: `gradle.properties` (`SDK_VERSION`) cho Android/KMP; `MARKETING_VERSION`
trong `PRMSDK.xcodeproj` cho iOS — **giữ trùng số**.

## [1.0.0] — 2026-07-20

Bản phát hành ổn định đầu tiên. Từ đây bề mặt public tuân theo SemVer (thay đổi breaking → tăng major).

### Kiến trúc
- Kotlin Multiplatform: lõi Data + Domain dùng chung (`:promotionLogic`); UI native mỗi nền tảng
  (Android XML/MVI, iOS UIKit/MVVM).
- Bề mặt SDK đối xứng 1:1 Android ↔ iOS — xem [docs/InitParity.md](./docs/InitParity.md).

### Bề mặt công khai
- Entry `PRMSDK`: `initialize` / `release` / `isInitialized` / `updateContext` /
  `configure(theme)` / `currentTheme` / `getCallback`.
- Màn hình: `openMyPromotion`, `openPromotionDetail`, widget checkout (`PRMEndowView` /
  `createEndowView`).
- Headless `PRMSDKApi` (5 hàm): `getVouchers`, `findEligible`, `getVoucherDetail`,
  `validateDiscounts`, `createRedemption` — trả DTO + `PRMApiResult`.
- Callback thống nhất 6 sự kiện (`PRMSDKCallback`); theming qua `PRMSDKTheme`.
- Feature flag **không** phơi ra host — mọi điểm vào tự gác qua `PromotionFeatureGate`.

### Phân phối
- Android: Maven (`com.ttcn.promotion:promotionSDK:1.0.0`) — xem [docs/Distribution.md](./docs/Distribution.md).
- iOS: `PRMSDK.xcframework`.
