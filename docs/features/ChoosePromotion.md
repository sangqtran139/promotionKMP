# Feature: Choose Promotion

Màn hình **chọn voucher để áp dụng vào đơn hàng**, có **validate stackable discount** trước khi áp dụng.
Hỗ trợ hai danh sách (voucher của tôi + voucher khác), phân trang riêng từng danh sách.

- **Package:** `ui/feature/promotion/choosepromotion`
- **Thành phần:** `ChoosePromotionFragment`, `ChoosePromotionViewModel`, `ChoosePromotionContract`, `adapter/`

---

## 1. Contract (MVI)

### State — `ChoosePromotionUiState`
| Nhóm | Field |
|------|-------|
| Trạng thái tải | `hasLoadedInitial`, `isLoading`, `isRefreshing`, `isLoadingMore`, `isLoadingMoreOther`, `isValidating`, `isEmpty` |
| Tab & tìm kiếm | `tabs`, `selectedTabCode`, `keyword` |
| Phân trang "my vouchers" | `page`, `size`, `isLastPage` |
| Phân trang "other vouchers" | `otherPage`, `otherSize`, `isLastOtherPage` |
| Dữ liệu | `vouchers`, `otherVouchers` (đều là `List<MyVoucherListItem>`) |

### Action — `ChoosePromotionAction`
- `LoadInitial` — load lần đầu.
- `PreloadVouchers(myVouchers, otherVouchers)` — **nhận data đã load sẵn từ `PRMEndowView`** để tránh gọi API 2 lần; nếu cả hai rỗng thì ViewModel tự gọi API.
- `Refresh` — làm mới.
- `SearchKeyword(keyword)` — tìm kiếm.
- `LoadMoreMyVouchers` / `LoadMoreOtherVouchers` — phân trang từng danh sách.
- `ValidateAndApply(selected)` — validate stackable discount cho các voucher đã chọn rồi áp dụng.

### Effect — `ChoosePromotionEffect`
- `OpenVoucherDetail(voucherId)`.
- `ShowError(errorCode)`.
- `ApplyValidatedVouchers(details: List<DiscountDetail>)` — phát khi `validateStackableDiscounts` thành công; `details` lấy từ `discountDetails` của response.

---

## 2. Luồng validate & apply

```
User chọn voucher → Action.ValidateAndApply(selected)
  ViewModel: setState(isValidating=true)
           → launch { validateStackableDiscountsUseCase(request) }
           → thành công → sendEffect(ApplyValidatedVouchers(details))
           → thất bại  → sendEffect(ShowError(code))
           → setState(isValidating=false)
Fragment: handleEffect(ApplyValidatedVouchers) → trả kết quả áp dụng về luồng gọi (vd PRMEndowView)
```

- Request validate được dựng qua extension `toStackableDiscountsRequest()` (`promotion/ext/StackableRequestExtensions.kt`).
- Use case: `ValidateStackableDiscountsUseCase` (Domain) → repository → API.

---

## 3. Tái sử dụng

- Dùng lại `MyVoucherListItem`, `TabItem` từ feature **My Promotion** (không định nghĩa lại model voucher).
- `PreloadVouchers` là tối ưu quan trọng: dữ liệu được nạp ở `PRMEndowView` rồi truyền sang để **tránh double API call**.

---

## 4. Lưu ý khi sửa

- Hai danh sách (mine/other) có phân trang độc lập — giữ tách biệt `page` và `otherPage`.
- Mọi thay đổi cấu trúc request stackable discount → đồng bộ với `core/data/dto/stackablediscount/` và `NetworkingGuide.md`.
- Liên quan: [EndowView.md](./EndowView.md), [MyPromotion.md](./MyPromotion.md).
