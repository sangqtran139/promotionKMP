# Tài liệu nền tảng — TTCN Promotion SDK (Kotlin Multiplatform)

Thư mục `/docs` chứa toàn bộ tài liệu nền tảng của **TTCN Promotion SDK**, phiên bản Kotlin Multiplatform.
Mục tiêu: giúp lập trình viên **và AI agent** hiểu nhanh kiến trúc, quy ước và quy tắc làm việc trước khi
viết hoặc sửa code.

> ⚠️ **Trước khi bắt đầu bất kỳ task nào, hãy đọc [AI_AGENT_RULES.md](./AI_AGENT_RULES.md).**

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
| `:promotionLogic` (KMP) | ✅ Xong. Build Android + iOS, 21 test xanh trên cả hai. |
| `:AndroidPromotionSDK` (Android) | ✅ Đã ở trong repo (module `AndroidPromotionSDK/`). Phát hành Maven — xem [android/Distribution.md](./android/Distribution.md). |
| `iosPromotionSDK` (iOS) | ✅ Đã ở trong repo (`iosPromotionSDK/`, project `PRM.xcodeproj`). Phát hành **XCFramework** — xem [ios/Distribution.md](./ios/Distribution.md). |
| App demo host | ✅ `androidApp/` và `iosApp/` — host mẫu tiêu thụ SDK. |
| `:networkKit` (KMP) | 🚧 Phase 1, UC4 xong — `HttpClient` factory + header tĩnh/động + token provider + request builder GET/POST an toàn, chưa parsing response, chưa tích hợp vào `promotionLogic`. Xem [common/SharedNetworkKit.md](./common/SharedNetworkKit.md). |
| Compose Multiplatform | 🔜 Để ngỏ, chưa dùng. Xem [common/ComposeGuide.md](./common/ComposeGuide.md). |

---

## 3. Tổng quan kỹ thuật

### Lõi dùng chung — `:promotionLogic`

| Hạng mục | Giá trị |
|----------|---------|
| Ngôn ngữ | Kotlin 2.4.0 |
| Target | `android` (minSdk 24, compileSdk 36), `iosArm64`, `iosSimulatorArm64` |
| Framework iOS | `PromotionLogic.framework` (static) |
| Networking | **Ktor Client 3.3.0** (OkHttp engine trên Android, Darwin trên iOS) |
| JSON | **kotlinx.serialization 1.8.1** |
| Bất đồng bộ | Kotlin Coroutines 1.10.2 |
| DI | Custom DI tự viết (`SdkDi`), không Hilt/Koin/Dagger |
| Storage | `PromotionPreferences` expect/actual — `SharedPreferences` / `NSUserDefaults` |
| Database | **Không có.** Không Room, không SQLDelight — xem [common/StorageGuide.md](./common/StorageGuide.md) |
| Annotation processor | **Không có.** Không kapt, không KSP |

### UI Android (nguồn: `ttcn-promotion-android-sdk`)

XML View + Data Binding + View Binding, kiến trúc **MVI** trên `PRMBaseViewModel<S, A, E>`.
Xem [android/UIGuide.md](./android/UIGuide.md).

### UI iOS (nguồn: `ttcn-promotion-ios-sdk`)

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
| [common/SharedNetworkKit.md](./common/SharedNetworkKit.md) | Module KMP network dùng chung mới (`:networkKit`, đang xây — Phase 1): mục tiêu, ranh giới, trạng thái từng use case. |
| [common/DependencyInjection.md](./common/DependencyInjection.md) | Custom DI: `SdkDi`, `ComponentRegistry`, các module. |
| [common/StorageGuide.md](./common/StorageGuide.md) | `PromotionPreferences`, cache feature flag. Vì sao không có DB. |
| [common/ErrorHandling.md](./common/ErrorHandling.md) | Exception, error code, `PromotionResult`, hiển thị lỗi. |
| [common/TlnvGap.md](./common/TlnvGap.md) | **Chỗ app còn lệch tài liệu nghiệp vụ** (`docs/tlnv/`) — đọc trước khi kết luận "bug". |
| [common/TestingGuide.md](./common/TestingGuide.md) | Test `commonTest` chạy trên cả hai nền tảng, `MockEngine`. |
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

---

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
