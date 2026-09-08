# Security — Bảo mật, dữ liệu & tuân thủ

Tài liệu này trả lời câu hỏi bộ phận an ninh thông tin của bên tích hợp sẽ hỏi: **SDK chạm vào cái
gì, giữ cái gì, gửi đi đâu, và để lại dấu vết ở đâu.**

> Phạm vi: TTCN Promotion SDK `1.0.0` (Android AAR + iOS XCFramework). Nguồn sự thật là source code;
> mọi khẳng định dưới đây đều nêu **chỗ kiểm chứng được** trong repo.

## Mục lục

<!-- toc -->
- [1. Quyền hệ thống](#1-quyền-hệ-thống)
- [2. Dữ liệu SDK xử lý](#2-dữ-liệu-sdk-xử-lý)
  - [2.1. Nhận từ host](#21-nhận-từ-host)
  - [2.2. Lưu xuống thiết bị](#22-lưu-xuống-thiết-bị)
  - [2.3. Gửi lên server](#23-gửi-lên-server)
- [3. Kênh truyền](#3-kênh-truyền)
- [4. Vòng đời token](#4-vòng-đời-token)
- [5. Log & thông tin nhạy cảm](#5-log--thông-tin-nhạy-cảm)
- [6. Bề mặt tấn công & hàng rào](#6-bề-mặt-tấn-công--hàng-rào)
  - [6.1. AAR không obfuscate — quyết định có ý thức](#61-aar-không-obfuscate--quyết-định-có-ý-thức)
- [7. Phụ thuộc bên thứ ba](#7-phụ-thuộc-bên-thứ-ba)
- [8. Danh mục kiểm tra cho bên an ninh](#8-danh-mục-kiểm-tra-cho-bên-an-ninh)
- [9. Báo lỗi bảo mật](#9-báo-lỗi-bảo-mật)
<!-- /toc -->

---

## 1. Quyền hệ thống

| Nền tảng | Quyền | Khai ở đâu |
|---|---|---|
| Android | **Chỉ** `android.permission.INTERNET` | `AndroidPromotionSDK/src/main/AndroidManifest.xml` |
| iOS | **Không** xin quyền nào | Không có key quyền trong Info.plist của framework |

SDK **không** dùng: vị trí, camera, danh bạ, bộ nhớ ngoài, sinh trắc học, thông báo, Bluetooth,
định danh quảng cáo.

---

## 2. Dữ liệu SDK xử lý

### 2.1. Nhận từ host

| Dữ liệu | Nguồn | SDK làm gì |
|---|---|---|
| Access token | `PromotionTokenSource.currentToken()` | **Đọc lại mỗi request**, gắn header `Authorization`. **Không lưu bản sao** |
| `baseUrl`, `environment`, `language` | `PromotionSessionConfig` | Giữ trong bộ nhớ đến khi `release()` |
| `orderId`, `productId`, `orderValue`, `metaData`, SKU | `updateOrderInfo(...)` | Giữ trong `PromotionMutableContext` (bộ nhớ), gửi kèm request tìm ưu đãi/đối soát |
| Theme | `configure(theme)` | Lưu cục bộ để lần mở sau không nhấp nháy |

### 2.2. Lưu xuống thiết bị

| Lưu gì | Ở đâu | Vì sao |
|---|---|---|
| 6 cờ feature flag | Android `SharedPreferences` · iOS `NSUserDefaults` **suite riêng** | Để mở màn không phải chờ mạng |
| Theme đã cấu hình | như trên | Giữ giao diện đúng brand ngay từ khung hình đầu |

**Không lưu:** token, thông tin định danh khách hàng, danh sách voucher, chi tiết đơn hàng, lịch sử
giao dịch. **Không có** cơ sở dữ liệu (không Room, không SQLDelight) — dữ liệu ưu đãi luôn lấy tươi
từ server.

> iOS dùng **suite `NSUserDefaults` riêng**, không ghi chung vùng của host. Android dùng file
> `SharedPreferences` riêng của SDK. Đã **cố ý loại** `multiplatform-settings-no-arg` vì module đó
> ghi/xoá vào prefs mặc định của app host.

### 2.3. Gửi lên server

Chỉ gửi tới **`baseUrl` do host truyền vào**. SDK không có endpoint mặc định, không gọi bất kỳ dịch
vụ bên thứ ba nào (không analytics, không crash reporting, không quảng cáo).

Payload gửi đi giới hạn trong: từ khoá tìm kiếm, mã dịch vụ, phân trang, thông tin đơn hàng host đã
bơm vào, và danh sách id ưu đãi user chọn.

---

## 3. Kênh truyền

| Hạng mục | Thiết kế |
|---|---|
| Giao thức | HTTPS — do `baseUrl` của host quyết định. SDK không tự hạ cấp về HTTP |
| Engine | Android: OkHttp · iOS: NSURLSession (qua Ktor) — dùng TLS stack của hệ điều hành |
| Certificate pinning | **Không có trong SDK.** Nếu host yêu cầu pinning, hiện phải cấu hình ở tầng app (Android Network Security Config / ATS + pinning của host) |
| Timeout | 30 giây cho connect / request / socket |
| `X-Request-ID` | UUID mới **mỗi** request — dùng để đối soát log với backend khi điều tra sự cố |

> **Điểm cần quyết định khi bàn giao:** pinning nằm ngoài SDK là lựa chọn có ý thức (SDK không biết
> chuỗi chứng chỉ của từng môi trường host). Nếu bên an ninh yêu cầu pinning **bên trong** SDK, đó
> là thay đổi hợp đồng cấu hình — phải bump minor và thêm tham số vào `PromotionSessionConfig`.

---

## 4. Vòng đời token

Ba khẳng định, tất cả kiểm chứng được bằng test:

1. **SDK không giữ bản sao token.** `currentToken()` nằm trong `defaultRequest { }` nên được gọi lại
   ở mỗi request — test `TokenPullPerRequestTest`.
2. **401 chỉ xin lại token đúng một lần**, có single-flight để nhiều request song song không làm host
   refresh nhiều lần — test `TokenRefreshGateTest`, `TokenRefreshRetryTest`.
3. **Không lấy được token mới ⇒ dừng hẳn**: `TOKEN_EXPIRED` + `PromotionSDKCallback.onExpireToken()`;
   SDK **không** tự đăng nhập lại, không tự điều hướng — quyết định đó thuộc về host.

`release()` xoá dữ liệu phiên. Host **phải gọi `release()` khi user đăng xuất**.

---

## 5. Log & thông tin nhạy cảm

| Kênh log | Bản debug | Bản phát hành |
|---|---|---|
| Ktor `Logging` (`LogLevel.BODY`) | Bật | **Tắt** (`LogLevel.NONE`) |
| `PromotionCurlLogging` (in lệnh cURL copy-paste được) | Bật | **Tắt** |

Cả hai đều **in header `Authorization`**. Điều kiện bật: Android — build variant DEBUG; iOS — biến
môi trường `PROMOTION_SDK_DEBUG=1`.

**Tuyệt đối không** bật hai kênh này trên bản phát hành cho đối tác. Đây là điểm bắt buộc kiểm trong
[Release Checklist](../release/ReleaseChecklist.md).

Ngoài hai kênh trên, SDK không ghi token/PII ra log.

---

## 6. Bề mặt tấn công & hàng rào

| Hàng rào | Cơ chế | Chặn được gì | **Không** chặn được gì |
|---|---|---|---|
| `internal` (Kotlin) | Compile-time | Host viết Kotlin import nội bộ | Host viết **Java** hoặc dùng reflection — bytecode vẫn là `public final` |
| `implementation(projects.promotionLogic)` | Compile classpath | Host thấy/gọi type của lõi | Tên class vẫn nằm trong APK |
| Không phát hành `sources.jar` | Artifact | Đọc nguyên văn source từ IDE | Decompile bytecode |
| `public.xml` rỗng | AAPT2 | Host tham chiếu resource của SDK (lint `PrivateResource`) | — |
| `@_implementationOnly` (iOS) | `.swiftinterface` | Type Kotlin lọt ra API công khai; host **không build được** nếu vi phạm | — |

### 6.1. AAR không obfuscate — quyết định có ý thức

Đã thử bật R8 trên chính module SDK và **đã gỡ bỏ**: nó rút gọn cả mapper Data Binding khiến host
crash `AbstractMethodError` ngay khi mở màn SDK đầu tiên. Đổi lại là một AAR khó debug mà không
thêm được hàng rào thật nào.

Hệ quả cần ghi nhận rõ khi bàn giao: **mọi tên class trong `com.ttcn.prm.**` và
`com.ttcn.promotionsdk.**` đều đọc được nếu decompile AAR.** Hàng rào ở đây là *hợp đồng API*, không
phải chống dịch ngược.

Bù lại: host bật R8 ở app của họ vẫn an toàn — `consumer-rules.pro` đóng gói sẵn trong AAR đã giữ
đúng phần API cần giữ (entry, config, domain model, serializer của kotlinx.serialization).

Nếu bên an ninh yêu cầu **giấu thật sự**, phương án duy nhất là gộp `promotionLogic` vào AAR (fat
AAR) rồi R8 một lượt kèm keep-list cho Data Binding/Gson/kotlinx.serialization — đó là **thay đổi
hợp đồng phát hành** (POM và `module.json` đổi theo, host nhận bộ dependency khác), không làm lặng
lẽ được.

---

## 7. Phụ thuộc bên thứ ba

Danh sách đầy đủ kèm giấy phép: [`../../THIRD_PARTY_NOTICES.md`](../../THIRD_PARTY_NOTICES.md).

Điểm đáng chú ý về bảo mật:

- Toàn bộ là thư viện mã nguồn mở phổ biến, giấy phép cho phép (chủ yếu Apache-2.0).
- **iOS**: mọi phụ thuộc được link **tĩnh vào bên trong** `Promotion.xcframework` và giấu sau
  `@_implementationOnly` — host không kéo thêm gì, không có nguy cơ xung đột version.
- **Android**: host **thấy** Ktor, coroutines, AppCompat, Glide, Gson trên classpath (SDK dùng
  `implementation` nhưng metadata Maven đưa xuống runtime + compile transitively). Host cần rà
  version trùng lặp với chính app của mình.

---

## 8. Danh mục kiểm tra cho bên an ninh

Chạy trên bản sắp phát hành:

- [ ] Bản release **không** in log body/cURL (bắt gói xem không có `Authorization` trong logcat / Console).
- [ ] `AndroidManifest.xml` của AAR chỉ khai `INTERNET`.
- [ ] Không có endpoint hard-code nào ngoài `BASE_PATH` ghép sau `baseUrl` của host.
- [ ] Dump `SharedPreferences` / `NSUserDefaults` sau khi dùng thử: chỉ thấy cờ tính năng + theme.
- [ ] `release()` khi đăng xuất → API tiếp theo không dùng lại token cũ.
- [ ] Bật R8 ở app host, mở đủ 4 màn + widget: không `NoClassDefFoundError` / `AbstractMethodError`.
- [ ] Kiểm tra `.swiftinterface` của framework iOS chỉ import `Foundation / UIKit / SwiftUI / Swift`.
- [ ] Đối chiếu danh sách phụ thuộc với danh sách CVE nội bộ.

---

## 9. Báo lỗi bảo mật

Phát hiện lỗ hổng: báo qua kênh nội bộ của đội SDK (không mở issue công khai, không đính kèm token
thật trong mô tả). Ghi kèm `X-Request-ID` của request lỗi và version SDK
(`vn.viettelpay.library:promotion:<version>` / `CFBundleShortVersionString` của framework).
