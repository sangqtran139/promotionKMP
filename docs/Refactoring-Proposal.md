# Đề xuất tổ chức lại & đổi tên — vds-promotion SDK

> Trạng thái: **ĐỀ XUẤT — chờ duyệt**. Khi thực thi sẽ làm **từng bước, hỏi ý kiến trước mỗi bước**.
> Mọi đường dẫn dưới đây tương đối với `vds-promotion/src/main/java/com/ttcn/promotionsdk/`.

## Nguyên tắc

1. **Không đổi public API ở Pha 1.** Đổi tên/đổi package của class public = đổi fully-qualified name = breaking cho integrator + app demo.
2. Đổi tên file/class chỉ khi tăng độ rõ ràng; giữ coding style & tiền tố `PRM` hiện có.
3. Mỗi bước = 1 nhóm thay đổi nhỏ, build/verify (do **user** chạy), rồi mới sang bước kế.

---

## DANH SÁCH PUBLIC API (không được đổi tên/package ở Pha 1)

App demo + integrator phụ thuộc trực tiếp:
`ui/entry/*` · `core/config/PromotionSDKConfig` (+ `PromotionRequestContextProvider`, `SdkEnvironment`) ·
`ui/base/PRMBaseActivity|PRMBaseFragment` · `ui/theme/*Token` + `PromotionThemeConfig|PromotionSDKTheme|PromotionThemeDisplay|PromotionThemeJson|PromotionListItemTheme|TabUnderlineTheme` ·
`ui/feature/promotion/endowview/PRMEndowView` · `choosepromotion/ChoosePromotionFragment` · `PromotionIntegrateManager` ·
`ui/utils/view/PRMButton|PRMSearchField` · `ui/utils/enum/PRMSearchType` · `ui/utils/extension/TokenColorParser|TokenDrawableFactory` ·
`core/di/PromotionContainer` · `core/domain/model/*` (request + result models, `PromotionResult`) · `ui/entry/AppliedDiscount`.

---

# PHA 1 — An toàn (internal-only, KHÔNG đụng public API)

### Bước 1.1 — Sửa typo & xóa file chết
| Hiện tại | Hành động |
|---|---|
| `core/data/dto/redemption/RedemptionSessionRespone.kt` | rename file → `RedemptionSessionResponse.kt` |
| `ui/utils/view/PRMAbtractButton.kt` | rename file → `PRMAbstractButton.kt` |
| `ui/utils/extension/PRMResourceExtention.kt` | rename file → `PRMResourceExtension.kt` |
| `ui/entry/PromotionSDKConfig.kt` | **xóa** (chỉ chứa comment chết; config thật ở `core/config`) |

### Bước 1.2 — Bỏ object rỗng / dọn mapper
| Hiện tại | Hành động |
|---|---|
| ~~`core/data/dto/PromotionMapper.kt`~~ ĐÃ XỬ LÝ | chuyển thành `voucher/VoucherMapper.kt` (ext fn `toVoucher*()` top-level, cùng sub-package voucher DTO) |
| `core/data/dto/FeatureFlagMapper.kt` → `object FeatureFlagMapper` (rỗng) | bỏ `object`, giữ ext fn top-level (giữ file — FeatureFlag là tính năng tương lai) |
| `core/utils/PrmSimpleSpanBuilder.kt` ↔ `class SimpleSpanBuilder` | đổi class → `PRMSimpleSpanBuilder`, file → `PRMSimpleSpanBuilder.kt` |

### Bước 1.3 — ~~`SdkDi.Module`~~ ĐÃ HỦY
**KHÔNG động đến thư viện DI** (`SdkDi`, `PromotionContainer`, `core/di/internal/*`, các `*Module`) theo yêu cầu. Bỏ bước này.

### Bước 1.4 — Đổi tên adapter lẫn lộn (theo feature)
| Hiện tại | Vấn đề | Đề xuất |
|---|---|---|
| `mypromotion/adapter/ChoosePromotionAdapter.kt` → `class ChoosePromotionAdapter` | nằm trong **mypromotion** nhưng tên "Choose" | file+class → `MyPromotionAdapter` |
| `mypromotion/adapter/ChoosePromotionAdapter.kt` → `class PromotionListItem` | tên trùng khái niệm | → `MyPromotionListItem` (nếu không trùng dùng chỗ khác) |
| `choosepromotion/adapter/ListChoosePromotionAdapter.kt` → `class ChoosePromotionMainAdapter` | file ≠ class | file → `ChoosePromotionMainAdapter.kt` |

> ⚠️ Kiểm tra `PromotionListItem`/`ChoosePromotionListItem` có bị dùng chéo feature trước khi đổi.

### Bước 1.5 — Prefix nhất quán cho DTO redemption
Nhóm `stackablediscount` đã prefix (`StackableCustomerInfo`, `StackableOrderInfo`); nhóm `redemption` thì chưa.
| Hiện tại (redemption) | Đề xuất |
|---|---|
| `CustomerInfo` | `RedemptionCustomerInfo` |
| `OrderInfo` | `RedemptionOrderInfo` |
| `OrderItem` | `RedemptionOrderItem` |
| `ValidationError` (RedemptionSessionResponse) | `RedemptionValidationErrorResponse` |

> Chỉ là DTO internal → an toàn. Nhớ sửa mapper (`RedemptionMapper`, data-layer) tương ứng.

### Bước 1.6 — Đổi tên presentation mapper
| Hiện tại | Hành động |
|---|---|
| `ui/feature/promotion/ext/StackableRequestExtensions.kt` | rename → `PromotionUiMapper.kt` (giờ map cả validate + redemption + domain→DTO) |

### Bước 1.7 — FeatureFlag: GIỮ NGUYÊN (tính năng tương lai)
Vertical FeatureFlag hiện chưa được nạp/sử dụng, **NHƯNG là tính năng sẽ triển khai trong tương lai** →
**KHÔNG xóa.** Chỉ áp các bước đổi tên/tổ chức chung (vd prefix, package) nếu phù hợp; giữ toàn bộ file.

### Bước 1.8 — ~~Gom DI theo tầng~~ ĐÃ HỦY
**KHÔNG động đến thư viện DI** theo yêu cầu — giữ nguyên `core/di`, `core/domain/di`, `ui/di` như hiện tại. Bỏ bước này.

### Bước 1.9 — Tách sub-package cho `ui/theme` (20 file phẳng)
| Nhóm | Vào package |
|---|---|
| `ButtonToken`, `SearchBarToken`, `ListItemToken`, `TabChipToken`, `TabUnderlineToken`, `DiscountBadgeToken` | `ui/theme/token/` |
| `*Applier`, `*Theme` (DiscountBadge/PromotionListItem/TabChip/TabLayout/TabUnderline) | `ui/theme/applier/` |
| `PromotionThemeConfig`, `Registry`, `Json`, `Display`, `Defaults`, `PromotionSDKTheme` | giữ ở `ui/theme/` |

> ⚠️ **PHÁT HIỆN KHI THỰC THI:** hầu hết class trong `ui/theme` là **public-by-default** (kể cả các `*Applier`, `DiscountBadgeTheme`). Chỉ `PromotionThemeDefaults` và `PromotionThemeRegistry` là `internal`.
> → Di chuyển applier sang sub-package = đổi FQN = **breaking public API**. Vì vậy **toàn bộ 1.9 chuyển sang Pha 2**.
> Prep an toàn (tùy chọn, Pha 1): đánh `internal` cho 5 object applier (app không dùng) để Pha 2 move tự do — cần verify compile.

---

# PHA 2 — Breaking public API (CẦN DUYỆT riêng)

> SDK giao AAR nội bộ chưa release ngoài → có thể chấp nhận breaking, nhưng phải cập nhật app demo + `INTEGRATION.md`.

| Hạng mục | Hiện tại | Đề xuất |
|---|---|---|
| Custom view → widget | `ui/utils/view/PRM*` | `ui/widget/PRM*` |
| Token public | `ui/theme/*Token` | `ui/theme/token/*` |
| ~~Theme helper~~ | `PromotionListItemTheme`, `TabUnderlineTheme`, `DiscountBadgeTheme` | **GIỮ Ở ROOT `ui.theme`** — facade public, phụ thuộc `toToken()` của `PromotionThemeDisplay` (root). Chỉ 4 `*Applier` (internal) vào `ui/theme/applier/`. |
| Enum/ext public | `ui/utils/enum/PRMSearchType`, `ui/utils/extension/TokenColorParser|TokenDrawableFactory` | gom về `ui/widget` hoặc `ui/theme` |
| VoucherStatus về domain | `core/data/dto/voucher/VoucherStatus` (enum) | `core/domain/model/VoucherStatus` |

---

## Quy ước đặt tên đề xuất (thống nhất)

- **Custom View / widget / base / enum UI:** tiền tố `PRM` (đang dùng) — giữ.
- **Entry/façade & theme công khai:** tiền tố `Promotion` — giữ.
- **Domain model / use case / repository:** không tiền tố — giữ.
- **DTO:** đặt theo nhóm + hậu tố rõ ràng (`*Request`/`*Response`/`*Dto`); prefix nhóm nếu tên generic.
- **File = tên class chính** (1 class công khai chính / file; nhóm data class nhỏ liên quan có thể chung file).

---

## Thứ tự thực thi đề xuất
Pha 1: 1.1 → 1.2 → 1.4 → 1.5 → 1.6 **(đã xong)**. ~~1.9~~ chuyển sang Pha 2 (theme đa số public).
~~1.3, 1.8~~ đã hủy (không động DI). 1.7: giữ FeatureFlag.
Pha 2: chỉ làm sau khi Pha 1 ổn và user đồng ý chịu breaking.

> **Ràng buộc tuyệt đối:** KHÔNG sửa thư viện DI (`SdkDi`, `PromotionContainer`, `core/di/internal/*`, các `*Module`).
