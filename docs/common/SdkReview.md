# SdkReview — Báo cáo rà soát & hoàn thiện SDK (5 hạng mục)

> Tài liệu **đánh giá — mô tả — so sánh** chi tiết cách 5 hạng mục rà soát đã được thực hiện trong
> project, kèm ví dụ cụ thể (file / đoạn code thật). Trạng thái phản ánh nhánh `developKMP`.

Năm hạng mục:

1. [Rà soát UI public trên iOS (đầy đủ + tái sử dụng)](#1-rà-soát-ui-public-trên-ios)
2. [Rà soát & hoàn thiện Use Case Wrapper](#2-rà-soát--hoàn-thiện-use-case-wrapper)
3. [Review & thống nhất terminology](#3-review--thống-nhất-terminology)
4. [Cập nhật biểu đồ kiến trúc & làm rõ class chủ chốt](#4-cập-nhật-biểu-đồ-kiến-trúc)
5. [Đánh version SDK](#5-đánh-version-sdk)

Kết quả tổng quát:

| # | Hạng mục | Kết quả | Đụng code? |
|---|---|---|---|
| 1 | UI public iOS | Đã đầy đủ & đối xứng 1:1 Android; ghi rõ 1 điểm N1 | Chỉ tài liệu |
| 2 | Use Case Wrapper | Bọc **đủ 5** hàm headless + `updateContext`, đối xứng 2 nền tảng | Có (demo) |
| 3 | Terminology | Thống nhất `PromotionSDK*` (bề mặt) + `PRM*` (nội bộ) | Có (docs + rename) |
| 4 | Biểu đồ kiến trúc | Bổ sung §1.1 luồng entry/facade/DI/wrapper + class chủ chốt | Chỉ tài liệu |
| 5 | Version | Nguồn version tập trung **1.0.0** + `CHANGELOG.md` | Có (config) |

---

## 1. Rà soát UI public trên iOS

### Mục tiêu
Đảm bảo bề mặt UI **public** của `PromotionSDKUI` (iOS) **đầy đủ** (host làm được mọi việc như Android)
và **tái sử dụng** được (không lộ type nội bộ, không buộc host viết lại logic).

### Đánh giá — đã đầy đủ & đối xứng
Bề mặt public iOS gồm đúng các nhóm như Android, **cùng tên hàm**:

```swift
// iosPromotionUI/PromotionSDKUI/Entry/PromotionSDK.swift
public static func initialize(options: PromotionSDKOptions)
public static func release()
public static func isInitialized() -> Bool
public static func getCallback() -> PromotionSDKCallback?
public static var api: PromotionSDKApi                 // headless
public static var session: PromotionSessionConfig?
public static var currentOrderId / currentOrderValue / currentServiceCode / currentMetaData
public static func updateContext(orderId:orderValue:serviceCode:metaData:)
public static func configure(theme:) / currentTheme()
public static func openMyPromotion(from:) / openPromotionDetail(voucherId:from:)
public static func createEndowView(from:...)           // widget checkout
```

Bằng chứng **tái sử dụng an toàn**: chữ ký chỉ dùng type Foundation/UIKit; type nội bộ giấu sau
`_impl: NSObject` + `@_implementationOnly import` → host **không** phải nạp `PRMKotlinBridge`/RxSwift.
Xem [PublicApi.md](./PublicApi.md).

### So sánh Android ↔ iOS (đối xứng 1:1)
| Khái niệm | Android | iOS |
|---|---|---|
| Entry | `object PromotionSDK` | `final class PromotionSDK` (+ `_impl` box) |
| Headless | `PromotionSDK.api: PromotionSDKApi` | `PromotionSDK.api: PromotionSDKApi` |
| Callback | `PromotionSDKCallback` (6 sự kiện) | `PromotionSDKCallback` (6 sự kiện) |
| Widget | `PRMEndowView` (View) | `createEndowView(from:)` (factory) — N1 |

### Điểm phát hiện & cách xử lý — `AppliedDiscount` (N1)
Rà soát phát hiện **một** bất đối xứng: chỉ **Android** phơi `AppliedDiscount` +
`PRMEndowView.setDiscountDetails(...)` để host đọc breakdown giảm giá từ widget; iOS `createEndowView`
trả `UIView` đục nên host chỉ nhận `onVoucherApplied(voucherId)`.

Kết luận: **không** phải thiếu sót — phơi `AppliedDiscount` bên iOS sẽ kéo type lõi vào
`.swiftinterface` (vỡ đóng gói). Đây là **N1 có chủ đích**. Đã ghi rõ (không đổi code) tại
[InitParity.md §5.3](./InitParity.md#53-widget) và [PublicApi.md §4](./PublicApi.md):

> *"Chi tiết giảm giá (`AppliedDiscount`) — N1, đã duyệt: chỉ Android phơi… iOS muốn breakdown thì gọi
> headless `api.validateDiscounts(...)`."*

→ **Kết quả hạng mục 1: bề mặt iOS đã đầy đủ, chỉ cần tài liệu hoá 1 điểm N1. Không phát sinh code.**

---

## 2. Rà soát & hoàn thiện Use Case Wrapper

### Mục tiêu
`PromotionManager` (lớp Anti-Corruption bọc SDK cho app host) phải **bọc đủ** bề mặt headless và
**đối xứng** giữa 2 nền tảng (xem [InitParity.md §6](./InitParity.md#6-wrapper-host--hợp-đồng-chung)).

### Phát hiện
`PromotionSDKApi` có **5** hàm headless, nhưng wrapper chỉ bọc **3**
(`getVouchers`/`validateDiscounts`/`createRedemption`) — **thiếu** `findEligible` + `getVoucherDetail`.
Ngoài ra iOS `PromotionServing` **thiếu** `updateContext` (Android có) → bất đối xứng.

### Hoàn thiện — bọc đủ 5 + `updateContext`
Hợp đồng `PromotionServing` sau khi hoàn thiện (Android):

```kotlin
// androidApp/.../app/PromotionManager.kt
fun updateContext(orderId, orderValue, serviceCode, metaData)          // ← thêm cho iOS parity
suspend fun fetchVouchers(keyword, serviceCode, tab, page): Result<VoucherPage>
suspend fun findEligibleOffers(order, tab, myPage, otherPage): Result<EligibleOffers>   // ← THÊM
suspend fun fetchVoucherDetail(voucherId, serviceCode): Result<VoucherDetail>           // ← THÊM
suspend fun validate(order, voucherIds): Result<ValidationSummary>
suspend fun createRedemption(order, voucherId): Result<String>
```

Kèm app-model mới (anti-corruption, **không** dùng type SDK ở tầng app): `EligibleOffer`,
`EligibleOffers`, `VoucherDetail` — có ở **cả hai** `PromotionManager.kt` và `PromotionManager.swift`.

### So sánh trước / sau + điểm lệch N1
| | Trước | Sau |
|---|---|---|
| Headless bọc | 3/5 | **5/5** |
| iOS `updateContext` | thiếu | có (đối xứng Android) |
| Kiểu bất đồng bộ | — | Android `suspend` · iOS `completion` (N1 — khác biệt ngôn ngữ, đã duyệt) |

→ **Kết quả hạng mục 2: wrapper 2 nền tảng bọc đủ 5 hàm headless + `updateContext`, đối xứng 1:1.**

---

## 3. Review & thống nhất terminology

### Mục tiêu
Xoá tên cũ/không nhất quán trong tài liệu và mã nguồn; chốt **một** quy ước đặt tên.

### Bước 1 — dọn tên facade cũ trong docs
Tên cũ `VDSPromotion*` mâu thuẫn code hiện tại (`PromotionSDK*`) rải rác 7 file docs. Đã nắn:
`class VDSPromotion` → `PromotionSDK`, `VDSPromotionImpl` → `PromotionSDKImpl`, `VDSPromotionError` →
`PromotionSDKError`, `VDSPromotionUseCases` → `PromotionSDKApi`, delegate `vdsPromotion(_:didX:)` →
callback `onAvailabilityChanged(enabled:)`. Giữ lại các tham chiếu **lịch sử** (repo iOS cũ) có chủ đích.

### Bước 2 — thống nhất prefix nội bộ về `PRM` (chống xung đột host)
Rà soát sâu hơn phát hiện **rủi ro đóng gói**: module nội bộ tên chung (`CoreUI`, `Utility`,
`PromotionUI`) + class chung (`GradientView`, `TapableView`…) + resource bundle (`CoreUI_CoreUI.bundle`)
**có thể trùng** với host khi tích hợp xcframework → cảnh báo runtime *"class implemented in both"* /
đụng bundle. Đã namespace toàn bộ bằng prefix **`PRM`**:

| Cũ | Mới |
|---|---|
| module `Utility` / `CoreUI` / `PromotionUI` / `PromotionKit` | `PRMFoundation` / `PRMDesignKit` / `PRMPromotionUI` / `PRMKotlinBridge` |
| class `GradientView`, `TapableView`, `VDSButton`… | `PRMGradientView`, `PRMTapableView`, `PRMButton`… |
| resource bundle `CoreUI_CoreUI.bundle` | `PRMDesignKit_PRMDesignKit.bundle` |

### Quy ước đã chốt (đồng nhất 2 nền tảng)
- **Bề mặt SDK** (host thấy): **không** prefix, trùng tên Android — `PromotionSDK`, `PromotionSDKCallback`,
  `MyPromotionViewController`.
- **Nội bộ**: tiền tố **`PRM`** (Android đã dùng `PRMBaseFragment`/`PRMEndowView`; iOS nay đồng bộ).

Xem [CodingStandards.md](./CodingStandards.md) / [ProjectStructure.md](./ProjectStructure.md).

→ **Kết quả hạng mục 3: docs sạch tên cũ; toàn bộ nội bộ về `PRM`; bề mặt public giữ `PromotionSDK*`.**

---

## 4. Cập nhật biểu đồ kiến trúc

### Mục tiêu
Biểu đồ cũ chỉ có tầng Presentation/Domain/Data; **thiếu** tầng entry/facade + wrapper và tên class
thật trong luồng. Đã bổ sung **§1.1** vào [Architecture.md](./Architecture.md).

### Đã bổ sung — sơ đồ "đường đi một lời gọi" + class chủ chốt
```
HOST APP → PromotionManager (wrapper/anti-corruption)
         → PromotionSDK (ENTRY, chữ ký sạch; Android object / iOS final class + _impl box)
         → PromotionSDKApi (RANH GIỚI headless: map model lõi → DTO)
         → PromotionContainer / SdkDi (DI tự viết) → PromotionUseCases · PromotionFeatureGate
         → DOMAIN ← DATA
```

Kèm bảng **"nút thắt cần nhớ"** làm rõ vai trò từng class: `PromotionManager` (chỗ duy nhất host chạm
SDK), `PromotionSDK` (chữ ký sạch), `PromotionSDKApi` (ranh giới phân phối, **không** nghiệp vụ),
`PromotionContainer/SdkDi` (DI — **không** sửa), `PromotionFeatureGate` (gác cờ cho cả UI lẫn headless).

→ **Kết quả hạng mục 4: kiến trúc có sơ đồ luồng đầy đủ tới tên class thật, kèm giải thích vai trò.**

---

## 5. Đánh version SDK

### Mục tiêu
Có **một nguồn version tập trung** cho toàn SDK (KMP + Android UI + iOS) + changelog.

### Đã thực hiện
- **Nguồn tập trung** ở `gradle.properties` (KMP `:promotionLogic` + `:AndroidPromotionUI` đọc property này):
  ```properties
  # gradle.properties
  SDK_VERSION=1.0.0    # override: -PSDK_VERSION=x.y.z hoặc ./scripts/build-android.sh --version x.y.z
  ```
- **iOS** đồng bộ tay: `MARKETING_VERSION = 1.0.0` trong `PromotionSDKUI.xcodeproj` — **giữ trùng số**.
- **`CHANGELOG.md`** mới (Keep a Changelog + SemVer), mục `[1.0.0]` mô tả bề mặt public đầu tiên.

### So sánh trước / sau
| | Trước | Sau |
|---|---|---|
| Nguồn version | fallback `1.0.0` rải ở 3 file gradle | **1 nơi** `gradle.properties` |
| iOS | `MARKETING_VERSION = 1.0` | `1.0.0` (trùng Android) |
| Changelog | không có | `CHANGELOG.md` |

> Lưu ý: sau đợt đồng bộ toolchain theo host (AGP/Kotlin/Gradle/JVM), **`SDK_VERSION` vẫn là `1.0.0`** —
> version SDK độc lập với version toolchain build.

→ **Kết quả hạng mục 5: version SDK chốt 1.0.0, tập trung 1 nguồn, có CHANGELOG.**

---

## Phụ lục — file/nơi kiểm chứng
| Hạng mục | Nơi kiểm chứng |
|---|---|
| 1 | `iosPromotionUI/PromotionSDKUI/Entry/PromotionSDK.swift`, [InitParity §5.3](./InitParity.md#53-widget), [PublicApi §4](./PublicApi.md) |
| 2 | `androidApp/.../app/PromotionManager.kt`, `iosApp/iosApp/PromotionManager.swift`, [InitParity §6](./InitParity.md) |
| 3 | `iosPromotionUI/Packages/PRM*`, [CodingStandards.md](./CodingStandards.md), [ProjectStructure.md](./ProjectStructure.md) |
| 4 | [Architecture.md §1.1](./Architecture.md) |
| 5 | `gradle.properties`, `PromotionSDKUI.xcodeproj` (`MARKETING_VERSION`), `CHANGELOG.md` |
