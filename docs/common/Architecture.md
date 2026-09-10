# Architecture — Kiến trúc tổng thể

TTCN Promotion SDK áp dụng **Clean Architecture**. Điểm khác biệt so với một app thông thường:
tầng Data + Domain được chia sẻ giữa Android và iOS qua Kotlin Multiplatform, còn tầng Presentation
**không** chia sẻ — mỗi nền tảng giữ mô hình UI native của mình.

## Mục lục

<!-- toc -->
- [1. Tổng quan](#1-tổng-quan)
  - [1.1. Bề mặt SDK, wrapper host & DI — class chủ chốt trong luồng](#11-bề-mặt-sdk-wrapper-host--di--class-chủ-chốt-trong-luồng)
- [2. Lõi dùng chung — `:promotionLogic`](#2-lõi-dùng-chung--promotionlogic)
  - [2.1. Data layer — `data/`](#21-data-layer--data)
  - [2.2. Domain layer — `domain/`](#22-domain-layer--domain)
  - [2.3. Hạ tầng nền tảng — `expect` / `actual`](#23-hạ-tầng-nền-tảng--expect--actual)
- [3. Luồng một request điển hình](#3-luồng-một-request-điển-hình)
- [4. Mô hình UI — Android](#4-mô-hình-ui--android)
- [5. Mô hình UI — iOS (MVVM + Builder/Router)](#5-mô-hình-ui--ios-mvvm--builderrouter)
- [6. Hai chế độ sử dụng SDK](#6-hai-chế-độ-sử-dụng-sdk)
- [7. Ràng buộc kiến trúc (không vi phạm)](#7-ràng-buộc-kiến-trúc-không-vi-phạm)
<!-- /toc -->

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
│ HOST APP (đối tác)  — gọi THẲNG PromotionSDK, không cần lớp bọc trung gian  │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │  gọi entry tĩnh
                                      ▼
  PromotionSDK            ← ENTRY công khai; chữ ký chỉ Foundation/UIKit (iOS) /
                            không lộ core type (Android). Android: `object`;
                            iOS: `final class` + `_impl: NSObject` box.
  ├─ vòng đời   initialize · release · isInitialized · updateOrderInfo · configure(theme)
  ├─ màn hình   openMyPromotion · openPromotionDetail · createOfferWidget → PRMOfferWidget
  ├─ headless   api: PromotionSDKApi
  └─ sự kiện    PromotionSDKCallback (6 sự kiện)
                                      │
        Android: `object` giữ callback/context rồi uỷ quyền.
        iOS: PromotionSDKImpl (box) giữ đồ thị sống + phát 6 sự kiện.
                                      ▼
  PromotionSDKApi        ← RANH GIỚI headless: map model lõi → DTO,
                            PromotionResult → PromotionApiResult. KHÔNG chứa nghiệp vụ.
                                      │
                                      ▼
  PromotionContainer  (di, `object` dùng chung KMP)  →  SdkDi (engine DI nội bộ)
  • dựng & giữ: PromotionUseCases (facade headless) · Repository · RemoteDataSource · PromotionPreferences
  • gác cờ:     PromotionFeatureGate — cùng nguồn sự thật cho UI (điểm điều hướng) lẫn headless
                                      │
                                      ▼
            DOMAIN ← DATA   (đã mô tả ở §1: UseCase → Repository → RemoteDataSource)
```

Các nút thắt cần nhớ:

- **Host không cần wrapper.** Khuyến nghị `PromotionManager` / `PromotionServing` đã bị **bỏ** — SDK
  tự lo token (đọc lại `tokenSource` mỗi request), context đơn hàng và callback, nên một lớp bọc chỉ
  thêm chỗ để lệch. Xem [InitParity.md §6](./InitParity.md#6-tích-hợp-trực-tiếp--host-không-cần-wrapper);
  cả hai integration guide đều liệt "tự viết wrapper" vào mục sai lầm thường gặp.
- **`PromotionSDK`** giữ chữ ký sạch (không lộ RxSwift/Kotlin/core type) — xem [PublicApi.md](./PublicApi.md).
- **`PromotionSDKApi`** là *ranh giới phân phối* (map DTO), **không** phải use case — nghiệp vụ, gác cờ,
  chuẩn hoá `errorCode` đều nằm ở `PromotionUseCases` của lõi.
- **`PromotionContainer` / `SdkDi`** là DI tự viết — **không** sửa (xem [DependencyInjection.md](./DependencyInjection.md)).
- **`PromotionFeatureGate`** gác cờ cho *cả* UI lẫn headless — kill-switch không có cửa sau.

---

## 2. Lõi dùng chung — `:promotionLogic`

### 2.1. Data layer — `data/`

- `data/dto/` — DTO khớp JSON của API, chia sub-package theo feature: `voucher/`, `redemption/`,
  `stackablediscount/`, `eligible/`, `featureflag/`. Mỗi package có Request/Response + Mapper `toXxx()`.
  Envelope chung `ApiResponseTemplate` đặt ở `data/remote/ApiResponse.kt`.
- `data/remote/` — `PromotionApiService` / `FeatureFlagApiService` (interface) và bản Ktor
  `KtorPromotionApiService` / `KtorFeatureFlagApiService`; `PromotionHttpClient`; các `RemoteDataSource`.
  Khi response lỗi, data source ném **exception domain** (`PromotionException` / `NetworkException`)
  mang theo `errorCode` / `httpStatus`.
- `data/local/` — `PromotionPreferences` (interface) + `SettingsPreferences` (thân dùng chung ở
  `commonMain`, `expect/actual` chỉ dựng delegate) và `FeatureFlagLocalDataSource`.
- `data/repository/` — `PromotionRepositoryImpl`, `FeatureFlagRepositoryImpl` — implement interface
  của Domain, gọi data source và map DTO → domain.

> Retrofit sinh implementation của interface lúc runtime bằng dynamic proxy. Kotlin/Native không có
> cơ chế đó, nên ở đây `KtorPromotionApiService` được **viết tay**.

### 2.2. Domain layer — `domain/`

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
| `createPreferences()` | `SharedPreferencesSettings` | `NSUserDefaultsSettings` | Chỉ **dựng delegate** — thân `SettingsPreferences` nằm ở `commonMain`. Cache feature flag + theme |
| `clearPlatformState()` | nhả `applicationContext` | no-op | Dọn khi `PromotionContainer.clear()` |

Engine của Ktor **không** cần `expect`/`actual`: Ktor tự chọn theo artifact có trên classpath
(`ktor-client-okhttp` ở androidMain, `ktor-client-darwin` ở iosMain).

`currentEpochMillis()` **từng là chỗ thứ tư** (`System.currentTimeMillis()` / `NSDate()`), nhưng từ
Kotlin 2.1.20 `kotlin.time.Clock` đã nằm sẵn trong **stdlib** — hai `actual` đã bỏ, không cần
`kotlinx-datetime`. Ba dòng còn lại trong bảng thì **không** có cách nào bỏ:

- `SdkLock` — ứng viên duy nhất là `kotlinx.atomicfu.locks`, mà tài liệu của nó ghi rõ *"not
  recommended to use in libraries that other projects depend on"* và *"no ABI guarantees"*.
  `:promotionLogic` chính là thư viện như vậy.
- `createPreferences()` + `clearPlatformState()` — hệ quả của đúng một sự thật: Android cần `Context`
  để mở `SharedPreferences`. Cách duy nhất cắt được là `multiplatform-settings-no-arg`, đã loại vì nó
  xoá prefs của app host (xem [StorageGuide.md §2.1](./StorageGuide.md)).

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

## 4. Mô hình UI — Android

`PRMStoreViewModel<S, I>` bọc store dùng chung. **Không** còn `UiState`/`Action`/`Effect` riêng cho
từng màn: Fragment đọc thẳng `State` và phát thẳng `Intent` của store.

```
User tương tác → viewModel.dispatch(intent)   // Intent của store
UI collect viewModel.state   → render         // State của store (nguồn sự thật, phát lại được)
UI collect viewModel.effects → toast          // PRMEffect: sự kiện MỘT LẦN
```

Vì sao bỏ lớp trung gian: `UiState`/`Action` gần như sao chép 1-1 `State`/`Intent` của store (màn chi
tiết chép nguyên si sáu field, kể cả `detail = detail`), còn `bindStore`/`render`/`handleError` thì
giống hệt nhau ở cả bốn màn. Chúng cũng **không** phải ranh giới layer — model UI vốn đã mang type
của lõi (`VoucherDetail`, `VoucherStatus`, `ApplicableProduct`).

Thứ **được giữ**: model hiển thị của Android (`MyVoucherListItem`, `TabItem` ở `*UiModels.kt`) — đó
là cách RecyclerView muốn nhìn dữ liệu, Fragment map tại chỗ dùng.

`PRMStoreViewModel` là **`abstract` + subclass ba dòng**, không phải một class generic dùng chung:
`by viewModels()` lấy tên class làm khoá mà generic thì erase, dùng chung một class là hai màn đè
khoá nhau và nổ `ClassCastException` lúc chạy.

Chi tiết: [AndroidUIGuide.md](../android/UIGuide.md).

## 5. Mô hình UI — iOS (MVVM + Builder/Router)

- **Builder** (`PRMBaseBuilder`): lắp ráp VC + VM + Router, inject dependency.
- **Router** (`PRMBaseRouter`): điều hướng (push/pop/present).
- **ViewModel** (`PRMStoreViewModel<Store>` / `PRMScreenViewModel<R, Store>`): bọc store dùng chung
  ở `promotionLogic`, **đối ứng 1-1 `PRMStoreViewModel<S, I>` bên Android** — phơi `state` /
  `onEffect` / `dispatch(_:)` bằng **callback thuần**, không Combine, không RxSwift.
  Bản không-router dùng cho widget `PRMOfferWidget`; bản có router cho ViewModel của màn.
  Giống Android, **không** còn `UiState`/`Action`/`Effect` riêng từng màn.

  > **Một điểm iOS buộc phải khác:** Android collect thẳng `store.effects`; Swift **không** collect
  > được vì `effects` là default member của interface Kotlin, mà Kotlin/Native chỉ đặt default member
  > lên *protocol* (và protocol thì bị erase generic), không lên class. Nên `PRMStoreViewModel` bên
  > iOS tự suy effect từ `errorCode` trong state — cùng ngữ nghĩa "một lần rồi `ConsumeError`".
  > Cùng lý do, `PRMStoreBridge` (protocol Swift) bám vào **class** store chứ không vào protocol Kotlin.
- **ViewController** (`PRMBaseViewController`): bind UI qua **một** `render(state)`, load XIB theo tên class.

Public facade `PromotionSDK` giữ một `_impl: NSObject` để app host không phải nạp module nội bộ
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
