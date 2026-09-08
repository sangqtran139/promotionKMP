# CodingStandards — Quy ước viết code

Quy ước code cho TTCN Promotion SDK. Tuân thủ trước khi commit (AI_AGENT_RULES checklist Pre-commit).
Repo có hai ngôn ngữ: **Kotlin** (lõi `:promotionLogic` + UI Android) và **Swift** (UI iOS).

## Mục lục

<!-- toc -->
- [1. Kotlin — style chung](#1-kotlin--style-chung)
  - [1.1. Riêng cho `commonMain`](#11-riêng-cho-commonmain)
- [2. Quy ước đặt tên — Kotlin](#2-quy-ước-đặt-tên--kotlin)
  - [2.1. Tiền tố](#21-tiền-tố)
- [3. MVI contract (UI Android)](#3-mvi-contract-ui-android)
- [4. Coroutines & Flow](#4-coroutines--flow)
- [5. Visibility](#5-visibility)
- [6. Swift — tóm tắt](#6-swift--tóm-tắt)
- [7. Comment & tài liệu](#7-comment--tài-liệu)
- [8. Resource (XML) — tóm tắt](#8-resource-xml--tóm-tắt)
- [9. Format & build](#9-format--build)
<!-- /toc -->

---

## 1. Kotlin — style chung

- Tuân theo [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Thụt lề **4 space**, không tab.
- Ưu tiên `val` hơn `var`; UI state là **immutable** (`data class` + `copy()`).
- Tránh `!!`. Dùng `?.`, `?:`, `requireNotNull`, hoặc xử lý null tường minh.
- Hàm ngắn, đơn nhiệm. Tách logic dùng chung thành extension/use case (AI_AGENT_RULES điều 7).
- Kotlin 2.2, JVM target 17 cho `:promotionLogic` — dùng được `sealed interface`, `data object`,
  `kotlin.uuid.Uuid`, `kotlin.concurrent.Volatile`.

### 1.1. Riêng cho `commonMain`

- **Không** import `android.*` hay `platform.*`. Cần API nền tảng → `expect`/`actual`.
- **Không** dùng `java.util.*`, `java.io.*`, `synchronized`, `@Volatile` (của JVM).
  Thay bằng `kotlin.uuid.Uuid`, `kotlinx.io.IOException`, `SdkLock`, `kotlin.concurrent.Volatile`.
- **Không** đặt chuỗi hiển thị (tiếng Việt) — copy thuộc tầng UI.

---

## 2. Quy ước đặt tên — Kotlin

| Thành phần | Quy ước | Ví dụ |
|------------|---------|-------|
| Class / Interface | PascalCase | `PromotionRepositoryImpl` |
| Hàm / biến | camelCase | `searchCustomerVouchers` |
| Hằng số | UPPER_SNAKE_CASE | `TAG_MY_PROMOTION` |
| Package | lowercase, không gạch dưới | `core.data.remote` |
| Use case | `<Động từ><Đối tượng>UseCase` | `CreateRedemptionSessionUseCase` |
| Repository | interface `XxxRepository`, impl `XxxRepositoryImpl` | `PromotionRepository(Impl)` |
| DTO | hậu tố `Request` / `Response` | `RedemptionSessionRequest` |
| ApiService bản Ktor | tiền tố `Ktor` | `KtorPromotionApiService` |
| DI module | hậu tố `Module` | `NetworkModule` |
| MVI contract | `XxxUiState` / `XxxAction` / `XxxEffect` | `MyPromotionUiState` |

### 2.1. Tiền tố

**`PRM` là tên của package/module, KHÔNG phải tiền tố dán vào mọi class.** Việc tránh trùng tên khi
nhúng vào host do **namespace** lo, nên tên type giữ nguyên nghĩa, dễ đọc:

| | Android | iOS |
|---|---|---|
| Namespace / module | `com.ttcn.prm` | `PRM` (`import PRM`) |
| Type bề mặt host | `PromotionSDK`, `PromotionSDKOptions`, `PromotionSessionConfig`, `AppliedDiscount` | y hệt |

- **Lõi `:promotionLogic`**: *không* prefix, package riêng `com.ttcn.promotionsdk.*`.
  `PromotionUseCases`, `PromotionSDKConfig`, `EligibleOffer`.
- **UI SDK**: base class và custom view **public** vẫn dùng `PRM` — `PRMBaseFragment`, `PRMEndowView`,
  `PRMButton`. Đây là những thứ host **kế thừa/đặt thẳng vào layout** nên tên dễ đụng nhất.
- Type bề mặt SDK khác giữ tên mô tả: `PromotionSDK`, `PromotionSDKApi`, `ButtonToken`… — **không**
  ép thành `PRMSDK`/`PRMButtonToken` (tối nghĩa, và namespace đã đủ tách biệt).
- **Bắt buộc trùng chữ Android ↔ iOS.** Cùng một khái niệm phải cùng tên type ở hai bên (điều 10
  [AI_AGENT_RULES](../AI_AGENT_RULES.md)). Đổi tên một bên = đổi cả hai trong cùng thay đổi.
- **Type `internal`/`private`** không cần bận tâm (`MyPromotionViewController`, `PromotionSDKImpl`,
  `PromotionUIStrings`, các ViewModel).

> ⚠️ **Module iOS tên `PRM`, không được đặt trùng tên một type public bên trong.** Nếu module trùng tên
> class (từng thử `PromotionSDK`/`PromotionSDK`), `.swiftinterface` sinh ra `PromotionSDK.PromotionVoucher`
> và Swift hiểu là type **lồng trong class** → build gãy:
> `error: 'PromotionVoucher' is not a member type of class 'PromotionSDK.PromotionSDK'`.
>
> ⚠️ **Đổi tên hàng loạt: dùng `perl -pi -e 's/\bTên\b/TênMới/g'`, KHÔNG dùng `sed` của macOS** —
> BSD sed không có word-boundary `\b`. Thiếu nó thì `PromotionSDKConfig` (type của **lõi**) bị cắt nhầm.
>
> ⚠️ **Android: `core.utils` (module UI) khác `core.util` (lõi)** — số nhiều/số ít. Khi sed package phải
> phân biệt, nếu không sẽ đổi nhầm import của lõi thành package của module.

---

## 3. MVI contract (UI Android)

```kotlin
data class MyPromotionUiState(
    val isLoading: Boolean = false,
    val vouchers: List<MyVoucherListItem> = emptyList(),
)

sealed interface MyPromotionAction {
    data object Refresh : MyPromotionAction
    data class SelectTab(val tabCode: String) : MyPromotionAction
}

sealed interface MyPromotionEffect {
    data class OpenVoucherDetail(val voucherId: String) : MyPromotionEffect
    data class ShowError(val errorCode: String) : MyPromotionEffect
}
```

- State: `data class`, field có default, cập nhật bằng `copy()`.
- Action/Effect: `sealed interface` + `data object` / `data class`.
- Mapping domain → UI item đặt cùng file contract dưới dạng extension
  (`fun VoucherItem.toMyVoucherListItem()`). **Không** dùng thẳng DTO ở UI.

---

## 4. Coroutines & Flow

- UI Android dùng `launch { }` của `PRMStoreViewModel` (đã có `CoroutineExceptionHandler`).
- Use case / repository / data source đều `suspend`. Không block main thread.
- UI quan sát `StateFlow` (state) và `SharedFlow` (effect) qua `repeatOnLifecycle`.
- Không `GlobalScope`. Không nuốt exception trong coroutine.
- Luôn rethrow `CancellationException` khi bắt `Throwable` chung.

---

## 5. Visibility

- Thành phần nội bộ dùng `internal` (DI helper, DTO, data source, use case đơn lẻ).
- Chỉ những gì host cần mới `public`. Bề mặt public của `:promotionLogic` gồm:
  `PromotionContainer`, `PromotionUseCases`, `PromotionFeatureFlagUseCases`, `PromotionSDKConfig`,
  `PromotionResult`, và các domain model.
- Lưu ý KMP: **inline function không truy cập được `private`**. Trong `SdkDi`, `registry` để
  visibility mặc định của class `internal` chính vì lý do này.
- Không import wildcard tuỳ tiện.

---

## 6. Swift — tóm tắt

- Tuân theo SwiftLint (`.swiftlint.yml` ở repo iOS).
- Chữ ký `public` chỉ dùng type Foundation/UIKit — không lộ RxSwift hay type Kotlin.
- `weak` cho `viewController` / `navigator` trong Router.
- Mọi subscription `.disposed(by: disposeBag)`.
- Chi tiết: [IosUIGuide.md](../ios/UIGuide.md).

---

## 7. Comment & tài liệu

- Comment tiếng Việt cho logic nghiệp vụ phức tạp; KDoc/DocC cho public API.
- Comment chỉ nêu **ràng buộc mà code không tự nói được** (vì sao phải reentrant lock, vì sao
  `encodeDefaults = true`), không mô tả lại dòng bên dưới.
- Không để code chết, biến không dùng, `TODO` mơ hồ.
- Sửa public API → cập nhật KDoc **và** docs liên quan (AI_AGENT_RULES điều 8).

---

## 8. Resource (XML) — tóm tắt

- Tên resource snake_case, tiền tố `prm_` (vd `prm_fragment_my_promotion.xml`).
- Dùng `dimen` (sdp) cho kích thước responsive; không hardcode dp.
- Chi tiết: [AndroidUIGuide.md](../android/UIGuide.md).

---

## 9. Format & build

Trước khi commit, build và test **cả hai nền tảng**:

```bash
./gradlew :promotionLogic:assemble
./gradlew :promotionLogic:testAndroidHostTest :promotionLogic:iosSimulatorArm64Test
```

Giữ diff tối thiểu, đúng phong cách code xung quanh (mật độ comment, cách đặt tên, idiom).
