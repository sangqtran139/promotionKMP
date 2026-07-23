# CodingStandards — Quy ước viết code

Quy ước code cho TTCN Promotion SDK. Tuân thủ trước khi commit (AI_AGENT_RULES checklist Pre-commit).
Repo có hai ngôn ngữ: **Kotlin** (lõi `:promotionLogic` + UI Android) và **Swift** (UI iOS).

---

## 1. Kotlin — style chung

- Tuân theo [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).
- Thụt lề **4 space**, không tab.
- Ưu tiên `val` hơn `var`; UI state là **immutable** (`data class` + `copy()`).
- Tránh `!!`. Dùng `?.`, `?:`, `requireNotNull`, hoặc xử lý null tường minh.
- Hàm ngắn, đơn nhiệm. Tách logic dùng chung thành extension/use case (AI_AGENT_RULES điều 7).
- Kotlin 2.4, JVM target 11 cho `:promotionLogic` — dùng được `sealed interface`, `data object`,
  `kotlin.uuid.Uuid`, `kotlin.concurrent.Volatile`.

### Riêng cho `commonMain`

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

### Tiền tố

- **Lõi `:promotionLogic`**: *không* prefix. `PromotionUseCases`, `EligibleOffer`, `KeyValueStorage`.
- **Mọi type `public` của UI SDK (cả Android lẫn iOS) dùng tiền tố `PRM`** — `PRMSDK`, `PRMSDKOptions`,
  `PRMSessionConfig`, `PRMBaseFragment`, `PRMEndowView`, `PRMButtonToken`.
- **Quy tắc đặt tên:** thêm `PRM` vào đầu, và **gộp chữ `Promotion` đứng đầu** để không sinh ra
  `PRMPromotion…`:

  | Cũ | Mới |
  |---|---|
  | `PromotionSDK` | `PRMSDK` |
  | `PromotionSDKOptions` | `PRMSDKOptions` |
  | `PromotionSessionConfig` | `PRMSessionConfig` |
  | `PromotionAvailableService` | `PRMAvailableService` |
  | `AppliedDiscount` | `PRMAppliedDiscount` |
  | `ButtonToken` | `PRMButtonToken` |

  Chữ `Promotion` **ở giữa** thì giữ: `ChoosePromotionFragment` → `PRMChoosePromotionFragment`.
- **Bắt buộc trùng chữ Android ↔ iOS.** Cùng một khái niệm phải cùng tên type ở hai bên (điều 10
  [AI_AGENT_RULES](../AI_AGENT_RULES.md)). Đổi tên một bên = đổi cả hai trong cùng thay đổi.
- **Type `internal`/`private` không cần prefix** — chúng không lọt ra host (`MyPromotionViewController`,
  `PromotionSDKImpl`, `PromotionUIStrings`, các ViewModel).
- **Lõi `:promotionLogic` vẫn KHÔNG prefix** — `PromotionUseCases`, `PromotionSDKConfig`, `EligibleOffer`.
  Lõi không nằm trên compile classpath của host nên không có nguy cơ trùng tên.

Mục đích của prefix là tránh trùng tên khi nhúng vào host app.

> ⚠️ Khi đổi tên hàng loạt: dùng `perl -pi -e 's/\bTên\b/TênMới/g'`, **không** dùng `sed` của macOS —
> BSD sed không hỗ trợ word-boundary `\b`, thiếu nó thì `PromotionSDKConfig` (type của **lõi**, không
> được đổi) sẽ bị cắt nhầm thành `PRMSDKConfig`.
>
> ⚠️ Trên iOS, **module** vẫn tên `PromotionSDK` còn **class entry** tên `PRMSDK`. Đừng để chúng trùng
> tên: nếu class trùng tên module, `.swiftinterface` sinh ra `PromotionSDK.PRMVoucher` sẽ bị Swift hiểu
> là type lồng trong class → `error: 'PRMVoucher' is not a member type of class 'PromotionSDK.PromotionSDK'`.

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

- UI Android dùng `launch { }` của `PRMBaseViewModel` (đã có `CoroutineExceptionHandler`).
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
