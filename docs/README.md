# Tài liệu nền tảng — TTCN Promotion SDK (Kotlin Multiplatform)

Thư mục `/docs` chứa toàn bộ tài liệu nền tảng của **TTCN Promotion SDK**, phiên bản Kotlin Multiplatform.
Mục tiêu: giúp lập trình viên **và AI agent** hiểu nhanh kiến trúc, quy ước và quy tắc làm việc trước khi
viết hoặc sửa code.

> ⚠️ **Trước khi bắt đầu bất kỳ task nào, hãy đọc [AI_AGENT_RULES.md](./AI_AGENT_RULES.md).**

## Mục lục

<!-- toc -->
- [1. SDK này là gì](#1-sdk-này-là-gì)
- [2. Trạng thái hiện tại](#2-trạng-thái-hiện-tại)
- [3. Tổng quan kỹ thuật](#3-tổng-quan-kỹ-thuật)
  - [3.1. Lõi dùng chung — `:promotionLogic`](#31-lõi-dùng-chung--promotionlogic)
  - [3.2. UI Android (nguồn: `ttcn-promotion-android-sdk`)](#32-ui-android-nguồn-ttcn-promotion-android-sdk)
  - [3.3. UI iOS (nguồn: `ttcn-promotion-ios-sdk`)](#33-ui-ios-nguồn-ttcn-promotion-ios-sdk)
- [4. Danh mục tài liệu](#4-danh-mục-tài-liệu)
- [5. Thứ tự đọc gợi ý](#5-thứ-tự-đọc-gợi-ý)
- [6. Lệnh thường dùng](#6-lệnh-thường-dùng)
- [7. Nguyên tắc cập nhật tài liệu](#7-nguyên-tắc-cập-nhật-tài-liệu)
<!-- /toc -->

---

## 1. SDK này là gì

Promotion SDK đi theo hướng **headless**: business logic nằm ở một lõi Kotlin Multiplatform dùng chung,
còn giao diện do **mỗi nền tảng tự dựng** bằng công nghệ native của mình. Kết quả là hai SDK hoàn chỉnh
(AAR cho Android, XCFramework cho iOS) đứng trên cùng một lõi.

```
┌───────────────────────┐        ┌──────────────────────┐
│ promotionSDK (Android) │        │ promotionSDK (iOS)    │
│ XML View + MVI        │        │ UIKit + MVVM/callback│
└──────────┬────────────┘        └──────────┬───────────┘
           │                                │
           └────────────┬───────────────────┘
                        ▼
              ┌───────────────────┐
              │  :promotionLogic  │   Kotlin Multiplatform, headless
              │  data / domain /  │   Ktor + kotlinx.serialization
              │  usecase          │   không UI, không chuỗi hiển thị
              └───────────────────┘
```

Hai chế độ dùng SDK:

- **Headless** — host tự dựng UI, chỉ gọi nghiệp vụ qua `PromotionUseCases()`.
- **UI mode** — host nhúng màn hình sẵn có của `promotionSDK`.

---

## 2. Trạng thái hiện tại

| Thành phần | Trạng thái |
|---|---|
| `:promotionLogic` (KMP) | ✅ Xong. Build Android + iOS, **43 file test / 383 case** `commonTest` chạy trên cả hai. |
| `:AndroidPromotionSDK` (Android) | ✅ Đã ở trong repo (module `AndroidPromotionSDK/`). Phát hành Maven — xem [android/Distribution.md](./android/Distribution.md). |
| `iosPromotionSDK` (iOS) | ✅ Đã ở trong repo (`iosPromotionSDK/`, project `PromotionKit.xcodeproj`). Phát hành **XCFramework** — xem [ios/Distribution.md](./ios/Distribution.md). |
| App demo host | ✅ `androidApp/` và `iosApp/` — host mẫu tiêu thụ SDK. |
| Compose Multiplatform | 🔜 Để ngỏ, chưa dùng. Xem [common/ComposeGuide.md](./common/ComposeGuide.md). |

---

## 3. Tổng quan kỹ thuật

### 3.1. Lõi dùng chung — `:promotionLogic`

| Hạng mục | Giá trị |
|----------|---------|
| Ngôn ngữ | Kotlin 2.2.0 |
| Target | `android` (minSdk 24, compileSdk 36), `iosArm64`, `iosSimulatorArm64` |
| Framework iOS | `PromotionLogic.framework` (static) |
| Networking | **Ktor Client 3.3.0** (OkHttp engine trên Android, Darwin trên iOS) |
| JSON | **kotlinx.serialization 1.8.1** |
| Bất đồng bộ | Kotlin Coroutines 1.10.2 |
| DI | Custom DI tự viết (`SdkDi`), không Hilt/Koin/Dagger |
| Storage | `PromotionPreferences` expect/actual — `SharedPreferences` / `NSUserDefaults` |
| Database | **Không có.** Không Room, không SQLDelight — xem [common/StorageGuide.md](./common/StorageGuide.md) |
| Annotation processor | **Không có.** Không kapt, không KSP |

### 3.2. UI Android (nguồn: `ttcn-promotion-android-sdk`)

XML View + Data Binding + View Binding, kiến trúc **MVI** trên `PRMStoreViewModel<S, I>` — lớp mỏng bọc
store dùng chung ở lõi; **không** còn `UiState`/`Action`/`Effect` riêng từng màn.
Xem [android/UIGuide.md](./android/UIGuide.md).

### 3.3. UI iOS (nguồn: `ttcn-promotion-ios-sdk`)

UIKit (XIB), kiến trúc **MVVM + Builder + Router**, ràng buộc View↔VM bằng **callback thuần**, modular SPM.
RxSwift đã được gỡ hoàn toàn (không dependency ngoài). Xem [ios/UIGuide.md](./ios/UIGuide.md).

---

## 4. Danh mục tài liệu

Docs chia ba tầng: **`common/`** (chung 2 nền tảng — lõi KMP + spec song ánh), **`android/`**, **`ios/`**.
Hai file gốc `AI_AGENT_RULES.md` và `README.md` đứng ngoài phân tầng (meta).

**Gốc (meta)**

| File | Nội dung |
|------|----------|
| [AI_AGENT_RULES.md](./AI_AGENT_RULES.md) | **Quy tắc bắt buộc** cho AI agent khi làm việc trên repo (ưu tiên cao nhất). |

**`common/` — chung 2 nền tảng**

| File | Nội dung |
|------|----------|
| [common/Architecture.md](./common/Architecture.md) | Kiến trúc tổng thể: lõi chung + hai UI native, luồng dữ liệu. |
| [common/ProjectStructure.md](./common/ProjectStructure.md) | Cấu trúc thư mục, vai trò từng package, đặt file mới ở đâu. |
| [common/PublicApi.md](./common/PublicApi.md) | **Bề mặt SDK cho app host**: `PromotionSDK`, `PromotionSDKApi`, DTO — song ánh Android ↔ iOS. |
| [common/HeadlessAPI.md](./common/HeadlessAPI.md) | API của lõi `:promotionLogic`: 5 use case nghiệp vụ + feature flag. Host **không** gọi vào đây. |
| [common/InitParity.md](./common/InitParity.md) | Spec khởi tạo Android ↔ iOS: `PromotionSDK`/`Options`/`SessionConfig`/`Callback`. |
| [common/SdkReview.md](./common/SdkReview.md) | Báo cáo rà soát & hoàn thiện SDK (UI public iOS, wrapper, terminology, kiến trúc, version) — kèm ví dụ. |
| [common/NetworkingGuide.md](./common/NetworkingGuide.md) | Ktor client, DTO, envelope, header, xử lý response. |
| [common/DependencyInjection.md](./common/DependencyInjection.md) | Custom DI: `SdkDi`, `ComponentRegistry`, các module. |
| [common/StorageGuide.md](./common/StorageGuide.md) | `PromotionPreferences`, cache feature flag. Vì sao không có DB. |
| [common/ErrorHandling.md](./common/ErrorHandling.md) | Exception, error code, `PromotionResult`, hiển thị lỗi. |
| [common/TlnvGap.md](./common/TlnvGap.md) | **Chỗ app còn lệch tài liệu nghiệp vụ** (`docs/tlnv/`) — đọc trước khi kết luận "bug". |
| [common/TestingGuide.md](./common/TestingGuide.md) | Test `commonTest` chạy trên cả hai nền tảng, `MockEngine`. |
| [common/Security.md](./common/Security.md) | **Bảo mật & dữ liệu**: quyền, dữ liệu lưu, token, log, hàng rào bề mặt, danh mục kiểm cho bên an ninh. |
| [common/CodingStandards.md](./common/CodingStandards.md) | Quy ước code Kotlin + Swift, prefix `PRM` / `VDS`. |
| [common/Theming.md](./common/Theming.md) | Hệ thống theme/token, tùy biến brand cho host — cả hai nền tảng. |
| [common/ComposeGuide.md](./common/ComposeGuide.md) | Trạng thái Compose Multiplatform và điều kiện áp dụng. |

**`android/` — riêng Android**

| File | Nội dung |
|------|----------|
| [android/UIGuide.md](./android/UIGuide.md) | UI Android: XML View, Data/View Binding, RecyclerView, MVI. |
| [android/Distribution.md](./android/Distribution.md) | Phát hành SDK Android: Maven (↔ AAR cũ) — cách làm, ràng buộc nào mất/còn. |

**`ios/` — riêng iOS**

| File | Nội dung |
|------|----------|
| [ios/UIGuide.md](./ios/UIGuide.md) | UI iOS: UIKit, XIB, MVVM + Builder/Router, **callback thuần** (không Combine/Rx). |
| [ios/Distribution.md](./ios/Distribution.md) | Phát hành SDK iOS: XCFramework, dSYM, slice, **đóng gói (host không cài thêm gì)**. |

**`features/` — theo tính năng (2 nền tảng)**

| File | Nội dung |
|------|----------|
| [features/](./features/README.md) | Tài liệu theo tính năng, ánh xạ màn hình Android ↔ iOS ↔ use case. |

**`api/`, `tlnv/` — tài liệu nguồn của đối tác (PDF, không sửa)**

| Thư mục | Nội dung |
|------|----------|
| `api/` | Spec API backend (5 PDF: Find Eligible, Create Redemption, Validate Stackable Discounts, Search Customer Vouchers, Voucher Detail) — nguồn đối chiếu cho [common/NetworkingGuide.md](./common/NetworkingGuide.md). |
| `tlnv/` | Tài liệu nghiệp vụ KBNV (5 PDF) — nguồn sự thật nghiệp vụ; chỗ app còn lệch ghi ở [common/TlnvGap.md](./common/TlnvGap.md). |

---

**`design/` — thiết kế hợp nhất (nghiệm thu)**

| File | Nội dung |
|------|----------|
| [design/SDD.md](./design/SDD.md) | **Tài liệu thiết kế chi tiết** — bản hợp nhất dùng để nghiệm thu: phạm vi, kiến trúc, dữ liệu, luồng nghiệp vụ, lỗi, bảo mật, ma trận truy vết yêu cầu. |

**`release/` — đóng gói, phát hành & bàn giao**

| File | Nội dung |
|------|----------|
| [release/PackagingGuide.md](./release/PackagingGuide.md) | Cách tạo gói bàn giao, nội dung gói, checksum, dSYM, sinh `.docx` + bản Confluence. |
| [release/ReleaseChecklist.md](./release/ReleaseChecklist.md) | Danh mục kiểm bắt buộc trước khi phát hành. |
| [release/ReleaseNotes.md](./release/ReleaseNotes.md) | Ghi chú phát hành **cho đội tích hợp phía host**. |
| [release/CompatibilityMatrix.md](./release/CompatibilityMatrix.md) | Yêu cầu tối thiểu, công cụ build, phụ thuộc, ma trận SDK ↔ backend. |
| [release/VersioningPolicy.md](./release/VersioningPolicy.md) | SemVer, vòng đời hỗ trợ, chính sách khai tử API. |
| [release/MigrationGuide.md](./release/MigrationGuide.md) | Hướng dẫn nâng cấp giữa các phiên bản, từng bước. |
| [release/TestReport.md](./release/TestReport.md) | Báo cáo kiểm thử: bộ test hiện có + biểu mẫu điền kết quả. |
| [release/HandoverChecklist.md](./release/HandoverChecklist.md) | Danh mục bàn giao cho đối tác. |

**Gốc — dành cho host**

| File | Nội dung |
|------|----------|
| [QuickStart.md](./QuickStart.md) | Tích hợp trong 15 phút, hai nền tảng. |
| [AndroidIntegrationGuide.md](./AndroidIntegrationGuide.md) | Hướng dẫn tích hợp đầy đủ cho app host Android. |
| [IosIntegrationGuide.md](./IosIntegrationGuide.md) | Hướng dẫn tích hợp đầy đủ cho app host iOS. |
| [Troubleshooting.md](./Troubleshooting.md) | Tra sự cố theo triệu chứng + FAQ. |

## 5. Thứ tự đọc gợi ý

1. **AI_AGENT_RULES.md** — luật chơi.
2. **common/Architecture.md** — bức tranh lớn.
3. **common/ProjectStructure.md** — biết file nằm ở đâu.
4. **common/PublicApi.md** — thứ đối tác nhìn thấy. Đọc trước khi đổi bất cứ gì `public`.
5. **common/HeadlessAPI.md** — bề mặt lõi mà cả hai UI đều gọi.
6. Guide chuyên đề: `common/` (Networking, DI, Storage) + `android/UIGuide.md` / `ios/UIGuide.md` theo nhu cầu task.
7. **common/CodingStandards.md** + **common/ErrorHandling.md** — trước khi commit.

---

## 6. Lệnh thường dùng

```bash
./gradlew :promotionLogic:assemble                    # AAR (Android) + framework (iOS)
./gradlew :promotionLogic:testAndroidHostTest         # test trên JVM
./gradlew :promotionLogic:iosSimulatorArm64Test       # test trên iOS simulator
./gradlew :promotionLogic:linkDebugFrameworkIosArm64  # framework cho iOS device
```

---

## 7. Nguyên tắc cập nhật tài liệu

Tài liệu phải **luôn đồng bộ với code thật**. Khi thay đổi API, storage, DI hoặc kiến trúc,
**bắt buộc cập nhật file docs tương ứng trong cùng một thay đổi** (xem AI_AGENT_RULES điều 8).

Nếu phát hiện docs mâu thuẫn với source: **tin source, sửa docs** (AI_AGENT_RULES điều 2).
