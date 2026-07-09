# Architecture — Kiến trúc tổng thể

TTCN Promotion SDK áp dụng **Clean Architecture**. Điểm khác biệt so với một app thông thường:
tầng Data + Domain được chia sẻ giữa Android và iOS qua Kotlin Multiplatform, còn tầng Presentation
**không** chia sẻ — mỗi nền tảng giữ mô hình UI native của mình.

---

## 1. Tổng quan

```
┌──────────────────────────────┐   ┌──────────────────────────────┐
│ PRESENTATION — Android       │   │ PRESENTATION — iOS           │
│ Fragment/View ←→ ViewModel   │   │ ViewController ←→ ViewModel  │
│ MVI: State / Action / Effect │   │ MVVM + Builder + Router      │
│ XML View, Data/View Binding  │   │ UIKit XIB, RxSwift           │
└──────────────┬───────────────┘   └──────────────┬───────────────┘
               │      gọi UseCase, nhận domain model
               └──────────────┬───────────────────┘
                              ▼
      ┌────────────────────────────────────────────────┐
      │ DOMAIN — :promotionLogic / commonMain          │
      │   UseCase → Repository (interface) → Model     │
      │   Thuần Kotlin. KHÔNG Android, KHÔNG iOS,      │
      │   KHÔNG Ktor, KHÔNG kotlinx.serialization      │
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
      OkHttp engine                     Darwin engine
      SharedPreferences                 NSUserDefaults
      ReentrantLock                     NSRecursiveLock
```

**Quy tắc phụ thuộc:** phụ thuộc luôn hướng **vào trong** (Presentation → Domain ← Data).
Domain là trung tâm, **không biết** gì về Android, iOS, Ktor hay kotlinx.serialization.

---

## 2. Lõi dùng chung — `:promotionLogic`

### 2.1. Data layer — `core/data/`

- `data/dto/` — DTO khớp JSON của API, chia sub-package theo feature: `voucher/`, `redemption/`,
  `stackablediscount/`, `eligible/`, `featureflag/`. Mỗi package có Request/Response + Mapper `toXxx()`.
  Envelope chung `ApiResponseTemplate` đặt ở `core/data/remote/ApiResponse.kt`.
- `data/remote/` — `PromotionApiService` / `FeatureFlagApiService` (interface) và bản Ktor
  `KtorPromotionApiService` / `KtorFeatureFlagApiService`; `PromotionHttpClient`; các `RemoteDataSource`.
  Khi response lỗi, data source ném **exception domain** (`PromotionException` / `NetworkException`)
  mang theo `errorCode` / `httpStatus`.
- `data/local/` — `KeyValueStorage` (expect/actual) và `FeatureFlagLocalDataSource`.
- `data/repository/` — `PromotionRepositoryImpl`, `FeatureFlagRepositoryImpl` — implement interface
  của Domain, gọi data source và map DTO → domain.

> Retrofit sinh implementation của interface lúc runtime bằng dynamic proxy. Kotlin/Native không có
> cơ chế đó, nên ở đây `KtorPromotionApiService` được **viết tay**.

### 2.2. Domain layer — `core/domain/`

Logic nghiệp vụ thuần, độc lập framework.

- `domain/model/` — model nghiệp vụ theo feature: `voucher/`, `redemption/`, `stackablediscount/`,
  `eligible/`, `featureflag/`, cùng `PromotionResult`.
- `domain/repository/` — **interface** repository (`PromotionRepository`, `FeatureFlagRepository`).
- `domain/usecase/` — use case đơn nhiệm, và hai facade công khai gom nhóm cho headless API:
  `PromotionUseCases`, `PromotionFeatureFlagUseCases`.
- `domain/exception/` — `PromotionException`, `NetworkException`, `FeatureFlagException`, `ErrorCodes`.

### 2.3. Hạ tầng nền tảng — `expect` / `actual`

Chỉ ba chỗ cần biết nền tảng, tất cả nằm ngoài Domain:

| Trừu tượng | androidMain | iosMain | Vì sao |
|---|---|---|---|
| `SdkLock` | `ReentrantLock` | `NSRecursiveLock` | `synchronized` là JVM-only; DI cần khoá **reentrant** vì `resolve()` gọi đệ quy |
| `KeyValueStorage` | `SharedPreferences` | `NSUserDefaults` | Cache feature flag |
| `clearPlatformState()` | nhả `applicationContext` | no-op | Dọn khi `PromotionContainer.clear()` |

Engine của Ktor **không** cần `expect`/`actual`: Ktor tự chọn theo artifact có trên classpath
(`ktor-client-okhttp` ở androidMain, `ktor-client-darwin` ở iosMain).

---

## 3. Luồng một request điển hình

Ví dụ: tìm voucher của khách.

1. UI gửi ý định (Android: `handleAction(LoadInitialIfNeeded)`; iOS: `transform(input:)`).
2. ViewModel gọi thẳng `SearchCustomerVouchersUseCase()` (facade `PromotionUseCases()` chỉ dành cho headless).
3. `SearchCustomerVouchersUseCase` gọi `PromotionRepository.searchCustomerVouchers(...)`.
4. `PromotionRepositoryImpl` gọi `PromotionRemoteDataSource` → `KtorPromotionApiService` (Ktor).
5. DTO trả về được map `toSearchCustomerVouchersResult()` → domain model.
6. `PromotionUseCases` bọc kết quả thành `PromotionResult.Success` hoặc `PromotionResult.Failure`
   — **không ném exception ra ngoài**.
7. UI render danh sách hoặc hiển thị lỗi theo `errorCode`.

---

## 4. Mô hình UI — Android (MVI)

`PRMBaseViewModel<S, A, E>` với ba generic: **S**tate (immutable, nguồn sự thật duy nhất),
**A**ction (ý định người dùng), **E**ffect (sự kiện một lần: điều hướng, toast, lỗi).

```
User tương tác → handleAction(action)
     ├─ launch { useCase(...) }        // gọi Domain
     ├─ setState { copy(...) }         // StateFlow
     └─ sendEffect(Effect.Xxx)         // SharedFlow, one-shot
UI collect uiState  → render
UI collect uiEffect → điều hướng / thông báo
```

Chi tiết: [AndroidUIGuide.md](./AndroidUIGuide.md).

## 5. Mô hình UI — iOS (MVVM + Builder/Router)

- **Builder** (`BaseBuilder`): lắp ráp VC + VM + Router, inject dependency.
- **Router** (`BaseRouter`): điều hướng (push/pop/present).
- **ViewModel** (`BaseViewModel`): theo `ViewModelType` với `transform(input:) -> Output`, dùng RxSwift.
- **ViewController** (`BaseViewController`): bind UI, load XIB theo tên class.

Public facade `VDSPromotion` giữ một `_impl: NSObject` để app host không phải nạp module nội bộ
(RxSwift, domain model) — tránh crash đệ quy `deserializeClass`.

Chi tiết: [IosUIGuide.md](./IosUIGuide.md).

---

## 6. Hai chế độ sử dụng SDK

- **Headless mode** — host tự dựng UI, chỉ gọi `PromotionUseCases()` / `PromotionFeatureFlagUseCases()`.
- **UI mode** — host nhúng màn hình sẵn có của `promotionUI` (Fragment trên Android,
  ViewController trên iOS).

Cả hai dùng chung Domain + Data qua Custom DI (xem [DependencyInjection.md](./DependencyInjection.md)).

---

## 7. Ràng buộc kiến trúc (không vi phạm)

- Presentation **chỉ** gọi Domain qua **use case**, không gọi thẳng Repository/DataSource.
- `commonMain` **không** import `android.*` hay `platform.*`. Cần API nền tảng → `expect`/`actual`.
- Domain **không** import Ktor, kotlinx.serialization, hay `core.data.dto.*`.
  Mọi method của `PromotionRepository` nhận & trả **domain model**, không phải DTO.
- Data map DTO ↔ domain model **trước khi** trả lên Domain.
- **Không chuỗi hiển thị trong lõi.** Ví dụ: `EligibleOffer` trả `minOrderValue` và `unmatchedRules`;
  câu "Đơn tối thiểu 1.000.000đ để áp dụng" do UI dựng, vì đó là copy và phụ thuộc locale.
- Mọi thay đổi kiến trúc/luồng dữ liệu phải cập nhật file này (AI_AGENT_RULES điều 5 & 8).
