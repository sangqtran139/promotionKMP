# SharedNetworkKit — Module network KMP dùng chung (`:networkKit`)

> Trạng thái: **Phase 1, UC0 xong** — khung module rỗng, chưa có logic networking. File này là tài
> liệu sống, cập nhật sau mỗi UC (xem AI_AGENT_RULES điều 8).

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
| UC1 | `HttpClient` factory per-instance (baseUrl, timeout) | 🔜 |
| UC2 | Header injection (tĩnh + động) | 🔜 |
| UC3 | Token provider (auth) | 🔜 |
| UC4 | Request builder GET/POST an toàn (auto-encode query) | 🔜 |
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

## 4. Cấu trúc module (UC0)

```
networkKit/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/vn/viettelpay/networkkit/
    │   └── NetworkKit.kt        # điểm neo tạm — chưa có logic, xem §3
    └── commonTest/kotlin/vn/viettelpay/networkkit/
        └── NetworkKitTest.kt    # xác nhận commonTest chạy được cả Android host lẫn iOS simulator
```

Chưa có `androidMain`/`iosMain`: giống `:promotionLogic`, engine Ktor sẽ tự chọn theo artifact có
trên classpath (`ktor-client-okhttp` ở androidMain, `ktor-client-darwin` ở iosMain) khi UC1 thêm
dependency — không cần `expect`/`actual` cho việc đó. Hai source set này chỉ xuất hiện nếu một UC sau
thật sự cần API riêng nền tảng (xem [ProjectStructure.md](./ProjectStructure.md) — nguyên tắc "chỉ ba
chỗ cần biết nền tảng" áp dụng tương tự ở đây).

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

**Đã xác nhận (UC0), cả hai nền tảng xanh:**
- `assemble` — AAR Android + 3 klib iOS.
- `testAndroidHostTest` — 1 test (`versionPlaceholderIsSet`).
- `iosSimulatorArm64Test` — cùng 1 test (`versionPlaceholderIsSet[iosSimulatorArm64]`).
- `publishToMavenLocal` — artifact ở `~/.m2/repository/vn/viettelpay/library/networkKit/0.1.0/`.

> Máy dev ban đầu bị chặn `iosSimulatorArm64Test` do `xcode-select` trỏ vào Command Line Tools thay vì
> Xcode.app đầy đủ (`sudo xcode-select -s /Applications/Xcode.app/Contents/Developer` đã sửa) — không
> phải lỗi module, ghi lại đây phòng máy khác gặp lại.

Đạt chuẩn Pre-commit checklist của [AI_AGENT_RULES.md](../AI_AGENT_RULES.md) — UC0 **đóng**.

---

## 7. Liên kết

- [NetworkingGuide.md](./NetworkingGuide.md) — cách `:promotionLogic` đang tự làm networking hôm nay;
  sẽ được thay bằng lời gọi vào `:networkKit` ở UC10, giữ nguyên `PromotionApiService`/
  `PromotionRemoteDataSource`/`ApiResponseTemplate`.
- [Architecture.md §2.4](./Architecture.md#24-module-dùng-chung-mới--networkkit-chưa-tích-hợp)
- [ProjectStructure.md](./ProjectStructure.md)
