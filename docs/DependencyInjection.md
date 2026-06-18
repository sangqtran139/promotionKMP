# DependencyInjection — Custom DI

TTCN Promotion SDK dùng **Custom DI tự viết** (phong cách Koin), **không** dùng Hilt/Koin/Dagger.
Lý do: SDK cần nhẹ, không kéo theo annotation processor/DI framework vào host app.

---

## 1. Thành phần cốt lõi

| Thành phần | File | Vai trò |
|------------|------|---------|
| `SdkDi` | `core/di/SdkDi.kt` | Container chính (singleton) + DSL (`module`, `single`, `factory`, `get`, `inject`) |
| `ComponentRegistry` | `core/di/internal/ComponentRegistry.kt` | Lưu provider, single instance, factory; resolve thread-safe |
| `DiKey` | `core/di/internal/DiKey.kt` | Khoá định danh `(KClass, qualifier)` |
| `PromotionContainer` | `core/di/PromotionContainer.kt` | Khởi tạo container & nạp modules khi SDK init |
| Các `*Module` | `core/di/`, `core/domain/di/`, `ui/di/` | Khai báo cách tạo dependency theo nhóm |

---

## 2. DSL & cách hoạt động

```kotlin
internal val module = module {
    // single: tạo MỘT lần, cache lại (singleton trong scope SDK)
    single<PromotionApiService> {
        RetrofitClient.promotionApiService(
            baseUrl = get<PromotionSDKConfig>().baseUrl,
            apiInterceptor = get(),
            isDebug = isDebug,
        )
    }

    // factory: tạo MỚI mỗi lần resolve
    factory<SomeType> { SomeTypeImpl(get()) }
}
```

- `single { }` — provider được gọi **một lần**, kết quả cache trong `singleInstances` (double-checked lock).
- `factory { }` — provider được gọi **mỗi lần** `resolve` (không cache).
- `get<T>(qualifier?)` — resolve dependency ngay lập tức.
- `inject<T>(qualifier?)` — resolve **lazy** (`by inject()`).
- `qualifier: String?` — phân biệt nhiều binding cùng kiểu (qua `named(...)`).
- Nếu resolve kiểu chưa đăng ký → ném `IllegalStateException("Dependency not found: ...")`.

> Các helper `get/inject/single/factory` ở top-level là `internal inline` — chỉ dùng trong SDK.

---

## 3. Vòng đời khởi tạo

```
PromotionSDK.init(context, options)
   └─ PromotionContainer.init(context, config)
        └─ SdkDi.getInstance().start(context, config, NetworkModule.module, RepositoryModule.module, UseCaseModule.module, ...)
             ├─ single { context.applicationContext }   // Context an toàn
             ├─ single { config }                        // PromotionSDKConfig
             └─ loadModules(...)                         // invoke từng module để đăng ký binding
   └─ ensureUiDiLoaded()  → nạp ViewModelModule (UI DI) một lần
```

- `start()` đăng ký `applicationContext` và `config` trước, rồi nạp modules.
- UI DI (`ui/di/ViewModelModule`) được nạp riêng, **một lần** (`isUiDiLoaded` volatile).
- `SdkDi` là singleton (`@Volatile instance`); `clear()` xoá registry và reset instance (dùng khi teardown).

---

## 4. Tổ chức Module (theo layer)

| Module | Đăng ký gì |
|--------|------------|
| `NetworkModule` | `ApiInterceptor`, `PromotionApiService`, `PromotionRemoteDataSource`, `RequestContextProvider` |
| `RepositoryModule` | `PromotionRepository` → `PromotionRepositoryImpl`, `FeatureFlagRepository` → impl |
| `UseCaseModule` (`core/domain/di`) | Các use case + `PromotionUseCases` |
| `FeatureFlagModule` | Phụ thuộc cho feature flag |
| `ViewModelModule` (`ui/di`) | Binding tạo ViewModel; dùng cùng `PromotionViewModelFactory` |

Bind interface → impl: `single<PromotionRepository> { PromotionRepositoryImpl(get()) }`.

---

## 5. Quy tắc sử dụng DI

1. **Khai báo binding ở đúng module theo layer** (network → `NetworkModule`, repo → `RepositoryModule`, use case → `UseCaseModule`, viewmodel → `ViewModelModule`). Không trộn lẫn.
2. **Bind interface, không bind impl trực tiếp** cho repository/data source (Presentation/Domain phụ thuộc abstraction).
3. **`single` cho thành phần stateless dùng lại** (ApiService, Repository, DataSource, UseCase). **`factory`** khi cần instance mới mỗi lần.
4. **Resolve nội bộ qua `get()`/`inject()`**; không tự `new` các class đã có binding.
5. **ViewModel** lấy qua `PromotionViewModelFactory`/`ViewModelModule`, không khởi tạo thủ công trong Fragment.
6. **Dùng `applicationContext`** (đã đăng ký) thay vì giữ Activity context để tránh leak.
7. **Thêm dependency mới vào DI** = thêm binding trong module phù hợp; **không** thêm DI framework mới (AI_AGENT_RULES điều 4).
8. Khi thay đổi binding/module/cách hoạt động DI → **cập nhật file này** (AI_AGENT_RULES điều 7).

---

## 6. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|-----------|
| `Dependency not found: Xxx` | Chưa đăng ký binding, sai qualifier, hoặc module chưa được nạp | Thêm binding vào module đúng; đảm bảo module nằm trong danh sách `start(...)`/`loadModules(...)` |
| Resolve trước khi `init()` | Gọi `PromotionSDK.useCases`/DI trước `PromotionSDK.init()` | Luôn `init()` trước |
| State cũ sau re-init | Container chưa `clear()` | Gọi `clear()` khi teardown nếu cần dựng lại |
