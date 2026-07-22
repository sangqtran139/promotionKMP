# Feature: Promotion Detail

Màn hình **chi tiết một voucher** — hiển thị thông tin, trạng thái và nút hành động (áp dụng…).
Có thêm tab nội dung qua `PrmContentDetailEndowFragment` + `PrmCustomFragmentPagerAdapter`.

- **Package:** `ui/feature/promotion/promotiondetail`
- **Thành phần:** `PromotionDetailFragment`, `PromotionDetailViewModel`, `PromotionDetailContract`, `PrmContentDetailEndowFragment`, `adapter/PrmCustomFragmentPagerAdapter`

> **Lệch có chủ đích Android ↔ iOS — SEED nút/card (chưa đồng bộ, cố ý):**
> `PromotionDetailStore` có sẵn intent `Seed(status)` (dùng chung). **iOS** dispatch nó để hiện ngay
> card + nút "Dùng ngay" seed từ voucher cơ bản (dữ liệu list truyền qua navigation) **trước khi** fetch
> detail. **Android KHÔNG seed**: nav chỉ mang `voucherId`, và màn loading ẩn toàn bộ `contentContainer`
> sau shimmer → muốn seed hiển thị phải **viết lại màn loading** (thay shimmer bằng card-seed).
> Quyết định **giữ nguyên** (idiom loading khác nhau: iOS seed-card, Android shimmer-until-loaded) — lợi
> ích chỉ là bớt "nháy" ~1 nhịp loading, không đáng rủi ro rewrite. Muốn parity thì port pattern
> `seedDisplay` của iOS: truyền voucher cơ bản qua args + `bindSeedContent` + dispatch `Seed(status)`.

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

## 4. API backend

- **Endpoint:** `GET /promotion/promotion-vtm-bff/api/v1/vtm/customer-vouchers/{voucherId}`
- **Query params:** `customerId` (bắt buộc), `service` (tuỳ chọn — dịch vụ đang thanh toán).
- **Response fields mới (v1.1):** `campaignId`, `campaignType`, `campaignStatus`, `applicableProducts[]` (gồm `productId`, `sku`, `name`, `image`, `type=INCLUDED|EXCLUDED`).

## 5. Lưu ý khi sửa

- Mapping/field mới của chi tiết voucher → cập nhật `VoucherDetail` (domain) + `toVoucherDetail()` (data).
- Logic enable/label nút theo trạng thái mới → bổ sung vào `VoucherStatus` và phần suy luận trong ViewModel.
- `campaignStatus` ảnh hưởng tới khả năng dùng voucher (DISABLED/EXPIRED/DELETED → không dùng được dù `status=ACTIVE`).
