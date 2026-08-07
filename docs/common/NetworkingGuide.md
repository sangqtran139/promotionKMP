# NetworkingGuide — Quy tắc Networking

TTCN Promotion SDK gọi API bằng **Ktor Client + kotlinx.serialization**. Toàn bộ networking nằm ở
**Data layer** của `:promotionLogic`, `commonMain`.

> Bản Android cũ dùng Retrofit + OkHttp + Gson; bản iOS cũ dùng Alamofire + SwiftyJSON.
> Cả hai đều JVM/Apple-only. §7 ghi lại ánh xạ để đọc code cũ.

---

## 1. Engine theo nền tảng

`PromotionHttpClient.create(...)` dựng `HttpClient` **không chỉ định engine**. Ktor tự chọn theo
artifact có trên classpath:

| Source set | Artifact | Engine |
|---|---|---|
| `androidMain` | `ktor-client-okhttp` | OkHttp |
| `iosMain` | `ktor-client-darwin` | NSURLSession |

Vì thế `PromotionHttpClient` **không cần** `expect`/`actual`.

---

## 2. Cấu hình client

```kotlin
HttpClient {
    expectSuccess = true                       // 4xx/5xx → ném ResponseException

    install(ContentNegotiation) { json(json) }

    install(HttpTimeout) {                     // 30s cho cả ba
        connectTimeoutMillis = 30_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis  = 30_000
    }

    install(Logging) { level = if (isDebug) LogLevel.BODY else LogLevel.NONE }

    // Chỉ khi isDebug: in thêm mỗi request dạng lệnh cURL copy-paste được (PromotionCurlLogging).
    if (isDebug) install(PromotionCurlLogging)

    defaultRequest {
        url(baseUrl.ensureTrailingSlash())
        // Bearer token, X-Request-ID (UUID mới mỗi request), Accept-Language, Accept
    }
}
```

### Cấu hình `Json` — hai cờ bắt buộc

```kotlin
private val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false      // bỏ field null, giống Gson
    encodeDefaults = true      // ghi giá trị mặc định, giống Gson
}
```

**Đừng bỏ hai cờ này.** Mặc định kotlinx.serialization ghi `null` và **bỏ qua** giá trị mặc định.
Không có `encodeDefaults = true`, request `createRedemption` sẽ thiếu `sessionOptions` và
`timeoutSeconds` — server nhận payload khác hẳn bản Retrofit. Có test khoá hành vi này.

### Header (thay cho `ApiInterceptor` của OkHttp)

`defaultRequest` chạy **mỗi request**, nên `X-Request-ID` là UUID mới mỗi lần.
Dùng `appendIfNameAbsent` để không ghi đè header do caller tự đặt.

| Header | Nguồn | Ghi chú |
|---|---|---|
| `Authorization` | `requestContextProvider.getAccessToken()` | Tự thêm tiền tố `Bearer ` nếu chưa có |
| `X-Request-ID` | `randomUuidString()` | `kotlin.uuid.Uuid`, không phải `java.util.UUID` |
| `Accept-Language` | `getLanguage()` | Mặc định `vi-VN` |
| `Accept` | — | `application/json` |
| `Content-Type` | `contentType(...)` ở POST | `application/json` |

---

## 3. ApiService — viết tay, không sinh tự động

Retrofit sinh implementation của interface `@GET`/`@POST` lúc runtime bằng **dynamic proxy**.
Kotlin/Native không có cơ chế đó, nên `KtorPromotionApiService` được **viết tay**:

```kotlin
override suspend fun searchCustomerVouchers(
    keyword: String?, /* … */
): ApiResponseTemplate<SearchCustomerVouchersResponse> =
    client.get("$BASE_PATH/customer-vouchers") {
        parameter("keyword", keyword)          // null → tự bỏ qua, giống @Query
    }.body()
```

`BASE_PATH = "promotion/promotion-vtm-bff/api/v1/vtm"`.

---

## 4. Envelope và bóc dữ liệu

Mọi response bọc trong `ApiResponseTemplate<T>`:

```kotlin
@Serializable
data class ApiResponseTemplate<T>(
    val status: Int? = null,
    val code: String? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val timestamp: String? = null,
    val metadata: ResponseMetadata? = null,
    val data: T? = null,
)
```

`PromotionRemoteDataSource.requireData()` ném `PromotionException` khi `success == false` hoặc
`status` ngoài `200..299` — kể cả khi HTTP là 200.

---

## 5. Ánh xạ lỗi

`apiCall { }` chuẩn hoá mọi lỗi transport sang exception **domain**:

| Bắt được | Ném ra |
|---|---|
| `PromotionException` | giữ nguyên |
| `ResponseException` (4xx/5xx) | `PromotionException` — parse error body lấy `code`/`message` của server |
| `HttpRequestTimeoutException` / `ConnectTimeoutException` / `SocketTimeoutException` | `NetworkException(TIMEOUT)` |
| `IOException` khác | `NetworkException(NETWORK_ERROR)` |

**Thứ tự `catch` quan trọng:** cả ba loại timeout của Ktor đều là con của `IOException`, nên phải
bắt trước. Xem [ErrorHandling.md](./ErrorHandling.md).

`FeatureFlagRemoteDataSource` khác một điểm: mọi lỗi HTTP gộp về `FeatureFlagException` không mang
error code — cờ tính năng không hiển thị lỗi cho người dùng, chỉ rơi về cache/mặc định.

---

## 6. DTO

- Đặt ở `data/dto/<nhóm>/`: `voucher/`, `redemption/`, `stackablediscount/`, `eligible/`, `featureflag/`.
- Mỗi DTO đánh `@Serializable`, dùng `@SerialName("...")` (không phải `@SerializedName`).
- Mỗi nhóm có `XxxMapper.kt` với hàm `toXxx()` map DTO → domain model.
- **Domain không được thấy DTO.**

### Hai bẫy khi chuyển từ Gson

**`Any` không serialize được.** kotlinx.serialization cần kiểu tĩnh:

| Gson | kotlinx.serialization |
|---|---|
| `metadata: Map<String, Any>` | `metadata: JsonObject` |
| `alternativeStacks: List<Any>` | `alternativeStacks: List<JsonElement>` |

JSON trên dây không đổi; chỉ chữ ký Kotlin đổi.

**Field non-null thiếu trong response.** Gson gán `null` vào biến non-null bằng reflection (unsafe);
kotlinx.serialization ném `MissingFieldException`. Vì vậy các field như `createdAt`, `expiresAt`,
`canStack` được cho **giá trị mặc định**. Khi thêm DTO mới, cân nhắc đặt default cho field mà server
có thể bỏ trống.

---

## 7. Ánh xạ với code cũ

| Bản Android cũ | Bản iOS cũ | Bản KMP |
|---|---|---|
| Retrofit interface `@GET`/`@POST` | `VDSNetwork.get/post` (Alamofire) | `KtorPromotionApiService` (viết tay) |
| `RetrofitClient` | `ServiceUrl` + `VDSNetwork` | `PromotionHttpClient` |
| `ApiInterceptor` (OkHttp) | tham số `token:` từng request | `defaultRequest { }` |
| Gson `@SerializedName` | `Codable` + SwiftyJSON | kotlinx.serialization `@SerialName` |
| `HttpException` | `NSError` từ closure `failure` | `ResponseException` |
| `java.util.UUID` | `UUID()` | `kotlin.uuid.Uuid` |

---

## 8. Quy tắc

1. Thêm endpoint → sửa `PromotionApiService` (interface) **và** `KtorPromotionApiService` (impl),
   rồi thêm hàm ở `PromotionRemoteDataSource` bọc trong `apiCall { }`.
2. Không gọi `HttpClient` trực tiếp từ Repository/UseCase/UI.
3. Không bắt `Exception` chung trong data source — bắt đúng loại và map sang exception domain.
4. Không log token. `LogLevel.BODY` **và** `PromotionCurlLogging` (in lệnh cURL) chỉ bật khi `isDebug` —
   cả hai đều lộ `Authorization`, tuyệt đối không bật ở bản phát hành. Bật debug: Android build DEBUG;
   iOS chạy với biến môi trường `PROMOTION_SDK_DEBUG=1`.
5. Mọi endpoint mới phải có test `commonTest` dùng `MockEngine`, kiểm cả **payload gửi lên** lẫn
   **kết quả map xuống**. Xem [TestingGuide.md](./TestingGuide.md).
6. Đổi endpoint/DTO/xử lý lỗi → cập nhật file này + [HeadlessAPI.md](./HeadlessAPI.md).
