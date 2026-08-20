# TTCN Promotion SDK

SDK ưu đãi/voucher cho **Android và iOS**, dựng trên một lõi **Kotlin Multiplatform** dùng chung.

Hướng **headless**: toàn bộ nghiệp vụ + logic hiển thị nằm ở lõi KMP, còn giao diện do mỗi nền tảng
tự dựng bằng công nghệ native của mình (Android XML View, iOS UIKit). Kết quả là hai SDK hoàn chỉnh —
**AAR** và **XCFramework** — đứng trên cùng một lõi.

## Cấu trúc

| Thư mục | Vai trò |
|---|---|
| [`promotionLogic/`](./promotionLogic/src) | 📦 Lõi KMP — data / domain / usecase / store dùng chung. Namespace `com.ttcn.promotionsdk`. |
| [`AndroidPromotionSDK/`](./AndroidPromotionSDK/src) | 📦 SDK Android (AAR) — Fragment, XML, adapter, theme. Namespace `com.ttcn.prm`. |
| [`iosPromotionSDK/`](./iosPromotionSDK) | 📦 SDK iOS (XCFramework) — UIKit, MVVM + Builder/Router. |
| [`androidApp/`](./androidApp/src) | App demo/host Android. |
| [`iosApp/`](./iosApp) | App demo/host iOS (mở bằng Xcode). |

`iosPromotionSDK` là project Xcode, **không** phải module Gradle — nó tiêu thụ lõi qua XCFramework do
`:promotionLogic` sinh ra. Vì vậy `settings.gradle.kts` chỉ include 3 module.

## Tài liệu

**Bắt đầu ở [`docs/README.md`](./docs/README.md).** Vài điểm vào hay dùng:

| Cần gì | Đọc |
|---|---|
| Tích hợp vào app | [AndroidIntegrationGuide](./docs/AndroidIntegrationGuide.md) · [IosIntegrationGuide](./docs/IosIntegrationGuide.md) |
| Kiến trúc | [Architecture](./docs/common/Architecture.md) |
| File nằm ở đâu / đặt file mới vào đâu | [ProjectStructure](./docs/common/ProjectStructure.md) |
| Public API (đổi = breaking host) | [PublicApi](./docs/common/PublicApi.md) |
| Phát hành | [android/Distribution](./docs/android/Distribution.md) · [ios/Distribution](./docs/ios/Distribution.md) |
| Từng màn hình | [`docs/features/`](./docs/features/README.md) |

Lịch sử thay đổi: [`CHANGELOG.md`](./CHANGELOG.md). Quy tắc cho AI agent:
[`docs/AI_AGENT_RULES.md`](./docs/AI_AGENT_RULES.md).

## Chạy app demo

```bash
./gradlew :androidApp:assembleDebug     # Android
```

iOS: mở [`iosApp/`](./iosApp) bằng Xcode rồi run.

## Test

Toàn bộ unit test nằm ở lõi (`promotionLogic/src/commonTest`), chạy trên cả hai target:

```bash
./gradlew :promotionLogic:testAndroidHostTest      # JVM
./gradlew :promotionLogic:iosSimulatorArm64Test    # iOS (cần macOS)
./gradlew :AndroidPromotionSDK:testDebugUnitTest   # test riêng tầng UI Android

./scripts/test-report.sh                           # cả 2 target + coverage Kover
```

## Build bản phát hành

Dùng script, đừng gọi tay từng Gradle task — version và toạ độ Maven được lấy từ
`gradle.properties` (`SDK_VERSION`, `SDK_GROUP`):

```bash
./scripts/build-android.sh      # AAR + publish Maven
./scripts/build-ios.sh          # XCFramework
```

Chi tiết ở [android/Distribution.md](./docs/android/Distribution.md) và
[ios/Distribution.md](./docs/ios/Distribution.md).
