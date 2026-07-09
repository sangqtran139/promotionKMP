# StorageGuide — Lưu trữ cục bộ

> Thay thế `DatabaseGuide.md` của bản Android cũ. File đó mô tả Room và `SharedPrefStorage`
> như thành phần chính của SDK — không đúng với repo này, và phần Room thì **chưa từng đúng**
> ngay cả với repo gốc (xem §4).

---

## 1. SDK này không có database

`:promotionLogic` **không** dùng Room, **không** dùng SQLDelight, **không** có kapt/KSP.
Toàn bộ dữ liệu nghiệp vụ (voucher, redemption, discount) được lấy trực tiếp từ API mỗi lần gọi;
không có cache bền vững cho chúng.

Thứ duy nhất được lưu cục bộ là **cờ feature flag** — sáu giá trị boolean.

---

## 2. `KeyValueStorage`

Trừu tượng key-value đồng bộ, đặt ở `core/data/local/KeyValueStorage.kt`.

```kotlin
internal interface KeyValueStorage {
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun contains(key: String): Boolean
    fun remove(key: String)
    fun clear()

    companion object { const val PREFS_NAME = "promotion_sdk_prefs" }
}

internal expect fun createKeyValueStorage(): KeyValueStorage
```

| Nền tảng | Hiện thực | Ghi chú |
|---|---|---|
| Android | `SharedPrefStorage` → `SharedPreferences("promotion_sdk_prefs", MODE_PRIVATE)` | Cần `Context` |
| iOS | `UserDefaultsStorage` → `NSUserDefaults(suiteName = "promotion_sdk_prefs")` | **Suite riêng**, không phải `standardUserDefaults` |

Dùng suite riêng trên iOS để `clear()` không xoá nhầm preference của app host — tương đương việc
Android mở file prefs riêng.

### Vì sao không dùng `multiplatform-settings` hay DataStore

Nhu cầu hiện tại là sáu cờ boolean. Một interface bốn hàm với hai `actual` rẻ hơn việc kéo thêm
dependency vào SDK phân phối cho app host (AI_AGENT_RULES điều 6). Nếu sau này cần lưu kiểu dữ liệu
phức tạp hoặc cần Flow, hãy đánh giá lại — và cập nhật file này.

### `Context` trên Android

`createKeyValueStorage()` phía Android lấy `applicationContext` từ `AndroidContextHolder`, được nạp bởi:

```kotlin
PromotionContainer.init(context, config)   // androidMain
```

Gọi thẳng `PromotionContainer.init(config)` trên Android sẽ ném lỗi rõ ràng khi feature flag cần tới
storage. **Trên Android luôn dùng overload có `Context`.**

SDK chỉ giữ `applicationContext`, không giữ Activity hay View context.
`PromotionContainer.clear()` gọi `clearPlatformState()` để nhả nó.

---

## 3. Cache feature flag

`FeatureFlagLocalDataSource` là nơi duy nhất chạm `KeyValueStorage`.

```
key: "feature_flag_has_cache"     → đã từng lưu cờ hay chưa
key: "PROMOTION.ENABLE_ALL"       → Boolean
key: "PROMOTION.VOUCHER_APPLY"    → Boolean
key: "PROMOTION.VOUCHER_REDEEM"   → Boolean
key: "PROMOTION.VOUCHER_SELECTION"→ Boolean
key: "PROMOTION.VOUCHER_DETAIL"   → Boolean
key: "PROMOTION.VOUCHER_LIST"     → Boolean
```

Hành vi:

- **Chưa có `feature_flag_has_cache`** → `load()` trả `PromotionFeatureFlags.AllEnabled` (bật hết).
  SDK không tự khoá tính năng khi chưa gọi được API lần nào.
- `FeatureFlagRepositoryImpl` đọc cache **một lần** lúc khởi tạo vào `@Volatile cachedFlags`.
- `refresh()` gọi API; thành công thì ghi cache + cập nhật `cachedFlags`; **thất bại thì nuốt lỗi**
  và giữ nguyên cờ cũ (`runCatching { … }.getOrNull()?.let { … }`).
- `isEnabled(name)` đọc từ `cachedFlags` — đồng bộ, không chạm I/O.

---

## 4. Lịch sử: Room chưa từng được dùng

Repo gốc `ttcn-promotion-android-sdk` khai báo `room-runtime`, `room-ktx`, `kapt(room-compiler)`
trong `build.gradle.kts`, và có bốn file trong `core/data/local/`. Nhưng cả bốn đều là **stub rỗng**:

```kotlin
abstract class PromotionDatabase   // 4 dòng, không @Database
interface PromotionCacheDao        // 4 dòng, không @Dao
interface FeatureFlagDao           // 4 dòng, không @Dao
```

Toàn repo có **0 dòng `import androidx.room`**, và không file nào tham chiếu tới chúng.
Room + kapt là dependency chết, chỉ làm chậm build.

Vì vậy khi chuyển sang KMP, ba class trên **không được port**. `SharedPrefStorage` thì được port —
nó là code thật (commit `2a69056 update feature flag api`), và trở thành `KeyValueStorage`.

---

## 5. Quy tắc

1. **Không thêm database** vào `:promotionLogic` khi chưa có yêu cầu rõ ràng. Thêm Room/SQLDelight
   nghĩa là thêm KSP — sẽ ảnh hưởng target iOS và thời gian build.
2. Chỉ `FeatureFlagLocalDataSource` được chạm `KeyValueStorage`. Repository/UseCase không đọc thẳng storage.
3. Không lưu token, thông tin khách hàng, hay dữ liệu nhạy cảm vào `KeyValueStorage` — nó không mã hoá.
   Token do host cấp qua `PromotionRequestContextProvider` mỗi request, SDK không lưu.
4. Đổi schema key hoặc thêm kiểu dữ liệu → **cập nhật file này** (AI_AGENT_RULES điều 8).
