
# Theming — Hệ thống theme & token

TTCN Promotion SDK cho phép host app **tùy biến giao diện** (màu, bo góc…) thông qua hệ thống **token**.
SDK không ép host dùng style cứng — host truyền các token màu, SDK tự áp vào view tương ứng.

**Hai nền tảng song ánh.** Cùng sáu token, cùng tên type, cùng tên field, cùng một định dạng JSON.
Sửa một bên thì sửa cả hai.

| Android `ui/theme/` | iOS `PromotionSDKUI/Theme/` |
|---|---|
| `PromotionSDKTheme.kt` + `token/*.kt` | `PromotionSDKTheme.swift` |
| `token/*.kt` (6 token) | `Token/*.swift` (6 token) |
| `PromotionThemeJson.kt` | `PromotionThemeJson.swift` |
| `ThemeHex.kt` | `ThemeHex.swift` |
| `PromotionThemeStore.kt` | `PromotionThemeStore.swift` |
| `PromotionThemeDefaults.kt` | `PromotionThemeDefaults.swift` |
| `PromotionThemeDisplay.kt` | `PromotionThemeDisplay.swift` |
| `PromotionThemeRegistry.kt` (internal) | `PRMThemeRegistry` trong PRMDesignKit (internal) — cố hữu, §4 |

---

## 1. Kiến trúc tổng quan

```
            (Host app)
                │ truyền token màu
                ▼
   PromotionSDK.configure(theme) / .currentTheme()   │ iOS: sdk.configure(theme:) / sdk.currentTheme

                │
                ▼
   PromotionThemeRegistry (internal, @Volatile singleton)
        giữ PromotionSDKTheme hiện tại
                │ getter: buttonToken(), discountBadgeToken()...
                ▼
   *Applier / *ThemeApplier  ──uses──▶  TokenExtensions (helper áp màu/bo góc)
        áp token vào View thật (PRMButton, RecyclerView item, TabLayout...)
```

Các lớp phụ trợ:
- `PromotionThemeDefaults` — giá trị mặc định **thật** của SDK (xem §6).
- `PromotionThemeDisplay` — biểu diễn token dạng **chuỗi hex** + `merge`/`toToken`; phục vụ màn cấu
  hình theme của host. Tên field của nó (`button`, `searchBar`…) cũng chính là **key của JSON**.
- `PromotionThemeJson` + `ThemeHex` — serialize (cùng tên hai bên).
- `PromotionThemeStore` — persist (xem §3.1).

> **SDK tự lưu theme.** Host cấu hình một lần (`configure` hoặc `init(theme=...)`); lần mở app sau chỉ
> cần `init` không truyền theme, SDK **tự khôi phục**. Persistence dùng chung ở lõi
> (`PromotionContainer.preferences` → `PromotionPreferences`, `expect/actual` chung với FeatureFlag) —
> một key, một cơ chế, không phải mỗi UI SDK tự viết một store. Xem §3.1.

---

## 2. Token — đơn vị tùy biến

`PromotionSDKTheme` gom **6 token**. Mọi field đều **nullable** — `null` nghĩa là "giữ mặc định của
SDK" (applier bỏ qua). Nhóm `null` = giữ nguyên cả nhóm.

| Token | Field | Áp vào |
|-------|-------|--------|
| `ButtonToken` | `backgroundColor`, `textColor`, `shadowColor`, `cornerRadius` | `PRMButton` / `PRMButton` |
| `SearchBarToken` | `borderColor`, `hintTextColor`, `textColor`, `iconColor`, `cornerRadius` | `PRMSearchField` / `PRMSearchTextField` |
| `ListItemToken` | `linkTextColor`, `usedBadgeTextColor`, `usedBadgeBackgroundColor`, `radioButtonStrokeColor`, `radioButtonSelectedStrokeColor` | Item voucher (`PromotionListItemApplier` / `PromotionCardView`) |
| `TabChipToken` | `activeBackgroundColor`, `inactiveBackgroundColor`, `activeTextColor`, `inactiveTextColor`, `cornerRadius` | Tab chip (`TabChipThemeApplier` / `PromotionTabView`) |
| `TabUnderlineToken` | `indicatorColor`, `activeTextColor`, `inactiveTextColor`, `backgroundColor` | Tab gạch chân (`TabLayoutThemeApplier` / `UnderlinedSegmentControlItem`) |
| `DiscountBadgeToken` | `availableTextColor`, `unavailableTextColor`, `availableBackgroundColor`, `unavailableBackgroundColor`, `actionTextColor` | Badge giảm giá (`DiscountBadgeApplier` / `PRMEndowView`) |

Màu: `@ColorInt` (Kotlin) / `UIColor` (Swift). Bo góc: `Float` **dp** / `CGFloat` **pt** — cùng con số
trong JSON.

> `radioButtonSelectedStrokeColor` mang chữ "Stroke" vì lịch sử, nhưng nó được áp làm màu **fill**
> của nút radio (xem `PromotionListItemApplier.kt:23`). Giữ tên để hai nền tảng không lệch.

---

## 3. JSON — một định dạng, dùng chung hai nền tảng

```json
{
  "button":     { "backgroundColor": "#EE0033", "cornerRadius": 8.0 },
  "listItem":   { "radioButtonSelectedStrokeColor": "#FFEE0033" }
}
```

- Màu là hex **`#AARRGGBB`** — alpha đứng **trước**, theo quy ước `Color.parseColor` của Android
  (**không** phải CSS). Rút gọn còn `#RRGGBB` khi màu đục.
- Key nhóm là `button`/`searchBar`/… (hình dạng của `PromotionThemeDisplay`), **không** phải
  `buttonToken`/… (hình dạng của API).
- Nhóm vắng mặt = giữ mặc định SDK cho nhóm đó.

```kotlin
PromotionThemeJson.toJson(theme)  // Android
PromotionThemeJson.fromJson(json)
```
```swift
PromotionThemeJson.toJson(theme)  // iOS — cùng tên type, cùng hàm
PromotionThemeJson.fromJson(json)
```

### 3.0. Dùng một file JSON cho cả hai nền tảng — **được**

Đối tác ship **một** file `promotion_theme.json`, hai nền tảng đọc chung. Host chỉ cần đọc file
thành chuỗi rồi `fromJson` — SDK không đọc file hộ, và **không** có API nhận đường dẫn file:

```kotlin
// Android — file ở assets/
val json = context.assets.open("promotion_theme.json").bufferedReader().use { it.readText() }
val theme = PromotionThemeJson.fromJson(json)   // null = JSON hỏng
PromotionSDK.configure(theme)
```
```swift
// iOS — file trong bundle
let url = Bundle.main.url(forResource: "promotion_theme", withExtension: "json")!
let theme = PromotionThemeJson.fromJson(try String(contentsOf: url, encoding: .utf8))
sdk.configure(theme: theme)
```

`fromJson` trả `null`/`nil` khi JSON hỏng — **không ném lỗi**, nên host phải tự xử: bỏ qua (giữ theme
cũ) hay báo lỗi. Đưa thẳng `null` vào `configure` sẽ **xoá** theme về mặc định, thường không phải ý
bạn muốn.

Cùng bộ token đó cũng dựng được **bằng object**, không qua JSON — chọn cách nào là tuỳ host:

| | File JSON | Object trong code |
|---|---|---|
| Đổi màu không cần build lại app | ✅ (nếu file tải từ server) | ❌ |
| Sai chính tả key → phát hiện lúc | chạy (im lặng bỏ qua field lạ) | biên dịch |
| Ship chung Android + iOS | ✅ một file | ❌ viết hai lần |

**Demo chạy được** ở màn Theme Playground, hai nút cạnh nút "Apply": *Load JSON file* và *Apply
object*. Cùng cho ra bộ teal `#2CA196` để thấy hai đường đi ra một kết quả.

| | Android | iOS |
|---|---|---|
| File theme | `androidApp/src/main/assets/promotion_theme.json` | `iosApp/iosApp/promotion_theme.json` |
| Hai nguồn theme | `DemoThemeSource.kt` | `DemoThemeSource.swift` |
| Nút + xử lý | `ThemePreviewFragment` | `ThemePreviewViewController` |

### 3.1. Persistence — SDK tự lưu, tự khôi phục

Serialize (trên) + lưu qua `PromotionThemeStore` (giữ key `promotion_theme_config_v1`) → xuống lõi
`PromotionContainer.preferences` (`PromotionPreferences`, `expect/actual`: SharedPreferences / UserDefaults).
`configure(theme)` lưu, `configure(null)` xoá, `init` không truyền theme thì `load()` khôi phục. Một
key và một cơ chế cho cả hai nền tảng — đã kiểm chứng round-trip trên thiết bị (save/restore/clear).

> **Từng hỏng thật, theo hai cách.** Comment `// mirror Android PromotionThemeJson` trong file Swift
> là sai sự thật: Android `gson.toJson(config)` thẳng nên sinh `{"buttonToken":{"backgroundColor":-1179597}}`
> — khác cả key lẫn kiểu. Và ngay cả "hex" cũng khác: iOS ghi `#RRGGBBAA`, nên `#EE0033FF` (đỏ đục)
> đọc sang Android thành `alpha=EE, b=FF` — một màu xanh mờ, **sai lặng lẽ, không exception**.
>
> `PromotionThemeJsonTest` (Android) khoá định dạng này lại, gồm cả một fixture JSON hình dạng iOS.

---

## 4. Registry & Applier

### `PromotionThemeRegistry` (internal)
- Singleton `@Volatile` giữ `PromotionSDKTheme?`.
- `configure(theme)` ghi đè; getter trả từng token (`buttonToken()`, `discountBadgeToken()`…).
- Token chưa cấu hình → trả `null`.

### Applier
Mỗi loại view có một applier **idempotent, null-safe** (`if (token == null) return`):

| Applier | Nhiệm vụ |
|---------|----------|
| `TabChipThemeApplier.applyChip(tv, selected, token)` | Màu nền/chữ chip theo trạng thái + bo góc |
| `TabLayoutThemeApplier.apply(tabs, token)` | Indicator, màu chữ active/inactive, nền `TabLayout` |
| `PromotionListItemApplier.apply(binding, token)` | Màu link, badge "đã dùng", radio chọn voucher |
| `DiscountBadgeApplier.apply(binding, token, available)` | Badge giảm giá theo trạng thái khả dụng |

Helper áp giá trị nằm ở `ui/utils/TokenExtensions.kt`. Tất cả **bỏ qua khi giá trị null**.

Bên iOS không có applier riêng: mỗi component tự đọc `PRMThemeRegistry.shared.<token>()` và dùng
`?? default` ở chỗ cần.

---

## 5. Public API cho host

Theme lifecycle nằm trên **`PromotionSDK`** ở cả hai nền tảng (khác duy nhất: Android là `object`
toàn cục, iOS là instance — xem §7). Helper serialize/preview gọi thẳng type, cùng tên hai bên.

```kotlin
// Android
PromotionSDK.configure(theme: PromotionSDKTheme?)  // áp + lưu; null = xoá, về mặc định
PromotionSDK.currentTheme(): PromotionSDKTheme?
PromotionThemeJson.toJson(theme) / .fromJson(json)
PromotionThemeDefaults.theme(context)              // giá trị mặc định SDK  (internal — dùng qua Display)
PromotionThemeDisplay.load(context) / .mergeWithSaved(context, sdk, saved) / .themeFromDisplayValues(display, sdk)
```
```swift
// iOS
sdk.configure(theme:)                              // áp + lưu; nil = xoá, về mặc định
sdk.currentTheme
PromotionThemeJson.toJson(_) / .fromJson(_)
PromotionThemeDefaults.theme
PromotionThemeDisplay.load() / .mergeWithSaved(sdk:saved:) / .themeFromDisplayValues(_:sdk:)
```

Khác biệt **cố hữu**: Android nhận `Context` (đọc màu resource) và có thêm `pxToDp`; iOS dùng pt nên
không cần. Tên type/hàm còn lại khớp nhau.

> `PromotionTheme` (object facade cũ) đã bị gỡ: nó trùng `PromotionSDK` (SDK cũng là object toàn cục),
> tách theme config ra khỏi `PromotionSDK` trong khi iOS đặt trên `sdk`, và có 5 hàm chết. Lifecycle
> gộp vào `PromotionSDK` cho khớp iOS.

### Hai cách cấu hình theme

**Cách 1 — qua `PromotionSDKOptions.theme` khi init (khuyến nghị):**
```kotlin
val options = PromotionSDKOptions(
    config = sdkConfig,
    theme = PromotionSDKTheme(
        buttonToken = ButtonToken(backgroundColor = Color.parseColor("#EE0033"), cornerRadius = 8f),
        tabChipToken = TabChipToken(activeBackgroundColor = ..., activeTextColor = ...),
    ),
)
PromotionSDK.initialize(context, options)
```

**Cách 2 — gọi `PromotionSDK.configure()` SAU `init()`:**
```kotlin
PromotionSDK.initialize(context, PromotionSDKOptions(config = sdkConfig))
PromotionSDK.configure(PromotionSDKTheme(buttonToken = ...))
```

Không truyền `theme` (mặc định `null`) → SDK **tự khôi phục** theme đã lưu lần trước (§3.1).

---

## 6. Giá trị mặc định — **đồng nhất hai nền tảng**

`PromotionThemeDefaults` cho cùng một bộ màu trên Android và iOS. **Màu lấy theo Android** (nguồn
`R.color.*`); bên iOS dùng đúng token design system của PRMDesignKit có giá trị **bằng** giá trị đó:

| Android `R.color` = hex | iOS `Colors.*` |
|---|---|
| `#EE0033` (đỏ VTP) | `tokenViettelPayRed100` |
| `#FFFFFF` / `#000000` | `tokenWhite` / `tokenBlack` |
| `#E9E9E9` / `#A7A7A7` / `#222222` | `tokenDark10` / `tokenDark40` / `tokenDark100` |
| `#F4F4F4` / `#4E4E4E` / `#7A7A7A` / `#FBFBFB` | `tokenDark05` / `tokenDark80` / `tokenDark60` / `tokenDark02` |
| `#2CA196` (teal) / `#EAF6F4` | `tokenPineBlue100` / `tokenPineBlue10` |

> **Từng lệch.** iOS trước đây chọn nhầm token: `tokenRed100 = #FF645C` (đỏ san hô) thay vì
> `tokenViettelPayRed100 = #EE0033`, `tokenDark60` thay vì `tokenDark100`… nên badge giảm giá ra đỏ
> san hô còn Android teal. Nay khớp từng dòng — nếu design system đổi token, cả hai đi theo.

`cornerRadius` là ngoại lệ cố hữu: Android dùng **sdp** (co giãn theo màn, không có một giá trị duy
nhất), iOS dùng pt cố định (button 24, search 8, chip 7). Không phải màu nên không đồng bộ tuyệt đối.

Ba field iOS **không có default trong code** (`listItem.linkTextColor`, hai màu radio): component giữ
màu XIB / ảnh asset. `PromotionThemeDefaults.swift` khai đúng giá trị Android để preview không trống.

---

## 7. ⚠️ Quy tắc thứ tự bắt buộc (foot-gun)

`PromotionSDK.initialize()` **luôn ghi đè** registry bằng `options.theme`, mà `PromotionSDKOptions.theme`
**mặc định là rỗng** (`PromotionSDKTheme()`).

> ❌ **Sai:** gọi `PromotionSDK.configure(myTheme)` **trước** rồi `PromotionSDK.initialize(context, PromotionSDKOptions(config))`
> mà **không** truyền theme vào options → theme vừa set bị **xoá âm thầm** về mặc định.

✅ **Đúng:** dùng **Cách 1** (truyền theme vào `options`) **hoặc** **Cách 2** (`configure()` **sau** `init()`).

`PromotionSDK.release()` reset theme về null (cùng với clear DI/callback).

---

## 8. Giới hạn đã biết

- **Re-theme lúc runtime:** các custom view (`PRMButton`, `PRMSearchField`, `PRMEndowView`) cache token đã áp
  trong `lastAppliedToken`; khi re-attach chúng ưu tiên token cache. Nếu host đổi theme **sau khi view đã render**,
  view đó chỉ cập nhật khi rebind. Item trong RecyclerView **không** bị ảnh hưởng (đọc registry mỗi lần bind).
  → Khuyến nghị: cấu hình theme **một lần** lúc khởi tạo. Nếu cần đổi theme động, làm mới màn hình liên quan.
- `TokenColorParser` (Android) chỉ nhận hex. Trước đây nó gọi `Color.parseColor` nên còn nhận cả tên
  màu (`"red"`); nay đi qua `ThemeHex` để dùng chung thuật toán với iOS.

---

## 9. Quy tắc cho lập trình viên SDK

1. **Thêm khả năng tùy biến mới** = thêm field vào token tương ứng (nullable, default `null`), rồi:
   - Cập nhật **applier** + helper trong `TokenExtensions` (Android) / nhánh `?? default` (iOS).
   - Cập nhật `PromotionThemeDisplay` (field hex + `merge` + `toToken`) và `PromotionThemeDefaults`
     **ở cả hai nền tảng**.
   - Cập nhật DTO JSON ở cả hai bên, và thêm case vào `PromotionThemeJsonTest`.
   - Cập nhật **bảng token ở §2 của file này**.
2. **Token mới (loại view mới)** = tạo `XxxToken` + applier + getter trong registry + field trong
   `PromotionSDKTheme`. Cùng package `ui/theme/` (Android) / `PromotionSDKUI/Theme/` (iOS).
3. **Luôn null-safe**: applier `return` khi token/field null; không ghi đè style mặc định khi host
   không cấu hình.
4. **Đơn vị nhất quán**: màu hex `#AARRGGBB` trong JSON, bo góc dp/pt.
5. Đổi public API theme = **breaking cho host** → cập nhật file này + [PublicApi.md](./PublicApi.md)
   (AI_AGENT_RULES điều 7).

---

## 10. Liên quan

- [PublicApi.md](./PublicApi.md) — toàn bộ bề mặt SDK cho host.
- [AndroidUIGuide.md](../android/UIGuide.md) — theme trong tổng thể nền tảng.
- [IosUIGuide.md](../ios/UIGuide.md) — tầng UI iOS.
- [features/EndowView.md](../features/EndowView.md) — `PRMEndowView` dùng `DiscountBadgeToken`.
