# Feature: Promotion Detail

Màn hình **chi tiết một voucher** — hiển thị thông tin, trạng thái và nút hành động (áp dụng…).
Có thêm tab nội dung qua `PrmContentDetailEndowFragment` + `PrmCustomFragmentPagerAdapter`.

- **Package:** `ui/feature/promotion/promotiondetail`
- **Thành phần:** `PromotionDetailFragment`, `PromotionDetailViewModel`, `PromotionDetailContract`, `PrmContentDetailEndowFragment`, `adapter/PrmCustomFragmentPagerAdapter`

> **KHÔNG seed từ ngoài — quy tắc chốt, áp dụng cả Android & iOS (2026-07-23).**
> Màn chi tiết **chỉ hiển thị khi `getCustomerVoucherDetail` trả về**. Navigation chỉ mang `voucherId`;
> dữ liệu từ màn danh sách (tên, logo, HSD, trạng thái) **không** được dùng để dựng card/nút — tránh
> hai nguồn sự thật lệch nhau (list có thể cũ hơn detail).
> Trong lúc chờ: shimmer toàn màn (Android `showDetailLoading`, iOS `PromotionDetailShimmerView` +
> `Display.empty`). Vì vậy `PromotionDetailStore` **không có** intent seed.

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
| `actionLabel` | Nhãn nút — **lấy nguyên từ server** (`VoucherDetail.displayStatusLabel`), store không tự dựng chuỗi |

### Action — `PromotionDetailAction`
- `LoadDetail(voucherId)` — tải chi tiết voucher.
- `OpenServiceSelector` — bấm nút "Dùng ngay".
- `ServiceSelected(service)` — đã chọn dịch vụ trong bottom sheet.

### Effect — `PromotionDetailEffect`
- `ShowError(errorCode)`.
- `ShowServiceSelector(services)`.

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
- Hiện/ẩn + cho bấm (`actionVisible`, `actionEnabled`) suy ra từ `status` **ở store** (`displayState().isUsable`),
  không hardcode ở Fragment/VC.
- **Nhãn nút (`actionLabel`) không hardcode**: store gán thẳng `VoucherDetail.displayStatusLabel` cho **mọi**
  trạng thái (trước đây bị xoá trắng khi voucher usable, rồi native đè chuỗi cứng "Sử dụng ngay"/"useNow").
  Native chỉ dùng nhãn mặc định (`R.string.prm_use_now` / `PromotionUIStrings.useNow`) khi server trả rỗng.

  > ⚠️ **Hạn chế phía API:** `displayStatusLabel` hiện map từ `metadata.disabledReason`
  > ([`VoucherMapper.kt`](../../promotionLogic/src/commonMain/kotlin/com/ttcn/promotionsdk/core/data/dto/voucher/VoucherMapper.kt)),
  > mà field này **chỉ có giá trị khi `usable="false"`**. Nút "Sử dụng" lại chỉ hiện khi voucher usable →
  > trên thực tế vẫn luôn rơi vào nhãn dự phòng. Muốn nhãn nút do server quyết định thật sự thì BE phải trả
  > thêm một field nhãn áp dụng cho cả voucher dùng được; khi có, chỉ cần map nó vào `displayStatusLabel`
  > là bỏ được nhánh dự phòng, **không phải sửa UI**.

---

## 3. Tab nội dung

- `PrmContentDetailEndowFragment` hiển thị nội dung mô tả/điều khoản.
- `PrmCustomFragmentPagerAdapter` quản lý các tab nội dung.

---

## 4. API backend

- **Endpoint:** `GET /promotion/promotion-vtm-bff/api/v1/vtm/customer-vouchers/{voucherId}`
- **Query params:** `service` (tuỳ chọn — dịch vụ đang thanh toán). Định danh khách lấy từ JWT `sub`, **không** truyền lên.
- **Response fields mới (v1.1):** `campaignId`, `campaignType`, `campaignStatus`, `applicableProducts[]` (gồm `productId`, `sku`, `name`, `image`, `type=INCLUDED|EXCLUDED`).

## 5. Lưu ý khi sửa

- Mapping/field mới của chi tiết voucher → cập nhật `VoucherDetail` (domain) + `toVoucherDetail()` (data).
- Logic enable/label nút theo trạng thái mới → bổ sung vào `VoucherStatus` và phần suy luận trong ViewModel.
- `campaignStatus` ảnh hưởng tới khả năng dùng voucher (DISABLED/EXPIRED/DELETED → không dùng được dù `status=ACTIVE`).
