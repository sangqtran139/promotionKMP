# ProjectStructure — Cấu trúc project

Biết **file nằm ở đâu** và **đặt file mới vào đâu cho đúng**.

---

## 1. Repo hiện tại

```
ttcn-promotion-android-sdk/
├── settings.gradle.kts         # include :androidApp, :promotionLogic, :AndroidPromotionSDK
├── gradle/libs.versions.toml   # Version catalog — nguồn duy nhất của dependency
├── gradle.properties           # SDK_VERSION / SDK_GROUP — nguồn version tập trung
├── CHANGELOG.md                # Keep a Changelog + SemVer
├── docs/                       # Tài liệu nền tảng (file này)
├── scripts/                    # build-android.sh, build-ios.sh, test-report.sh
│
├── promotionLogic/             # 📦 Lõi KMP headless — data / domain / usecase / presentation
├── AndroidPromotionSDK/        # 📦 SDK Android (AAR) — Fragment, XML, adapter, theme
├── iosPromotionSDK/            # 📦 SDK iOS (XCFramework) — UIKit, MVVM + Builder/Router
│
├── androidApp/                 # App Android demo/host
└── iosApp/                     # App iOS demo/host (Xcode)
```

Ba module đầu là **sản phẩm phát hành**, hai module cuối chỉ để demo/thử tích hợp:

| Module | Namespace / vị trí | Vai trò |
|---|---|---|
| `:promotionLogic` | `com.ttcn.promotionsdk` | Lõi dùng chung. **Mọi thay đổi nghiệp vụ ở đây.** Phát hành `$SDK_GROUP:promotionLogic`. |
| `:AndroidPromotionSDK` | `com.ttcn.prm` | UI Android. Phát hành `$SDK_GROUP:promotion` (AAR). |
| `iosPromotionSDK/` | `PRM.xcodeproj` + SPM | UI iOS. Phát hành `VDSPromotionSDK.xcframework`. |

> ⚠️ `iosPromotionSDK` **không** là module Gradle — nó là project Xcode, tiêu thụ lõi qua
> XCFramework do `:promotionLogic` sinh ra. Vì vậy `settings.gradle.kts` chỉ có 3 `include`.

### 1.1. Cấu trúc hai module UI

```
AndroidPromotionSDK/src/main/java/com/ttcn/prm/
├── entry/            # ⭐ Public API — PromotionSDK, PromotionSDKOptions, callback
│   └── api/          #    Headless API + DTO public (PromotionSDKApi, PromotionVoucherDetail…)
└── ui/               # internal
    ├── base/         #    PRMBaseFragment, PRMStoreViewModel, PrmSystemBackInterceptor
    ├── di/           #    promotionViewModelFactory()
    ├── feature/      #    mypromotion/ searchmypromotion/ choosepromotion/
    │                 #      promotiondetail/ endowview/ serviceselector/
    ├── theme/        # ⭐ public — PromotionSDKTheme + token/
    ├── utils/        #    extension/, PRMImageExt, PRMClick…
    └── widget/       #    PRMButton, PRMShadowView, PRMSlideButton…

iosPromotionSDK/
├── Entry/            # ⭐ Public API — PromotionSDK, PromotionSDKImpl, Config, Options
│   └── API/          #    Headless API + model public
├── PromotionSDKUI/   #    Base/ (MVVM), Common/, Theme/ + một thư mục cho mỗi màn:
│                     #      MyPromotions/ Search/ SelectPromotion/ DetailPromotion/
│                     #      Endow/ ServiceSelector/
├── Packages/         #    SPM nội bộ — PRMFoundation, PRMDesignKit, PRMPromotionUI, PRMKotlinBridge
└── Frameworks/       #    XCFramework của lõi (sinh ra, không commit)
```

Mỗi màn có **một thư mục ở cả ba module** — ví dụ màn "Ưu đãi của tôi":
`promotionLogic/…/presentation/mypromotion/` + `AndroidPromotionSDK/…/ui/feature/mypromotion/` +
`iosPromotionSDK/PromotionSDKUI/MyPromotions/`. Sửa nghiệp vụ ở thư mục đầu, hai thư mục sau chỉ render.

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
│   └── presentation/            # Store UI-logic dùng chung cho hai nền tảng
│       ├── base/                #   PRMStore, PRMEffect
│       ├── common/              #   tiện ích dùng chung giữa các store
│       └── <feature>/           #   mypromotion/ searchmypromotion/ choosepromotion/
│                                #     promotiondetail/ endow/ serviceselector/
│                                #     mỗi thư mục: XxxStore + XxxContract
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
└── commonTest/kotlin/com/ttcn/promotionsdk/        # 40 file, để phẳng
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
| Test | `promotionLogic/commonTest/` — chạy trên cả hai nền tảng |
| **Logic UI** của màn mới | `promotionLogic/…/presentation/<tên>/` — `XxxStore` + `XxxContract`. Viết **một lần**, hai nền tảng dùng chung. |
| Màn hình Android mới | `AndroidPromotionSDK/…/ui/feature/<tên>/` — Fragment + `PRMStoreViewModel` con + layout XML |
| Màn hình iOS mới | `iosPromotionSDK/PromotionSDKUI/<Tên>/` — bộ Builder / Router / ViewModel / ViewController |

---

## 4. Quy ước đặt tên

- **Lõi KMP**: không prefix. `PromotionUseCases`, `EligibleOffer`, `PromotionPreferences`.
- **UI Android**: base class và widget dùng tiền tố **`PRM`** (`PRMBaseFragment`, `PRMEndowView`).
  Tiền tố **không** đồng nghĩa với "public": chỉ `PRMEndowView` ở `entry/endowview/` là bề mặt host,
  còn `PRMBaseFragment`, `PRMButton`… đều `internal`.
- **UI iOS**: bề mặt SDK **không** prefix, đồng nhất tên với Android (`PromotionSDK`, `PromotionSDKCallback`, `MyPromotionViewController`); riêng design-system dùng chung `PRMDesignKit` dùng tiền tố **`PRM`** (`PRMButton`, `PRMButtonThemeToken`).
- DTO kết thúc bằng `Request` / `Response`; domain model dùng tên nghiệp vụ (`VoucherDetail`).
- Module DI kết thúc bằng `Module`. Bản Ktor của ApiService bắt đầu bằng `Ktor`.
- **Feature contract nằm ở lõi**, không ở tầng UI: `promotionLogic/…/presentation/<feature>/XxxContract.kt`
  chứa `XxxState` (data class) + `XxxIntent` (sealed interface) + model hiển thị của màn
  (`MyPromotionVoucher`, `MyPromotionTab`…) + mapper `toXxx()`.

> 📌 Trước đây mỗi màn Android còn có thêm `XxxUiState` / `XxxAction` chép gần 1-1 `State`/`Intent`
> của store — **đã bỏ hết**. Fragment `dispatch` thẳng `Intent` và đọc thẳng `State`. Đừng dựng lại
> lớp trung gian đó; xem KDoc `PRMStoreViewModel`.

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
- `gradle.properties` (`SDK_VERSION` / `SDK_GROUP`) và `MARKETING_VERSION` trong `PRM.xcodeproj` —
  phải **giữ trùng số**. Xem [Distribution.md](../android/Distribution.md).
- `AndroidPromotionSDK/consumer-rules.pro` — bỏ một `-keep` là host chết `NoClassDefFoundError` ở
  bản minify, mà build debug vẫn xanh nên rất dễ lọt.
- `iosPromotionSDK/Frameworks/`, `**/build/`, `.spm/` — **sinh ra**, không commit, không sửa tay.

---

## 6. Repo nguồn (chỉ đọc)

Việc port đã xong: UI Android và lõi `core/` **đã nằm trong repo này**. Chỉ còn một repo ngoài giữ
vai trò tham chiếu:

| Repo | Vai trò |
|---|---|
| `~/IosProject/ttcn-promotion-ios-sdk` | SDK iOS gốc — đối chiếu khi port tiếp UI iOS / `findEligible` |

Không sửa repo đó từ project hiện tại.
