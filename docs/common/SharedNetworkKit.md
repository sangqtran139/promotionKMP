# SharedNetworkKit — Module network KMP dùng chung (`:networkKit`)

> Trạng thái: **Phase 1, UC4 xong** — `HttpClient` per-instance + header tĩnh/động + token provider +
> request builder GET/POST an toàn, chưa có parsing response. File này là tài liệu sống, cập nhật sau
> mỗi UC (xem AI_AGENT_RULES điều 8).

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

---

## 2. Ranh giới: cơ chế, không phải chính sách

Module là tầng **common**, không phụ thuộc bất kỳ domain feature nào (Promotion, eKYC, miniapp…).

| Thuộc về `:networkKit` | Thuộc về consumer (`:promotionLogic`, SDK KMP khác…) |
|---|---|
| Cách dựng `HttpClient` theo baseUrl/timeout, per-instance | Base URL thật, giá trị header nghiệp vụ (Product, Channel…) |
| Cách gắn header tĩnh/động, token provider | Envelope response cụ thể (`ApiResponseTemplate` của Promotion khác `VDOBaseResponse` của ekyc) |
| Cách chạy chuỗi "business status code → lỗi" | Danh sách code nào là lỗi, message hiển thị gì |
| Cách phân loại lỗi transport (timeout/IO/HTTP/serialization) | Map lỗi transport → exception domain riêng (`PromotionException`…) |

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
| UC5 | Giải mã response generic bằng kotlinx.serialization | 🔜 |
| UC6 | Sealed error cho lỗi transport | 🔜 |
| UC7 | Business status-code handler chain | 🔜 |
| UC8 | Debug logging tường minh (cờ, không khoá theo enum môi trường) | 🔜 |
| UC9 | Lắp ráp `NetworkClient` facade | 🔜 |
| UC10 | `:promotionLogic` tiêu thụ thử — thay `PromotionHttpClient.kt` | 🔜 |

Ngoài phạm vi Phase 1: bộ tải file (`file_loader` của `network-kit-android`), mã hoá RSA cho
`X-SESSION-ID`, retry-policy phức tạp mặc định, migrate `ekyc-service`/`sdk-miniapp` sang dùng module
này ngay.

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
    │   ├── NetworkKitHttpClient.kt     # create(config): HttpClient — factory per-instance
    │   └── NetworkKitRequests.kt       # getRequest/postRequest — path segments + query an toàn
    └── commonTest/kotlin/vn/viettelpay/networkkit/
        ├── NetworkKitHttpClientTest.kt # MockEngine: URL, timeout, header, token — không đè giá trị caller
        └── NetworkKitRequestsTest.kt   # MockEngine: path/query round-trip với ký tự đặc biệt
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

**Đã xác nhận (UC0 + UC1 + UC2 + UC3 + UC4), cả hai nền tảng xanh:**
- `assemble` — AAR Android + 3 klib iOS.
- `testAndroidHostTest` — 11 test (`NetworkKitHttpClientTest`) + 3 test (`NetworkKitRequestsTest`), 0 lỗi.
- `iosSimulatorArm64Test` — cùng 14 test, 0 lỗi (kể cả timeout live-fire).
- `publishToMavenLocal` — artifact ở `~/.m2/repository/vn/viettelpay/library/networkKit/0.1.0/`,
  vẫn xanh sau khi thêm request builder.

> Máy dev ban đầu bị chặn `iosSimulatorArm64Test` do `xcode-select` trỏ vào Command Line Tools thay vì
> Xcode.app đầy đủ (`sudo xcode-select -s /Applications/Xcode.app/Contents/Developer` đã sửa) — không
> phải lỗi module, ghi lại đây phòng máy khác gặp lại.

Đạt chuẩn Pre-commit checklist của [AI_AGENT_RULES.md](../AI_AGENT_RULES.md) — UC0, UC1, UC2, UC3 và
UC4 **đóng**.

---

## 7. Liên kết

- [NetworkingGuide.md](./NetworkingGuide.md) — cách `:promotionLogic` đang tự làm networking hôm nay;
  sẽ được thay bằng lời gọi vào `:networkKit` ở UC10, giữ nguyên `PromotionApiService`/
  `PromotionRemoteDataSource`/`ApiResponseTemplate`.
- [Architecture.md §2.4](./Architecture.md#24-module-dùng-chung-mới--networkkit-chưa-tích-hợp)
- [ProjectStructure.md](./ProjectStructure.md)
