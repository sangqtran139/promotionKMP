# CompatibilityMatrix — Ma trận tương thích

Số liệu lấy từ `gradle/libs.versions.toml`, `gradle.properties`, `AndroidPromotionSDK/build.gradle.kts`
và `iosPromotionSDK/PromotionKit.xcodeproj` tại thời điểm lập tài liệu (SDK `1.0.0`).

> Khi nâng bất kỳ dòng nào dưới đây, **cập nhật file này trong cùng thay đổi** và nêu trong
> [ReleaseNotes](./ReleaseNotes.md).

## Mục lục

<!-- toc -->
- [1. Yêu cầu tối thiểu của app host](#1-yêu-cầu-tối-thiểu-của-app-host)
- [2. Công cụ build (phía đội SDK)](#2-công-cụ-build-phía-đội-sdk)
- [3. Phụ thuộc của lõi `:promotionLogic`](#3-phụ-thuộc-của-lõi-promotionlogic)
- [4. Phụ thuộc của SDK Android `:AndroidPromotionSDK`](#4-phụ-thuộc-của-sdk-android-androidpromotionsdk)
- [5. Phụ thuộc của SDK iOS](#5-phụ-thuộc-của-sdk-ios)
- [6. Quan hệ giữa hai artifact Android](#6-quan-hệ-giữa-hai-artifact-android)
- [7. Ma trận version SDK ↔ backend](#7-ma-trận-version-sdk--backend)
<!-- /toc -->

---

## 1. Yêu cầu tối thiểu của app host

| Hạng mục | Android | iOS |
|---|---|---|
| Nền tảng tối thiểu | **minSdk 24** (Android 7.0) | **iOS 13.0** |
| compileSdk / targetSdk | 36 / 36 | — |
| Kiến trúc | AndroidX bắt buộc (`android.useAndroidX=true`) | UIKit |
| UI của SDK | XML View + View/DataBinding — **không** Compose | UIKit + XIB — **không** SwiftUI |
| Slice binary | AAR (mọi ABI, không có native code riêng) | `ios-arm64` (thiết bị) + `ios-arm64_x86_64-simulator` |
| Cách nhúng | `implementation("vn.viettelpay.library:promotion:<version>")` | SPM `binaryTarget` (`url:` + `checksum:`) → **Embed & Sign**; kéo tay xcframework là phương án dự phòng |
| Yêu cầu khác | Tài khoản đọc Artifactory nội bộ (repo Maven `gradle-viettelmoney`) | Tài khoản đọc Artifactory nội bộ (repo generic `vdo-ios-frameworks`) khai trong `~/.netrc` |

> **Embed & Sign là bắt buộc** bên iOS: `Promotion.xcframework` là **dynamic framework**, dylib phải
> được copy vào `.app/Frameworks`. Để "Do Not Embed" sẽ crash `dyld: Library not loaded` khi mở app.

---

## 2. Công cụ build (phía đội SDK)

| Công cụ | Version | Ghi chú |
|---|---|---|
| Kotlin | **2.2.0** | KMP |
| AGP (Android Gradle Plugin) | **8.13.2** | dùng plugin KMP kiểu mới `com.android.kotlin.multiplatform.library` |
| Gradle | theo `gradle/wrapper` trong repo | luôn dùng `./gradlew` |
| JDK | theo cấu hình toolchain của repo | — |
| Xcode | phiên bản hỗ trợ iOS 13 target + Swift hiện hành | cần macOS để build/test iOS |
| SKIE | **0.10.13** | cầu Kotlin → Swift |
| Kover | **0.9.9** | ⚠️ 0.9.1 **không chạy** với plugin `androidLibrary` kiểu mới của KMP |

---

## 3. Phụ thuộc của lõi `:promotionLogic`

| Thư viện | Version | Vai trò |
|---|---|---|
| Ktor Client | **3.3.0** | HTTP (`okhttp` engine ở Android, `darwin` ở iOS) |
| kotlinx.serialization | **1.8.1** | JSON |
| kotlinx.coroutines | **1.10.2** | Bất đồng bộ |
| multiplatform-settings | **1.3.0** | Kho key-value (SharedPreferences / NSUserDefaults) |

**Không dùng:** Room, SQLDelight, kapt, KSP, Hilt, Koin, Dagger, kotlinx-datetime.

---

## 4. Phụ thuộc của SDK Android `:AndroidPromotionSDK`

| Thư viện | Version |
|---|---|
| androidx.appcompat | 1.7.1 |
| androidx.fragment-ktx | 1.6.2 |
| androidx.lifecycle (viewmodel/runtime ktx) | 2.7.0 |
| androidx.recyclerview | 1.3.2 |
| androidx.constraintlayout | 2.2.0-alpha10 |
| androidx.swiperefreshlayout | 1.1.0-alpha02 |
| com.google.android.material | 1.13.0 |
| Glide | 4.16.0 |
| Facebook Shimmer | 0.5.0 |
| Intuit sdp-android | 1.0.6 |
| Timber | 4.7.1 |
| Gson | 2.11.0 (chỉ để parse theme JSON của host) |

> ⚠️ **Host Android thấy các thư viện này trên classpath.** SDK khai `implementation`, nhưng metadata
> Maven đưa chúng xuống runtime + compile transitively. Nếu app host dùng version khác, Gradle sẽ
> chọn version cao hơn theo luật thường; xung đột thì xử lý bằng `resolutionStrategy` phía host.
> iOS **không** có vấn đề này — mọi thứ link tĩnh và giấu bên trong framework.

---

## 5. Phụ thuộc của SDK iOS

Bốn package SPM **nội bộ**, không phải dependency ngoài: `PRMFoundation`, `PRMDesignKit`,
`PRMPromotionUI`, `PRMKotlinBridge` — tất cả `platforms: [.iOS(.v13)]`, không phụ thuộc thư viện bên
ngoài nào.

- **RxSwift đã được gỡ bỏ hoàn toàn.** View↔ViewModel ràng buộc bằng callback thuần.
  (Thư mục `iosPromotionSDK/.spm/checkouts/RxSwift` nếu còn chỉ là cache cũ — không nằm trong gói.)

---

## 6. Quan hệ giữa hai artifact Android

`promotion` (UI) và `promotionLogic` (lõi) **luôn dùng chung một số version** và **luôn publish cùng
lượt**. Lý do: `:AndroidPromotionSDK` khai `implementation(projects.promotionLogic)`, nên Gradle ghi
version lõi vào POM + `module.json` của `promotion` — đẩy lệch một bên là host resolve ra bản lõi
không tồn tại.

Host **không** khai `promotionLogic`; nó về theo metadata của `promotion`.

---

## 7. Ma trận version SDK ↔ backend

| SDK | `BASE_PATH` | Wire format cần lưu ý |
|---|---|---|
| 1.0.0 | `promotion/promotion-vtm-bff/api/v1/vtm` | `POST feature-flag/list` body có `userId`/`sessionId` |
| 2.0.0 (dự kiến) | như trên | `POST feature-flag/list` body **chỉ còn** `{"properties":{}}`; `findEligible` **không gửi** `skuSourceId` khi để trống |

Backend phải chấp nhận cả hai dạng body feature flag trong giai đoạn hai bản SDK cùng tồn tại.
