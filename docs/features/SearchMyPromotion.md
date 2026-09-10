# Feature: Search My Promotion

Màn hình **tìm kiếm voucher của khách hàng** theo từ khoá, có phân trang và trạng thái rỗng/lỗi.

- **Package:** `ui/feature/promotion/searchmypromotion`
- **Thành phần:** `SearchMyPromotionFragment`, `SearchMyPromotionViewModel`, `SearchMyPromotionContract`

## Mục lục

<!-- toc -->
  - [Quyết định hiển thị — native không tự suy](#quyết-định-hiển-thị--native-không-tự-suy)
- [1. Contract (dùng chung 2 nền tảng)](#1-contract-dùng-chung-2-nền-tảng)
  - [1.1. State — `SearchMyPromotionState`](#11-state--searchmypromotionstate)
  - [1.2. Intent — `SearchMyPromotionIntent`](#12-intent--searchmypromotionintent)
  - [1.3. Bottom sheet "Chọn dịch vụ" — không đi qua store](#13-bottom-sheet-chọn-dịch-vụ--không-đi-qua-store)
- [2. Luồng dữ liệu](#2-luồng-dữ-liệu)
- [3. API backend](#3-api-backend)
- [4. Chọn dịch vụ ("Sử dụng")](#4-chọn-dịch-vụ-sử-dụng)
- [5. Lưu ý khi sửa](#5-lưu-ý-khi-sửa)
<!-- /toc -->

---

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
## 1. Contract (dùng chung 2 nền tảng)

Contract ở lõi: `promotionLogic/…/presentation/searchmypromotion/SearchMyPromotionContract.kt`.

### 1.1. State — `SearchMyPromotionState`
| Field | Ý nghĩa |
|-------|---------|
| `keyword` | Từ khoá hiện tại |
| `vouchers` | `List<MyPromotionVoucher>` kết quả (dùng lại model của màn "Ưu đãi của tôi") |
| `isLoading` / `isLoadingMore` | Trạng thái tải |
| `isEmpty` | Không có kết quả |
| `isLastPage` | Đã hết trang |
| `page` / `pageSize` | Phân trang (mặc định `pageSize = 10`) |
| `errorCode` | Mã lỗi một-lần; hiển thị xong `dispatch(ConsumeError)` |

### 1.2. Intent — `SearchMyPromotionIntent`
- `QueryChanged(keyword)` — người dùng gõ từ khoá.
- `Search` — thực hiện tìm kiếm.
- `LoadMore` — tải trang tiếp.
- `ClearKeyword` — nút "X" xoá tường minh.
- `Retry` — thử lại sau lỗi.
- `ConsumeError` — xoá `errorCode` sau khi đã hiển thị.

### 1.3. Bottom sheet "Chọn dịch vụ" — **không** đi qua store

Bấm "Sử dụng" trên một kết quả **không** phát Intent nào. Fragment gọi thẳng
`viewModel.serviceOptions(voucher)` rồi `ServiceSelectorBottomSheet.present(...)`; danh sách dịch vụ
dựng từ hàm dùng chung `presentation/serviceselector/ServiceSelector.kt`
(`servicesForApplicableProducts` / `configuredServicesFor`). Chọn xong thì bắn thẳng
`PromotionSDKCallback.onServiceSelected` ra host.

> `SearchMyPromotionEffect` / `SearchMyPromotionAction` / `SearchMyPromotionUiState` **đã bị bỏ** —
> lỗi đi qua `errorCode` trong state + `PRMEffect.ShowError` dùng chung.

---

## 2. Luồng dữ liệu

```
Fragment gõ → dispatch(QueryChanged(keyword)) → state.copy(keyword = …)
dispatch(Search)   → state.copy(isLoading = true, page = 0)
                   → SearchCustomerVouchersUseCase(request(keyword, page, size))
                   → state.copy(vouchers = …, isEmpty = …, isLastPage = …, isLoading = false)
dispatch(LoadMore) → tăng page → nối thêm vouchers
Lỗi → state.copy(errorCode = …) → PRMEffect.ShowError → popup → dispatch(ConsumeError)
```

- Dùng chung use case `SearchCustomerVouchersUseCase` và model `MyPromotionVoucher` của lõi; riêng Android map tiếp sang `MyVoucherListItem` để RecyclerView render.

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
