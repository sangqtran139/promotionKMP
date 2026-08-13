# Feature: Search My Promotion

Màn hình **tìm kiếm voucher của khách hàng** theo từ khoá, có phân trang và trạng thái rỗng/lỗi.

- **Package:** `ui/feature/promotion/searchmypromotion`
- **Thành phần:** `SearchMyPromotionFragment`, `SearchMyPromotionViewModel`, `SearchMyPromotionContract`

### Quyết định hiển thị — **native không tự suy**

Extension của `SearchMyPromotionState` trong `SearchMyPromotionContract.kt`:

| Hàm | Trả lời | Android | iOS |
|---|---|---|---|
| `showsNoResult()` | Hiện view "không tìm thấy kết quả" | `ctlNoResult` / `imgNoData` / `tvNoResultSubtext` | `searchNoResultView.isHidden` |
| `showsResults()` | Hiện list + tiêu đề "Kết quả tìm kiếm" | `rcvSearchList`, `tvTitle` | `tableView`, `resultSearchLabel` |
| `voucher(id)` | Tra voucher theo id (điều hướng, bottom sheet) | — (adapter đưa thẳng item) | cả hai VM |

> ⚠️ **Hai nền tảng từng phát biểu luật này khác nhau.** Android đòi
> `keyword.trim().length >= MIN_KEYWORD_LENGTH && !isLoading && isEmpty`; iOS chỉ xét `isEmpty` (và
> mượn `!isLoading` từ nhánh `if` bao ngoài), thiếu hẳn điều kiện từ khoá. Chúng **ra cùng kết quả**
> chỉ vì `SearchMyPromotionStore.resetSearchResults()` set `isEmpty = false` khi xoá từ khoá — sửa
> chỗ đó là hai bên lệch âm thầm. Hằng `MIN_KEYWORD_LENGTH = 1` đã bỏ: nó chỉ là cách viết khác của
> "từ khoá không rỗng". Test khoá luật: `SearchMyPromotionStoreTest.showsNoResult_*`.

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
- `OpenServiceSelector(voucher)` — bấm "Sử dụng" trên một kết quả.
- `ServiceSelected(voucher, service)` — đã chọn dịch vụ trong bottom sheet.

> Chỉ khai báo action mà màn **thật sự phát** — khớp 1-1 `SearchMyPromotionViewModel.Input` bên iOS.
> `SearchMyPromotionIntent.ClearKeyword` / `Retry` của store hiện chưa màn nào dùng (gõ trắng đã đi
> qua `QueryChanged("")`), nên không có action UI tương ứng.

### Effect — `SearchMyPromotionEffect`
- `ShowError(errorCode)`.
- `ShowServiceSelector(voucher, services)`.

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

## 3. API backend

- Dùng chung endpoint với MyPromotion: `GET /promotion/promotion-vtm-bff/api/v1/vtm/customer-vouchers`
- Search luôn truyền `tab=all`; keyword free search (không giới hạn độ dài tối thiểu).
- Response: flat `content[]` + pagination (`number`, `size`, `last`).

## 4. Chọn dịch vụ ("Sử dụng")

Bấm "Sử dụng" trên một kết quả → `ServiceSelectorBottomSheet` (đúng bộ dịch vụ như màn "Ưu đãi của
tôi"): lọc bằng `servicesForApplicableProducts` ở `promotionLogic`, giao giữa `applicableProducts` của
voucher và `availableServices` host truyền lúc `initialize`. Chọn xong → phát
`PromotionSDK.getCallback()?.onServiceSelected(...)` cho host.

Đối ứng `SearchMyPromotionViewController.myPromotionCellDidTapUse` bên iOS — xem
[InitParity §3](../common/InitParity.md#3-promotionsdkcallback--hợp-nhất-theo-ios-6-sự-kiện-tên-trùng-cả-2-bên).

## 5. Lưu ý khi sửa

- `DEFAULT_PAGE_SIZE` khai báo trong `companion object` của state — đổi ở đây, không hardcode rải rác.
- Debounce search xử lý trong `SearchMyPromotionStore` (`DEBOUNCE_MS = 400ms`), không ở Fragment.
- Keyword rỗng sau trim → reset kết quả về rỗng, không gọi API.
- Bấm vào card mở chi tiết bằng `openPromotionDetail(voucher)` (có **seed**) — không dùng bản chỉ
  truyền `voucherId`, để card + nút hiện ngay không chờ mạng.
- Hiển thị trạng thái rỗng/lỗi dựa trên `isEmpty` + `Effect.ShowError`.
