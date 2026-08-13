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
├── commonMain/kotlin/com/ttcn/promotionsdk/
│   ├── config/                  # PromotionSDKConfig, PromotionRequestContextProvider,
│   │                            #   SdkEnvironment, AvailableService
│   ├── common/                  # PromotionClock (thuần common), SdkLock (expect), Uuid
│   ├── data/
│   │   ├── dto/                 # voucher/ redemption/ stackablediscount/ eligible/ featureflag/
│   │   │                        #   mỗi nhóm: Request + Response + Mapper
│   │   ├── remote/              # ApiService (+ Ktor impl), RemoteDataSource,
│   │   │                        #   PromotionHttpClient, ApiResponse (envelope), NetworkModule
│   │   ├── local/               # PromotionPreferences + SettingsPreferences,
│   │   │                        #   FeatureFlagLocalDataSource, LocalModule
│   │   └── repository/          # PromotionRepositoryImpl, FeatureFlagRepositoryImpl, RepositoryModule
│   ├── domain/
│   │   ├── model/               # voucher/ redemption/ stackablediscount/ eligible/ featureflag/
│   │   │                        #   + PromotionResult
│   │   ├── repository/          # PromotionRepository, FeatureFlagRepository (interface)
│   │   ├── usecase/             # 5 use case + PromotionUseCases
│   │   │                        #   4 use case flag + PromotionFeatureFlagUseCases, UseCaseModule
│   │   └── exception/           # PromotionException, NetworkException, ErrorCodes
│   ├── di/                      # Custom DI — CHỈ composition root + engine.
│   │   │                        #   Các *Module nằm cùng package với lớp nó dựng (xem data/, domain/)
│   │   ├── PromotionContainer.kt     # init / clear / requireConfig — nội bộ SDK, host KHÔNG thấy
│   │   ├── PlatformState.kt          # expect clearPlatformState()
│   │   └── internal/                 # SdkDi, ComponentRegistry, DiKey
│   └── presentation/            # Store dùng chung cho UI hai nền tảng
│
├── androidMain/kotlin/com/ttcn/promotionsdk/
│   ├── di/PromotionContainerAndroid.kt             # initialize(context, config) — BẮT BUỘC trên Android
│   ├── di/PlatformState.android.kt
│   ├── data/local/PromotionPreferences.android.kt  # AndroidContextHolder + dựng SharedPreferencesSettings
│   └── common/SdkLock.android.kt                   # ReentrantLock
│
├── iosMain/kotlin/com/ttcn/promotionsdk/
│   ├── di/PlatformState.ios.kt
│   ├── data/local/PromotionPreferences.ios.kt      # dựng NSUserDefaultsSettings (suite riêng)
│   └── common/SdkLock.ios.kt                       # NSRecursiveLock
│
└── commonTest/kotlin/com/ttcn/promotionsdk/        # 35 file, để phẳng
    ├── PromotionPipelineTest.kt
    ├── EligibleCampaignsTest.kt
    ├── FeatureFlagTest.kt
    ├── PromotionPreferencesTest.kt
    ├── PromotionClockTest.kt
    └── PromotionContainerTest.kt
```

### 2.1. Vì sao chia như vậy

- `data/`, `domain/`, `presentation/` — **ba tầng Clean Architecture**, ngang hàng nhau. Đọc cây
  package là thấy ngay tầng nào có gì, không phải chui qua một lớp `core/` trung gian.
- `di/` **không** nằm trong `common/`: nó là composition root, phụ thuộc vào *mọi* tầng
  (`LocalModule` → data, `UseCaseModule` → domain). `common` nghĩa là thứ mọi tầng phụ thuộc *vào* —
  đặt `di` trong đó là đảo ngược ý nghĩa của chính cái tên.
- `config/` ở top-level vì **cả ba tầng đều đọc nó** (`data/remote` nhận `PromotionRequestContextProvider`,
  `di` giữ `PromotionSDKConfig`, tầng UI dựng nó) và nó là **bề mặt hợp đồng** — `consumer-rules.pro`
  giữ `config.**` khỏi R8. Nhét vào `common/` sẽ trộn hợp đồng public với plumbing nội bộ.
- `common/` để **phẳng**, chỉ 3 tiện ích cross-cutting. `common/util/` là thừa một tầng.

---

## 3. Quy ước đặt file mới

| Loại file | Đặt ở đâu |
|-----------|-----------|
| API endpoint mới | `data/remote/PromotionApiService.kt` (interface + Ktor impl) + `PromotionRemoteDataSource` |
| DTO mới | `data/dto/<nhóm>/` kèm hàm mapping `toXxx()` |
| Domain model mới | `domain/model/<nhóm>/` |
| Use case mới | `domain/usecase/`, đăng ký ở `di/UseCaseModule.kt`, phơi qua `PromotionUseCases` |
| Repository mới | interface ở `domain/repository/`, impl ở `data/repository/`, đăng ký ở `RepositoryModule` |
| Cần API riêng nền tảng | `expect` ở `commonMain`, `actual` ở `androidMain` **và** `iosMain` |
| Test | `commonTest/` — chạy trên cả hai nền tảng |
| Màn hình Android mới | `:promotionSDK` — `feature/<tên>/` với Fragment + ViewModel + Contract |
| Màn hình iOS mới | `promotionSDK` — bộ Builder / Router / ViewModel / ViewController |

---

## 4. Quy ước đặt tên

- **Lõi KMP**: không prefix. `PromotionUseCases`, `EligibleOffer`, `PromotionPreferences`.
- **UI Android**: base class và widget dùng tiền tố **`PRM`** (`PRMBaseFragment`, `PRMEndowView`).
  Tiền tố **không** đồng nghĩa với "public": chỉ `PRMEndowView` ở `entry/endowview/` là bề mặt host,
  còn `PRMBaseFragment`, `PRMButton`… đều `internal`.
- **UI iOS**: bề mặt SDK **không** prefix, đồng nhất tên với Android (`PromotionSDK`, `PromotionSDKCallback`, `MyPromotionViewController`); riêng design-system dùng chung `PRMDesignKit` dùng tiền tố **`PRM`** (`PRMButton`, `PRMButtonThemeToken`).
- DTO kết thúc bằng `Request` / `Response`; domain model dùng tên nghiệp vụ (`VoucherDetail`).
- Module DI kết thúc bằng `Module`. Bản Ktor của ApiService bắt đầu bằng `Ktor`.
- Feature contract Android: `XxxUiState` / `XxxAction` / `XxxEffect`, gộp trong `XxxContract.kt`.

---

## 5. Nơi KHÔNG nên chạm nếu không cần

- `di/internal/` — cơ chế DI lõi. Sửa thì phải cập nhật `DependencyInjection.md`.
- `common/SdkLock.kt` — đổi sang khoá không reentrant sẽ deadlock lúc init.
- `PromotionContainer`, `PromotionUseCases`, `PromotionFeatureFlagUseCases`, `PromotionSDKConfig`
  — bề mặt lõi. Host **không** thấy chúng (`implementation(projects.promotionLogic)`), nhưng cả hai
  UI SDK đều dựa vào; đổi = sửa Android + iOS cùng lúc.
- `com.ttcn.prm.entry.**` của `AndroidPromotionSDK` (`entry/`, `entry/api/`) cùng hai nhóm public
  nằm ngoài nó vì lý do lịch sử — `ui/theme/` (+ `ui/theme/token/`) và `ui/feature/endowview/` —
  **public API thật sự**: mọi thứ khác đều `internal`. Thay đổi = breaking cho host app, và phải sửa đối ứng bên `iosPromotionSDK/Entry/`
  của iOS. Xem [PublicApi.md](./PublicApi.md).
- `gradle/libs.versions.toml` — chỉ thêm dependency khi được yêu cầu (AI_AGENT_RULES điều 6).
- `sharedLogic/`, `sharedUI/` — scaffold template, không phải nơi đặt logic Promotion.

---

## 6. Repo nguồn (chỉ đọc)

| Repo | Vai trò |
|---|---|
| `~/Personal/ttcn-promotion-android-sdk` | SDK Android gốc — nguồn của UI Android và của `core/` đã port |
| `~/IosProject/ttcn-promotion-ios-sdk` | SDK iOS gốc — nguồn của UI iOS và của `findEligible` |

Không sửa hai repo này từ project hiện tại.
