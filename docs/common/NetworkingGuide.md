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
| `Authorization` | `requestContextProvider.getAccessToken()` | Tự thêm tiền tố `Bearer ` nếu chưa có. **Đọc lại mỗi request** — xem *Token là pull* dưới đây |
| `X-Request-ID` | `randomUuidString()` | `kotlin.uuid.Uuid`, không phải `java.util.UUID` |
| `Accept-Language` | `getLanguage()` | Mặc định `vi-VN` |
| `Accept` | — | `application/json` |
| `Content-Type` | `contentType(...)` ở POST | `application/json` |

#### Token là **pull**, không phải push

`getAccessToken()` nằm trong `defaultRequest { }` nên nó được gọi lại ở **mỗi** request — SDK không
bao giờ giữ một bản sao token. Đây không phải chi tiết cài đặt tuỳ tiện mà là hợp đồng: token của
host sống ngắn (≈15 phút) và host **tự** refresh theo cơ chế riêng của nó. Nếu SDK cache token nhận
lúc `initialize`, thì ngay sau lần refresh đầu tiên của host, SDK cầm một chuỗi đã chết trong khi
phiên đăng nhập vẫn còn sống — mọi API trả 401 dù người dùng chưa hề đăng xuất.

Tầng UI mỗi nền tảng nối nguồn token của host vào đây qua `PromotionSessionConfig.tokenSource`
(`PromotionTokenSource` — `interface` ở Android, `protocol` ở iOS). Đó là **đường duy nhất**: không
có tham số `accessToken`, không có bản sao token nào trong SDK. Test khoá hành vi:
`TokenPullPerRequestTest`.

**Đừng "tối ưu" bằng cách cache token vào biến lúc dựng client** — đó chính là bug mà cơ chế này
sinh ra để chữa.

#### Thử lại khi 401

`apiCall` bắt **401** và chạy lại request **đúng một lần** sau khi xin được token mới từ host:

```
block()  ──401──▶  TokenRefreshGate.refresh()  ──true──▶  block()   (lần 2, token đã mới)
                            │
                            └──false──▶ ném tiếp → TOKEN_EXPIRED → onExpireToken()
```

- **401 tới theo hai đường** — `expectSuccess` ném `ResponseException` cho HTTP 4xx, còn envelope
  `{"status":401}` trên HTTP 200 thì `requireData()` ném `PromotionException(httpStatus = 401)`.
  `isUnauthorized()` phải nhận cả hai, y như `ErrorCodeExtensions` khi ra `TOKEN_EXPIRED`.
- **Một lần, không phải vòng lặp.** Refresh xong vẫn 401 = phiên chết thật.
- **Single-flight** — `TokenRefreshGate` đánh số `generation`, tăng sau mỗi lần refresh thành công.
  Nơi gọi chụp số **trước khi gửi**; lúc hỏng, số đã chụp khác số hiện tại nghĩa là có người vừa
  refresh xong → thử lại luôn, khỏi hỏi host lần nữa. Mở một màn là vài request song song, thiếu
  chốt này là host ăn vài lần refresh cho cùng một sự kiện.
- **Cổng trả `Boolean`, không trả token.** Token là *pull* — lượt thử lại tự đọc lại
  `getAccessToken()`. Host ghi token mới vào kho của mình **rồi** báo `true`; không có đường thứ hai
  để token đi vào SDK, nên không có chỗ nào để nhầm.
- **Hai lớp chắn cho code của host** — hết 15 giây mà host chưa gọi callback thì coi như hỏng (nếu
  không, `Mutex` treo và **mọi** API của SDK chết theo); callback gọi hai lần thì chỉ lần đầu tính.
- `FeatureFlagRemoteDataSource` **không** thử lại: cờ tính năng fail-open, 401 ở đó chỉ rơi về cache.

#### Thread — vì sao `apiCall` bọc `withContext(ioDispatcher)`

Ktor chạy pipeline **phía client** trong context của coroutine gọi nó; chỉ engine mới tự nhảy sang
thread nền. Mà store dùng chung nhận `scope` từ nền tảng và Android truyền thẳng `viewModelScope`
(`Dispatchers.Main.immediate`). Không có `withContext`, toàn bộ `defaultRequest { }` — kể cả lambda
cấp token của host — chạy trên **main thread** ở Android.

`PromotionRemoteDataSource.apiCall` và `FeatureFlagRemoteDataSource.apiCall` vì vậy đều bọc
`withContext(ioDispatcher)`. `ioDispatcher` là `expect`/`actual` ở `common/IoDispatcher.kt`
(`Dispatchers.IO` không tồn tại ở `commonMain`). Hệ quả cho host: `PromotionTokenSource.currentToken()`
được gọi từ thread nền, nên nó phải thread-safe và **không** được `@MainActor` ở Swift.

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
7. Hàm mới ở `PromotionRemoteDataSource` phải đi qua `apiCall { }` — nó vừa map lỗi, vừa thử lại khi
   401, vừa giữ lời hứa "không chạm main thread". Gọi thẳng `apiService` là mất cả ba.
