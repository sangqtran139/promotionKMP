# ProjectStructure — Cấu trúc project

Mô tả cấu trúc thư mục và vai trò từng package để biết **file nằm ở đâu** và **đặt file mới vào đâu cho đúng**.

---

## 1. Cấu trúc cấp cao (multi-module)

```
ttcn-promotion-android-sdk/
├── settings.gradle.kts          # include :vds-promotion, :app
├── build.gradle.kts             # cấu hình root
├── gradle/libs.versions.toml    # Version catalog (nguồn duy nhất của dependency)
├── docs/                        # Tài liệu nền tảng (file này)
├── vds-promotion/               # 📦 Module SDK chính (com.android.library)
└── app/                         # 🧪 Module demo tích hợp (com.android.application)
```

- **`vds-promotion`** — sản phẩm thật, namespace `com.ttcn.promotionsdk`. Mọi thay đổi nghiệp vụ ở đây.
- **`app`** — chỉ để demo/test tích hợp (mock API, theme preview, headless demo). **Không** đặt logic SDK ở đây.

---

## 2. Cấu trúc module `vds-promotion`

```
vds-promotion/src/main/
├── java/com/ttcn/promotionsdk/
│   ├── core/                         # Tầng lõi (không phụ thuộc UI feature)
│   │   ├── config/                   # PromotionSDKConfig, RequestContextProvider
│   │   ├── di/                       # Custom DI
│   │   │   ├── SdkDi.kt              # Container + DSL (module/single/factory/get/inject)
│   │   │   ├── PromotionContainer.kt # Khởi tạo & load modules
│   │   │   ├── NetworkModule.kt      # Đăng ký Retrofit/DataSource
│   │   │   ├── RepositoryModule.kt   # Đăng ký Repository
│   │   │   ├── FeatureFlagModule.kt
│   │   │   └── internal/             # ComponentRegistry, DiKey
│   │   ├── data/                     # DATA layer
│   │   │   ├── dto/                  # DTO + mapping toXxx() (voucher, redemption, stackablediscount)
│   │   │   ├── remote/               # ApiService, RemoteDataSource, RetrofitClient, ApiInterceptor
│   │   │   ├── local/                # PromotionDatabase, Dao, SharedPrefStorage
│   │   │   └── repository/           # RepositoryImpl
│   │   ├── domain/                   # DOMAIN layer
│   │   │   ├── model/                # Domain model
│   │   │   ├── repository/           # Repository interface
│   │   │   ├── usecase/              # UseCase + PromotionUseCases
│   │   │   ├── di/                   # UseCaseModule
│   │   │   └── exception/            # NetworkException, PromotionException, ErrorCodes
│   │   └── utils/                    # Tiện ích lõi
│   └── ui/                           # PRESENTATION layer
│       ├── base/                     # PRMBaseActivity / PRMBaseFragment / PRMBaseViewModel
│       ├── entry/                    # PUBLIC API: PromotionSDK, Options, Config, Callback, Theme
│       ├── di/                       # ViewModelModule, PromotionViewModelFactory
│       ├── feature/                  # Từng màn hình
│       │   ├── promotion/
│       │   │   ├── mypromotion/      # Fragment + ViewModel + Contract + adapter
│       │   │   ├── choosepromotion/
│       │   │   ├── promotiondetail/
│       │   │   ├── searchmypromotion/
│       │   │   ├── endowview/
│       │   │   └── ext/
│       │   └── featureflag/
│       ├── theme/                    # PromotionSDKTheme, registry
│       └── utils/                    # extension/, enum/, view/ (custom view)
└── res/                              # layout/, drawable*, values/, font/, animator/, xml/
```

---

## 3. Quy ước đặt file mới

| Loại file | Đặt ở đâu |
|-----------|-----------|
| Màn hình mới | `ui/feature/<tên_feature>/` gồm `XxxFragment`, `XxxViewModel`, `XxxContract` (State/Action/Effect), `adapter/` nếu cần — và thêm tài liệu vào [`docs/features/`](./features/README.md) |
| Use case mới | `core/domain/usecase/`, đăng ký trong `core/domain/di/UseCaseModule.kt` |
| Repository mới | interface ở `core/domain/repository/`, impl ở `core/data/repository/`, đăng ký ở `core/di/RepositoryModule.kt` |
| API endpoint mới | thêm vào `core/data/remote/PromotionApiService.kt` + `PromotionRemoteDataSource` |
| DTO mới | `core/data/dto/<nhóm>/` kèm hàm mapping `toDomainModel()` |
| Domain model mới | `core/domain/model/` |
| Custom view / extension dùng chung | `ui/utils/view/` hoặc `ui/utils/extension/` |
| Public API thay đổi | `ui/entry/` — **thận trọng**, cập nhật docs + ghi breaking change |

---

## 4. Quy ước đặt tên file (xem chi tiết `CodingStandards.md`)

- Base class & nhiều public class dùng tiền tố **`PRM`** (`PRMBaseFragment`, `PRMEditText`…).
- Feature contract tách thành: `XxxUiState` / `XxxAction` (hoặc `XxxUIAction`) / `XxxEffect`, có thể gộp trong `XxxContract.kt`.
- DTO kết thúc bằng `Request` / `Response`. Domain model dùng tên nghiệp vụ (`VoucherDetail`).
- Module DI kết thúc bằng `Module` (`NetworkModule`, `RepositoryModule`).

---

## 5. Nơi KHÔNG nên chạm nếu không cần

- `core/di/internal/` — cơ chế DI lõi; chỉ sửa khi thay đổi cách hoạt động DI (và cập nhật `DependencyInjection.md`).
- `ui/entry/` — public API; thay đổi = breaking cho host app.
- `gradle/libs.versions.toml` — chỉ thêm dependency khi được yêu cầu (AI_AGENT_RULES điều 4).
