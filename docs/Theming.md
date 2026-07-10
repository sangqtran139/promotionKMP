
# Theming — Hệ thống theme & token

TTCN Promotion SDK cho phép host app **tùy biến giao diện** (màu, bo góc…) thông qua hệ thống **token**.
SDK không ép host dùng style cứng — host truyền các token màu, SDK tự áp vào view tương ứng.

**Hai nền tảng song ánh.** Cùng sáu token, cùng tên type, cùng tên field, cùng một định dạng JSON.
Sửa một bên thì sửa cả hai.

| Android `ui/theme/` | iOS `PromotionSDK/Theme/` |
|---|---|
| `PromotionSDKTheme.kt` + `token/*.kt` | `PromotionSDKTheme.swift` |
| `PromotionThemeJson.kt` + `ThemeHex.kt` | (phần `ThemeDTO` trong `PromotionSDKTheme.swift`) |
| `PromotionThemeDefaults.kt` | `PromotionThemeDefaults.swift` |
| `PromotionThemeDisplay.kt` | `PromotionThemeDisplay.swift` |
| `PromotionThemeRegistry.kt` (internal) | `VDSThemeRegistry` trong CoreUI (internal) |

---

## 1. Kiến trúc tổng quan

```
            (Host app)
                │ truyền token màu
                ▼
   PromotionTheme (public API, ui/entry)      │ iOS: sdk.configure(theme:)
        configure() / clear() / currentTheme()│
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
- `PromotionThemeJson` / `ThemeHex` (Android), `ThemeDTO` (iOS) — serialize.

> **SDK không lưu theme.** Host truyền theme mỗi lần `init`, và tự lưu nếu muốn — cả hai app demo
> đều có `ThemePreferenceManager` riêng. Trước đây Android có `PromotionThemeStore` ghi xuống
> SharedPreferences, nhưng `load()` **không có call-site nào**: nó chỉ ghi rác. Đã xoá.

---

## 2. Token — đơn vị tùy biến

`PromotionSDKTheme` gom **6 token**. Mọi field đều **nullable** — `null` nghĩa là "giữ mặc định của
SDK" (applier bỏ qua). Nhóm `null` = giữ nguyên cả nhóm.

| Token | Field | Áp vào |
|-------|-------|--------|
| `ButtonToken` | `backgroundColor`, `textColor`, `shadowColor`, `cornerRadius` | `PRMButton` / `VDSButton` |
| `SearchBarToken` | `borderColor`, `hintTextColor`, `textColor`, `iconColor`, `cornerRadius` | `PRMSearchField` / `VDSSearchTextField` |
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
PromotionTheme.toJson(theme)      // Android
PromotionTheme.fromJson(json)
```
```swift
theme.jsonString()                // iOS
PromotionSDKTheme.from(jsonString: json)
```

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

Bên iOS không có applier riêng: mỗi component tự đọc `VDSThemeRegistry.shared.<token>()` và dùng
`?? default` ở chỗ cần.

---

## 5. Public API cho host

```kotlin
object PromotionTheme {                            // Android
    fun configure(theme: PromotionSDKTheme)        // set theme + đồng bộ PromotionSDK
    fun currentTheme(): PromotionSDKTheme?
    fun clear()                                    // xoá theme về mặc định
    fun toJson(theme) / fun fromJson(json)
    fun sdkDefaults(context): PromotionSDKTheme    // giá trị mặc định SDK
    fun loadDisplayDefaults(context) / fun mergeDisplayWithSaved(...)
    fun colorToHex(color) / fun pxToDp(context, px)
}
```
```swift
sdk.configure(theme:)                              // iOS
sdk.currentTheme
PromotionThemeDefaults.theme                       // ≡ sdkDefaults
PromotionThemeDisplay.load() / .mergeWithSaved(sdk:saved:) / .themeFromDisplayValues(_:sdk:)
```

`pxToDp` không có bên iOS — pt không cần quy đổi.

Có sẵn typealias để host Android import gọn: `ThemeConfig`, `ThemeButtonToken`,
`ThemeDiscountBadgeToken`… (`ThemeConfig` nay trỏ tới `PromotionSDKTheme`; `PromotionThemeConfig` đã
bị gộp vào đó — nó vốn là bản sao sáu field y hệt.)

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
PromotionSDK.init(context, options)
```

**Cách 2 — gọi `PromotionTheme.configure()` SAU `init()`:**
```kotlin
PromotionSDK.init(context, PromotionSDKOptions(config = sdkConfig))
PromotionTheme.configure(PromotionSDKTheme(buttonToken = ...))
```

---

## 6. ⚠️ Default của hai nền tảng **không giống nhau**

`PromotionThemeDefaults` **báo cáo trung thực** những gì mỗi nền tảng đang render, chứ không sao chép
lẫn nhau. Sáu design token nền (`tokenDark10`, `tokenPineBlue100`…) trùng khớp tuyệt đối, nhưng
component thì chọn khác nhau:

| Field | Android | iOS |
|---|---|---|
| `discountBadge.availableTextColor` | `#2CA196` (teal) | `#FF645C` (đỏ san hô) |
| `discountBadge.availableBackgroundColor` | `#EAF6F4` | `#FF645C` @ 8% |
| `discountBadge.actionTextColor` | `#EE0033` | `#FF645C` |
| `tabUnderline.indicatorColor` | `#EE0033` | `#FF645C` |
| `tabUnderline.activeTextColor` | `#000000` | `#222222` |
| `tabUnderline.backgroundColor` | `#FBFBFB` | *(không có default)* |
| `listItem.usedBadgeTextColor` | `#222222` | `#7A7A7A` |
| `tabChip.cornerRadius` | 7 | 8 |
| `button.cornerRadius` | `tokenBorderRadius24` (~24) | `height/2` = 24 ở size `.large` |

Chú ý `Colors.tokenRed100` của iOS là `#FF645C`, **không** phải `#EE0033` (`tokenViettelPayRed100`).

Đây là **lệch thiết kế**, không phải bug của tầng theme. Muốn hai app trông giống nhau thì phải đổi
default của một bên — quyết định của Product/Design, không phải của SDK.

Ba field iOS **không có default trong code** (`listItem.linkTextColor`, hai màu radio): component giữ
màu của XIB / ảnh asset. `PromotionThemeDefaults.swift` khai giá trị mà asset đang thể hiện, để màn
preview không trống.

---

## 7. ⚠️ Quy tắc thứ tự bắt buộc (foot-gun)

`PromotionSDK.init()` **luôn ghi đè** registry bằng `options.theme`, mà `PromotionSDKOptions.theme`
**mặc định là rỗng** (`PromotionSDKTheme()`).

> ❌ **Sai:** gọi `PromotionTheme.configure(myTheme)` **trước** rồi `PromotionSDK.init(context, PromotionSDKOptions(config))`
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
   `PromotionSDKTheme`. Cùng package `ui/theme/` (Android) / `PromotionSDK/Theme/` (iOS).
3. **Luôn null-safe**: applier `return` khi token/field null; không ghi đè style mặc định khi host
   không cấu hình.
4. **Đơn vị nhất quán**: màu hex `#AARRGGBB` trong JSON, bo góc dp/pt.
5. Đổi public API theme = **breaking cho host** → cập nhật file này + [PublicApi.md](./PublicApi.md)
   (AI_AGENT_RULES điều 7).

---

## 10. Liên quan

- [PublicApi.md](./PublicApi.md) — toàn bộ bề mặt SDK cho host.
- [AndroidUIGuide.md](./AndroidUIGuide.md) — theme trong tổng thể nền tảng.
- [IosUIGuide.md](./IosUIGuide.md) — tầng UI iOS.
- [features/EndowView.md](./features/EndowView.md) — `PRMEndowView` dùng `DiscountBadgeToken`.
