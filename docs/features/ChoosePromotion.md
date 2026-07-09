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
| Dữ liệu | `vouchers`, `otherVouchers` (đều là `List<MyVoucherListItem>`) — bản **đã lọc theo từ khoá** |

Nguồn sự thật là hai field private `myLoaded` / `otherLoaded` kiểu `List<EligibleOffer>`; state chỉ
giữ bản đã lọc và đã map sang model UI.

### Nguồn dữ liệu: `findEligible`

Màn này gọi `FindEligibleCampaignsUseCase` (`POST .../redemption/eligible`), **không** phải
`searchVouchers`. Nhờ đó `otherOffers` (campaign công khai khách chưa nhận) mới có dữ liệu — trước
đây Android gọi `searchVouchers` nên section "Ưu đãi khác" luôn rỗng, lệch với iOS.

`findEligible` **không nhận `keyword`** → ô tìm kiếm **chưa chạy**. Khung đã dựng theo đúng khuôn
`SearchMyPromotionViewModel`, còn `search()` để trống — xem `TODO(search)`. Không lọc trong bộ nhớ,
vì lọc client chỉ đúng trên trang đầu và sẽ im lặng trả sai kết quả khi danh sách dài hơn một trang.

### Action — `ChoosePromotionAction`
- `LoadInitial` — load lần đầu (cả hai nhóm, `section = null`).
- `PreloadVouchers(myOffers, otherOffers)` — **nhận data đã load sẵn từ `PRMEndowView`** để tránh gọi API 2 lần; nếu cả hai rỗng thì ViewModel tự gọi API. Mang `List<EligibleOffer>` chứ không phải model UI, vì bộ lọc từ khoá chạy trên `campaignName` của bản gốc.
- `Refresh` — làm mới.
- `QueryChanged(keyword)` — gõ mỗi ký tự → debounce 400ms → `search()` (**chưa triển khai**).
- `Search` — bấm Enter: chạy ngay, bỏ debounce.
- `ClearKeyword` — xoá trắng → hiện lại toàn bộ danh sách đã tải.
- `LoadMoreMyVouchers` / `LoadMoreOtherVouchers` — phân trang từng nhóm **độc lập** (`section = MY_OFFERS` / `OTHER_OFFERS`); response chỉ chứa nhóm được hỏi, nhóm kia là null.
- `ValidateAndApply(selected)` — validate stackable discount cho các voucher đã chọn rồi áp dụng.

### Effect — `ChoosePromotionEffect`
- `OpenVoucherDetail(voucherId)` — Fragment mở qua `openPromotionDetail()`, gác bởi cờ `VOUCHER_DETAIL`.
- `ShowError(errorCode)`.
- `ApplyValidatedVouchers(details: List<AppliedDiscount>)` — phát khi `validateStackableDiscounts` thành công; `details` map từ domain result sang model public `AppliedDiscount`.

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

- Request validate được dựng qua extension `toValidateDiscountsRequest()` (`ui/feature/promotion/ext/PromotionUiMapper.kt`).
- Use case: `ValidateStackableDiscountsUseCase` (Domain) → repository → API.
- `objectId` gửi lên là `EligibleOffer.id`: `voucherId` nếu khách đã sở hữu, ngược lại `campaignId`.

---

## 3. Tái sử dụng

- Dùng lại `MyVoucherListItem`, `TabItem` từ feature **My Promotion** (không định nghĩa lại model voucher).
- `PreloadVouchers` là tối ưu quan trọng: dữ liệu được nạp ở `PRMEndowView` rồi truyền sang để **tránh double API call**.

---

## 4. Lưu ý khi sửa

- Hai danh sách (mine/other) có phân trang độc lập — giữ tách biệt `page` và `otherPage`.
- Mọi thay đổi cấu trúc request stackable discount → đồng bộ với `core/data/dto/stackablediscount/` và `NetworkingGuide.md`.
- Liên quan: [EndowView.md](./EndowView.md), [MyPromotion.md](./MyPromotion.md).
