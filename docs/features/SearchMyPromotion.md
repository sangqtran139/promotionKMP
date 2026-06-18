# Feature: Search My Promotion

Màn hình **tìm kiếm voucher của khách hàng** theo từ khoá, có phân trang và trạng thái rỗng/lỗi.

- **Package:** `ui/feature/promotion/searchmypromotion`
- **Thành phần:** `SearchMyPromotionFragment`, `SearchMyPromotionViewModel`, `SearchMyPromotionContract`

---

## 1. Contract (MVI)

### State — `SearchMyPromotionUiState`
| Field | Ý nghĩa |
|-------|---------|
| `keyword` | Từ khoá hiện tại |
| `vouchers` | `List<MyVoucherListItem>` kết quả |
| `isLoading` / `isLoadingMore` | Trạng thái tải |
| `isEmpty` | Không có kết quả |
| `isLastPage` | Đã hết trang |
| `page` / `pageSize` | Phân trang (`DEFAULT_PAGE_SIZE = 10`) |
| `validationError` | Lỗi (nếu có) |

### Action — `SearchMyPromotionAction`
- `QueryChanged(keyword)` — người dùng gõ từ khoá.
- `Search` — thực hiện tìm kiếm.
- `LoadMore` — tải trang tiếp.
- `ClearKeyword` — xoá từ khoá.
- `Retry` — thử lại sau lỗi.

### Effect — `SearchMyPromotionEffect`
- `ShowError(errorCode)`.

---

## 2. Luồng dữ liệu

```
Fragment gõ → Action.QueryChanged(keyword) → setState(keyword=...)
Action.Search → setState(isLoading=true, page=0)
            → launch { searchVouchersUseCase(request(keyword, page, size)) }
            → setState(vouchers=..., isEmpty=..., isLastPage=..., isLoading=false)
Action.LoadMore → tăng page → append vouchers
Lỗi → onError → sendEffect(ShowError)
```

- Dùng chung use case `SearchCustomerVouchersUseCase` và model `MyVoucherListItem` (tái sử dụng từ My Promotion).

---

## 3. Lưu ý khi sửa

- `DEFAULT_PAGE_SIZE` khai báo trong `companion object` của state — đổi ở đây, không hardcode rải rác.
- Debounce/throttle khi gõ (nếu thêm) xử lý ở ViewModel, không ở Fragment.
- Hiển thị trạng thái rỗng/lỗi dựa trên `isEmpty` / `validationError` + `Effect.ShowError`.
