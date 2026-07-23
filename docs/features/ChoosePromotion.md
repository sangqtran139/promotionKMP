# Feature: Choose Promotion

Màn hình **chọn voucher để áp dụng vào đơn hàng**, có **validate stackable discount** trước khi áp dụng.
Hỗ trợ hai danh sách (voucher của tôi + voucher khác), phân trang riêng từng danh sách.

- **Package:** `ui/feature/promotion/choosepromotion`
- **Thành phần:** `PRMChoosePromotionFragment`, `ChoosePromotionViewModel`, `ChoosePromotionContract`, `adapter/`

> **Cập nhật (tầng UI-logic dùng chung `ChoosePromotionStore`):**
> - **Selection** (`selectedIds`, rule single/multi qua `ToggleSelection`, seed `SetPreSelected`) và
>   **"Xem thêm/Thu gọn"** (`myExpanded` + state-machine `SeeMoreMy`, `mySeeMoreState()`/`visibleMyOffers()`)
>   nay nằm trong store — dùng chung Android & iOS (trước đây mỗi bên tự viết ở Fragment/VC).
> - **Validate KHÔNG còn ở màn này.** Bấm "Áp dụng" chỉ **trả offers đang chọn** (`ApplySelectedOffers`);
>   validate & apply do **`EndowStore`** lo (xem [EndowView.md](./EndowView.md)).

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

Tìm kiếm chạy **server-side** (parity Android ↔ iOS): `keyword` gửi kèm mỗi request `findEligible`
(v1.6 §7.3), server lọc cả `myOffers` lẫn `otherOffers`; đổi keyword → reload trang 0, keyword đi
kèm cả load-more. **Không** lọc client trong bộ nhớ — lọc client chỉ đúng trên trang đã tải và sẽ im
lặng trả sai khi danh sách dài hơn một trang (iOS trước đây lọc client qua `PRMOfferSearchFilter`, nay đã gỡ).

### Action — `ChoosePromotionAction`
- `LoadInitial` — load lần đầu (cả hai nhóm, `section = null`).
- `PreloadVouchers(myOffers, otherOffers)` — **nhận data đã load sẵn từ `PRMEndowView`** để tránh gọi API 2 lần; nếu cả hai rỗng thì ViewModel tự gọi API. Mang `List<EligibleOffer>` (model lõi) chứ không phải model UI để giữ nguồn sự thật ở domain, map sang UI khi publish. Cả contract này là `internal` — `EligibleOffer` thuộc lõi.
- `Refresh` — làm mới.
- `QueryChanged(keyword)` — gõ mỗi ký tự → debounce 400ms → `search()` → reload server-side kèm `keyword`.
- `Search` — bấm Enter: chạy ngay, bỏ debounce.
- `ClearKeyword` — xoá trắng → reload danh sách đầy đủ (không gửi `keyword`).
- `LoadMoreMyVouchers` / `LoadMoreOtherVouchers` — phân trang từng nhóm **độc lập** (`section = MY_OFFERS` / `OTHER_OFFERS`); response chỉ chứa nhóm được hỏi, nhóm kia là null.
- `ValidateAndApply(selected)` — validate stackable discount cho các voucher đã chọn rồi áp dụng.

### Effect — `ChoosePromotionEffect`
- `OpenVoucherDetail(voucherId)` — Fragment mở qua `openPromotionDetail()`, gác bởi cờ `VOUCHER_DETAIL`.
- `ShowError(errorCode)`.
- `ApplySelectedOffers(offers: List<EligibleOffer>)` — phát khi bấm "Áp dụng"; chỉ **trả offers đang chọn**, không validate.

---

## 2. Luồng apply (validate nằm ở `EndowStore`)

```
User bấm "Áp dụng" → Action.ValidateAndApply
  ViewModel: lọc offers theo store.selectedIds → sendEffect(ApplySelectedOffers(offers))
Fragment: onApplySelectedOffers(offers) { errorCode -> ... }
        → PRMEndowView.applySelectedOffers(offers, onSettled)
        → PRMEndowViewModel.validateAndApply(offers) { state -> onSettled(state.errorCode) }
        → EndowStore: isValidating=true → validateStackableDiscounts → isValidating=false
  errorCode != null → showToast(mapPromotionError(code)), **Ở LẠI** màn chọn (không áp)
  errorCode == null → onBackFragment() (đóng màn; widget đã cập nhật qua state)
```

> **Màn chỉ đóng khi validate xong và không lỗi** — đối xứng `PromotionSDKImpl.openChoosePromotion`
> bên iOS (completion của `endowVM.validateAndApply`). Trước đây Android đóng ngay lập tức nên nuốt
> mất lỗi validate.

- Use case: `ValidateStackableDiscountsUseCase` (Domain) → repository → API, gọi từ `EndowStore`.
- `objectId` gửi lên là `EligibleOffer.id`: `voucherId` nếu khách đã sở hữu, ngược lại `campaignId`.
- Pre-select khi mở lại màn: **tất cả** `discountDetails` đang áp (kể cả item không còn hợp lệ) để user
  thấy và bỏ chọn được — khớp iOS.

---

## 3. Tái sử dụng

- Dùng lại `MyVoucherListItem`, `TabItem` từ feature **My Promotion** (không định nghĩa lại model voucher).
- `PreloadVouchers` là tối ưu quan trọng: dữ liệu được nạp ở `PRMEndowView` rồi truyền sang để **tránh double API call**.

---

## 4. Lưu ý khi sửa

- Hai danh sách (mine/other) có phân trang độc lập — giữ tách biệt `page` và `otherPage`.
- Mọi thay đổi cấu trúc request stackable discount → đồng bộ với `core/data/dto/stackablediscount/` và `NetworkingGuide.md`.
- Liên quan: [EndowView.md](./EndowView.md), [MyPromotion.md](./MyPromotion.md).
