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
│ XML View, Data/View Binding  │   │ UIKit XIB, callback thuần    │
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

### 1.1. Bề mặt SDK, wrapper host & DI — class chủ chốt trong luồng

Sơ đồ §1 là *tầng kiến trúc*. Sơ đồ dưới là *đường đi của một lời gọi* từ app host xuống lõi, kèm
**tên class thật** ở mỗi chặng (hai nền tảng đối xứng 1:1 — xem [InitParity.md](./InitParity.md)).

```
┌──────────────────────────────────────────────────────────────────────────┐
│ HOST APP (đối tác)  — chỉ chạm PromotionManager, KHÔNG import SDK rải rác   │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      ▼
  PromotionManager        (androidApp / iosApp — wrapper / anti-corruption)
  • map model APP ⇄ model SDK   • singleton   • adapter cho PRMSDKCallback
  • nuốt ràng buộc: token chụp lúc init · updateContext trước màn có voucher
                                      │  gọi entry tĩnh
                                      ▼
  PRMSDK            ← ENTRY công khai; chữ ký chỉ Foundation/UIKit (iOS) /
                            không lộ core type (Android). Android: `object`;
                            iOS: `final class` + `_impl: NSObject` box.
  ├─ vòng đời   initialize · release · isInitialized · updateContext · configure(theme)
  ├─ màn hình   openMyPromotion · openPromotionDetail · createEndowView → PRMEndowView
  ├─ headless   api: PRMSDKApi
  └─ sự kiện    PRMSDKCallback (6 sự kiện)
                                      │
        Android: `object` giữ callback/context rồi uỷ quyền.
        iOS: PromotionSDKImpl (box) giữ đồ thị sống + phát 6 sự kiện.
                                      ▼
  PRMSDKApi        ← RANH GIỚI headless: map model lõi → DTO,
                            PromotionResult → PRMApiResult. KHÔNG chứa nghiệp vụ.
                                      │
                                      ▼
  PromotionContainer  (core/di, `object` dùng chung KMP)  →  SdkDi (engine DI nội bộ)
  • dựng & giữ: PromotionUseCases (facade headless) · Repository · RemoteDataSource · KeyValueStorage
  • gác cờ:     PromotionFeatureGate — cùng nguồn sự thật cho UI (điểm điều hướng) lẫn headless
                                      │
                                      ▼
            DOMAIN ← DATA   (đã mô tả ở §1: UseCase → Repository → RemoteDataSource)
```

Các nút thắt cần nhớ:

- **`PromotionManager`** là chỗ *duy nhất* host chạm SDK — upgrade/đổi SDK chỉ sửa một file. Hợp đồng
  `PromotionServing` đối xứng hai nền tảng, xem [InitParity.md §6](./InitParity.md#6-wrapper-host--hợp-đồng-chung).
- **`PRMSDK`** giữ chữ ký sạch (không lộ RxSwift/Kotlin/core type) — xem [PublicApi.md](./PublicApi.md).
- **`PRMSDKApi`** là *ranh giới phân phối* (map DTO), **không** phải use case — nghiệp vụ, gác cờ,
  chuẩn hoá `errorCode` đều nằm ở `PromotionUseCases` của lõi.
- **`PromotionContainer` / `SdkDi`** là DI tự viết — **không** sửa (xem [DependencyInjection.md](./DependencyInjection.md)).
- **`PromotionFeatureGate`** gác cờ cho *cả* UI lẫn headless — kill-switch không có cửa sau.

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

Chi tiết: [AndroidUIGuide.md](../android/UIGuide.md).

## 5. Mô hình UI — iOS (MVVM + Builder/Router)

- **Builder** (`PRMBaseBuilder`): lắp ráp VC + VM + Router, inject dependency.
- **Router** (`PRMBaseRouter`): điều hướng (push/pop/present).
- **ViewModel** (`PRMBaseViewModel`): bọc store dùng chung ở `promotionLogic`; phơi `onState` /
  `onEffect` / `handleAction(_:)` bằng **callback thuần** — đối ứng 1-1 `uiState` / `uiEffect` /
  `handleAction` bên Android. Không Combine, không RxSwift.
- **ViewController** (`PRMBaseViewController`): bind UI qua **một** `render(state)`, load XIB theo tên class.

Public facade `PRMSDK` giữ một `_impl: NSObject` để app host không phải nạp module nội bộ
(domain model Kotlin) — tránh crash đệ quy `deserializeClass`.

Chi tiết: [IosUIGuide.md](../ios/UIGuide.md).

---

## 6. Hai chế độ sử dụng SDK

- **Headless mode** — host tự dựng UI, chỉ gọi `PromotionUseCases()` / `PromotionFeatureFlagUseCases()`.
- **UI mode** — host nhúng màn hình sẵn có của `promotionSDK` (Fragment trên Android,
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
