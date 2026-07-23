# SdkReview — Báo cáo rà soát & hoàn thiện SDK

> Tài liệu **rà soát** hiện trạng SDK sau khi bổ sung **tầng store dùng chung** (`promotionLogic/presentation`).
> Mỗi mục nêu **kết luận + dẫn chứng file/dòng thật**, và trả lời luôn các câu hỏi phụ hay gặp.
> Trạng thái phản ánh nhánh `refactor/kmp-shared-ui-stores` (đã build + chạy Android & iOS).

| # | Hạng mục | Kết luận |
|---|---|---|
| 1 | UI public iOS (foundation/kit) | **KHÔNG** public ra host; ẩn qua `@_implementationOnly` + link tĩnh vào 1 xcframework → host tích hợp bằng **đúng 1 framework, 1 import** |
| 2 | Use Case Wrapper / Store | Đã thêm **tầng store** bọc UI-logic quanh use case; host chỉ chạm **model UI**, không chọc xuống Logic |
| 3 | Terminology | `PRMSDK*` = bề mặt public; `PRM*` = nội bộ; `*Store` = UI-logic dùng chung |
| 4 | Kiến trúc & class chủ chốt | 3 module, Clean Arch + store; luồng + đóng gói ở §4 |
| 5 | Version | Nguồn tập trung `gradle.properties: SDK_VERSION=1.0.0`; Android 2 artifact Maven, iOS 1 xcframework |

---

## 1. UI public trên iOS — foundation/kit có lộ ra host không?

**Câu hỏi:** Các package `PRMFoundation`, `PRMDesignKit`, `PRMKotlinBridge`, `PRMPromotionUI` có public ra
ngoài không, có ảnh hưởng lúc tích hợp host app không?

**Kết luận: KHÔNG public.** Chúng là **dependency nội bộ**, được **ẩn** khỏi module interface và **link tĩnh**
vào **một** framework `PRMSDK.xcframework`. Host tích hợp chỉ cần thêm **1 xcframework** và
`import PromotionSDKUI` — không thấy, không cần khai báo package con nào.

**Dẫn chứng:**
- Mọi chỗ `PromotionSDKUI` dùng package con đều qua `@_implementationOnly import` → **không tái xuất** sang
  module interface (host không import xuyên được): `PRMKotlinBridge` (×23), `PRMDesignKit` (×13),
  `PRMFoundation` (×9), `PRMPromotionUI` (×6).
- `iosPromotionSDK/scripts/build-xcframework.sh` archive rồi `-create-xcframework`; cuối script **verify** archive
  **chỉ** chứa `PromotionSDKUI.framework` → in *"(không có — mọi dependency đã link tĩnh)"* (dòng 111).
- Host thật (`iosApp/iosApp/*.swift`) chỉ có `import PromotionSDKUI` (+ `UIKit`/`Foundation`) — **không** import
  package con nào.

**Ảnh hưởng tích hợp:** Tối thiểu & đúng ý đồ đóng gói — 1 xcframework, 1 import. Không xung đột version SPM,
không lộ symbol nội bộ. (Bề mặt API public đầy đủ & đối xứng Android — xem [PublicApi.md](./PublicApi.md).)

**Câu hỏi phụ:**
- *Host muốn tuỳ biến theme (PRMDesignKit)?* → Không import PRMDesignKit; theme cấu hình qua **public API**
  `PRMSDKTheme` trong `PRMSDKOptions` (bề mặt `PromotionSDKUI`).
- *Link tĩnh có phình app?* → Static, symbol dedup khi link vào app; đổi lại chỉ **1** binary, tránh
  "dylib not found" runtime.
- *Đối xứng Android?* → Android phát hành 2 AAR Maven (§5), nhưng nguyên tắc "host chỉ chạm bề mặt" giống nhau (§2).

---

## 2. Use Case Wrapper → Tầng Store bọc UI-logic

**"Use Case Wrapper" ở SDK này là gì?** Là **use case domain** bọc repository — mỗi use case = **một** nghiệp vụ,
khai `@Throws(PromotionException, NetworkException, …)` để bridge lỗi sang iOS (`NSError`). Ví dụ:
`GetCustomerVoucherDetailUseCase`, `FindEligibleCampaignsUseCase`, `ValidateStackableDiscountsUseCase`,
`SearchCustomerVouchersUseCase`.

**Bổ sung — tầng Store (UI-logic dùng chung):** Thêm **`promotionLogic/presentation/*Store`** bọc **quanh use case**,
giữ **toàn bộ nghiệp-vụ-trình-bày** (state, phân trang, tab-cache, debounce search, selection, validate&apply,
quyết định widget-state) — **viết một lần, chạy cả Android & iOS**:

| Store | Màn | Gói nghiệp vụ |
|---|---|---|
| `MyPromotionStore` | Ưu đãi của tôi | tab, phân trang, cache theo tab, latest-wins |
| `SearchMyPromotionStore` | Tìm ưu đãi | debounce, search server-side, phân trang |
| `ChoosePromotionStore` | Chọn ưu đãi | 2 nhóm phân trang độc lập, selection, see-more |
| `PromotionDetailStore` | Chi tiết | fetch detail, seed nút, quyết định "Dùng ngay" |
| `EndowStore` | Widget checkout | findEligible + **validate&apply** + widget-state |

**Chuỗi bọc:** `View` → `ViewModel/Impl (mỏng)` → **`Store` (UI-logic)** → `UseCase` → `Repository` →
`RemoteDataSource` → Ktor. Android `*ViewModel` (kế `PRMBaseViewModel`) và iOS `*ViewModel`/`PromotionSDKImpl`
+ `EndowViewModel` chỉ **forward intent + map state → bề mặt view**.

**Quan điểm "host dùng model của SDK UI, KHÔNG phải SDK Logic"** (đảm bảo host không chọc xuống tầng sâu) —
dẫn chứng bảo vệ:
- **iOS:** `@_implementationOnly import PRMKotlinBridge` khiến type Logic (`VoucherItem`, `EligibleOffer`,
  `VoucherDetail`…) **không** lộ qua interface → host **không** tham chiếu được. Host dùng **DTO public của UI**:
  `PRMVoucher`, `PRMVoucherDetail`, `PRMEligibleOffer`, `PRMEligibleResult`,
  `PRMAppliedDiscount`, `PRMApiResult` (`Entry/API/PromotionApiModels.swift`).
- **Android:** thành viên mang type Logic để `internal` — vd `PRMEndowView.myVouchers/otherVouchers:
  List<EligibleOffer>` và `PRMChoosePromotionFragment.onApplySelectedOffers` đều `internal`. **Public API** chỉ
  dùng DTO ở `ui/entry`: `PRMVoucher`, `PRMVoucherDetail`, `PRMEligibleOffer`,
  `PRMAppliedDiscount`, `PRMApiResult`.
- Việc **map Logic → DTO UI** nằm ở tầng UI (`toUiState()`/`buildOutput()` và facade `PRMSDKApi`), nên
  ranh giới rõ: **Logic không rò lên host**.

**Câu hỏi phụ:**
- *Đã có use case, sao cần store?* → use case = 1 lời gọi domain; store = **orchestration** nhiều use case +
  **state** (phân trang/selection/validate/widget-state) dùng chung — trước đây mỗi nền tảng tự viết, nay 1 nguồn.
- *Type Logic (`EligibleOffer`) là `public` trong Kotlin mà?* → Đúng (mặc định Kotlin public) **nhưng** không tới
  được host: (iOS) không tái xuất, (Android) thành viên UI để `internal` → **access control ở tầng UI**.
- *Store có giữ chuỗi hiển thị không?* → Không (đúng rule lõi); state chỉ mang dữ liệu có cấu trúc
  (enum/số/ngày thô), native lo định dạng.

---

## 3. Terminology (thống nhất)

| Tiền tố | Ý nghĩa | Ví dụ |
|---|---|---|
| `PRMSDK*` | **Bề mặt public** host gọi | `PRMSDK`, `PRMSDKOptions`, `PromotionSDKConfig`, `PRMSDKTheme` |
| `Promotion*` (DTO) | **Model public UI** trả cho host | `PRMVoucher`, `PRMAppliedDiscount`, `PRMEligibleOffer` |
| `PRM*` | **Nội bộ** (view/base/package con) | `PRMEndowView`, `PRMBaseViewModel`, `PRMKotlinBridge` |
| `*Store` | **UI-logic dùng chung** ở `promotionLogic` | `EndowStore`, `MyPromotionStore` |
| `*UseCase` / `*Repository` | domain / data (lõi) | `FindEligibleCampaignsUseCase` |

Bề mặt public **không** prefix (trùng tên Android để đối xứng); nội bộ dùng `PRM`. Xem
[CodingStandards.md](./CodingStandards.md) / [ProjectStructure.md](./ProjectStructure.md).

---

## 4. Kiến trúc hiện tại & class chủ chốt

### 4.1 Module & tầng
```
┌───────────────────────── Host app (Android / iOS) ─────────────────────────┐
│  chỉ chạm: PRMSDK(.initialize/open*/makeEndowView/api) + DTO public   │
└─────────────────────────────────────────────────────────────────────────────┘
   │ Android: Maven com.ttcn.promotion:promotionSDK      │ iOS: PRMSDK.xcframework
   ▼                                                     ▼
┌──────────── AndroidPromotionSDK ───────────┐  ┌──────── iosPromotionSDK/PromotionSDKUI ────────┐
│ entry PRMSDK · Fragment/View         │  │ entry PRMSDK · PromotionSDKImpl · VC     │
│ · *ViewModel (kế PRMBaseViewModel) — MỎNG  │  │ · *ViewModel · EndowViewModel · PRMEndowView   │
└───────────────────────┬─────────────────────┘  └───────────────────────┬────────────────────────┘
                        └──────────────┬─────────────────────────────────┘
                                       ▼   promotionLogic (KMP shared — Clean Arch + MVI-store)
   presentation/ : 5 Store (state · intent · UI-logic dùng chung)          ← §2
   core/di       : PromotionContainer (DI tự viết) · requestContextProvider
   core/config   : PromotionSDKConfig · PromotionRequestContextProvider (getOrderId/Value/Items/Service…)
   core/domain   : model (VoucherItem/EligibleOffer/VoucherDetail…) · usecase · repository (interface)
   core/data     : repository impl · RemoteDataSource · ApiService (Ktor) · dto + mapper
```

### 4.2 Class chủ chốt / dùng chung (bắt buộc nắm khi maintain)
| Class | Vai trò |
|---|---|
| `PromotionContainer` | DI tự viết; `initialize(config)`, `requireConfig()`, `requestContextProvider` — nguồn context duy nhất |
| `PromotionRequestContextProvider` | Host cấp token/order mỗi request: `getOrderId/getOrderValue/getOrderItems/getService/getAccessToken` |
| `*Store` (×5) | UI-logic dùng chung — `state`/`dispatch`/`watchState`/`currentState`/`clear` |
| `PromotionRepository(Impl)` + `RemoteDataSource` + `PromotionApiService` | data layer; envelope `ApiResponseTemplate<T>` bóc qua `requireData()` |
| `PRMSDK` (Android/iOS) + `PromotionSDKImpl` (iOS) | facade public: init / open màn / `makeEndowView` / headless api |
| `PRMBaseViewModel` (Android) · `EndowViewModel`/`PromotionSDKImpl` (iOS) | lớp bọc mỏng quanh store |

### 4.3 Luồng hoạt động
1. Host: `PRMSDK.initialize(options)` → `PromotionContainer.initialize(config)` (base URL, provider…).
2. Host mở màn: `openMyPromotion(...)` / `makeEndowView(...)` (hoặc headless `PRMSDK.api`).
3. `ViewModel/Impl` tạo `Store`, `dispatch(intent)`; `Store` gọi `UseCase` → `Repository` → Ktor.
4. `Store.state` (StateFlow) phát ngược → VM map `state → bề mặt view` (Android `setState`/collect; iOS
   `watchState` → publisher). Lỗi: `state.errorCode` one-shot → VM phát effect → view map code → chuỗi.

### 4.4 Đóng gói
- **Android:** 2 artifact Maven — `com.ttcn.promotion:promotionLogic` (KMP AAR) + `com.ttcn.promotion:promotionSDK`
  (UI). Host khai toạ độ `promotionSDK` (kéo theo `promotionLogic`).
- **iOS:** 1 `PRMSDK.xcframework` (link tĩnh `PromotionLogic.xcframework` + 4 package `PRM*`). Build:
  `scripts/build-ios.sh` (gradle dựng `PromotionLogic.xcframework` → archive Swift → xcframework).

---

## 5. Đánh version & Maven

**Nguồn version tập trung:** `gradle.properties:19` → **`SDK_VERSION=1.0.0`**.
- `promotionLogic/build.gradle.kts` và `AndroidPromotionSDK/build.gradle.kts` đọc `findProperty("SDK_VERSION") ?: "1.0.0"`;
  `group = "com.ttcn.promotion"`; `AndroidPromotionSDK` artifactId = **`promotionSDK`**; expose `BuildConfig.SDK_VERSION`.
- iOS: `MARKETING_VERSION = 1.0.0` (`PRMSDK.xcodeproj`) — giữ trùng số.
- **`CHANGELOG.md`** (Keep a Changelog + SemVer): mục `[1.0.0] — 2026-07-20`.

**Publish (Android → Maven):**
```bash
./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal   # 1.0.0
./gradlew ... -PSDK_VERSION=1.2.0                                                        # đổi version
# hoặc: ./scripts/build-android.sh --version 1.2.0   (ép đúng thứ tự: publish SDK → build app demo)
```
Host Android khai `implementation("com.ttcn.promotion:promotionSDK:<SDK_VERSION>")`.

**Phát hành iOS:** dựng lại `PRMSDK.xcframework` (`./scripts/build-ios.sh --skip-app`) rồi giao cho host
(không qua Maven — xem [docs/ios/Distribution.md](../ios/Distribution.md)).

**Câu hỏi phụ:**
- *Bump version thế nào?* → sửa `gradle.properties SDK_VERSION` (hoặc `-PSDK_VERSION=x.y.z`) + `MARKETING_VERSION`
  (iOS) + ghi `CHANGELOG.md`.
- *Vì sao Android 2 artifact?* → tách lõi KMP (`promotionLogic`) khỏi UI (`promotionSDK`) để tái dùng lõi; host chỉ
  khai `promotionSDK`, `promotionLogic` kéo theo transitively.
- *iOS bao nhiêu artifact?* → **1** xcframework (đã gộp lõi + package con), khớp mô hình phân phối iOS.

---

## Phụ lục — nơi kiểm chứng
| # | Nơi kiểm chứng |
|---|---|
| 1 | `@_implementationOnly import` trong `iosPromotionSDK/PromotionSDKUI/**`; `iosPromotionSDK/scripts/build-xcframework.sh:111`; `iosApp/iosApp/*.swift` |
| 2 | `promotionLogic/presentation/*Store.kt`; `ui/entry/*` (Android) & `Entry/API/PromotionApiModels.swift` (iOS); `PRMEndowView.myVouchers` (internal) |
| 3 | [CodingStandards.md](./CodingStandards.md), [ProjectStructure.md](./ProjectStructure.md) |
| 4 | [Architecture.md](./Architecture.md); `core/di/PromotionContainer.kt`; 5 `*Store.kt` |
| 5 | `gradle.properties:19`; `*/build.gradle.kts`; `PRMSDK.xcodeproj` (`MARKETING_VERSION`); `CHANGELOG.md` |
