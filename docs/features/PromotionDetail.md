# Feature: Promotion Detail

Màn hình **chi tiết một voucher** — hiển thị thông tin, trạng thái và nút hành động (áp dụng…).
Có thêm tab nội dung qua `PrmContentDetailEndowFragment` + `PrmCustomFragmentPagerAdapter`.

- **Package:** `ui/feature/promotion/promotiondetail`
- **Thành phần:** `PromotionDetailFragment`, `PromotionDetailViewModel`, `PromotionDetailContract`, `PrmContentDetailEndowFragment`, `adapter/PrmCustomFragmentPagerAdapter`

---

## 1. Contract (MVI)

### State — `PromotionDetailUiState`
| Field | Ý nghĩa |
|-------|---------|
| `isLoading` | Đang tải chi tiết |
| `detail` | `VoucherDetail?` (domain model) |
| `status` | `VoucherStatus` (UNKNOWN/…)|
| `actionVisible` | Có hiện nút hành động không |
| `actionEnabled` | Nút có cho bấm không |
| `actionLabel` | Nhãn nút (vd "Áp dụng") |

### Action — `PromotionDetailAction`
- `LoadDetail(voucherId)` — tải chi tiết voucher.

### Effect — `PromotionDetailEffect`
- `ShowError(errorCode)`.

---

## 2. Luồng dữ liệu

```
Fragment(voucherId) → handleAction(LoadDetail(voucherId))
  ViewModel: setState(isLoading=true)
           → launch { getCustomerVoucherDetail(...) }   // GetCustomerVoucherDetailUseCase
           → DTO map toVoucherDetail() → domain VoucherDetail
           → tính status/actionVisible/actionEnabled/actionLabel theo VoucherStatus
           → setState(detail=..., isLoading=false)
  Lỗi → sendEffect(ShowError)
Fragment: render thông tin + cấu hình nút theo state
```

- Use case: `GetCustomerVoucherDetailUseCase`.
- Trạng thái nút (`actionEnabled`, `actionLabel`) **suy ra từ `status`** trong ViewModel, không hardcode ở Fragment.

---

## 3. Tab nội dung

- `PrmContentDetailEndowFragment` hiển thị nội dung mô tả/điều khoản.
- `PrmCustomFragmentPagerAdapter` quản lý các tab nội dung.

---

## 4. Lưu ý khi sửa

- Mapping/field mới của chi tiết voucher → cập nhật `VoucherDetail` (domain) + `toVoucherDetail()` (data).
- Logic enable/label nút theo trạng thái mới → bổ sung vào `VoucherStatus` và phần suy luận trong ViewModel.
