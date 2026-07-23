# ProjectStructure — Cấu trúc project

Biết **file nằm ở đâu** và **đặt file mới vào đâu cho đúng**.

---

## 1. Repo hiện tại

```
MyApplication13/
├── settings.gradle.kts        # include :androidApp, :sharedLogic, :sharedUI, :promotionLogic
├── gradle/libs.versions.toml  # Version catalog — nguồn duy nhất của dependency
├── docs/                      # Tài liệu nền tảng (file này)
├── promotionLogic/            # 📦 Lõi KMP headless — data / domain / usecase
├── iosApp/                    # Entry point iOS (Xcode)
├── androidApp/                # App Android demo/host
├── sharedLogic/               # Scaffold KMP mặc định (chưa dùng cho Promotion)
└── sharedUI/                  # Scaffold Compose Multiplatform (chưa dùng cho Promotion)
```

- **`promotionLogic`** — sản phẩm thật, namespace `com.ttcn.promotionsdk`. Mọi thay đổi nghiệp vụ ở đây.
- **`sharedLogic` / `sharedUI`** — scaffold do template KMP sinh ra, **chưa** liên quan Promotion SDK.
  Không đặt logic Promotion vào đó.

### Module dự kiến (chưa tạo)

| Module | Nội dung | Nguồn |
|---|---|---|
| `:promotionSDK` (Android) | Fragment, XML, adapter, theme, MVI | `ttcn-promotion-android-sdk/vds-promotion/ui/` |
| `promotionSDK` (iOS) | ViewController, XIB, MVVM/Router, RxSwift | `ttcn-promotion-ios-sdk/VDSPromotion` + `Packages/PRMPromotionUI` |

---

## 2. Cấu trúc `promotionLogic`

```
promotionLogic/src/
├── commonMain/kotlin/com/ttcn/promotionsdk/core/
│   ├── config/                  # PromotionSDKConfig, PromotionRequestContextProvider
│   ├── di/                      # Custom DI
│   │   ├── PromotionContainer.kt     # init / clear / requireConfig — nội bộ SDK, host KHÔNG thấy
│   │   ├── NetworkModule.kt          # HttpClient, ApiService, RemoteDataSource
│   │   ├── LocalModule.kt            # KeyValueStorage, FeatureFlagLocalDataSource
│   │   ├── RepositoryModule.kt
│   │   ├── UseCaseModule.kt
│   │   ├── FeatureFlagModule.kt
│   │   ├── PlatformState.kt          # expect clearPlatformState()
│   │   └── internal/                 # SdkDi, ComponentRegistry, DiKey
│   ├── data/
│   │   ├── dto/                 # voucher/ redemption/ stackablediscount/ eligible/ featureflag/
│   │   │                        #   mỗi nhóm: Request + Response + Mapper
│   │   ├── remote/              # ApiService (+ Ktor impl), RemoteDataSource,
│   │   │                        #   PromotionHttpClient, ApiResponse (envelope)
│   │   ├── local/               # KeyValueStorage (expect), FeatureFlagLocalDataSource
│   │   └── repository/          # PromotionRepositoryImpl, FeatureFlagRepositoryImpl
│   ├── domain/
│   │   ├── model/               # voucher/ redemption/ stackablediscount/ eligible/ featureflag/
│   │   │                        #   + PromotionResult
│   │   ├── repository/          # PromotionRepository, FeatureFlagRepository (interface)
│   │   ├── usecase/             # 5 use case + PromotionUseCases
│   │   │                        #   4 use case flag + PromotionFeatureFlagUseCases
│   │   └── exception/           # PromotionException, NetworkException, ErrorCodes
│   └── util/                    # SdkLock (expect), Uuid
│
├── androidMain/kotlin/com/ttcn/promotionsdk/core/
│   ├── di/PromotionContainerAndroid.kt        # init(context, config) — BẮT BUỘC trên Android
│   ├── di/PlatformState.android.kt
│   ├── data/local/KeyValueStorage.android.kt  # SharedPrefStorage + AndroidContextHolder
│   └── util/SdkLock.android.kt                # ReentrantLock
│
├── iosMain/kotlin/com/ttcn/promotionsdk/core/
│   ├── di/PlatformState.ios.kt
│   ├── data/local/KeyValueStorage.ios.kt      # UserDefaultsStorage
│   └── util/SdkLock.ios.kt                    # NSRecursiveLock
│
└── commonTest/kotlin/com/ttcn/promotionsdk/core/
    ├── PromotionPipelineTest.kt
    ├── EligibleCampaignsTest.kt
    ├── FeatureFlagTest.kt
    └── PromotionContainerTest.kt
```

---

## 3. Quy ước đặt file mới

| Loại file | Đặt ở đâu |
|-----------|-----------|
| API endpoint mới | `core/data/remote/PromotionApiService.kt` (interface + Ktor impl) + `PromotionRemoteDataSource` |
| DTO mới | `core/data/dto/<nhóm>/` kèm hàm mapping `toXxx()` |
| Domain model mới | `core/domain/model/<nhóm>/` |
| Use case mới | `core/domain/usecase/`, đăng ký ở `core/di/UseCaseModule.kt`, phơi qua `PromotionUseCases` |
| Repository mới | interface ở `core/domain/repository/`, impl ở `core/data/repository/`, đăng ký ở `RepositoryModule` |
| Cần API riêng nền tảng | `expect` ở `commonMain`, `actual` ở `androidMain` **và** `iosMain` |
| Test | `commonTest/` — chạy trên cả hai nền tảng |
| Màn hình Android mới | `:promotionSDK` — `feature/<tên>/` với Fragment + ViewModel + Contract |
| Màn hình iOS mới | `promotionSDK` — bộ Builder / Router / ViewModel / ViewController |

---

## 4. Quy ước đặt tên

- **Lõi KMP**: không prefix. `PromotionUseCases`, `EligibleOffer`, `KeyValueStorage`.
- **UI Android**: base class và nhiều public class dùng tiền tố **`PRM`** (`PRMBaseFragment`, `PRMEndowView`).
- **UI iOS**: bề mặt SDK **không** prefix, đồng nhất tên với Android (`PromotionSDK`, `PromotionSDKCallback`, `MyPromotionViewController`); riêng design-system dùng chung `PRMDesignKit` dùng tiền tố **`PRM`** (`PRMButton`, `PRMButtonThemeToken`).
- DTO kết thúc bằng `Request` / `Response`; domain model dùng tên nghiệp vụ (`VoucherDetail`).
- Module DI kết thúc bằng `Module`. Bản Ktor của ApiService bắt đầu bằng `Ktor`.
- Feature contract Android: `XxxUiState` / `XxxAction` / `XxxEffect`, gộp trong `XxxContract.kt`.

---

## 5. Nơi KHÔNG nên chạm nếu không cần

- `core/di/internal/` — cơ chế DI lõi. Sửa thì phải cập nhật `DependencyInjection.md`.
- `core/util/SdkLock.kt` — đổi sang khoá không reentrant sẽ deadlock lúc init.
- `PromotionContainer`, `PromotionUseCases`, `PromotionFeatureFlagUseCases`, `PromotionSDKConfig`
  — bề mặt lõi. Host **không** thấy chúng (`implementation(projects.promotionLogic)`), nhưng cả hai
  UI SDK đều dựa vào; đổi = sửa Android + iOS cùng lúc.
- `ui/entry/` và `ui/entry/api/` của `AndroidPromotionSDK` — **public API thật sự**; thay đổi =
  breaking cho host app, và phải sửa đối ứng bên `PromotionSDKUI/Entry/API/` của iOS.
  Xem [PublicApi.md](./PublicApi.md).
- `gradle/libs.versions.toml` — chỉ thêm dependency khi được yêu cầu (AI_AGENT_RULES điều 6).
- `sharedLogic/`, `sharedUI/` — scaffold template, không phải nơi đặt logic Promotion.

---

## 6. Repo nguồn (chỉ đọc)

| Repo | Vai trò |
|---|---|
| `~/Personal/ttcn-promotion-android-sdk` | SDK Android gốc — nguồn của UI Android và của `core/` đã port |
| `~/IosProject/ttcn-promotion-ios-sdk` | SDK iOS gốc — nguồn của UI iOS và của `findEligible` |

Không sửa hai repo này từ project hiện tại.
