
# Theming — Hệ thống theme & token

TTCN Promotion SDK cho phép host app **tùy biến giao diện** (màu, bo góc…) thông qua hệ thống **token**.
SDK không ép host dùng style cứng — host truyền các token màu, SDK tự áp vào view tương ứng.

- **Package:** `ui/theme/` + public API `ui/entry/PromotionTheme.kt`
- **Áp dụng cho:** XML View (xem [XMLViewGuide.md](./XMLViewGuide.md)). Không liên quan Compose.

---

## 1. Kiến trúc tổng quan

```
            (Host app)
                │ truyền token màu
                ▼
   PromotionTheme (public API, ui/entry)
        configure() / clear() / currentConfig()
                │
                ▼
   PromotionThemeRegistry (internal, @Volatile singleton)
        giữ PromotionThemeConfig hiện tại
                │ getter: buttonToken(), discountBadgeToken()...
                ▼
   *Applier / *ThemeApplier  ──uses──▶  TokenExtensions (helper áp màu/bo góc)
        áp token vào View thật (PRMButton, RecyclerView item, TabLayout...)
```

Các lớp phụ trợ:
- `PromotionThemeDefaults` — giá trị mặc định của SDK (dùng cho preview/tài liệu).
- `PromotionThemeDisplay` — biểu diễn token dạng **chuỗi hex** (JSON-serializable) + logic `merge`/`toToken`; phục vụ lưu/đọc cấu hình và màn preview theme trong app demo.
- `PromotionThemeJson` — parse/serialize cấu hình theme.

---

## 2. Token — đơn vị tùy biến

`PromotionThemeConfig` gom **6 token**. Mọi field đều **nullable** — `null` nghĩa là "giữ mặc định của SDK" (applier sẽ bỏ qua).

| Token | Field | Áp vào |
|-------|-------|--------|
| `ButtonToken` | `backgroundColor`, `textColor`, `shadowColor`, `cornerRadius` (dp) | `PRMButton` |
| `SearchBarToken` | `borderColor`, `hintTextColor`, `textColor`, `iconColor`, `cornerRadius` (dp) | `PRMSearchField` |
| `ListItemToken` | `linkTextColor`, `usedBadgeTextColor`, `usedBadgeBackgroundColor`, `radioButtonStrokeColor`, `radioButtonSelectedStrokeColor` | Item voucher trong list (`PromotionListItemApplier`) |
| `TabChipToken` | `activeBackgroundColor`, `inactiveBackgroundColor`, `activeTextColor`, `inactiveTextColor`, `cornerRadius` (dp) | Tab dạng chip (`MyPromotionTabAdapter` → `TabChipThemeApplier`) |
| `TabUnderlineToken` | `indicatorColor`, `activeTextColor`, `inactiveTextColor`, `backgroundColor` | `TabLayout` gạch chân (`TabLayoutThemeApplier`) |
| `DiscountBadgeToken` | `availableTextColor`, `unavailableTextColor`, `availableBackgroundColor`, `unavailableBackgroundColor`, `actionTextColor` | Badge giảm giá ở `PRMEndowView` / `ApplyPromotionAdapter` |

> Màu là `@ColorInt` (Int). `cornerRadius` tính theo **dp**.

---

## 3. Registry & Applier

### `PromotionThemeRegistry` (internal)
- Singleton `@Volatile` giữ `PromotionThemeConfig?`.
- `configure(config)` ghi đè cấu hình; getter trả từng token (`buttonToken()`, `discountBadgeToken()`…).
- Token chưa cấu hình → trả `null`.

### Applier
Mỗi loại view có một applier **idempotent, null-safe** (`if (token == null) return`):

| Applier | Nhiệm vụ |
|---------|----------|
| `TabChipThemeApplier.applyChip(tv, selected, token)` | Màu nền/chữ chip theo trạng thái + bo góc |
| `TabLayoutThemeApplier.apply(tabs, token)` | Indicator, màu chữ active/inactive, nền `TabLayout` |
| `PromotionListItemApplier.apply(binding, token)` | Màu link, badge "đã dùng", radio chọn voucher |
| `DiscountBadgeApplier.apply(binding, token, available)` | Badge giảm giá theo trạng thái khả dụng |

Helper áp giá trị nằm ở `ui/utils/TokenExtensions.kt`: `applyTextColorIfSet`, `applyBackgroundColorIfSet`,
`applyCornerRadiusDp`, `applyDrawableBackgroundTintIfSet`, `applyStrokeColorIfSet`, `applyRadioStrokeColors`…
Tất cả đều **bỏ qua khi giá trị null** (an toàn khi token thiếu field).

---

## 4. Public API cho host — `PromotionTheme`

```kotlin
object PromotionTheme {
    fun configure(config: PromotionThemeConfig)     // set theme + đồng bộ PromotionSDK
    fun currentConfig(): PromotionThemeConfig?
    fun clear()                                     // xoá theme về mặc định
    fun sdkDefaults(context): PromotionThemeConfig  // giá trị mặc định SDK
    // tiện ích preview: colorToHex, pxToDp, loadDisplayDefaults, mergeDisplayWithSaved
}
```

Có sẵn typealias để host import gọn: `ThemeConfig`, `ThemeButtonToken`, `ThemeDiscountBadgeToken`…

### Hai cách cấu hình theme

**Cách 1 — qua `PromotionSDKOptions.theme` khi init (khuyến nghị):**
```kotlin
val options = PromotionSDKOptions(
    config = sdkConfig,
    theme = PromotionSDKTheme(
        config = PromotionThemeConfig(
            buttonToken = ButtonToken(backgroundColor = Color.parseColor("#EE0033"), cornerRadius = 8f),
            tabChipToken = TabChipToken(activeBackgroundColor = ..., activeTextColor = ...),
        ),
    ),
)
PromotionSDK.init(context, options)
```

**Cách 2 — gọi `PromotionTheme.configure()` SAU `init()`:**
```kotlin
PromotionSDK.init(context, PromotionSDKOptions(config = sdkConfig))
PromotionTheme.configure(PromotionThemeConfig(buttonToken = ...))
```

---

## 5. ⚠️ Quy tắc thứ tự bắt buộc (foot-gun)

`PromotionSDK.init()` **luôn ghi đè** registry bằng `options.theme.config`, mà `PromotionSDKOptions.theme`
**mặc định là rỗng** (`PromotionSDKTheme()`).

> ❌ **Sai:** gọi `PromotionTheme.configure(myTheme)` **trước** rồi `PromotionSDK.init(context, PromotionSDKOptions(config))`
> mà **không** truyền theme vào options → theme vừa set bị **xoá âm thầm** về mặc định.

✅ **Đúng:** dùng **Cách 1** (truyền theme vào `options`) **hoặc** **Cách 2** (`configure()` **sau** `init()`).

`PromotionSDK.release()` sẽ reset theme về null (cùng với việc clear DI/callback).

---

## 6. Giới hạn đã biết

- **Re-theme lúc runtime:** các custom view (`PRMButton`, `PRMSearchField`, `PRMEndowView`) cache token đã áp
  trong `lastAppliedToken`; khi re-attach chúng ưu tiên token cache. Nếu host đổi theme **sau khi view đã render**,
  view đó chỉ cập nhật khi rebind. Item trong RecyclerView **không** bị ảnh hưởng (đọc registry mỗi lần bind).
  → Khuyến nghị: cấu hình theme **một lần** lúc khởi tạo. Nếu cần đổi theme động, làm mới màn hình liên quan.

---

## 7. Quy tắc cho lập trình viên SDK

1. **Thêm khả năng tùy biến mới** = thêm field vào token tương ứng (nullable, có default `null`), rồi:
   - Cập nhật **applier** + helper trong `TokenExtensions` để áp field đó.
   - Cập nhật `PromotionThemeDisplay` (field hex + `merge` + `toToken`) và `PromotionThemeDefaults`.
   - Cập nhật **bảng token ở mục 2 của file này**.
2. **Token mới (loại view mới)** = tạo `XxxToken` + `XxxApplier` + getter trong `PromotionThemeRegistry`
   + field trong `PromotionThemeConfig`. Đặt cùng package `ui/theme/`.
3. **Luôn null-safe**: applier `return` khi token/field null; không ghi đè style mặc định khi host không cấu hình.
4. **Đơn vị nhất quán**: màu `@ColorInt`, bo góc `dp`.
5. Đổi public API theme (`PromotionTheme`, token, config) = **breaking cho host** → cập nhật file này + `INTEGRATION.md` (AI_AGENT_RULES điều 7).

---

## 8. Liên quan

- [AndroidGuide.md](./AndroidGuide.md) — theme trong tổng thể nền tảng.
- [XMLViewGuide.md](./XMLViewGuide.md) — UI nền XML mà theme áp lên.
- [features/EndowView.md](./features/EndowView.md) — `PRMEndowView` dùng `DiscountBadgeToken`.
- `INTEGRATION.md` (gốc repo) — hướng dẫn tích hợp cho host.
