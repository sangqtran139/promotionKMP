# StorageGuide — Lưu trữ cục bộ

> Bản Android cũ từng có một `DatabaseGuide` mô tả Room và `SharedPrefStorage` như thành phần chính
> của SDK — **không** đúng với repo này (không có database), và phần Room thì **chưa từng đúng** ngay
> cả với repo gốc (xem §4). Tài liệu đó đã bị gỡ, thay bằng file này.

## Mục lục

<!-- toc -->
- [1. SDK này không có database](#1-sdk-này-không-có-database)
- [2. `PromotionPreferences`](#2-promotionpreferences)
  - [2.1. Bắt buộc dùng module chuẩn, KHÔNG dùng `-no-arg`](#21-bắt-buộc-dùng-module-chuẩn-không-dùng--no-arg)
  - [2.2. Vì sao KHÔNG dùng DataStore](#22-vì-sao-không-dùng-datastore)
  - [2.3. `Context` trên Android](#23-context-trên-android)
- [3. Cache feature flag](#3-cache-feature-flag)
- [4. Lịch sử: Room chưa từng được dùng](#4-lịch-sử-room-chưa-từng-được-dùng)
- [5. Quy tắc](#5-quy-tắc)
<!-- /toc -->

---

## 1. SDK này không có database

`:promotionLogic` **không** dùng Room, **không** dùng SQLDelight, **không** có kapt/KSP.
Toàn bộ dữ liệu nghiệp vụ (voucher, redemption, discount) được lấy trực tiếp từ API mỗi lần gọi;
không có cache bền vững cho chúng.

Thứ được lưu cục bộ chỉ gồm: **sáu cờ feature flag** (boolean, + một cờ `has_cache`) và **theme của
host** (một chuỗi JSON, do tầng UI ghi). Tổng cộng tám key.

---

## 2. `PromotionPreferences`

Trừu tượng key-value **đồng bộ**, đặt ở `data/local/PromotionPreferences.kt`.

```kotlin
interface PromotionPreferences {          // public: tầng UI nằm ở module khác
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, default: Boolean = false): Boolean
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun contains(key: String): Boolean
    fun remove(key: String)
    fun clear()

    companion object { const val PREFS_NAME = "promotion_sdk_prefs" }
}

internal expect fun createPreferences(): PromotionPreferences
```

Hiện thực là **một class duy nhất ở `commonMain`** — `SettingsPreferences`, uỷ quyền cho `Settings`
của [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings). `expect/actual`
chỉ còn lo phần **dựng delegate**, thứ duy nhất thật sự khác nhau giữa hai nền tảng:

| Nền tảng | Delegate | Ghi chú |
|---|---|---|
| Android | `SharedPreferencesSettings(getSharedPreferences("promotion_sdk_prefs", MODE_PRIVATE))` | Cần `Context` |
| iOS | `NSUserDefaultsSettings(NSUserDefaults(suiteName = "promotion_sdk_prefs"))` | **Suite riêng**, không phải `standardUserDefaults` |

Dùng suite riêng trên iOS để `clear()` không xoá nhầm preference của app host — tương đương việc
Android mở file prefs riêng.

`Settings` **không** lọt ra bề mặt public: build khai `implementation`, không `api`. Host và SKIE
bridge chỉ thấy `PromotionPreferences` do ta sở hữu, nên đổi thư viện sau này không phải breaking change.

### 2.1. Bắt buộc dùng module chuẩn, KHÔNG dùng `-no-arg`

`multiplatform-settings-no-arg` cho cú pháp `Settings()` không tham số, bỏ được `expect/actual` —
nhưng **cấm dùng trong SDK này**, vì nó gắn cứng:

| | no-arg dùng | Hậu quả |
|---|---|---|
| Android | `PreferenceManager.getDefaultSharedPreferences()` | `clear()` xoá **prefs mặc định của app host** |
| iOS | `NSUserDefaults.standardUserDefaults` | Xoá nhầm dữ liệu host |
| Context | `androidx-startup` | Nhét ContentProvider vào manifest mọi app host |

`PromotionContainer.clear()` là API public host gọi khi logout — với no-arg, một lần logout xoá sạch
setting của họ. Tự truyền delegate để giữ file/suite riêng là điều kiện tiên quyết, không thương lượng.

### 2.2. Vì sao KHÔNG dùng DataStore

**Lý do gốc: DataStore không có API đọc đồng bộ, và SDK này bắt buộc phải đọc đồng bộ trong `initialize()`.**

Toàn bộ interface của nó chỉ có hai thành viên — không cái nào đồng bộ:

```kotlin
public interface DataStore<T> {
    public val data: Flow<T>
    public suspend fun updateData(transform: suspend (T) -> T): T
}
```

KDoc của `data` còn chặn luôn cửa hậu: *"Do not layer a cache on top of this API: it will be
impossible to guarantee consistency."*

Cơ chế: SharedPreferences/NSUserDefaults nạp cả file vào map trong RAM lúc mở, nên đọc sau đó là truy
cập bộ nhớ. DataStore **cố tình không** giữ snapshot — `inMemoryCache` rỗng cho tới lần collect đầu
tiên, và mọi truy cập đi qua một actor coroutine để đảm bảo giao dịch. Đọc đồng bộ = block actor đó.

Hai chỗ trong repo đòi đọc đồng bộ:

1. `PromotionSDK.kt` — `PromotionThemeRegistry.configure(resolved ?: PromotionThemeStore.load())`
   chạy **trong `initialize()`**, trước khi Activity đầu tiên inflate. Async ⇒ nháy sai theme.
   Đối ứng iOS: `PromotionSDKImpl.restoreOrApplyTheme`.
2. `FeatureFlagRepositoryImpl` — `cachedFlags = localDataSource.load()` ở **property initializer của
   constructor**; constructor Kotlin không thể `suspend`.

Chi phí nếu vẫn làm: breaking change `initialize()` trên cả hai nền tảng, thêm `datastore-core` +
`okio` + `atomicfu` vào SDK phân phối, migration iOS viết tay (Android có `SharedPreferencesMigration`,
iOS không có gì), và phải giữ DataStore singleton **ngoài** vòng đời DI — vì `OkioStorage` ném
`"There are multiple DataStores active for the same file"` khi `clear()` → `initialize()` lại, vốn là
luồng hợp lệ của SDK này.

Tất cả để lưu sáu boolean và một chuỗi JSON. **Đừng đánh giá lại trừ khi ràng buộc đồng bộ ở mục (1)
và (2) biến mất.**

> Nếu sau này cần **observe** thay đổi (vd theme đổi lúc runtime): dùng `ObservableSettings` của
> thư viện hiện tại — không cần DataStore, không cần dependency mới.

### 2.3. `Context` trên Android

`createPreferences()` phía Android lấy `applicationContext` từ `AndroidContextHolder`, được nạp bởi:

```kotlin
PromotionContainer.initialize(context, config)   // androidMain
```

Gọi thẳng `PromotionContainer.initialize(config)` trên Android sẽ ném lỗi rõ ràng khi feature flag cần
tới storage. **Trên Android luôn dùng overload có `Context`.**

SDK chỉ giữ `applicationContext`, không giữ Activity hay View context.
`PromotionContainer.clear()` gọi `clearPlatformState()` để nhả nó.

---

## 3. Cache feature flag

`FeatureFlagLocalDataSource` là nơi duy nhất trong **lõi** chạm `PromotionPreferences` (tầng UI chạm
qua `PromotionContainer.preferences` để lưu theme — xem §5.2).

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
trong `build.gradle.kts`, và có bốn file trong `data/local/`. Nhưng cả bốn đều là **stub rỗng**:

```kotlin
abstract class PromotionDatabase   // 4 dòng, không @Database
interface PromotionCacheDao        // 4 dòng, không @Dao
interface FeatureFlagDao           // 4 dòng, không @Dao
```

Toàn repo có **0 dòng `import androidx.room`**, và không file nào tham chiếu tới chúng.
Room + kapt là dependency chết, chỉ làm chậm build.

Vì vậy khi chuyển sang KMP, ba class trên **không được port**. `SharedPrefStorage` thì được port —
nó là code thật (commit `2a69056 update feature flag api`), và trở thành `PromotionPreferences`.
Về sau phần thân của nó được thay bằng `SettingsPreferences` dùng chung ở `commonMain` (§2); interface
giữ nguyên nên host không phải sửa gì.

---

## 5. Quy tắc

1. **Không thêm database** vào `:promotionLogic` khi chưa có yêu cầu rõ ràng. Thêm Room/SQLDelight
   nghĩa là thêm KSP — sẽ ảnh hưởng target iOS và thời gian build.
2. Trong lõi, chỉ `FeatureFlagLocalDataSource` được chạm `PromotionPreferences`. Repository/UseCase
   không đọc thẳng storage. Tầng UI dùng `PromotionContainer.preferences` và **tự giữ key của mình**
   (vd `PromotionThemeStore` hai bên).
3. Không lưu token, thông tin khách hàng, hay dữ liệu nhạy cảm vào `PromotionPreferences` — nó không
   mã hoá. Token do host cấp qua `PromotionRequestContextProvider` mỗi request, SDK không lưu.
4. Đổi schema key hoặc thêm kiểu dữ liệu → **cập nhật file này** (AI_AGENT_RULES điều 8). Thêm kiểu
   mới chỉ sửa `SettingsPreferences` ở `commonMain` — không đụng `actual`.
5. **Không** đổi sang `multiplatform-settings-no-arg` (§2.1) và **không** đổi sang DataStore (§2.2).
