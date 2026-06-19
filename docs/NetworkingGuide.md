# NetworkingGuide — Quy tắc Networking

TTCN Promotion SDK gọi API bằng **Retrofit + OkHttp + Gson**. Toàn bộ networking nằm ở **Data layer**
(`core/data/remote/`) và được cấu hình qua **Custom DI** (`core/di/NetworkModule.kt`).

---

## 1. Stack & thành phần

| Thành phần | File | Vai trò |
|------------|------|---------|
| Retrofit | `core/data/remote/RetrofitClient.kt` | Khởi tạo `Retrofit` (singleton, lazy, thread-safe) |
| OkHttp | trong `RetrofitClient` | Client + timeout + interceptor |
| ApiService | `core/data/remote/PromotionApiService.kt`, `FeatureFlagApiService.kt` | Khai báo endpoint (Retrofit interface) |
| RemoteDataSource | `core/data/remote/PromotionRemoteDataSource.kt` | Gọi ApiService, là ranh giới giữa Data ↔ network |
| Interceptor | `core/data/remote/ApiInterceptor.kt` | Gắn header/context dùng chung cho mọi request |
| Exception | `core/domain/exception/PromotionException.kt` | Lỗi API (domain) — `errorCode`/`status`, do RemoteDataSource ném |
| Converter | Gson (`converter-gson`) | Serialize/deserialize JSON |

---

## 2. Cấu hình client (RetrofitClient)

```kotlin
OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(apiInterceptor)   // header/context
    .addInterceptor(logging)          // log
    .build()
```

Quy tắc:
- **Timeout mặc định 30s** (connect & read). Giữ nhất quán, chỉ chỉnh khi có lý do nghiệp vụ.
- `baseUrl` lấy từ `PromotionSDKConfig.baseUrl`, tự đảm bảo có dấu `/` cuối (`ensureTrailingSlash`).
- Retrofit là **singleton lazy** (`@Volatile` + double-checked). Không tạo nhiều instance Retrofit.
- `GsonConverterFactory` là converter chuẩn. Không trộn converter khác trừ khi được yêu cầu.

---

## 3. Logging

- Mức log HTTP phụ thuộc cờ debuggable của host app:
  `HttpLoggingInterceptor.Level.BODY` khi debug, `Level.NONE` khi release (xem `NetworkModule`).
- **Không** bật log BODY ở release; tránh lộ token/PII trong logcat.

---

## 4. Header & request context

- Header/context dùng chung (vd token, thông tin phiên) gắn tại `ApiInterceptor` qua
  `PromotionRequestContextProvider` (lấy từ `PromotionSDKConfig.requestContextProvider`,
  fallback `EmptyPromotionRequestContextProvider`).
- **Không** tự gắn header riêng lẻ trong từng `@GET/@POST` nếu đó là header dùng chung — thêm vào interceptor/provider.

---

## 5. Khai báo endpoint (ApiService)

- Mọi endpoint khai báo trong `PromotionApiService` (hoặc `FeatureFlagApiService`) là **interface Retrofit**.
- Hàm endpoint là `suspend`, trả về **DTO** (`*Response`), không trả domain model.
- Request body dùng DTO `*Request`. Dùng `@Query`/`@Path`/`@Body` đúng ngữ nghĩa.
- Endpoint mới phải đi kèm hàm tương ứng trong `PromotionRemoteDataSource`.

---

## 6. DTO & mapping

- DTO đặt trong `core/data/dto/<nhóm>/`, đặt tên `XxxRequest` / `XxxResponse`.
- Field map JSON dùng `@SerializedName` khi tên JSON khác tên Kotlin.
- **Mapping DTO → domain model** thực hiện ở Data layer (hàm `toXxx()`), trước khi trả lên Domain.
  Ví dụ: `toSearchCustomerVouchersResult()`, `toVoucherDetail()` trong repository.
- **Không** để DTO rò rỉ lên Domain/Presentation.

---

## 7. Luồng gọi API chuẩn

```
ViewModel
  → UseCase (suspend)
    → Repository (interface, domain)
      → RepositoryImpl (data)
        → RemoteDataSource
          → ApiService (Retrofit)  → JSON
        ← Response DTO
      ← map toDomainModel()
    ← domain model
  ← domain model → setState / sendEffect
```

- ViewModel **không** gọi thẳng `RemoteDataSource`/`ApiService` (phải qua use case → repository).
- Coroutine chạy trong `launch { }` của `PRMBaseViewModel` (đã có exception handler).

---

## 8. Xử lý lỗi network

- Lỗi tầng API được `PromotionRemoteDataSource` ném dưới dạng exception domain ở `core/domain/exception/`
  (`PromotionException` mang `errorCode`/`status`, `NetworkException`) với mã trong `ErrorCodes`.
- Repository/UseCase chuyển lỗi network thành exception/giá trị domain rõ ràng (không nuốt lỗi âm thầm).
- UI nhận lỗi qua `Effect` (vd `ShowError(errorCode)`) và tra cứu thông điệp hiển thị.
- Chi tiết phân loại & hiển thị: xem `ErrorHandling.md`.

---

## 9. Khi thay đổi API — bắt buộc

Theo AI_AGENT_RULES điều 7, mọi thay đổi endpoint/DTO/header/timeout phải:
- Cập nhật `PromotionApiService` + `RemoteDataSource` + DTO + mapping.
- **Cập nhật file này** nếu thay đổi quy tắc chung (timeout, interceptor, converter, base URL).
- Không thêm thư viện networking mới (Ktor, Volley…) — đã chuẩn hoá Retrofit/OkHttp (điều 4).
