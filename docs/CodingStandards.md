# CodingStandards — Quy ước viết code

Quy ước code cho TTCN Promotion SDK (Kotlin). Tuân thủ trước khi commit (AI_AGENT_RULES checklist Pre-commit).

---

## 1. Ngôn ngữ & style chung

- **Kotlin** là ngôn ngữ chính. Tuân theo [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) và Android Kotlin style.
- Thụt lề **4 space**, không dùng tab.
- Ưu tiên `val` hơn `var`; dữ liệu UI state là **immutable** (`data class` + `copy()`).
- Tránh `!!`. Dùng `?.`, `?:`, `requireNotNull`, hoặc xử lý null tường minh.
- Hàm ngắn, đơn nhiệm. Tách logic dùng chung thành extension/use case (AI_AGENT_RULES điều 5).
- JVM toolchain 17, Kotlin 2.2 — được dùng tính năng ngôn ngữ hiện đại (sealed interface, `data object`…).

---

## 2. Quy ước đặt tên

| Thành phần | Quy ước | Ví dụ |
|------------|---------|-------|
| Class / Interface | PascalCase | `PromotionRepositoryImpl` |
| Hàm / biến | camelCase | `searchCustomerVouchers` |
| Hằng số | UPPER_SNAKE_CASE | `TAG_MY_PROMOTION` |
| Package | lowercase, không gạch dưới | `core.data.remote` |
| Use case | `<Động từ><Đối tượng>UseCase` | `CreateRedemptionSessionUseCase` |
| Repository | interface `XxxRepository`, impl `XxxRepositoryImpl` | `PromotionRepository(Impl)` |
| DTO | hậu tố `Request` / `Response` | `RedemptionSessionRequest` |
| DI module | hậu tố `Module` | `NetworkModule` |
| MVI contract | `XxxUiState` / `XxxAction` / `XxxEffect` | `MyPromotionUiState` |

### Tiền tố `PRM`
Nhiều thành phần public/base trong SDK dùng tiền tố **`PRM`** (Promotion) để tránh trùng tên khi nhúng vào host app:
`PRMBaseActivity`, `PRMBaseFragment`, `PRMBaseViewModel`, `PRMEditText`, `PRMEndowViewModel`…
**Giữ nguyên quy ước này** khi thêm base class / custom view dùng chung.

---

## 3. Tổ chức MVI contract

Mỗi feature định nghĩa rõ 3 thành phần (xem `MyPromotionContract.kt` làm mẫu):

```kotlin
data class MyPromotionUiState(
    val isLoading: Boolean = false,
    val vouchers: List<MyVoucherListItem> = emptyList(),
    // ... immutable, có default
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

- State: dùng `data class`, field có default, cập nhật bằng `copy()`.
- Action/Effect: `sealed interface` + `data object`/`data class`.
- Hàm mapping DTO/domain → UI item đặt cùng file contract dưới dạng extension (`fun VoucherItem.toMyVoucherListItem()`).

---

## 4. Coroutines & Flow

- Dùng `viewModelScope` qua hàm `launch { }` của `PRMBaseViewModel` (đã có `CoroutineExceptionHandler`).
- Use case `suspend`; repository/data source `suspend`. Không block main thread.
- UI quan sát qua `StateFlow` (state) và `SharedFlow` (effect) bằng `repeatOnLifecycle`/`flowWithLifecycle`.
- Không `GlobalScope`. Không nuốt exception trong coroutine — để `onError`/handler xử lý.

---

## 5. Tổ chức import & visibility

- Thành phần nội bộ SDK dùng `internal` khi không thuộc public API (vd: DI helper `internal inline fun get()`).
- Chỉ những gì host cần mới để `public` — tập trung ở `ui/entry/`.
- Không import wildcard tùy tiện; theo cấu hình IDE của project.

---

## 6. Comment & tài liệu

- Comment bằng tiếng Việt cho logic nghiệp vụ phức tạp; KDoc cho public API (xem `PromotionUseCases`).
- Không để code chết, biến không dùng, `TODO` mơ hồ không ngữ cảnh.
- Khi sửa public API → cập nhật KDoc **và** docs liên quan.

---

## 7. Resource (XML) — tóm tắt

- Tên resource snake_case có tiền tố theo loại/feature (vd: `prm_fragment_my_promotion.xml`).
- Dùng `dimen` (sdp) cho kích thước responsive; không hardcode dp rải rác.
- Chi tiết ở `XMLViewGuide.md`.

---

## 8. Format & build

- Đảm bảo build pass trước khi commit: `./gradlew :vds-promotion:assemble`.
- Giữ diff tối thiểu, đúng phong cách code xung quanh (mật độ comment, cách đặt tên, idiom).
