# Tài liệu Thiết kế Chi tiết — TTCN Promotion SDK

| Mục | Giá trị |
|---|---|
| Tên sản phẩm | TTCN Promotion SDK (Android + iOS) |
| Phiên bản sản phẩm | `1.0.0` |
| Phiên bản tài liệu | 1.0 |
| Ngày phát hành | 2026-09-08 |
| Trạng thái | Trình nghiệm thu |
| Đối tượng đọc | QA, kỹ sư nghiệm thu nội bộ, kiến trúc sư, đội tích hợp phía host |
| Nguồn sự thật | Source code trong repo. Khi tài liệu lệch code → **tin code**, sửa tài liệu |

> **Quan hệ với các tài liệu khác.** File này là bản thiết kế **hợp nhất** dùng để nghiệm thu: nó
> mô tả *cái gì được xây, xây thế nào, kiểm chứng ở đâu*. Các guide chuyên đề trong `docs/common/`,
> `docs/android/`, `docs/ios/` là bản chi tiết dành cho người sửa code; tài liệu này trích dẫn và
> trỏ tới chúng thay vì chép lại.

## Mục lục

<!-- toc -->
- [1. Tổng quan sản phẩm](#1-tổng-quan-sản-phẩm)
  - [1.1. Mục tiêu](#11-mục-tiêu)
  - [1.2. Chiến lược kỹ thuật cốt lõi](#12-chiến-lược-kỹ-thuật-cốt-lõi)
  - [1.3. Sản phẩm phát hành](#13-sản-phẩm-phát-hành)
  - [1.4. Phạm vi](#14-phạm-vi)
  - [1.5. Thuật ngữ](#15-thuật-ngữ)
- [2. Ràng buộc & giả định](#2-ràng-buộc--giả-định)
  - [2.1. Ràng buộc kỹ thuật](#21-ràng-buộc-kỹ-thuật)
  - [2.2. Giả định về môi trường host](#22-giả-định-về-môi-trường-host)
- [3. Kiến trúc tổng thể](#3-kiến-trúc-tổng-thể)
  - [3.1. Phân tầng](#31-phân-tầng)
  - [3.2. Đường đi một lời gọi từ host](#32-đường-đi-một-lời-gọi-từ-host)
  - [3.3. Bảng phân rã thành phần](#33-bảng-phân-rã-thành-phần)
  - [3.4. Cấu trúc repo](#34-cấu-trúc-repo)
- [4. Thiết kế bề mặt công khai](#4-thiết-kế-bề-mặt-công-khai)
  - [4.1. Hai chế độ dùng](#41-hai-chế-độ-dùng)
- [5. Thiết kế dữ liệu](#5-thiết-kế-dữ-liệu)
  - [5.1. Ba lớp model, ba vai trò](#51-ba-lớp-model-ba-vai-trò)
  - [5.2. Envelope chung](#52-envelope-chung)
  - [5.3. Cấu hình `Json` — hai cờ bắt buộc](#53-cấu-hình-json--hai-cờ-bắt-buộc)
  - [5.4. Hai quy ước đã chốt về DTO public](#54-hai-quy-ước-đã-chốt-về-dto-public)
- [6. Thiết kế tích hợp backend](#6-thiết-kế-tích-hợp-backend)
  - [6.1. Danh mục endpoint](#61-danh-mục-endpoint)
  - [6.2. Header mỗi request](#62-header-mỗi-request)
  - [6.3. Timeout & engine](#63-timeout--engine)
  - [6.4. Token là pull, không phải push](#64-token-là-pull-không-phải-push)
  - [6.5. Thử lại khi 401 — một lần, có single-flight](#65-thử-lại-khi-401--một-lần-có-single-flight)
  - [6.6. Thread](#66-thread)
- [7. Thiết kế luồng nghiệp vụ](#7-thiết-kế-luồng-nghiệp-vụ)
  - [7.1. Khởi tạo phiên](#71-khởi-tạo-phiên)
  - [7.2. Màn "Ưu đãi của tôi" (E1)](#72-màn-ưu-đãi-của-tôi-e1)
  - [7.3. Chi tiết ưu đãi + "Áp dụng" (E2)](#73-chi-tiết-ưu-đãi--áp-dụng-e2)
  - [7.4. Widget checkout + "Chọn ưu đãi" (E5 → E4 → E3)](#74-widget-checkout--chọn-ưu-đãi-e5--e4--e3)
  - [7.5. Feature flag (E6)](#75-feature-flag-e6)
  - [7.6. Hết hạn token giữa phiên](#76-hết-hạn-token-giữa-phiên)
- [8. Thiết kế tầng hiển thị](#8-thiết-kế-tầng-hiển-thị)
  - [8.1. Store dùng chung](#81-store-dùng-chung)
  - [8.2. Android](#82-android)
  - [8.3. iOS](#83-ios)
  - [8.4. Ánh xạ màn hình](#84-ánh-xạ-màn-hình)
- [9. Thiết kế xử lý lỗi](#9-thiết-kế-xử-lý-lỗi)
  - [9.1. Bốn tầng](#91-bốn-tầng)
  - [9.2. Bảng mã lỗi](#92-bảng-mã-lỗi)
  - [9.3. Ánh xạ transport → domain](#93-ánh-xạ-transport--domain)
  - [9.4. Quy tắc hiển thị — chỉ popup hoặc im lặng](#94-quy-tắc-hiển-thị--chỉ-popup-hoặc-im-lặng)
  - [9.5. Sáu nhánh lỗi phơi ra host (headless)](#95-sáu-nhánh-lỗi-phơi-ra-host-headless)
- [10. Thiết kế feature flag / kill-switch](#10-thiết-kế-feature-flag--kill-switch)
  - [10.1. Sáu cờ](#101-sáu-cờ)
  - [10.2. Ba quyết định thiết kế](#102-ba-quyết-định-thiết-kế)
  - [10.3. Hai tầng gác](#103-hai-tầng-gác)
- [11. Thiết kế lưu trữ cục bộ](#11-thiết-kế-lưu-trữ-cục-bộ)
- [12. Đồng thời & hạ tầng nền tảng](#12-đồng-thời--hạ-tầng-nền-tảng)
- [13. Bảo mật & quyền riêng tư](#13-bảo-mật--quyền-riêng-tư)
- [14. Hiệu năng & giới hạn](#14-hiệu-năng--giới-hạn)
- [15. Thiết kế cho kiểm thử](#15-thiết-kế-cho-kiểm-thử)
- [16. Rủi ro & điểm lệch đã biết](#16-rủi-ro--điểm-lệch-đã-biết)
- [17. Ma trận truy vết yêu cầu](#17-ma-trận-truy-vết-yêu-cầu)
- [18. Tham chiếu](#18-tham-chiếu)
<!-- /toc -->

---

## 1. Tổng quan sản phẩm

### 1.1. Mục tiêu

Cung cấp cho app host (ví dụ Viettel Money) một bộ thư viện **ưu đãi/voucher** dùng được ngay:
danh sách ưu đãi của khách, xem chi tiết, chọn ưu đãi cho đơn hàng, đối soát & tạo phiên sử dụng
ưu đãi tại bước thanh toán — trên **cả Android lẫn iOS**, với **một** bản nghiệp vụ duy nhất.

### 1.2. Chiến lược kỹ thuật cốt lõi

**Headless core + native UI.** Toàn bộ nghiệp vụ (data + domain) và cả **logic hiển thị**
(store State/Intent) nằm trong một lõi **Kotlin Multiplatform** (`:promotionLogic`); mỗi nền tảng chỉ
dựng phần vẽ bằng công nghệ native của mình (Android XML View, iOS UIKit).

Hệ quả cần nhớ khi nghiệm thu:

- Một quy tắc nghiệp vụ sai thì sai **giống nhau** ở hai nền tảng — và sửa một lần là hết ở cả hai.
- Một lỗi *chỉ xuất hiện ở một nền tảng* gần như chắc chắn nằm ở tầng vẽ, không phải ở nghiệp vụ.
- Test nghiệp vụ viết một lần trong `commonTest` và **chạy trên cả JVM lẫn Kotlin/Native**.

### 1.3. Sản phẩm phát hành

| Nền tảng | Artifact | Cách phân phối |
|---|---|---|
| Android | AAR qua Maven — `vn.viettelpay.library:promotion:1.0.0` (kéo theo `:promotionLogic`) | JFrog Artifactory nội bộ |
| iOS | `Promotion.xcframework` (dynamic framework, module `PRM`) | Zip **có version** trên repo generic Artifactory `vdo-ios-frameworks/Martech/Promotion/<version>/`, host tiêu thụ bằng SPM `binaryTarget` |

### 1.4. Phạm vi

**Trong phạm vi:** 4 màn hình + 1 widget checkout, 5 API nghiệp vụ, feature flag/kill-switch,
theming theo brand host, headless API cho host tự dựng UI, xử lý lỗi & hết hạn token.

**Ngoài phạm vi (bản 1.0.0):** thanh toán (host tự làm, SDK chỉ trả kết quả ưu đãi), đăng nhập/cấp
token (host tự làm), lưu trữ cơ sở dữ liệu cục bộ, đa ngôn ngữ ngoài `vi-VN`, Compose Multiplatform /
SwiftUI, obfuscate AAR.

### 1.5. Thuật ngữ

| Thuật ngữ | Nghĩa trong tài liệu này |
|---|---|
| **Host** | App của đối tác nhúng SDK |
| **Voucher** | Ưu đãi khách hàng **đã sở hữu** |
| **Campaign / offer** | Chương trình ưu đãi công khai, khách **chưa sở hữu** nhưng đơn hàng có thể đủ điều kiện |
| **Redemption session** | Phiên sử dụng ưu đãi do backend tạo, gắn với một đơn hàng |
| **Headless** | Dùng SDK mà không dùng màn hình của SDK — host tự vẽ, gọi `PromotionSDK.api` |
| **Store** | Lớp logic hiển thị dùng chung (`PRMStore<State, Intent>`) ở lõi KMP |
| **Kill-switch** | Cờ `PROMOTION.ENABLE_ALL` — tắt là toàn bộ SDK ngừng phục vụ |
| **Parity** | Ràng buộc: bề mặt Android và iOS phải trùng tên và trùng thứ tự |

---

## 2. Ràng buộc & giả định

### 2.1. Ràng buộc kỹ thuật

| # | Ràng buộc | Vì sao |
|---|---|---|
| RB-1 | Domain **không** biết Android/iOS, **không** biết Ktor/kotlinx.serialization | Giữ nghiệp vụ chạy được trên mọi target, test không cần thiết bị |
| RB-2 | Type của `:promotionLogic` **không** được lọt vào chữ ký public của SDK | Host không được phụ thuộc nội bộ; iOS sẽ fail build nếu vi phạm |
| RB-3 | Không thêm thư viện/plugin mới nếu chưa được yêu cầu | Kiểm soát bề mặt phụ thuộc mà host phải gánh |
| RB-4 | Không dùng annotation processor (kapt/KSP), không Room/SQLDelight | Kotlin/Native không có dynamic proxy; DB không cần cho bài toán này |
| RB-5 | Bề mặt public Android ↔ iOS phải **cùng tên, cùng thứ tự** | Một tài liệu tích hợp đọc được cho hai nền tảng; giảm lỗi lệch |
| RB-6 | Lõi **không chứa chuỗi hiển thị tiếng Việt** | Copy và locale là việc của tầng UI |
| RB-7 | Không log token; log body/cURL chỉ bật ở bản debug | Tránh rò rỉ `Authorization` trong log sản phẩm |

### 2.2. Giả định về môi trường host

- Host tự quản lý phiên đăng nhập và **tự refresh** access token (token sống ≈ 15 phút).
- Host có tài khoản đọc Artifactory nội bộ để kéo artifact.
- Android: host là ứng dụng AndroidX, `minSdk ≥ 24`. iOS: `iOS ≥ 13.0`, UIKit.
- Backend BFF khả dụng tại `baseUrl` host truyền vào lúc khởi tạo.

---

## 3. Kiến trúc tổng thể

### 3.1. Phân tầng

```
┌──────────────────────────────┐   ┌──────────────────────────────┐
│ PRESENTATION — Android       │   │ PRESENTATION — iOS           │
│ Fragment/View ←→ ViewModel   │   │ ViewController ←→ ViewModel  │
│ XML View, Data/View Binding  │   │ UIKit XIB, callback thuần    │
└──────────────┬───────────────┘   └──────────────┬───────────────┘
               │        dispatch(Intent) / render(State)
               └──────────────┬───────────────────┘
                              ▼
      ┌────────────────────────────────────────────────┐
      │ PRESENTATION dùng chung — :promotionLogic      │
      │   PRMStore<State, Intent> theo từng màn        │
      └──────────────────────▲─────────────────────────┘
                             │ gọi UseCase, nhận domain model
      ┌──────────────────────┴─────────────────────────┐
      │ DOMAIN — :promotionLogic / commonMain          │
      │   UseCase → Repository (interface) → Model     │
      │   Thuần Kotlin                                 │
      └──────────────────────▲─────────────────────────┘
                             │ implement interface, map DTO → domain
      ┌──────────────────────┴─────────────────────────┐
      │ DATA — :promotionLogic / commonMain            │
      │   RepositoryImpl → RemoteDataSource (Ktor)     │
      │   DTO, ApiService, HttpClient, LocalDataSource │
      └────────────────────────────────────────────────┘
                             │ expect / actual
              ┌──────────────┴──────────────┐
              ▼                             ▼
      androidMain                       iosMain
      OkHttp engine                     Darwin (NSURLSession)
      SharedPreferences                 NSUserDefaults
      ReentrantLock                     NSRecursiveLock
```

**Quy tắc phụ thuộc:** luôn hướng vào trong. Presentation → Domain ← Data. Domain là trung tâm và
không phụ thuộc ai.

### 3.2. Đường đi một lời gọi từ host

```
HOST APP (đối tác)
   │  gọi THẲNG entry tĩnh — không cần lớp bọc trung gian
   ▼
PromotionSDK                 ← entry công khai; chữ ký sạch, không lộ type lõi
   ├─ vòng đời   initialize · release · isInitialized · updateOrderInfo · configure(theme)
   ├─ màn hình   openMyPromotion · openPromotionDetail · openChoosePromotion · PRMEndowView
   ├─ headless   api: PromotionSDKApi
   └─ sự kiện    PromotionSDKCallback
   ▼
PromotionSDKApi              ← RANH GIỚI: map model lõi → DTO public. KHÔNG chứa nghiệp vụ
   ▼
PromotionContainer → SdkDi   ← DI tự viết: dựng & giữ UseCases / Repository / DataSource / Preferences
   ▼
DOMAIN ← DATA                ← UseCase → Repository → RemoteDataSource (Ktor)
```

### 3.3. Bảng phân rã thành phần

| Thành phần | Vị trí | Trách nhiệm | Ai gọi |
|---|---|---|---|
| `PromotionSDK` | `AndroidPromotionSDK/entry/`, `iosPromotionSDK/Entry/` | Entry công khai: vòng đời, mở màn, phát sự kiện | Host |
| `PromotionSDKApi` | `entry/api/`, `Entry/API/` | Headless: uỷ quyền use case + map sang DTO public | Host (headless) |
| `PromotionSDKCallback` | `entry/` | 3 sự kiện: `onVoucherApplied`, `onServiceSelected`, `onExpireToken` | SDK → Host |
| `PRMEndowView` | `ui/feature/endowview/` (Android), `Endow/` (iOS) | Widget ưu đãi ở màn thanh toán | Host |
| `PromotionSDKTheme` | `ui/theme/` | Token màu/typography theo brand host | Host |
| `PRMStore<S, I>` | `promotionLogic/presentation/` | Logic hiển thị dùng chung: nhận Intent, gọi use case, phát State/Effect | UI hai nền tảng |
| `PromotionUseCases` | `promotionLogic/domain/usecase/` | Facade nghiệp vụ: gác cờ, bọc lỗi thành `PromotionResult` | `PromotionSDKApi`, Store |
| `PromotionFeatureGate` | `domain/usecase/` | Nguồn sự thật duy nhất cho kill-switch (UI lẫn headless) | UseCase, Router |
| `PromotionRepository(Impl)` | `domain/repository/`, `data/repository/` | Hợp đồng dữ liệu + map DTO → domain | UseCase |
| `PromotionRemoteDataSource` | `data/remote/` | `apiCall{}`: map lỗi, thử lại 401, ép xuống IO thread | Repository |
| `KtorPromotionApiService` | `data/remote/` | Gọi HTTP thật (viết tay, không sinh tự động) | RemoteDataSource |
| `PromotionHttpClient` | `data/remote/` | Cấu hình Ktor: timeout, JSON, header, logging | DI |
| `PromotionPreferences` | `data/local/` | Kho key-value: cache feature flag, theme | Repository |
| `PromotionContainer` / `SdkDi` | `di/` | Composition root, vòng đời đồ thị phụ thuộc | `PromotionSDK` |

### 3.4. Cấu trúc repo

```
promotionLogic/       📦 Lõi KMP (phát hành: vn.viettelpay.library:promotionLogic)
AndroidPromotionSDK/  📦 SDK Android (phát hành: vn.viettelpay.library:promotion)
iosPromotionSDK/      📦 SDK iOS (phát hành: Promotion.xcframework) — project Xcode, KHÔNG phải module Gradle
androidApp/, iosApp/     App demo/host để nghiệm thu tích hợp
scripts/                 build-android.sh · build-ios.sh · test-report.sh
docs/                    Tài liệu
```

---

## 4. Thiết kế bề mặt công khai

Nguyên tắc một dòng: **chỉ `Entry` mới public.**

| | Bề mặt public | Mọi thứ khác |
|---|---|---|
| Android | `com.ttcn.prm.entry.**` + `ui.theme.**` + `ui.feature.endowview` | `internal` |
| iOS | `iosPromotionSDK/Entry/**` trong module `PRM` | không có `public` |

Cơ chế cưỡng chế (không phải quy ước lỏng):

- **Android** — SDK khai `implementation(projects.promotionLogic)`, nên `com.ttcn.promotionsdk.*`
  **không** nằm trên compile classpath của host: host import là lỗi biên dịch.
- **iOS** — `@_implementationOnly import PRMKotlinBridge`; nếu type Kotlin lọt vào chữ ký public thì
  nó bị ghi vào `.swiftinterface` và **app host không build được**.

Chi tiết đầy đủ (chữ ký từng hàm, 10 DTO public, song ánh Android↔iOS): [`../common/PublicApi.md`](../common/PublicApi.md).

### 4.1. Hai chế độ dùng

| Chế độ | Host làm gì | Ai vẽ UI |
|---|---|---|
| **UI mode** | `PromotionSDK.openMyPromotion(...)`, nhúng `PRMEndowView` | SDK |
| **Headless** | `PromotionSDK.api.getVouchers(...)` … | Host |

Cả hai đi qua **cùng** `PromotionUseCases`, nên gác cờ và chuẩn hoá lỗi giống hệt nhau — không có
cửa sau nào bỏ qua kill-switch.

---

## 5. Thiết kế dữ liệu

### 5.1. Ba lớp model, ba vai trò

| Lớp | Ví dụ | Ở đâu | Ai thấy |
|---|---|---|---|
| **DTO mạng** | `SearchCustomerVouchersResponse` | `data/dto/<nhóm>/` | Chỉ tầng Data |
| **Domain model** | `VoucherDetail`, `EligibleOffer` | `domain/model/<nhóm>/` | Data ↔ Domain ↔ Store |
| **DTO public** | `PromotionVoucherDetail` | `entry/api/`, `Entry/API/` | Host |

Ranh giới: `data/dto/**Mapper.kt` map DTO → domain; `PromotionSDKApi` map domain → DTO public.
**Không** có đường tắt nào bỏ qua hai chỗ này.

### 5.2. Envelope chung

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

`requireData()` ném `PromotionException` khi `success == false` **hoặc** `status` ngoài `200..299` —
kể cả khi HTTP là 200. Đây là điểm chặn cho lỗi nghiệp vụ ẩn trong HTTP 200.

### 5.3. Cấu hình `Json` — hai cờ bắt buộc

```kotlin
Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }
```

Bỏ `encodeDefaults` thì request `createRedemption` thiếu `sessionOptions`/`timeoutSeconds` mà code
vẫn biên dịch và test map-xuống vẫn xanh — chỉ test **payload gửi lên** mới bắt được. Có test khoá
hành vi này.

### 5.4. Hai quy ước đã chốt về DTO public

1. **Ngày tháng là chuỗi thô của server.** Parse ở tầng này thì định dạng lạ trả `null`, host không
   phân biệt được "vô thời hạn" với "server trả sai định dạng". Định dạng ngày là việc của UI.
2. **Viết tắt để hoa** — `imageURL`, `bannerURL`, `logoURL` (theo Swift API Design Guidelines);
   đổi lại iOS nhường ở `vouchers` / `isLastPage` / `description` / `page` / `size`.

---

## 6. Thiết kế tích hợp backend

### 6.1. Danh mục endpoint

`BASE_PATH = promotion/promotion-vtm-bff/api/v1/vtm`

| # | Nghiệp vụ | Method + path | Use case | Tài liệu API |
|---|---|---|---|---|
| E1 | Tìm voucher của khách | `GET {BASE_PATH}/customer-vouchers` | `SearchCustomerVouchersUseCase` | 3.5.10 |
| E2 | Chi tiết voucher | `GET {BASE_PATH}/customer-vouchers/{voucherId}` | `GetCustomerVoucherDetailUseCase` | 3.5.9 |
| E3 | Tạo phiên sử dụng ưu đãi | `POST {BASE_PATH}/redemptions/sessions` | `CreateRedemptionSessionUseCase` | 3.5.2 |
| E4 | Đối soát ưu đãi cộng gộp | `POST {BASE_PATH}/redemptions/validate/stackable-discounts` | `ValidateStackableDiscountsUseCase` | 3.5.3 |
| E5 | Tìm ưu đãi đủ điều kiện | `POST {BASE_PATH}/redemptions/eligible` | `FindEligibleCampaignsUseCase` | 3.5.4 |
| E6 | Lấy cờ tính năng | `POST {BASE_PATH}/feature-flag/list` | `PromotionFeatureFlagUseCases` | — |

Bản PDF đặc tả nằm ở [`../api/`](../api/).

> **E1 vs E5 — chọn đúng hàm.** `searchVouchers` (E1) trả voucher khách **đã sở hữu**, không xét đơn
> hàng. `findEligible` (E5) trả ưu đãi **đủ điều kiện cho đơn hiện tại**, chia hai nhóm phân trang
> độc lập `myOffers` (đã sở hữu) + `otherOffers` (campaign công khai).

### 6.2. Header mỗi request

| Header | Nguồn | Ghi chú |
|---|---|---|
| `Authorization` | `tokenSource.currentToken()` | Tự thêm tiền tố `Bearer ` nếu chưa có |
| `X-Request-ID` | `kotlin.uuid.Uuid` | **UUID mới mỗi request** — dùng để đối soát log với backend |
| `Accept-Language` | cấu hình phiên | Mặc định `vi-VN` |
| `Accept` / `Content-Type` | — | `application/json` |

### 6.3. Timeout & engine

`connect` / `request` / `socket` = **30 giây**. Engine do Ktor tự chọn theo artifact có trên
classpath: OkHttp ở `androidMain`, Darwin (NSURLSession) ở `iosMain` — không cần `expect/actual`.

### 6.4. Token là **pull**, không phải push

SDK **không giữ bản sao token**. `currentToken()` được gọi lại trong `defaultRequest { }` ở **mỗi**
request.

```
interface PromotionTokenSource {
    fun currentToken(): String?
    fun refreshToken(onResult: (Boolean) -> Unit)   // mặc định: onResult(false)
}
```

Lý do thiết kế: token host sống ≈15 phút và host tự refresh. Nếu SDK cache token nhận lúc
`initialize`, thì ngay sau lần refresh đầu tiên của host, SDK cầm một chuỗi đã chết trong khi phiên
đăng nhập vẫn sống → **mọi API trả 401 dù người dùng chưa hề đăng xuất**.

`refreshToken` trả `Boolean` chứ không trả token mới là **cố ý**: token vào SDK theo đúng một đường
là `currentToken()`, không có đường thứ hai để nhầm.

### 6.5. Thử lại khi 401 — một lần, có single-flight

```
block()  ──401──▶  TokenRefreshGate.refresh()  ──true──▶  block()   (lần 2, token đã mới)
                            │
                            └──false──▶ ném tiếp → TOKEN_EXPIRED → onExpireToken()
```

| Cơ chế | Thiết kế |
|---|---|
| Nhận 401 theo **hai đường** | `ResponseException` (HTTP 4xx) **và** envelope `{"status":401}` trên HTTP 200 |
| Chỉ thử lại **một lần** | Refresh xong vẫn 401 = phiên chết thật, không lặp vô hạn |
| **Single-flight** | `TokenRefreshGate` đánh số `generation`; mở một màn là vài request song song, thiếu chốt này host ăn nhiều lần refresh cho cùng một sự kiện |
| **Hai lớp chắn cho code host** | Quá **15 giây** host chưa gọi callback ⇒ coi như hỏng (nếu không `Mutex` treo và **mọi** API của SDK chết theo); callback gọi hai lần chỉ tính lần đầu |
| Feature flag **không** thử lại | Cờ fail-open; 401 ở đó chỉ rơi về cache |

### 6.6. Thread

`apiCall` bọc `withContext(ioDispatcher)`. Lý do: Ktor chạy pipeline phía client trong context của
coroutine gọi nó, mà Android truyền thẳng `viewModelScope` (`Dispatchers.Main.immediate`) — không có
`withContext` thì `defaultRequest { }`, **kể cả lambda cấp token của host**, chạy trên main thread.

> **Hệ quả bắt buộc cho host:** `currentToken()` bị gọi từ thread nền ⇒ phải thread-safe, và
> **không** được `@MainActor` ở Swift.

---

## 7. Thiết kế luồng nghiệp vụ

Ký hiệu: `H` = host, `S` = SDK (UI), `C` = lõi (store/use case), `B` = backend.

### 7.1. Khởi tạo phiên

```
H  PromotionSDK.initialize(context, options{ session{tokenSource, baseUrl, language, environment},
                                             availableServices, theme, callback })
S  → PromotionContainer.initialize(context, PromotionSDKConfig)
C  → dựng đồ thị: HttpClient(baseUrl) · Repository · UseCases · Preferences · FeatureGate
S  ← isInitialized() == true
H  PromotionSDK.updateOrderInfo(orderId, productId, orderValue, …)   ← trước khi vào màn có voucher
```

Ràng buộc:
- `baseUrl` / `environment` / `language` / `theme` là **cấu hình tĩnh**: đặt một lần rồi dùng lại.
- Đăng nhập lại ⇒ gọi lại `initialize`. **Không có** `updateToken` / `updateSession` (đã bỏ).
- Order/dịch vụ **động** đi qua `updateOrderInfo` → ghi vào `PromotionMutableContext`, lõi đọc lại ở
  **mỗi** request; không cần `initialize` lại.
- `release()` xoá dữ liệu phiên nhưng **giữ** cấu hình tĩnh + theme đã lưu.

### 7.2. Màn "Ưu đãi của tôi" (E1)

```
H  openMyPromotion(activity, containerViewId?)
S  → FeatureGate.check(VOUCHER_LIST)   ── tắt ──▶ popup PRM_MOB_021 + onFeatureDisabled → DỪNG
S  → mở MyPromotionFragment / MyPromotionViewController
S  → dispatch(LoadInitialIfNeeded)
C  MyPromotionStore → SearchCustomerVouchersUseCase → Repository → RemoteDataSource
B  ← GET /customer-vouchers?keyword&serviceCode&tab&page&size
C  DTO → domain → State{ vouchers, tabs, isLastPage, isLoading, errorCode }
S  render(state): danh sách / shimmer / empty-view
```

Phân trang bằng `page`/`size`; kéo-để-tải-lại và tải-thêm đều đi qua Intent của cùng store nên
hai nền tảng có **cùng** hành vi latest-wins khi response về muộn.

### 7.3. Chi tiết ưu đãi + "Áp dụng" (E2)

```
H  openPromotionDetail(voucherId, activity, returnVoucherOnApply = true, hostHandlesDismiss = false, onVoucherApplied)
S  → FeatureGate.check(VOUCHER_DETAIL)  ── tắt ──▶ popup + DỪNG
C  GetCustomerVoucherDetailUseCase → E2 → VoucherDetail
S  render; nút "Áp dụng" mở khoá sau khi detail về
H  ← onVoucherApplied(PromotionVoucherDetail)   ← trả nguyên object, host KHÔNG phải gọi API lần nữa
S  → tự pop màn (trừ khi hostHandlesDismiss = true)
```

| `returnVoucherOnApply` | Nhãn nút | Bấm thì |
|---|---|---|
| `true` (mặc định) | "Áp dụng" | trả object về `onVoucherApplied` rồi SDK tự đóng màn |
| `false` | "Dùng ngay" | mở bottom sheet chọn dịch vụ → `onServiceSelected` |

`hostHandlesDismiss = true`: callback vẫn chạy **trước**, nhưng SDK để nguyên màn để host tự xử lý
(hỏi xác nhận, animation riêng, đẩy thẳng sang màn khác).

### 7.4. Widget checkout + "Chọn ưu đãi" (E5 → E4 → E3)

```
H  nhúng <PRMEndowView> vào layout màn thanh toán;  updateOrderInfo(...) trước đó
S  EndowStore → FindEligibleCampaignsUseCase → E5 (myOffers + otherOffers)
S  widget hiển thị: có ưu đãi / chưa áp / không khả dụng
   │
   └─ user bấm widget → PRMEndowView tự gọi PromotionSDK.openChoosePromotion(activity, this)
S     màn "Chọn ưu đãi" DÙNG LẠI dữ liệu widget đã tải (không gọi E5 lần hai), pre-select ưu đãi đang áp
S     user chọn → "Áp dụng" → ValidateStackableDiscountsUseCase → E4
C        không hợp lệ → popup lỗi, KHÔNG đóng màn
C        hợp lệ      → đẩy AppliedDiscount ngược về widget + đóng màn
   │
H  bấm nút thanh toán của host → PRMEndowView.confirmRedemption(onSuccess, onError)
S     → CreateRedemptionSessionUseCase → E3 → sessionId, totalDiscount, finalAmount
H  ← onSuccess(...) → host tiếp tục luồng thanh toán của mình
```

Điểm thiết kế: **host không chạm** `EligibleOffer` (type của lõi) và không cần biết
`ChoosePromotionFragment` tồn tại — toàn bộ wiring nằm trong SDK.

### 7.5. Feature flag (E6)

```
S  init → đọc cache đồng bộ (KHÔNG gọi mạng) → phục vụ ngay
S  refresh nền / refreshFeatureFlags() → E6 → cập nhật cache
   API hỏng → giữ cờ đang cache;  chưa từng có cache → BẬT HẾT (fail-open)
```

### 7.6. Hết hạn token giữa phiên

```
bất kỳ API nào của màn SDK → 401
   → TokenRefreshGate.refresh() → host.refreshToken { true/false }
     true  → chạy lại request, người dùng không thấy gì
     false → errorCode = TOKEN_EXPIRED
             → hiển thị theo quy tắc §9
             → PromotionSDKCallback.onExpireToken()   ← host tự điều hướng (login lại)
```

---

## 8. Thiết kế tầng hiển thị

### 8.1. Store dùng chung

Mỗi màn có đúng một cặp ở lõi:

```
presentation/<feature>/
├── XxxContract.kt   # data class XxxState + sealed interface XxxIntent + model hiển thị + mapper
└── XxxStore.kt      # PRMStore<XxxState, XxxIntent> — gọi use case, phát State/Effect
```

```
User tương tác → dispatch(Intent)
UI đọc state   → render(state)        // nguồn sự thật, phát lại được
UI đọc effects → popup / điều hướng   // PRMEffect: sự kiện MỘT LẦN
```

**Không** dựng lại `UiState`/`Action` riêng cho mỗi nền tảng — bản trước có, chép gần 1-1 `State`/
`Intent` ở bốn màn, và đã bị bỏ.

### 8.2. Android

`PRMStoreViewModel<S, I>` bọc store; mỗi màn có một subclass ba dòng (bắt buộc phải là subclass:
`by viewModels()` lấy tên class làm khoá, generic bị erase nên dùng chung một class là hai màn đè
khoá nhau và nổ `ClassCastException` lúc chạy).

### 8.3. iOS

MVVM + **Builder** (lắp ráp VC+VM+Router) + **Router** (điều hướng); View↔VM ràng buộc bằng
**callback thuần** — không Combine, không RxSwift.

> **Một điểm iOS buộc phải khác:** Android collect thẳng `store.effects`; Swift không collect được
> vì `effects` là default member của interface Kotlin (Kotlin/Native chỉ đặt default member lên
> *protocol*, mà protocol thì erase generic). Nên `PRMStoreViewModel` bên iOS **tự suy effect từ
> `errorCode` trong state** — cùng ngữ nghĩa "một lần rồi `ConsumeError`".

### 8.4. Ánh xạ màn hình

| Tính năng | Android | iOS | Use case |
|---|---|---|---|
| Ưu đãi của tôi | `MyPromotionFragment` | `MyPromotionViewController` | E1 |
| Tìm kiếm ưu đãi | `SearchMyPromotionFragment` | `SearchMyPromotionViewController` | E1 (kèm `keyword`) |
| Chi tiết ưu đãi | `PromotionDetailFragment` | `PromotionDetailViewController` | E2 |
| Chọn ưu đãi | `ChoosePromotionFragment` | `ChoosePromotionViewController` | E5, E4, E3 |
| Widget checkout | `PRMEndowView` | `PRMEndowView` | E5 |

---

## 9. Thiết kế xử lý lỗi

### 9.1. Bốn tầng

```
Ktor  →  ResponseException | HttpRequestTimeoutException | IOException
   │  apiCall  ── map ──▶  PromotionException | NetworkException      (exception domain)
   ▼
Repository & UseCase  ── truyền thẳng, không bắt ──▶
   │  PromotionUseCases.headlessCall  ── bọc ──▶  PromotionResult.Failure
   ▼
UI  →  Success → render;  Failure → hiển thị theo errorCode
```

**`PromotionUseCases` là nơi exception dừng lại.** Public API **không ném** exception nghiệp vụ —
chỉ `CancellationException` được rethrow để structured concurrency của host còn hoạt động.

### 9.2. Bảng mã lỗi

| Mã | Sinh ra khi | Hiển thị |
|---|---|---|
| `missing_customer_id` | Thiếu định danh khách | Popup |
| `no_result` | Server trả `data: null` | Danh sách → rỗng; chi tiết/validate/redemption → `ParseFailed` |
| `INSUFFICIENT_BUDGET` | Ngân sách campaign hết | Popup |
| `error_general` | Lỗi không phân loại được | Popup |
| `network_error` | Mất mạng / IO | Theo màn |
| `timeout` | Quá 30s | Theo màn |
| `TOKEN_EXPIRED` | HTTP 401 sau khi refresh thất bại | Popup + `onExpireToken()` |
| `PRM_MOB_021` | Tính năng bị cờ chặn | **SDK tự** hiện popup |

Quy tắc map 401: `Throwable.toErrorCode()` ưu tiên `PromotionException.httpStatus` — `401` **luôn**
ra `TOKEN_EXPIRED` bất kể server gửi `errorCode` gì; `403` (không đủ quyền, token còn hợp lệ)
**không** map sang mã này.

### 9.3. Ánh xạ transport → domain

| Bắt được | Ném ra |
|---|---|
| `PromotionException` | giữ nguyên |
| `ResponseException` (4xx/5xx) | `PromotionException` — parse error body lấy `code`/`message` của server |
| `HttpRequestTimeoutException` / `ConnectTimeoutException` / `SocketTimeoutException` | `NetworkException(TIMEOUT)` |
| `IOException` khác | `NetworkException(NETWORK_ERROR)` |

> ⚠️ **Thứ tự `catch` quan trọng:** cả ba loại timeout của Ktor đều kế thừa `IOException`. Bắt
> `IOException` trước sẽ nuốt mất timeout và báo sai mã lỗi.

### 9.4. Quy tắc hiển thị — chỉ popup hoặc im lặng

SDK **đã bỏ hẳn toast** ở cả hai nền tảng (bản trước có cổng bật/tắt toast mặc định TẮT, khiến phần
lớn lỗi bị nuốt im lặng trong khi nhìn code lại tưởng có báo).

| Cách | Dùng khi | Android | iOS |
|---|---|---|---|
| **Popup** | user vừa chủ động bấm và đang chờ kết quả; hoặc tính năng bị cờ chặn | `PRMBaseFragment.showErrorDialog` | `PRMBaseViewController.showErrorDialog(_:)` |
| **Không hiện gì** | màn đã có empty-view / shimmer / list cũ nói thay | nhánh `ShowError -> Unit` | `_ = error` |

Đang dùng popup ở: validate hỏng khi bấm "Áp dụng", kéo-để-tải-lại hỏng ở màn "Chọn ưu đãi", và mọi
đường bị feature flag chặn.

### 9.5. Sáu nhánh lỗi phơi ra host (headless)

`NetworkFailure(code, message)` · `SessionExpired` · `Timeout` · `ParseFailed` · `FeatureDisabled` ·
`Unknown(error)` — giống nhau hai nền tảng (Kotlin: `sealed class`; Swift: `enum … LocalizedError`).

---

## 10. Thiết kế feature flag / kill-switch

### 10.1. Sáu cờ

| Cờ | Gác cái gì |
|---|---|
| `PROMOTION.ENABLE_ALL` | **Công tắc tổng** — tắt là mọi cờ con tắt theo |
| `PROMOTION.VOUCHER_LIST` | Màn danh sách + tìm kiếm (E1) |
| `PROMOTION.VOUCHER_DETAIL` | Màn chi tiết (E2) |
| `PROMOTION.VOUCHER_SELECTION` | Màn chọn ưu đãi / widget (E5) |
| `PROMOTION.VOUCHER_APPLY` | Đối soát ưu đãi (E4) |
| `PROMOTION.VOUCHER_REDEEM` | Tạo phiên sử dụng (E3) |

### 10.2. Ba quyết định thiết kế

1. **Fail-open.** Chưa `initialize` / chưa có cache / API hỏng ⇒ **bật hết**. Không hàm nào ném lỗi.
   Cờ tính năng không phải thứ để hiện lỗi cho người dùng.
2. **Cờ lạ ⇒ bật.** Tên cờ SDK chưa biết trả `true`: server chưa từng trả `false` cho nó nên không
   có căn cứ để tắt. Cùng luật với mapper — chỉ `enabled: false` mới tắt.
3. **Cache giữ giá trị thô**, không giữ bản đã áp `ENABLE_ALL`. Nhờ vậy server bật lại `ENABLE_ALL`
   thì các cờ con trở về đúng giá trị riêng thay vì kẹt `false`.

### 10.3. Hai tầng gác

| Tầng | Ai làm | Hành vi |
|---|---|---|
| **Bắt buộc** | SDK | Mọi điểm vào tự gác qua `PromotionFeatureGate`; bị chặn → SDK tự hiện popup `PRM_MOB_021`, host vẫn nhận `FeatureDisabled` **để dừng luồng thanh toán** |
| **Tuỳ chọn** | Host | `featureFlags()` / `isFeatureEnabled(...)` / `isSdkEnabled()` / `refreshFeatureFlags { }` để **ẩn entry point của chính host** thay vì để user bấm rồi ăn popup |

Ba hàm đầu đọc **cache đồng bộ**, không gọi mạng; `refreshFeatureFlags` gọi server rồi trả snapshot
trên **main thread**.

---

## 11. Thiết kế lưu trữ cục bộ

| Hạng mục | Thiết kế |
|---|---|
| Trừu tượng | `PromotionPreferences` (interface) + `SettingsPreferences` (thân **dùng chung** ở `commonMain`) |
| Android | `SharedPreferencesSettings` — cần `Context`, lấy từ `AndroidContextHolder` |
| iOS | `NSUserDefaultsSettings` — **suite riêng**, không dùng chung với host |
| Lưu gì | Cache 6 cờ feature flag; theme host đã cấu hình |
| **Không** lưu | Token, thông tin cá nhân, dữ liệu voucher, lịch sử giao dịch |
| Không có DB | Không Room, không SQLDelight — dữ liệu voucher luôn lấy tươi từ server |

Đã loại `multiplatform-settings-no-arg` vì nó **xoá prefs của app host**.

---

## 12. Đồng thời & hạ tầng nền tảng

Chỉ **ba** chỗ cần biết nền tảng, tất cả nằm ngoài Domain:

| Trừu tượng | androidMain | iosMain | Vì sao |
|---|---|---|---|
| `SdkLock` | `ReentrantLock` | `NSRecursiveLock` | `synchronized` là JVM-only; DI cần khoá **reentrant** vì `resolve()` gọi đệ quy |
| `createPreferences()` | `SharedPreferencesSettings` | `NSUserDefaultsSettings` | Android cần `Context` để mở `SharedPreferences` |
| `clearPlatformState()` | nhả `applicationContext` | no-op | Dọn khi `PromotionContainer.clear()` |

`currentEpochMillis()` từng là chỗ thứ tư, đã bỏ từ khi `kotlin.time.Clock` vào stdlib.
`SdkLock` **không** thay bằng `kotlinx.atomicfu.locks` được: tài liệu của nó ghi rõ *"not recommended
to use in libraries that other projects depend on"* và *"no ABI guarantees"*.

Cơ chế đồng thời khác đã thiết kế: **latest-wins** khi response về muộn, chặn tải-thêm khi đang
refresh, và single-flight cho refresh token (§6.5).

---

## 13. Bảo mật & quyền riêng tư

Chi tiết đầy đủ: [`../common/Security.md`](../common/Security.md). Tóm tắt để nghiệm thu:

| Hạng mục | Thiết kế |
|---|---|
| Quyền Android | Chỉ `android.permission.INTERNET` |
| Quyền iOS | Không xin quyền nào |
| Dữ liệu lưu cục bộ | Cờ tính năng + theme. **Không** token, **không** PII |
| Token trong bộ nhớ | SDK **không giữ bản sao**; đọc lại từ host mỗi request |
| Log | `LogLevel.BODY` và log cURL **chỉ bật ở bản debug** (Android build DEBUG; iOS cần biến môi trường `PROMOTION_SDK_DEBUG=1`) — cả hai đều lộ `Authorization` |
| Kênh truyền | HTTPS do host cấp qua `baseUrl`. SDK **không** tự cài certificate pinning |
| Obfuscate | AAR phát hành **không** obfuscate — xem rủi ro R-4 §16 |

---

## 14. Hiệu năng & giới hạn

| Hạng mục | Giá trị |
|---|---|
| Timeout mạng | 30s (connect / request / socket) |
| Phân trang mặc định | `size = 10`; `findEligible` phân trang **hai nhóm độc lập** |
| Gọi mạng khi mở màn "Chọn ưu đãi" từ widget | **0** — dùng lại dữ liệu widget đã tải |
| Đọc cờ tính năng | Đồng bộ từ cache, **không** chạm mạng |
| Thread | Mọi lời gọi mạng ép xuống `ioDispatcher`; callback trả về main thread |
| Kích thước | Android: AAR + lõi kéo theo Ktor/coroutines/AppCompat/Glide/Gson. iOS: một dynamic framework đã link tĩnh mọi thứ bên trong |

---

## 15. Thiết kế cho kiểm thử

| Hạng mục | Giá trị |
|---|---|
| Nơi đặt test nghiệp vụ | `promotionLogic/src/commonTest` — **43 file**, chạy trên **cả** JVM và Kotlin/Native |
| Lệnh | `./gradlew :promotionLogic:testAndroidHostTest` · `:promotionLogic:iosSimulatorArm64Test` · `./scripts/test-report.sh` |
| Test mạng | `MockEngine` dùng **đúng** `PromotionHttpClient.configure(...)` của production |
| Test storage | `MapSettings` làm delegate cho chính `SettingsPreferences` của production — không viết fake tay |
| Coverage | Kover, ngưỡng **LINE ≥ 93% · INSTRUCTION ≥ 92% · BRANCH ≥ 90%**, đặt sát dưới mức hiện tại để PR làm tụt là fail ngay |

Hai luật kiểm thử đặc thù của SDK này:

1. **Kiểm cả hai chiều.** Một test API tốt kiểm **payload gửi lên** lẫn **kết quả map xuống** —
   payload mới bắt được lỗi `encodeDefaults`/`explicitNulls`.
2. **Chạy cả hai target trước khi commit.** Kotlin/Native có khác biệt về freeze, thread và khởi tạo
   lazy mà JVM không lộ ra. "BUILD SUCCESSFUL" **không** đảm bảo có test nào đã chạy — phải đọc số
   trong `build/test-results/**/*.xml`.

---

## 16. Rủi ro & điểm lệch đã biết

| # | Rủi ro / điểm lệch | Mức | Cách xử lý hiện tại |
|---|---|---|---|
| R-1 | **Lệch Android ↔ iOS (N1)**: `AppliedDiscount` chỉ có ở Android; widget iOS là factory `createEndowView(...)`; `openChoosePromotion` là Android-only | Trung bình | Ghi rõ trong `PublicApi.md` và `InitParity.md`; host iOS gọi `api.validateDiscounts` nếu cần breakdown |
| R-2 | **`onExpireToken()` chưa áp dụng cho headless** `PromotionSDKApi` (`SessionExpired` chưa wiring) | Trung bình | Host headless phải tự bắt `PromotionSDKError.SessionExpired` |
| R-3 | Host Android **vẫn** thấy Ktor/coroutines/AppCompat/Glide/Gson trên compile classpath (khác iOS giấu tuyệt đối) | Thấp | Ghi trong Integration Guide §1; xung đột version xử lý bằng `resolutionStrategy` phía host |
| R-4 | AAR **không obfuscate** ⇒ decompile đọc được tên class | Thấp (đã cân nhắc) | Hàng rào là *hợp đồng*: `internal` + không phát hành sources.jar + resource private. Bật R8 trên chính SDK đã thử và **gỡ bỏ** vì rút gọn nhầm mapper Data Binding → host crash `AbstractMethodError` |
| R-5 | Đổi `SDK_GROUP` hoặc đổi tên class iOS = **breaking** với mọi host đang tích hợp | Cao nếu xảy ra | Bắt buộc bump major + thông báo đối tác (§ VersioningPolicy) |
| R-6 | Version Android (`SDK_VERSION`) và iOS (`MARKETING_VERSION`) đồng bộ **thủ công** | Trung bình | Có trong Release Checklist; sai số là lệch gói |
| R-7 | Còn chỗ app lệch tài liệu nghiệp vụ | Trung bình | Danh sách tại [`../common/TlnvGap.md`](../common/TlnvGap.md) — **đọc trước khi kết luận "bug"** |
| R-8 | Có **code chết đã biết**: `ChoosePromotionStore` có nhánh `isMultiSelection` mà không Intent nào bật được | Thấp | Ghi nhận trong TestingGuide; không tính là thiếu test |

---

## 17. Ma trận truy vết yêu cầu

Dùng cho nghiệm thu: mỗi tài liệu nghiệp vụ → màn hình → use case → endpoint → nơi kiểm chứng.

| Tài liệu nghiệp vụ (`docs/tlnv/`) | Tính năng | Màn Android / iOS | Use case | API | Test chính (`commonTest`) |
|---|---|---|---|---|---|
| MOB_000 Danh mục dùng chung | Theme, mã lỗi, danh mục | — | — | — | `PresentationSharedTest`, `VoucherStatusTest` |
| MOB_001 Ưu đãi của tôi | Danh sách + tìm kiếm | `MyPromotionFragment` / `MyPromotionViewController`, `SearchMyPromotion…` | `searchVouchers` | E1 | `MyPromotionStoreTest`, `MyPromotionBranchTest`, `SearchMyPromotionStoreTest`, `ResolveActiveTabTest` |
| MOB_002 Xem chi tiết | Chi tiết + Áp dụng | `PromotionDetailFragment` / `…ViewController` | `getVoucherDetail` | E2 | `PromotionDetailStoreTest`, `VoucherDetailFieldBranchTest`, `PromotionHtmlContentTest` |
| MOB_003 Đánh dấu đã sử dụng | *(chưa triển khai bản 1.0.0)* | — | — | — | — |
| MOB_004 Áp dụng ưu đãi | Chọn ưu đãi + widget + thanh toán | `ChoosePromotionFragment` / `…ViewController`, `PRMEndowView` | `findEligible`, `validateDiscounts`, `createRedemption` | E5, E4, E3 | `EligibleCampaignsTest`, `ChoosePromotionStoreTest`, `EndowStoreTest`, `ValidateDiscountsOutcomeTest`, `DiscountMapperTest` |
| — (yêu cầu vận hành) | Kill-switch | mọi điểm vào | `featureFlags.*` | E6 | `FeatureFlagTest`, `FeatureFlagGateTest` |
| — (yêu cầu vận hành) | Phiên & token | mọi API | — | mọi endpoint | `TokenPullPerRequestTest`, `TokenRefreshGateTest`, `TokenRefreshRetryTest` |

> **MOB_003** nằm trong bộ tài liệu nghiệp vụ nhưng **không** thuộc phạm vi bản 1.0.0 — xem §1.4.
> Cần đưa vào thì mở một mục riêng ở `docs/features/` trước khi code.

---

## 18. Tham chiếu

| Chủ đề | Tài liệu |
|---|---|
| Kiến trúc chi tiết | [`../common/Architecture.md`](../common/Architecture.md) |
| Cấu trúc thư mục | [`../common/ProjectStructure.md`](../common/ProjectStructure.md) |
| Bề mặt public đầy đủ | [`../common/PublicApi.md`](../common/PublicApi.md) |
| API lõi (headless) | [`../common/HeadlessAPI.md`](../common/HeadlessAPI.md) |
| Spec khởi tạo & parity | [`../common/InitParity.md`](../common/InitParity.md) |
| Networking | [`../common/NetworkingGuide.md`](../common/NetworkingGuide.md) |
| Xử lý lỗi | [`../common/ErrorHandling.md`](../common/ErrorHandling.md) |
| Lưu trữ | [`../common/StorageGuide.md`](../common/StorageGuide.md) |
| DI | [`../common/DependencyInjection.md`](../common/DependencyInjection.md) |
| Theme | [`../common/Theming.md`](../common/Theming.md) |
| Kiểm thử | [`../common/TestingGuide.md`](../common/TestingGuide.md) |
| Từng màn hình | [`../features/`](../features/README.md) |
| Tích hợp | [`../AndroidIntegrationGuide.md`](../AndroidIntegrationGuide.md) · [`../IosIntegrationGuide.md`](../IosIntegrationGuide.md) |
| Phát hành | [`../android/Distribution.md`](../android/Distribution.md) · [`../ios/Distribution.md`](../ios/Distribution.md) |
| Bảo mật | [`../common/Security.md`](../common/Security.md) |
| Đóng gói & bàn giao | [`../release/PackagingGuide.md`](../release/PackagingGuide.md) |
