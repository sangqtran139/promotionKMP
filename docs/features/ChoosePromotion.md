# Feature: Choose Promotion

Màn hình **chọn voucher để áp dụng vào đơn hàng**, có **validate stackable discount** trước khi áp dụng.
Hỗ trợ hai danh sách (voucher của tôi + voucher khác), phân trang riêng từng danh sách.

- **Package:** `ui/feature/promotion/choosepromotion`
- **Thành phần:** `ChoosePromotionFragment`, `ChoosePromotionViewModel`, `ChoosePromotionContract`, `adapter/`

> **Cập nhật (tầng UI-logic dùng chung `ChoosePromotionStore`):**
> - **Selection** (`selectedIds`, rule single/multi qua `ToggleSelection`, seed `SetPreSelected`) và
>   **"Xem thêm/Thu gọn"** (`myExpanded` + state-machine `SeeMoreMy`, `mySeeMoreState()`/`visibleMyOffers()`)
>   nằm trong store — dùng chung Android & iOS.

### Nút "Xem thêm/Thu gọn" — `mySeeMoreState()`

| Điều kiện | Trạng thái nút |
|---|---|
| `myOffers.size <= COLLAPSED_MY_COUNT` (= 2) | **`HIDDEN`** — thu gọn đã thấy hết, không có gì để mở |
| đang mở hết **và** `myIsLastPage` | `COLLAPSE` ("Thu gọn") |
| còn lại | `EXPAND` ("Xem thêm") |

Nhánh `HIDDEN` **không** xét `myIsLastPage`: nhóm "của tôi" nạp theo trang 10 item
(`EndowStore.PAGE_SIZE`), nên ≤ 2 item nghĩa là server đã trả hết.

### Hai dòng chữ trên card

Cùng quy ước với màn "Ưu đãi của tôi" — sửa một bên thì sửa cả hai:

| Dòng | Style | Nguồn | Android | iOS |
|---|---|---|---|---|
| Nhỏ, trên | Regular 12 / `tokenDark60`, 1 dòng | `partnerName ?: campaignName` (merchant) | `MyVoucherListItem.merchantName` → `txtVoucherName` | `MyPromotionCellViewModel.title` → `titleLabel` |
| To, dưới | Medium 16 / `tokenDark100`, 2 dòng | `estimatedDiscount` đã format ("Giảm giá 50.000đ") | `.title` → `tvContent` | `.description` → `descriptionLabel` |

> ⚠️ Tên field dễ nhầm ở **hai tầng**:
> - iOS `MyPromotionCellViewModel.title` là dòng **nhỏ**, `description` là dòng **to** — ngược trực giác.
> - Android `MyVoucherListItem.title` mang **số tiền giảm**, không phải tên ưu đãi.
>
> Đây là chỗ iOS từng đổ ngược (số tiền giảm lên dòng nhỏ, merchant xuống dòng to). Android là bản
> đúng; iOS đã nắn theo. `campaignName` chỉ dùng làm fallback khi thiếu `partnerName`.
>
> Khác màn "Ưu đãi của tôi" (dòng to là **tên voucher**): ở luồng checkout, con số giảm được ưu tiên
> hiển thị vì user đang so các ưu đãi để chọn.

### Shimmer

Hiện theo `state.isLoading` — tức cả lúc tải trang đầu **và** mỗi lần tìm kiếm (search đi qua
`loadOffers`, không phải `loadMore`). Debounce 400ms nằm trước `loadOffers` nên shimmer chỉ bật khi
thật sự gọi API, không nháy theo từng ký tự.

Cả hai nền tảng đều phải **che hẳn danh sách cũ**, nếu không user tìm kiếm sẽ thấy kết quả cũ và
tưởng shimmer không chạy:

| | Cách che | Bẫy |
|---|---|---|
| Android | `rcvVoucher.isVisible = !state.isLoading` | `shimmer_provider` là con **đầu tiên** của `ConstraintLayout` còn `rcvVoucher` là con sau nó → RecyclerView (nền đục `color_f4f4f4`) vẽ đè kín shimmer. Ẩn list là bắt buộc, không phải cho đẹp |
| iOS | `shimmerView.backgroundColor = Colors.tokenDark05` | `PRMShimmerReplicatorView` **không** tự có nền; để trong suốt thì table lộ xuyên qua. Đối ứng `shimmerOverlay` (nền `tokenDark02`) ở `MyPromotionViewController` |

### Vạch ngăn hai nhóm

Giữa "Ưu đãi của tôi" và "Ưu đãi khác" có một vạch tràn hết bề ngang: **cao 16dp, nền `#FBFBFB`,
cách nhóm trên và nhóm dưới 8dp**. Dùng `dp/pt` cứng (không `sdp`) để hai nền tảng bằng nhau tuyệt đối.

| | Android | iOS |
|---|---|---|
| Dựng | `ChoosePromotionListItem.SectionDivider` + `prm_item_section_divider.xml` | footer của section, `viewForFooterInSection` |
| Chiều cao khối | 8 + 16 + 8 (margin + view + margin) | `dividerFooterHeight` = 8 + 16 + 8 = 32pt |
| Màu | `@color/color_FBFBFB` | `Colors.tokenDark02` |
| Chỉ kẻ khi | `items.isNotEmpty()` — có nhóm ở trên | `section < sections.count - 1` — còn section phía sau |

> ⚠️ **Khoảng 8dp phía trên phải đều ở cả hai trường hợp** — nhóm "Ưu đãi của tôi" có hiện hàng
> "Xem thêm" hay không. Vạch luôn đứng **sau** hàng đó nên thứ tự không đổi, nhưng chân đế thì có:
> `item_choose_promotion.xml` có `paddingVertical="@dimen/_6sdp"` còn `prm_item_see_more.xml` trước
> đây **không có** `paddingBottom` → thiếu 6sdp khi nút hiện. Đã thêm `paddingBottom="@dimen/_6sdp"`
> vào hàng "Xem thêm" cho bằng. Sửa padding của một trong hai file thì phải sửa file kia.

### Field của `findEligible` mà SDK chưa đọc

Spec: [`docs/api/3.5.4 API Find Eligible`](../api/). Đã map đủ `campaignId`, `voucherId`,
`voucherName ?: campaignName`, `campaignType`, `discountType`, `usable`, `logoUrl`, `partnerName`,
`voucherCode`, `discountPreview.*`, `validity.*`, `expiresAt`, `budgetStatus.available`,
`eligibilityDetails.unmatchedRules`, `last`, `totalElements`, `expireWarningDate`. Còn lại **cố ý
chưa dùng** — đừng tưởng là sót:

| Field | Spec nói | Hiện trạng |
|---|---|---|
| `description` | Mô tả ngắn về ưu đãi (`campaigns.description`) | DTO có khai, domain `EligibleOffer` không có field |
| `voucherStatus` | Trạng thái voucher đang sở hữu (chỉ `myOffers`) | DTO có khai, không map — trạng thái suy từ `usable` |
| `metadata` | Key nghiệp vụ cashback/tiết kiệm (§6.2.2) | **DTO chưa khai** |
| `discountPreview.discountFormula`, `applicableItems[]` | — | DTO chưa khai |
| `eligibilityDetails.eligibilityScore` / `matchedRules` / `warnings` | `eligibilityScore` dùng để sort | Không map — server đã sort sẵn, SDK giữ nguyên thứ tự |

### Cờ phân trang khi mở màn từ widget

`EndowStore` giữ `myIsLastPage` / `otherIsLastPage` của chính lần `findEligible` nó gọi. Màn "Chọn
ưu đãi" nhận lại qua `Preload` nên biết đúng còn trang hay không:

| Chặng | Android | iOS |
|---|---|---|
| Widget giữ cờ | `EndowState` → `PRMEndowUiState` → `PRMEndowView.myIsLastPage` | `EndowState` (đọc thẳng qua `endowVM.state`) |
| Truyền sang màn chọn | `ChoosePromotionFragment.forEndowView` → `PreloadVouchers` | `PromotionSDKImpl.openChoosePromotion` → `ChoosePromotionBuilder.DataModel` |

Kết quả `findEligible` là `null` (API lỗi) → cả hai cờ về `true`, không mở đường gọi trang kế.
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

Màn này gọi `FindEligibleCampaignsUseCase` (`POST .../redemptions/eligible`), **không** phải
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
> bên iOS (completion của `endowVM.validateAndApply`).

- Use case: `ValidateStackableDiscountsUseCase` (Domain) → repository → API, gọi từ `EndowStore`.
- `objectId` gửi lên là `EligibleOffer.id`: `voucherId` nếu khách đã sở hữu, ngược lại `campaignId`.
- Pre-select khi mở lại màn: **tất cả** `discountDetails` đang áp (kể cả item không còn hợp lệ) để user
  thấy và bỏ chọn được — khớp iOS.

---

## 3. Hiển thị 1 item (parity Android ↔ iOS)

Mọi **quyết định** đều lấy từ `ChooseOffer` do store dựng — native chỉ format, **không tự suy lại**:

| Quyết định | Nguồn | Android | iOS |
|---|---|---|---|
| Còn dùng được | `ChooseOffer.isUsable` → `MyVoucherListItem.isEnabled` | mờ card + dải "Chưa đủ điều kiện áp dụng" + nhãn lý do, ẩn "Chi tiết" & checkbox | `isDisabled` (blur overlay) + `isEligible` (warningView) + `stateText` |
| Lý do không đủ điều kiện | `EligibleOffer.unmatchedRules.first`, dự phòng "Không đủ điều kiện" | `displayStatusLabel` → `txtExpired` | `stateText` |
| Sắp hết hạn | `ChooseOffer.expiringInDays` (theo `expireWarningDate` của server) | "HSD còn X ngày" (màu cam `#F47527`), dự phòng "HSD: dd/MM/yyyy" (màu mặc định), không có HSD → "HSD: Không hết hạn" | như trên |
| Highlight từ khoá | `state.keyword.trim()` | `toHighlightedSpannable` | `PromotionCardModel.highlightKeyword` |

- **Không dùng `status` để suy trạng thái ở màn này**: `EligibleOffer.toMyVoucherListItem()` chỉ sinh
  `AVAILABLE` / `INELIGIBLE`, nên mọi nhánh theo `EXPIRED`/`REVOKED` sẽ **không bao giờ chạy**.
- **Ưu đãi không dùng được thì không chọn được** ở cả hai bên: Android chặn ở `root.setOnClickListener`,
  iOS chặn ở `ChoosePromotionItemCell.promotionCardViewDidTap` (tap thân card ở màn này = *chọn*, không
  phải xem chi tiết) — checkbox đã bị `PromotionCardView` chặn sẵn.
- Thanh "Đã chọn N voucher" chỉ hiện khi `isMultiSelection` **và** có item đang chọn — Android
  `updateApplyButtonState`, iOS `UiState.showsSelectedCount`. Không bên nào hiện số tiền giảm.
- Bấm "Áp dụng" khi **không chọn gì** → không làm gì (không đóng màn, không gỡ ưu đãi đang áp).
  Muốn gỡ thì bấm vào widget `PRMEndowView` (`clearApplied`).

## 4. Tái sử dụng

- Dùng lại `MyVoucherListItem`, `TabItem` từ feature **My Promotion** (không định nghĩa lại model voucher).
- `PreloadVouchers` là tối ưu quan trọng: dữ liệu được nạp ở `PRMEndowView` rồi truyền sang để **tránh double API call**.

---

## 5. Lưu ý khi sửa

- Hai danh sách (mine/other) có phân trang độc lập — giữ tách biệt `page` và `otherPage`.
- Mọi thay đổi cấu trúc request stackable discount → đồng bộ với `data/dto/stackablediscount/` và `NetworkingGuide.md`.
- Liên quan: [EndowView.md](./EndowView.md), [MyPromotion.md](./MyPromotion.md).
