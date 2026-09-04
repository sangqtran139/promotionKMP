# SharedNetworkKit — Module network KMP dùng chung (`:networkKit`)

> Trạng thái: **Phase 1, UC9 xong** — `HttpClient` per-instance + header tĩnh/động + token provider +
> request builder an toàn + giải mã JSON generic + `NetworkError` cho lỗi transport +
> `StatusCodeHandlerChain` cho lỗi nghiệp vụ + `NetworkInterceptor` cho logic trong lúc request→response
> + debug logging tường minh + `NetworkClient` facade ráp toàn bộ UC1–UC8. File này là tài liệu sống,
> cập nhật sau mỗi UC (xem AI_AGENT_RULES điều 8).

---

## 1. Vì sao module này tồn tại

`ttcn-promotion-android-sdk` (`:promotionLogic`) **đã có** một networking layer KMP hoạt động tốt —
Ktor + kotlinx.serialization, xem [NetworkingGuide.md](./NetworkingGuide.md). Module này **không** thay
thế nó ngay; mục tiêu là rút phần *cơ chế* dùng chung ra một module riêng, để `:promotionLogic` (và
các SDK KMP khác sau này) tiêu thụ thay vì mỗi nơi tự viết lại.

Bối cảnh: workspace `VDS-SDK-Development` (repo khác — `network-kit-android`) đang có sẵn một thư
viện network dùng chung thuần Android (OkHttp + Gson) mà `ekyc-service` và `sdk-miniapp` cùng dùng.
Soi vào cách hai module đó dùng nó, thấy rõ ba vấn đề đáng sửa khi làm bản KMP:

1. **Không port thẳng được.** Toàn bộ tầng giải mã JSON của `network-kit-android` dựa vào Gson +
   `Class<S>`/`reflect.Type` — phản chiếu (reflection) thuần JVM. Kotlin/Native không có
   `java.lang.reflect`, nên đây là rào cản kỹ thuật cứng, không phải chỉ đổi cú pháp.
2. **`object VDONetworkSDK`** là singleton toàn cục giữ header/interceptor/token/status-handler dùng
   chung cho *toàn bộ process*. Cụ thể: `VDONetworkService.Builder.statusCodeHandler` không copy danh
   sách mà giữ **cùng tham chiếu** tới list toàn cục — handler đăng ký cho service của một feature sẽ
   lặng lẽ áp dụng cho mọi `VDONetworkService` khác cùng process. `ekyc-service` và `sdk-miniapp` chạy
   song song trong cùng host hôm nay; Promotion sẽ là SDK thứ ba.
3. **Trùng lặp do không có chỗ dùng chung.** `EkycStatusCodeHandler` và `ApiStatusHandler` (miniapp)
   gần như copy-paste nguyên khối `handleStatusCode()`, chỉ lệch đúng một dòng (`body = null` ở ekyc
   vs `body = response as? T` ở miniapp) — một bug lệch hành vi do không có nơi sửa một lần.
   `ekyc-service` còn chạy song song **hai** stack network (Retrofit riêng + `VDONetworkService`)
   trong cùng module.

`:networkKit` giải quyết đúng ba điểm trên bằng KMP: cơ chế chạy được cả Android/iOS (Ktor +
kotlinx.serialization), cấu hình **per-instance** (không singleton), và một chỗ duy nhất cho logic
"business status code → lỗi" để các consumer không phải chép tay.

**Thế hệ trước cả `network-kit-android`** — module `core/network` gốc của app Viettel Money
(`viettelpay4-android-demo`) — cho thêm bằng chứng định hình UC7: `CoroutineCall`/`BaseCallAdapter`
so khớp `body.status.code` thành **3 loại kết quả riêng ở cấp kiểu dữ liệu** (`Success`/
`Unauthenticated`/`Error`, sealed class `NetworkResponse`), còn `RequestInterceptor` (OkHttp) chỉ làm
đúng 3 việc ở tầng HTTP thô, **chưa bao giờ đọc JSON body**: gắn header, retry mù 3 lần trên bất kỳ
status không thành công (anti-pattern, không copy), và log analytics. Không có refresh-rồi-tự-động-retry
nào trong tầng network cả hai đời — `Unauthenticated` mặc định chỉ hiện dialog, việc refresh (nếu có)
luôn nằm ở tầng cao hơn (ViewModel/Controller thời đó; `TokenRefreshGate` ở `:promotionLogic` hôm nay).
Bằng chứng này xác nhận: xử lý business status (đọc body) và can thiệp transport (chưa đọc body) luôn
là hai mối quan tâm tách biệt qua cả ba thế hệ code — UC7 giữ nguyên ranh giới đó bằng hai cơ chế riêng
(`StatusCodeHandlerChain` và `NetworkInterceptor`), xem chi tiết ở §"UC7 — quyết định thiết kế".

---

## 2. Ranh giới: cơ chế, không phải chính sách

Module là tầng **common**, không phụ thuộc bất kỳ domain feature nào (Promotion, eKYC, miniapp…).

| Thuộc về `:networkKit` | Thuộc về consumer (`:promotionLogic`, SDK KMP khác…) |
|---|---|
| Cách dựng `HttpClient` theo baseUrl/timeout, per-instance | Base URL thật, giá trị header nghiệp vụ (Product, Channel…) |
| Cách gắn header tĩnh/động, token provider | Envelope response cụ thể (`ApiResponseTemplate` của Promotion khác `VDOBaseResponse` của ekyc) |
| Cách chạy chuỗi "business status code → lỗi" (sau khi decode) | Danh sách code nào là lỗi, message hiển thị gì, có refresh/retry hay không |
| Cách phân loại lỗi transport (timeout/IO/HTTP/serialization) | Map lỗi transport → exception domain riêng (`PromotionException`…) |
| Cơ chế chain-of-responsibility cho interceptor (trước khi đọc body) | Interceptor cụ thể làm gì (retry theo điều kiện, log, đo thời gian…) |

`:networkKit` **không** import bất cứ thứ gì từ `:promotionLogic` hay ngược lại theo hướng phụ thuộc
domain — chiều phụ thuộc luôn là `:promotionLogic` → `:networkKit`, không bao giờ đảo ngược.

---

## 3. Trạng thái các use case

| UC | Nội dung | Trạng thái |
|----|----------|------------|
| UC0 | Khung module rỗng — commonMain/androidMain/iosMain, publish mavenLocal | ✅ Xong — xanh cả Android host test lẫn iOS simulator test, publish mavenLocal xác nhận |
| UC1 | `HttpClient` factory per-instance (baseUrl, timeout) | ✅ Xong — `NetworkKitHttpClient.create()`, xanh cả Android host test lẫn iOS simulator test (4 test, kể cả timeout live-fire qua `MockEngine`) |
| UC2 | Header injection (tĩnh + động) | ✅ Xong — `NetworkClientConfig.headers`/`dynamicHeaders`, xanh cả hai nền tảng (7 test, kể cả "không ghi đè header caller tự đặt") |
| UC3 | Token provider (auth) | ✅ Xong — `TokenProvider` + Bearer, xanh cả hai nền tảng (11 test) |
| UC4 | Request builder GET/POST an toàn (auto-encode query) | ✅ Xong — `getRequest`/`postRequest`, xanh cả hai nền tảng (3 test, round-trip path/query có ký tự đặc biệt) |
| UC5 | Giải mã response generic bằng kotlinx.serialization | ✅ Xong — `getJson`/`postJson` + `ContentNegotiation`, xanh cả hai nền tảng (7 test: field bắt buộc thiếu → lỗi, field có default thiếu → dùng default, field có default nhưng server trả → dùng giá trị server, field nullable không default thiếu → null) |
| UC6 | Sealed error cho lỗi transport | ✅ Xong — `NetworkError` + `networkCall {}`, xanh cả hai nền tảng (4 test: timeout, mất mạng, HTTP lỗi, JSON lệch) |
| UC7 | Business status-code handler + Interceptor | ✅ Xong — `StatusCodeHandlerChain` (action-chain, sau khi decode) + `NetworkInterceptor` (trong lúc request→response, qua Ktor `HttpSend`), xanh cả hai nền tảng (12 test) |
| UC8 | Debug logging tường minh (cờ, không khoá theo enum môi trường) | ✅ Xong — `isDebug` + Ktor `Logging` + `NetworkKitCurlLogging`, xanh cả hai nền tảng (8 test) |
| UC9 | Lắp ráp `NetworkClient` facade | ✅ Xong — `NetworkClient` luôn bọc `networkCall {}`, xanh cả hai nền tảng (3 test lắp ráp end-to-end: thành công/lỗi nghiệp vụ/lỗi transport) |
| UC10 | `:promotionLogic` tiêu thụ thử — thay `PromotionHttpClient.kt` | 🔜 |

Ngoài phạm vi Phase 1: bộ tải file (`file_loader` của `network-kit-android`), mã hoá RSA cho
`X-SESSION-ID`, retry-policy phức tạp mặc định, migrate `ekyc-service`/`sdk-miniapp` sang dùng module
này ngay, và **hỗ trợ caller Java gọi thẳng `:networkKit`** (`getJson`/`postJson` chỉ Kotlin gọi được
— xem giới hạn ở §4). Nếu phase sau có module Android thuần Java cần dùng trực tiếp (ngoài nhánh KMP),
đó là việc của phase đó: cần một lớp adapter Kotlin mỏng expose hàm nhận `Class<T>` thay vì `reified`,
không phải sửa API hiện có của `:networkKit`.

---

## 4. Cấu trúc module

```
networkKit/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/vn/viettelpay/networkkit/
    │   ├── NetworkClientConfig.kt      # baseUrl + timeoutMillis + headers + dynamicHeaders + tokenProvider
    │   ├── DynamicHeader.kt            # header tính lại giá trị mỗi request (vd X-Request-ID)
    │   ├── TokenProvider.kt            # fun interface — provideToken(): String?
    │   ├── NetworkKitHttpClient.kt     # create(config): HttpClient — factory per-instance + ContentNegotiation, expectSuccess=true
    │   ├── NetworkKitRequests.kt       # getRequest/postRequest/getJson/postJson
    │   ├── NetworkError.kt            # sealed class: Timeout/NoConnection/Http/Serialization/Unknown
    │   ├── NetworkKitErrors.kt         # networkCall {} — bọc & phân loại lỗi transport
    │   ├── BusinessStatus.kt           # interface BusinessStatus + BusinessError
    │   ├── StatusCodeHandler.kt        # StatusCodeHandler (action-chain)/StatusCodeHandlerChain + checkBusinessStatus
    │   ├── NetworkInterceptor.kt       # NetworkInterceptor (fun interface) + InterceptorChain — model OkHttp Chain.proceed()
    │   ├── NetworkKitCurlLogging.kt    # buildCurlCommand() + plugin in cURL, chỉ cài khi isDebug
    │   └── NetworkClient.kt            # facade — getJson/postJson luôn bọc networkCall {}
    └── commonTest/kotlin/vn/viettelpay/networkkit/
        ├── NetworkKitHttpClientTest.kt # MockEngine: URL, timeout, header, token — không đè giá trị caller
        ├── NetworkKitRequestsTest.kt   # MockEngine: path/query round-trip với ký tự đặc biệt
        ├── NetworkKitJsonTest.kt       # MockEngine: giải mã JSON — unknown field, số↔string, thiếu field
        ├── NetworkKitErrorTest.kt      # MockEngine: timeout/IOException/HTTP lỗi/JSON lệch → đúng nhánh NetworkError
        ├── NetworkKitStatusCodeTest.kt # hai envelope khác hình dạng dùng chung handler, onMatch throw/return
        ├── NetworkKitInterceptorTest.kt # MockEngine: pass-through, retry theo tín hiệu riêng, đổi header lúc retry, thứ tự nhiều interceptor
        ├── NetworkKitCurlLoggingTest.kt # buildCurlCommand đủ nhánh (port từ PromotionCurlLoggingTest) + plugin cài/không cài theo isDebug
        └── NetworkClientTest.kt        # lắp ráp end-to-end: header+token+interceptor+decode+status cùng chạy, lỗi nghiệp vụ, lỗi transport không lọt raw exception
```

Vẫn chưa có `androidMain`/`iosMain` **file** nào — chỉ có khai báo dependency riêng từng nền tảng
trong `build.gradle.kts` (`ktor-client-okhttp` cho androidMain, `ktor-client-darwin` cho iosMain).
Ktor tự chọn engine theo artifact có trên classpath, không cần `expect`/`actual` (xem
[ProjectStructure.md](./ProjectStructure.md) — nguyên tắc "chỉ ba chỗ cần biết nền tảng" của
`:promotionLogic` áp dụng tương tự ở đây: engine không phải một trong ba chỗ đó).

### UC1 — quyết định thiết kế

- **`NetworkClientConfig` là toàn bộ input** — không có field nào đọc từ biến toàn cục. Hai
  `NetworkClientConfig` khác nhau (hai host khác nhau) tạo ra hai `HttpClient` độc lập tuyệt đối,
  khác hẳn `object VDONetworkSDK` của `network-kit-android` (xem phát hiện #2 ở §1).
- **`configure()` tách khỏi `create()`** để test dựng cùng cấu hình trên `MockEngine` — đúng kỹ thuật
  `PromotionHttpClient` của `:promotionLogic` đã dùng.
- **`ensureTrailingSlash()`**: Ktor nối path tương đối vào URL nền theo luật URL chuẩn (RFC 3986) —
  thiếu `/` cuối thì segment cuối của `baseUrl` bị **thay thế** thay vì được nối tiếp
  (`"https://a.com/base"` + `"sub"` → `.../sub`, mất `base`). Test
  `baseUrlWithoutTrailingSlashStillJoinsPathCorrectly` khoá đúng hành vi này.
- **Timeout là một con số duy nhất** (`timeoutMillis`, áp cho cả connect/request/socket) thay vì ba
  tham số riêng như `VDONetworkService.Builder.setTimeout(connect, read, write)` — chưa ai cần tách
  riêng, tách sau nếu có nhu cầu thật (YAGNI). Test `requestTimesOutWhenServerIsSlowerThanConfiguredTimeout`
  xác nhận `HttpTimeout` plugin thật sự bắn `HttpRequestTimeoutException`, không chỉ kiểm tra config
  được set — dùng `MockEngine` trễ (`delay`) dài hơn timeout, chạy dưới `runTest` (thời gian ảo).

### UC2 — quyết định thiết kế

- **Header tĩnh (`headers: Map<String, String>`) và header động (`dynamicHeaders: List<DynamicHeader>`)
  tách riêng** — tĩnh thì cùng giá trị suốt vòng đời client (vd `Product`, `Channel`), động thì phải
  tính lại mỗi request (vd `X-Request-ID`). Gộp chung một chỗ sẽ buộc caller giả vờ "tĩnh" bằng cách
  tự sinh giá trị trước rồi nhét vào Map — sai bản chất của `X-Request-ID`.
- **`DynamicHeader` là `class` giữ lambda (`provideValue: () -> String`), không phải `abstract class`
  bắt subclass** như `VDOFreshHeader` cũ — cùng chức năng, ít boilerplate hơn cho caller (không phải
  tạo object riêng cho mỗi header động).
- **Cả hai loại header đều đi qua `appendIfNameAbsent`, gắn trong `defaultRequest { }`** — chạy lại mỗi
  request (đúng lý do header động cần) nhưng **không ghi đè header caller tự đặt ở lời gọi cụ thể**.
  Test `callerHeaderIsNotOverriddenByStaticConfigHeader` xác nhận đúng thứ tự: `defaultRequest` chạy
  sau khi builder của caller đã set header, nên `appendIfNameAbsent` thấy header đã có và bỏ qua —
  đúng quy tắc `PromotionHttpClient` ghi ở [NetworkingGuide.md §2](./NetworkingGuide.md).
- **Chưa có** validate trùng tên giữa `headers` và `dynamicHeaders`, hay giới hạn số lượng — chưa ai
  cần, thêm khi có nhu cầu thật.

### UC3 — quyết định thiết kế

- **`TokenProvider` là `fun interface` đồng bộ** (`provideToken(): String?`) — khớp đúng
  `PromotionRequestContextProvider.getAccessToken()` hiện có của `:promotionLogic`, **không** kèm
  refresh-token flow (host gọi lại khi 401 là chuyện `PromotionRequestContextProvider` đã có bằng
  callback `onResult` riêng — đó là chính sách của consumer, không phải cơ chế chung; xem ranh giới
  ở §2). Thêm biến thể `suspend` bây giờ là speculative — chưa consumer nào cần, để dành khi UC7
  (status-code handler) thật sự đụng tới luồng refresh.
- **Cùng `defaultRequest { }` với UC2**, cùng `appendIfNameAbsent` — token cũng là một header, không
  cần cơ chế riêng. Token rỗng/toàn khoảng trắng bị `trim()`+`takeIf` lọc trước khi gắn, tránh gửi
  `Authorization: Bearer ` (Bearer không kèm token).
- **`toBearerToken()` so khớp không phân biệt hoa/thường** (`ignoreCase = true`) — token đã có sẵn
  `Bearer `/`bearer ` thì giữ nguyên, không lặp tiền tố. Test
  `tokenAlreadyPrefixedWithBearerIsNotDoubled` khoá đúng hành vi này, đúng bẫy `PromotionHttpClient`
  đã gặp và ghi trong `NetworkingGuide.md`.

### UC4 — quyết định thiết kế

- **Bug gốc (`VDORequest.Builder.buildPath()` nối chuỗi tay, không encode) đã tự hết** từ lúc chọn
  Ktor ở UC1 — `parameter()` của Ktor vốn tự percent-encode. Việc UC4 thực sự thêm là **path segment**:
  `client.get("voucher/$id")` kiểu string-interpolation KHÔNG tự encode `$id` (khác `parameter()`),
  cùng loại rủi ro với bug cũ nhưng ở chỗ khác — `getRequest`/`postRequest` dùng
  `url { appendPathSegments(*pathSegments) }` để mỗi segment cũng được encode có cấu trúc, không phải
  nối chuỗi.
- **Test xác nhận bằng round-trip, không so khớp chuỗi percent-encode cụ thể**: `queryParameterWithReservedCharactersRoundTripsCorrectly`
  gửi `"a&b=c"` (chứa ký tự phân tách query thật) và đọc lại qua `url.parameters["q"]` — nếu encode
  sai, giá trị đọc lại sẽ không còn nguyên vẹn hoặc bị tách thành nhiều param. Cách này bền hơn hard-code
  chuỗi `%26`/`%3D` vì không phụ thuộc thuật toán encode cụ thể của Ktor.
- **`postRequest.body: Any?` truyền thẳng cho `setBody`, chưa serialize JSON** — `ContentNegotiation`
  chưa cài (đó là UC5). Test `postSendsBodyToConfiguredPath` chỉ xác nhận method + path, không xác
  nhận nội dung body.

### UC5 — quyết định thiết kế

- **`Json` cấu hình cứng trong `NetworkKitHttpClient`, không lộ ra `NetworkClientConfig`** — copy
  nguyên bốn cờ đã chứng minh đúng của `PromotionHttpClient`
  (`ignoreUnknownKeys`/`explicitNulls`/`encodeDefaults`/`isLenient`, xem
  [NetworkingGuide.md §2](./NetworkingGuide.md)), vì mọi backend trong hệ sinh thái Viettel đang phục
  vụ (kể cả `network-kit-android` cũ) đều xuất phát từ Gson — bốn cờ này tái hiện đúng hành vi Gson,
  không phải tuỳ chọn per-host. Chưa consumer nào cần khác đi; lộ ra làm config item khi có nhu cầu
  thật (YAGNI, giống quyết định ở UC1 với `timeoutMillis`).
- **`getJson`/`postJson` là `inline` `reified`**, gọi `HttpResponse.body<T>()` của Ktor — không tự
  định nghĩa envelope. `T` là bất kỳ kiểu `@Serializable` nào consumer cần, kể cả envelope riêng của
  họ (`ApiResponseTemplate<...>`) — đúng ranh giới "cơ chế, không phải chính sách" ở §2.
- **Test khoá đúng các bẫy đã biết**, không đoán: (1) field lạ trong response không phá decode
  (`ignoreUnknownKeys`); (2) server trả số cho field khai kiểu String không ném lỗi
  (`isLenient` — bẫy thật đã ghi trong `NetworkingGuide.md`, không phải giả định); (3) field **không**
  có giá trị default thiếu trong response → ném `JsonConvertException` rõ ràng (Ktor bọc
  `MissingFieldException` của kotlinx.serialization), không phải `null`/giá trị rác âm thầm; (4) field
  **có** giá trị default thiếu trong response → dùng default, không ném lỗi; (5) field có default
  nhưng server **có** trả → dùng giá trị server, không lặng lẽ rơi về default.
- **(4) không phải do `explicitNulls`** — dễ nhầm. `explicitNulls = false` chỉ nới field **nullable
  không có default** (`val x: String?`, không `= null`) được phép vắng mặt hoàn toàn trong JSON; còn
  "field có default value thì không bắt buộc trong JSON" là hành vi **gốc** của
  `@Serializable`/kotlinx.serialization, độc lập với mọi cờ. `SampleDto` dùng cả 4 kiểu field trong
  cùng một DTO để đối chiếu trực tiếp thay vì suy luận rời rạc: `id`/`name`/`amount` (không default =
  bắt buộc), `note: String = "no-note"` (có default = tuỳ chọn, dùng default nếu thiếu), `altId:
  String?` (nullable, không default — thiếu trong JSON thì nhờ `explicitNulls = false` mới không ném
  lỗi, coi như `null`).

### UC6 — quyết định thiết kế

- **`NetworkError` không mang error code/exception domain** — chỉ phân loại lỗi *transport*
  (timeout/mất mạng/HTTP lỗi/JSON lệch/không rõ). Map sang exception nghiệp vụ cụ thể
  (`PromotionException`/`ErrorCodes`…) là việc của consumer, đúng ranh giới ở §2 — `:networkKit`
  không biết `ErrorCodes.TIMEOUT` là gì.
- **`expectSuccess = true` bắt buộc phải bật** (thêm vào `NetworkKitHttpClient.configure()` ở UC6) để
  4xx/5xx thực sự ném `ResponseException` — thiếu cờ này `networkCall {}` sẽ không bao giờ thấy nhánh
  `Http`, response lỗi sẽ lặng lẽ trôi qua như thành công. Chạy lại toàn bộ 21 test của UC1–UC5 sau khi
  bật cờ để xác nhận không có regression (không test nào dùng status ngoài 2xx mà không chủ đích).
- **Thứ tự `catch` giữ nguyên như `apiCall {}`** của `PromotionRemoteDataSource`: ba loại timeout của
  Ktor đều là con của `IOException` nên bắt trước; `CancellationException` luôn rethrow, không bọc
  thành `NetworkError.Unknown` (CodingStandards §4) — nhánh này không có test tự động (ném
  `CancellationException` trong `runTest` dễ bị hiểu nhầm thành huỷ chính coroutine của test, không an
  toàn để assert), chỉ giữ đúng thứ tự code + comment giải thích.
- **`Http.rawBody` đọc bằng `runCatching { … }.getOrNull()`**, không để việc đọc lại body (có thể đã
  tiêu thụ hoặc lỗi encoding) làm hỏng luôn việc phân loại lỗi ban đầu.

### UC7 — quyết định thiết kế

UC7 trải qua vài vòng thiết kế lại trước khi chốt — giữ lại lý do ở đây để không phải tranh luận lại
từ đầu.

**Vì sao tách 2 cơ chế thay vì 1**

Bản đầu chỉ có `StatusCodeHandlerChain` với `onMatch: (BusinessStatus) -> Throwable` — tức thời
consumer muốn "session hết hạn → refresh token → tự retry request" thì phải tự bắt lỗi ném ra rồi gọi
lại thủ công ở nơi khác (phân mảnh logic ra ngoài). Đọc lại `core/network` gốc (§1) mới thấy: refresh
là chuyện **trước khi có response quyết định cuối** (tầng transport), còn so khớp business code là
chuyện **sau khi response đã decode** — hai thời điểm khác nhau, không thể gộp vào một cơ chế mà không
làm yếu đi cả hai. Quyết định: giữ 2 cơ chế riêng, đúng ranh giới ba thế hệ code đã chứng minh.

**1. `StatusCodeHandler`/`StatusCodeHandlerChain` — sau khi response đã decode**

- `BusinessStatus` là interface cấu trúc tối thiểu (`code`/`message`), không phải DTO cụ thể — consumer
  tự viết `fun MyEnvelope.toBusinessStatus(): BusinessStatus?`. Module không biết `ApiResponseTemplate<T>`
  của Promotion hay `VDOBaseResponse`-style của SDK khác trông ra sao — đúng ranh giới ở §2, tránh lặp
  lại lỗi `VDOBaseResponse` cũ ép mọi DTO kế thừa.
- `StatusCodeHandler.of(vararg codes)` là phần dùng chung thật sự — đúng đoạn logic đang bị
  `EkycStatusCodeHandler`/`ApiStatusHandler` chép tay gần giống nhau (lệch nhau ở `body = null` vs
  `body = response as? T`, xem §1). Test `twoConsumersWithDifferentEnvelopesShareTheSameHandlerWithoutDrift`
  dựng hai envelope khác hình dạng, xác nhận cả hai ném lỗi **giống hệt nhau** khi dùng chung một
  handler — chứng minh trực tiếp bug lệch hành vi kiểu đó không thể xảy ra nữa.
- **`onMatch` là `suspend (BusinessStatus) -> Unit`, không phải `-> Throwable`.** Đây là điểm chỉnh
  quan trọng nhất sau khi review: bản `-> Throwable` ép handler chỉ được làm đúng một việc (tạo ra lỗi
  để module ném hộ). Bản `Unit` để handler **tự quyết định và tự hành động** — ném lỗi domain riêng,
  gọi refresh token (`suspend` cho phép await), log, hoặc return bình thường nếu không có gì để báo.
  Test `onMatchCanReturnNormallyInsteadOfThrowing` khoá đúng khả năng "xử lý xong, không throw".
- `checkBusinessStatus` là extension `suspend T.() -> T`, **không đăng ký lúc tạo client** — chain
  dựng ở đâu, gọi khi nào có response cũng được (`response.checkBusinessStatus(chain) { adapter }`).
  Cân nhắc bọc luôn vào `getJson`/`getRequest` lúc tạo `NetworkClientConfig`, nhưng vậy sẽ buộc mọi
  request qua client đó đều phải khớp một envelope cố định — sai ranh giới "cơ chế, không chính sách".
- `BusinessError` (ánh xạ mặc định của `.of(vararg codes)`) tách khỏi `NetworkError` (UC6) — khác loại
  lỗi: `NetworkError` là request không đến được server đúng nghĩa hoặc HTTP lỗi; `BusinessError` là
  response **thành công** (2xx) nhưng nội dung mang code lỗi nghiệp vụ.
- Chain chạy tuần tự, dừng ở handler khớp đầu tiên (`onlyFirstMatchingHandlerInChainRuns`) — không có
  state toàn cục nào để rò rỉ giữa các chain khác nhau (khác `VDOStatusCodeHandlerContainer` cũ dùng
  `ArrayList` alias tham chiếu toàn cục, xem phát hiện #2 ở §1).

**2. `NetworkInterceptor`/`InterceptorChain` — trong lúc request→response, trước khi đọc body**

- Model chain-of-responsibility giống hệt OkHttp `Interceptor.Chain.proceed()` — quen thuộc với ai đã
  quen `network-kit-android`/`core/network` cũ, và cho phép một interceptor gọi `proceed()` nhiều lần
  để tự retry (điều `RequestInterceptor` cũ làm bằng vòng `while` mù, ở đây consumer viết điều kiện
  riêng thay vì bị ép retry-3x-trên-mọi-lỗi).
- Đăng ký qua `NetworkClientConfig.interceptors` **lúc tạo client**, khác `StatusCodeHandlerChain` —
  vì interceptor cần được cài vào pipeline HTTP thật của Ktor (`HttpSend`), việc này chỉ làm được khi
  client tồn tại, không phải thứ gọi tuỳ ý về sau.
- **Không cài qua `install(HttpSend) { }`** — config block của `HttpSend` chỉ có `maxSendCount`,
  không có `intercept()`. Phải gọi `client.plugin(HttpSend).intercept { }` sau khi `HttpClient` đã dựng
  xong (`HttpSend` là plugin lõi Ktor, luôn có sẵn, không cần `install()` riêng) — tách thành hàm
  `applyInterceptors()` riêng khỏi `configure()`, gọi từ cả `create()` lẫn test, giữ đúng lý do UC1 đã
  tách `configure()`: test dựng được cùng cấu hình trên `MockEngine`.
- Test `interceptorCanChangeHeaderOnRetry` là test quan trọng nhất — dựng lại chính xác kịch bản
  "request đầu mang token cũ, phát hiện cần refresh, đổi `Authorization` trước khi gọi lại" — xác nhận
  interceptor thật sự sửa được request và Ktor gửi lại đúng request đã sửa, không phải request cũ.
- **`:networkKit` không tự làm refresh-token-rồi-retry mẫu nào** — đó là chính sách của consumer, viết
  bằng chính `NetworkInterceptor` này. Module chỉ cho cơ chế `proceed()` nhiều lần.

### UC8 — quyết định thiết kế

- **`isDebug: Boolean` trên `NetworkClientConfig`, không khoá theo enum môi trường** — đúng lỗi của
  `network-kit-android` cũ: `enableHttpLog()` chỉ có tác dụng khi `appEnvironment == STAGING`, gọi ở
  môi trường khác thì im lặng không log gì, không có cảnh báo. Ở đây `isDebug` là input duy nhất,
  hành vi tất định — khớp cách `PromotionHttpClient` của `:promotionLogic` đã làm.
- **`NetworkKitCurlLogging` port nguyên logic `PromotionCurlLogging`** — không viết lại, không tinh
  chỉnh, chỉ đổi tên và bỏ nhãn "PromotionSDK". Hàm `buildCurlCommand()` đã có bộ test đầy đủ ở
  `:promotionLogic` (6 nhánh: header, JSON body, escape dấu nháy đơn, body dạng byte array, body rỗng,
  body không phải `OutgoingContent`) — port nguyên bộ test đó thay vì viết lại, vì logic **giống hệt**
  không đổi gì ngoài package.
- **Cả `Logging` (Ktor) và `NetworkKitCurlLogging` đều lộ `Authorization`** — `LogLevel.BODY` in toàn
  bộ header/body, cURL cũng vậy (dựng lại đúng request thật để dán chạy được, kể cả token). Cả hai chỉ
  bật khi `isDebug`, tuyệt đối không ở bản phát hành — giữ đúng cảnh báo NetworkingGuide.md §8 điều 4.
- **Không test được `Logging.level` sau khi cài** — plugin `Logging` của Ktor dựng qua
  `createClientPlugin`, `level` chỉ là biến cục bộ chụp trong lambda cài đặt, không lộ ra thành property
  đọc lại được trên instance đã cài (đã tra sources jar để xác nhận, không đoán). Thay vào đó test verify
  **hành vi quan sát được thật sự** của phần `:networkKit` tự viết: `NetworkKitCurlLogging` có được cài
  vào client hay không (`pluginOrNull`), đúng với những gì mã nguồn `configure()` quyết định theo
  `isDebug` — không cố test lại hành vi nội bộ của Ktor.

### UC9 — quyết định thiết kế

- **`NetworkClient` KHÔNG nhận `statusHandlers` lúc dựng**, khác mô tả ban đầu của UC9 trong kế hoạch
  gốc — vì UC7 (sau khi review lại) đã chốt `StatusCodeHandlerChain` không đăng ký lúc tạo client, áp
  dụng ad-hoc bất cứ đâu có response. `NetworkClient` tôn trọng đúng quyết định đó thay vì đảo ngược:
  `checkBusinessStatus` vẫn là bước consumer tự gọi tiếp sau `getJson`/`postJson` của facade.
- **Giá trị thật của UC9 là đóng đúng lỗ hổng đã phát hiện lúc bàn UC6**: gọi thẳng
  `HttpClient.getJson`/`postJson` (UC4/UC5) không có gì ép phải bọc `networkCall {}` — quên bọc là lỗi
  hoàn toàn có thể xảy ra, và lỗi transport thô của Ktor sẽ lọt thẳng ra ngoài, không phân loại.
  `NetworkClient.getJson`/`postJson` **luôn** bọc `networkCall {}` bên trong — dùng facade thì không
  còn cách nào quên. Test `transportErrorNeverLeaksRawKtorExceptionThroughNetworkClient` khoá đúng
  cam kết này.
- **`httpClient` public, không giấu đi** — escape hatch cho consumer cần gọi thẳng Ktor API mà facade
  chưa bọc (vd một verb khác GET/POST), đúng tinh thần "cơ chế, không chính sách" — facade không ép
  mọi thứ phải đi qua nó.
- **Test lắp ráp (`everyMechanismParticipatesInASingleSuccessfulCall`) là trọng tâm của UC9**, không
  phải test đơn vị: dựng MỘT request thật qua `NetworkClient` với header tĩnh + header động + token +
  interceptor + `isDebug=true` bật đồng thời, xác nhận cả bốn đều có mặt đúng trên cùng một request
  gửi đi, rồi giải mã + kiểm business status trên cùng kết quả đó. Từng UC đã có test riêng chứng minh
  đúng một mình; test này chứng minh **ráp lại với nhau vẫn đúng** — hai việc khác nhau.

`getJson<T>`/`postJson<T>` là `inline fun <reified T>` — đây là cơ chế **chỉ Kotlin hiểu**, hai lớp
chặn tách biệt nhau nếu một module định nghĩa DTO bằng Java:

1. **`@Serializable` không áp được lên class Java.** Plugin serialization của Kotlin sinh
   `KSerializer` lúc biên dịch **file `.kt`** — javac không chạy qua plugin này, nên một `.java` class
   không thể có serializer, bất kể có "default value" hay không. Đây là rào chặn cứng, không phải chi
   tiết default value như câu hỏi ban đầu.
2. **Java không gọi được hàm `inline reified`.** `reified` chỉ tồn tại nhờ Kotlin compiler inline y
   nguyên thân hàm vào chỗ gọi — Java không có cơ chế này, nên `getJson<T>()` không xuất hiện như một
   API gọi được từ `.java` theo cách thông thường.

Trong kiến trúc hiện tại, chuyện này không phát sinh: `:networkKit` chỉ được consumer **KMP khác**
gọi (`:promotionLogic`, SDK KMP tương lai) — và `commonMain` của một module KMP **bắt buộc** là
Kotlin, không có chỗ cho file Java. Chỉ trở thành vấn đề thật nếu sau này một module Android thuần
Java (khác nhánh — không qua KMP) muốn gọi thẳng `:networkKit`; khi đó cần một lớp adapter Kotlin
mỏng expose hàm non-generic/non-inline (nhận `Class<T>` như cách `network-kit-android` cũ từng làm),
chứ bản thân `getJson<T>` không port được nguyên trạng.

**Cố tình để ngoài phạm vi Phase 1** (đã ghi ở §3) — không thiết kế/code adapter đó bây giờ, vì chưa
có consumer Java thật nào cần. Ghi lại ở đây để phase sau không phải khám phá lại từ đầu.

---

## 5. Toạ độ Gradle & phát hành

| | Giá trị | Ghi chú |
|---|---|---|
| Module Gradle | `:networkKit` | Ngang hàng `:promotionLogic` trong repo này |
| Kotlin package | `vn.viettelpay.networkkit` | **Khác** `com.ttcn.promotionsdk.*` — module này không thuộc riêng Promotion |
| Android namespace | `vn.viettelpay.networkkit` | Trùng package, không có collision `R` cần né như `AndroidPromotionSDK`/`promotionLogic` |
| Maven `groupId` | `SDK_GROUP` hiện có (`vn.viettelpay.library`) | **Giả định Phase 1** — tái dùng cơ chế mavenLocal/Artifactory/Viettelmoney đã khai sẵn ở root `build.gradle.kts`. Có thể tách group riêng trung lập thương hiệu khi có SDK thứ hai thật sự tiêu thụ module |
| Maven `artifactId` | `networkKit` (= tên module) | Theo đúng quy ước đã áp dụng cho `promotionLogic` — Gradle Module Metadata đọc theo tên module, đổi ở publication không đổi được toạ độ |
| Version | property `NETWORK_KIT_VERSION` (`gradle.properties`) | **Độc lập** với `SDK_VERSION` của Promotion — module này không khoá chung nhịp phát hành với bất kỳ SDK cụ thể nào |
| Phát hành iOS | Không qua Maven, không XCFramework riêng | Consumer KMP (`:promotionLogic`…) khai `implementation(projects.networkKit)`; Kotlin/Native tự link tĩnh klib vào framework của chính consumer đó — giống hệt cách `:promotionLogic` tự link vào `:AndroidPromotionSDK` phía Android |

---

## 6. Lệnh thường dùng

```bash
./gradlew :networkKit:assemble                 # AAR (Android) + klib (iOS, 3 target)
./gradlew :networkKit:testAndroidHostTest       # commonTest trên JVM
./gradlew :networkKit:iosSimulatorArm64Test     # commonTest trên iOS simulator
./gradlew :networkKit:publishToMavenLocal       # publish thử vào ~/.m2
```

**Đã xác nhận (UC0–UC9), cả hai nền tảng xanh:**
- `assemble` — AAR Android + 3 klib iOS.
- `testAndroidHostTest` — 48 test (`NetworkKitHttpClientTest` 11 + `NetworkKitRequestsTest` 3 +
  `NetworkKitJsonTest` 7 + `NetworkKitErrorTest` 4 + `NetworkKitStatusCodeTest` 7 +
  `NetworkKitInterceptorTest` 5 + `NetworkKitCurlLoggingTest` 8 + `NetworkClientTest` 3), 0 lỗi.
- `iosSimulatorArm64Test` — cùng 48 test, 0 lỗi (kể cả timeout live-fire, kể cả interceptor tự retry
  và đổi header giữa hai lần gửi thật, kể cả lắp ráp end-to-end).
- `publishToMavenLocal` — artifact ở `~/.m2/repository/vn/viettelpay/library/networkKit/0.1.0/`,
  vẫn xanh sau khi thêm `NetworkClient`.

> Máy dev ban đầu bị chặn `iosSimulatorArm64Test` do `xcode-select` trỏ vào Command Line Tools thay vì
> Xcode.app đầy đủ (`sudo xcode-select -s /Applications/Xcode.app/Contents/Developer` đã sửa) — không
> phải lỗi module, ghi lại đây phòng máy khác gặp lại.

Đạt chuẩn Pre-commit checklist của [AI_AGENT_RULES.md](../AI_AGENT_RULES.md) — UC0 đến UC9 **đóng**.

---

## 7. Liên kết

- [NetworkingGuide.md](./NetworkingGuide.md) — cách `:promotionLogic` đang tự làm networking hôm nay;
  sẽ được thay bằng lời gọi vào `:networkKit` ở UC10, giữ nguyên `PromotionApiService`/
  `PromotionRemoteDataSource`/`ApiResponseTemplate`.
- [Architecture.md §2.4](./Architecture.md#24-module-dùng-chung-mới--networkkit-chưa-tích-hợp)
- [ProjectStructure.md](./ProjectStructure.md)
