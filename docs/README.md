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
│ promotionUI (Android) │        │ promotionUI (iOS)    │
│ XML View + MVI        │        │ UIKit + MVVM/RxSwift │
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
- **UI mode** — host nhúng màn hình sẵn có của `promotionUI`.

---

## 2. Trạng thái hiện tại

| Thành phần | Trạng thái |
|---|---|
| `:promotionLogic` (KMP) | ✅ Xong. Build Android + iOS, 21 test xanh trên cả hai. |
| `:promotionUI` (Android) | ⏳ Chưa tạo. UI nguồn ở repo `ttcn-promotion-android-sdk`. |
| `promotionUI` (iOS) | ⏳ Chưa kéo sang. UI nguồn ở repo `ttcn-promotion-ios-sdk`. |
| Compose Multiplatform | 🔜 Để ngỏ, chưa dùng. Xem [ComposeGuide.md](./ComposeGuide.md). |

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
| Storage | `KeyValueStorage` expect/actual — `SharedPreferences` / `NSUserDefaults` |
| Database | **Không có.** Không Room, không SQLDelight — xem [StorageGuide.md](./StorageGuide.md) |
| Annotation processor | **Không có.** Không kapt, không KSP |

### UI Android (nguồn: `ttcn-promotion-android-sdk`)

XML View + Data Binding + View Binding, kiến trúc **MVI** trên `PRMBaseViewModel<S, A, E>`.
Xem [AndroidUIGuide.md](./AndroidUIGuide.md).

### UI iOS (nguồn: `ttcn-promotion-ios-sdk`)

UIKit (XIB), kiến trúc **MVVM + Builder + Router**, reactive bằng **RxSwift**, modular SPM.
Xem [IosUIGuide.md](./IosUIGuide.md).

---

## 4. Danh mục tài liệu

| File | Nội dung |
|------|----------|
| [AI_AGENT_RULES.md](./AI_AGENT_RULES.md) | **Quy tắc bắt buộc** cho AI agent khi làm việc trên repo. |
| [Architecture.md](./Architecture.md) | Kiến trúc tổng thể: lõi chung + hai UI native, luồng dữ liệu. |
| [ProjectStructure.md](./ProjectStructure.md) | Cấu trúc thư mục, vai trò từng package, đặt file mới ở đâu. |
| [PublicApi.md](./PublicApi.md) | **Bề mặt SDK cho app host**: `PromotionSDK`, `PromotionSDKApi`, DTO — song ánh Android ↔ iOS. |
| [HeadlessAPI.md](./HeadlessAPI.md) | API của lõi `:promotionLogic`: 5 use case nghiệp vụ + feature flag. Host **không** gọi vào đây. |
| [NetworkingGuide.md](./NetworkingGuide.md) | Ktor client, DTO, envelope, header, xử lý response. |
| [DependencyInjection.md](./DependencyInjection.md) | Custom DI: `SdkDi`, `ComponentRegistry`, các module. |
| [StorageGuide.md](./StorageGuide.md) | `KeyValueStorage`, cache feature flag. Vì sao không có DB. |
| [ErrorHandling.md](./ErrorHandling.md) | Exception, error code, `PromotionResult`, hiển thị lỗi. |
| [TestingGuide.md](./TestingGuide.md) | Test `commonTest` chạy trên cả hai nền tảng, `MockEngine`. |
| [CodingStandards.md](./CodingStandards.md) | Quy ước code Kotlin + Swift, prefix `PRM` / `VDS`. |
| [AndroidUIGuide.md](./AndroidUIGuide.md) | UI Android: XML View, Data/View Binding, RecyclerView, MVI. |
| [IosUIGuide.md](./IosUIGuide.md) | UI iOS: UIKit, XIB, MVVM + Builder/Router, RxSwift. |
| [Theming.md](./Theming.md) | Hệ thống theme/token, tùy biến brand cho host — cả hai nền tảng. |
| [Distribution.md](./Distribution.md) | Phát hành SDK Android: file AAR (hiện tại) ↔ Maven — cách làm, ràng buộc nào mất, ràng buộc nào còn. |
| [ComposeGuide.md](./ComposeGuide.md) | Trạng thái Compose Multiplatform và điều kiện áp dụng. |
| [features/](./features/README.md) | Tài liệu theo tính năng, ánh xạ màn hình Android ↔ iOS. |

---

## 5. Thứ tự đọc gợi ý

1. **AI_AGENT_RULES.md** — luật chơi.
2. **Architecture.md** — bức tranh lớn.
3. **ProjectStructure.md** — biết file nằm ở đâu.
4. **PublicApi.md** — thứ đối tác nhìn thấy. Đọc trước khi đổi bất cứ gì `public`.
5. **HeadlessAPI.md** — bề mặt lõi mà cả hai UI đều gọi.
6. Guide chuyên đề (Networking, DI, Storage, AndroidUI, IosUI) theo nhu cầu task.
7. **CodingStandards.md** + **ErrorHandling.md** — trước khi commit.

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
