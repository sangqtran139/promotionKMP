# Feature: My Promotion

Màn hình hiển thị **danh sách voucher của khách hàng** với tab, phân trang và tìm kiếm.
Đây là màn hình UI mode chính được host mở qua `PRMSDK` (`PRMMyPromotionFragment`).

- **Package:** `ui/feature/promotion/mypromotion`
- **Thành phần:** `PRMMyPromotionFragment`, `MyPromotionViewModel`, `MyPromotionContract`, `adapter/`

---

## 1. Contract (MVI)

### State — `MyPromotionUiState`
| Field | Ý nghĩa |
|-------|---------|
| `hasLoadedInitial` | Đã load lần đầu hay chưa (tránh load lại) |
| `isLoading` / `isRefreshing` / `isRefreshingTab` / `isLoadingMore` | Các trạng thái tải |
| `isEmpty` | Danh sách rỗng |
| `tabs` / `selectedTabCode` | Danh sách tab (`TabItem`) và tab đang chọn |
| `keyword` | Từ khoá tìm kiếm hiện tại |
| `page` / `size` / `isLastPage` | Trạng thái phân trang |
| `vouchers` | Danh sách `MyVoucherListItem` để render |

### Action — `MyPromotionAction`
- `LoadInitialIfNeeded` — load lần đầu nếu chưa có.
- `Refresh` — kéo làm mới.
- `SelectTab(tabCode)` — đổi tab.
- `SearchKeyword(keyword)` — lọc theo từ khoá.
- `LoadMore` — tải trang kế tiếp.

### Effect — `MyPromotionEffect`
- `OpenVoucherDetail(voucherId)` — mở màn chi tiết.
- `ShowError(errorCode)` — hiển thị lỗi.

---

## 2. Luồng dữ liệu

```
Fragment → handleAction(LoadInitialIfNeeded)
  ViewModel: setState(isLoading=true)
           → launch { useCases.searchVouchers(request) }  // :promotionLogic — Domain → Data → Ktor
           → DTO map toMyVoucherListItem()/toMyVoucherTabUi()
           → setState(vouchers=..., tabs=..., isLoading=false)
  Lỗi → onError → setState(isLoading=false) + sendEffect(ShowError)
Fragment: collect uiState → render danh sách/tab; collect uiEffect → mở detail/hiển thị lỗi
```

---

## 3. UI item & mapping

- `MyVoucherListItem` — model UI cho mỗi voucher (id, merchant, title, logo, status, `isSelected`, `isAutoApplied`…).
- `TabItem` — tab (`code`, `label`, `count`, `order`).
- Mapping: `VoucherItem.toMyVoucherListItem()`, `VoucherTabItem.toMyVoucherTabUi()` (trong `MyPromotionContract.kt`).
- Adapter dùng `ListAdapter` + `DiffUtil` so theo `voucherId`.

---

## 4. API backend

- **Endpoint:** `GET /promotion/promotion-vtm-bff/api/v1/vtm/customer-vouchers`
- **Request params:** `customerId`, `keyword`, `serviceCode`, `tab`, `page` (0-based), `size`
- **Response:** Spring Page phẳng — `content[]` + `number`, `size`, `last`, `totalElements`; kèm `tabs[]` động (mỗi tab có `code`, `label`, `count`, `order`, `default`).
- **Không còn:** `myVouchers`/`otherVouchers`/`sectionCode` (đã bỏ từ v1.3).
- Search keyword: free search, không giới hạn độ dài tối thiểu; rỗng/whitespace = không filter.

## 5. Lưu ý khi sửa

- Đổi tham số tìm kiếm/phân trang → đồng bộ qua use case `SearchCustomerVouchersUseCase`, **không** gọi thẳng repository.
- Thêm trạng thái UI → thêm field vào `MyPromotionUiState` (immutable, có default).
- Click voucher → phát `Effect.OpenVoucherDetail`, không tự điều hướng trong ViewModel.
- Tab active xác định qua `SearchCustomerVouchersResult.resolveActiveTab(requestedTab)` — rule **dùng chung Android & iOS** (`selectedTab` → `defaultTab` → tab client yêu cầu → tab đầu theo `order`); UI chỉ đọc, không hardcode "all".
- Liên quan: [ChoosePromotion.md](./ChoosePromotion.md) (dùng lại `MyVoucherListItem`, `TabItem`).
