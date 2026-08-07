# DependencyInjection — Custom DI

TTCN Promotion SDK dùng **Custom DI tự viết** (phong cách Koin), **không** dùng Hilt/Koin/Dagger.
Lý do: SDK cần nhẹ, không kéo annotation processor hay DI framework vào host app — và trên KMP,
kapt/KSP sẽ làm vỡ target iOS.

Toàn bộ DI nằm ở `:promotionLogic`, `commonMain`.

---

## 1. Thành phần cốt lõi

| Thành phần | File | Vai trò |
|------------|------|---------|
| `SdkDi` | `di/internal/SdkDi.kt` | Container singleton + DSL (`module`, `single`, `factory`, `get`, `inject`) |
| `ComponentRegistry` | `di/internal/ComponentRegistry.kt` | Lưu provider, singleton, factory; resolve thread-safe |
| `DiKey` | `di/internal/DiKey.kt` | Khoá định danh `(KClass, qualifier)` |
| `SdkLock` | `common/SdkLock.kt` | Khoá **reentrant** expect/actual — xem §3 |
| `PromotionContainer` | `di/PromotionContainer.kt` | Public entry: khởi tạo container & nạp modules |
| Các `*Module` | `di/` | Khai báo cách tạo dependency theo nhóm |

---

## 2. DSL & cách hoạt động

```kotlin
internal val module = module {
    // single: tạo MỘT lần, cache lại (singleton trong scope SDK)
    single<HttpClient> {
        val config = get<PromotionSDKConfig>()
        PromotionHttpClient.create(config.baseUrl, get(), config.isDebug)
    }

    // factory: tạo MỚI mỗi lần resolve
    factory<SomeType> { SomeTypeImpl(get()) }
}
```

- `single { }` — provider gọi **một lần**, cache trong `singleInstances` (double-checked locking).
- `factory { }` — provider gọi **mỗi lần** `resolve`.
- `get<T>(qualifier?)` — resolve ngay.
- `inject<T>(qualifier?)` — resolve **lazy** (`by inject()`).
- Resolve kiểu chưa đăng ký → `IllegalStateException("Dependency not found: ...")`.

> `get/inject/single/factory` ở top-level là `internal inline` — chỉ dùng trong SDK.

---

## 3. Thread-safety trên KMP

`ConcurrentHashMap` và `synchronized` là **JVM-only**. Bản KMP thay bằng `MutableMap` được bảo vệ
bởi `SdkLock`:

```kotlin
internal expect class SdkLock() {
    fun lock()
    fun unlock()
}
```

| Nền tảng | Hiện thực |
|---|---|
| Android | `java.util.concurrent.locks.ReentrantLock` |
| iOS | `platform.Foundation.NSRecursiveLock` |

**Phải là khoá reentrant.** `resolve()` giữ khoá khi gọi provider, mà provider thường gọi `get()` →
`resolve()` đệ quy. Khoá không reentrant sẽ deadlock ngay lần init đầu tiên.

---

## 4. Vòng đời khởi tạo

```
Android:  PromotionContainer.init(context, config)     [androidMain]
             ├─ AndroidContextHolder.set(appContext)   // cho SharedPreferences
             ├─ isDebug ← ApplicationInfo.FLAG_DEBUGGABLE
             └─ init(config.copy(isDebug = ...))
                    │
iOS/common:  PromotionContainer.init(config)
                    └─ SdkDi.getInstance().start(config, modules…)
                         ├─ registry.single { config }      // đăng ký config TRƯỚC
                         └─ loadModules(
                              NetworkModule, LocalModule,
                              RepositoryModule, UseCaseModule, FeatureFlagModule)
```

`PromotionContainer.clear()`:
1. Đóng `HttpClient` **chỉ khi nó đã thực sự được dựng** (`SdkDi.hasInstance(HttpClient::class)`) —
   resolve thẳng sẽ tạo mới một client rồi đóng ngay, vô nghĩa.
2. Xoá registry, reset singleton `SdkDi`.
3. `clearPlatformState()` — nhả `applicationContext` trên Android, no-op trên iOS.

---

## 5. Tổ chức Module

| Module | Đăng ký gì |
|--------|------------|
| `NetworkModule` | `PromotionRequestContextProvider`, `HttpClient`, `PromotionApiService`, `PromotionRemoteDataSource` |
| `LocalModule` | `PromotionPreferences`, `FeatureFlagLocalDataSource` |
| `RepositoryModule` | `PromotionRepository` → `PromotionRepositoryImpl` |
| `UseCaseModule` | 5 use case nghiệp vụ + `PromotionUseCases` |
| `FeatureFlagModule` | `FeatureFlagApiService`, `FeatureFlagRemoteDataSource`, `FeatureFlagRepository`, 4 use case, `PromotionFeatureFlagUseCases` |

Bind interface → impl: `single<PromotionRepository> { PromotionRepositoryImpl(get()) }`.

> `LocalModule` phải được nạp trước `FeatureFlagModule` trong thứ tự đọc, nhưng vì binding là lazy
> nên thứ tự `loadModules(...)` không ảnh hưởng — chỉ `config` cần đăng ký trước.

---

## 6. Quy tắc sử dụng DI

1. **Khai báo binding ở đúng module theo layer.** Network → `NetworkModule`, storage → `LocalModule`,
   repo → `RepositoryModule`, use case → `UseCaseModule`. Không trộn lẫn.
2. **Bind interface, không bind impl** cho repository/data source.
3. **`single`** cho thành phần stateless dùng lại (ApiService, Repository, DataSource, UseCase).
   **`factory`** khi cần instance mới mỗi lần.
4. **Resolve nội bộ qua `get()`/`inject()`**; không tự `new` class đã có binding.
5. **Không thêm DI framework mới** (AI_AGENT_RULES điều 6).
6. Khi thay đổi binding/module/cách hoạt động DI → **cập nhật file này** (điều 8).

---

## 7. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
|-------------|-------------|-----------|
| `Dependency not found: Xxx` | Chưa đăng ký binding, sai qualifier, hoặc module chưa nạp | Thêm binding vào module đúng; đảm bảo module nằm trong `start(...)` |
| Resolve trước khi `init()` | Gọi `PromotionUseCases()` trước `init()` | Luôn `init()` trước |
| `Promotion SDK chưa có Context` | Trên Android gọi `init(config)` thay vì `init(context, config)` | Dùng overload có `Context` |
| State cũ sau re-init | Container chưa `clear()` | Gọi `clear()` khi teardown |
| Deadlock lúc init | Đổi `SdkLock` sang khoá không reentrant | Giữ `ReentrantLock`/`NSRecursiveLock` (§3) |
