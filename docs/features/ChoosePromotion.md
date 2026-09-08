# Feature: Choose Promotion

Màn hình **chọn voucher để áp dụng vào đơn hàng**, có **validate stackable discount** trước khi áp dụng.
Hỗ trợ hai danh sách (voucher của tôi + voucher khác), phân trang riêng từng danh sách.

- **Package:** `ui/feature/promotion/choosepromotion`
- **Thành phần:** `ChoosePromotionFragment`, `ChoosePromotionViewModel`, `ChoosePromotionContract`, `adapter/`

> **Cập nhật (tầng UI-logic dùng chung `ChoosePromotionStore`):**
> - **Selection** (`selectedIds`, rule single/multi qua `ToggleSelection`, seed `SetPreSelected`) và
>   **"Xem thêm/Thu gọn"** (`myExpanded` + state-machine `SeeMoreMy`, `mySeeMoreState()`/`visibleMyOffers()`)
>   nằm trong store — dùng chung Android & iOS.

## Mục lục

<!-- toc -->
  - [Quyết định hiển thị — native không tự suy](#quyết-định-hiển-thị--native-không-tự-suy)
  - [Nút "Xem thêm/Thu gọn" — `mySeeMoreState()`](#nút-xem-thêmthu-gọn--myseemorestate)
  - [Hai dòng chữ trên card](#hai-dòng-chữ-trên-card)
  - [Shimmer](#shimmer)
  - [Vạch ngăn hai nhóm](#vạch-ngăn-hai-nhóm)
  - [Loading khi tải thêm trang](#loading-khi-tải-thêm-trang)
  - [Tìm không ra kết quả](#tìm-không-ra-kết-quả)
  - [Field của `findEligible` mà SDK chưa đọc](#field-của-findeligible-mà-sdk-chưa-đọc)
  - [Cờ phân trang khi mở màn từ widget](#cờ-phân-trang-khi-mở-màn-từ-widget)
- [1. Contract (MVI)](#1-contract-mvi)
  - [1.1. State — `ChoosePromotionUiState`](#11-state--choosepromotionuistate)
  - [1.2. Nguồn dữ liệu: `findEligible`](#12-nguồn-dữ-liệu-findeligible)
  - [1.3. Action — `ChoosePromotionAction`](#13-action--choosepromotionaction)
  - [1.4. Effect — `ChoosePromotionEffect`](#14-effect--choosepromotioneffect)
- [2. Luồng apply (validate nằm ở `EndowStore`)](#2-luồng-apply-validate-nằm-ở-endowstore)
  - [2.1. Chống spam nút "Áp dụng"](#21-chống-spam-nút-áp-dụng)
- [3. Hiển thị 1 item (parity Android ↔ iOS)](#3-hiển-thị-1-item-parity-android--ios)
  - [3.1. Dải "Chưa đủ điều kiện áp dụng" phải luồn xuống dưới card](#31-dải-chưa-đủ-điều-kiện-áp-dụng-phải-luồn-xuống-dưới-card)
- [4. `serviceCode` đi vào request bằng đường nào](#4-servicecode-đi-vào-request-bằng-đường-nào)
- [5. Tái sử dụng](#5-tái-sử-dụng)
- [6. Lưu ý khi sửa](#6-lưu-ý-khi-sửa)
<!-- /toc -->

---

### Quyết định hiển thị — **native không tự suy**

Mọi hàm dưới đây là extension của `ChoosePromotionState` trong `ChoosePromotionContract.kt`. Trước
đây mỗi cái tồn tại **hai bản**, một trong `ChoosePromotionFragment`/`ChoosePromotionViewModel`
(Android) và một trong `Display`/`ViewController` (iOS). Thêm quyết định hiển thị mới thì thêm ở đây,
đừng viết vào Fragment/VC.

| Hàm | Trả lời | Android dùng ở | iOS dùng ở |
|---|---|---|---|
| `selectedOffers()` | Ưu đãi user đang chọn → gửi đi validate & áp | `ChoosePromotionViewModel.selectedOffers()` | `ChoosePromotionViewModel.selectedOffers()` |
| `allOffers()` | Gộp hai nhóm về `EligibleOffer`, "của tôi" trước | (qua `selectedOffers`) | `openDetail(voucherId:)` |
| `canApply()` | Nút "Áp dụng" bấm được chưa (gồm cả **đang chờ kết quả áp**) | `btnApply.isEnabled` | `Display.canApply` |
| `showsSelectedCount()` | Hiện thanh "Đã chọn N voucher" | `updateApplyButtonState` | `Display.showsSelectedCount` |
| `showsNoResult()` | Hiện view "không tìm thấy" thay cho list | `observeData` | `Display.showsNoResult` |
| `highlightKeyword()` | Từ khoá tô đậm (đã trim) | `rebuildList` | `buildSections` |
| `isSelected(id)` | Card có tick không | `rebuildList` | `buildSections` |
| `shouldLoadMoreOther(visibleIndex)` | Nạp trang kế nhóm "Ưu đãi khác" chưa | scroll listener | `willDisplay` |

> **`QueryChanged("")` ≡ `ClearKeyword`.** Store tự huỷ debounce và nạp lại ngay khi từ khoá rỗng
> (`ChoosePromotionStore.onQueryChanged`), nên native **không** rẽ nhánh `if keyword.isEmpty()` nữa —
> cả hai bên từng chép cùng một nhánh đó. `ClearKeyword` vẫn còn cho nút "X" xoá tường minh.

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
| To, dưới | Medium 16 / `tokenDark100`, 2 dòng | **`campaignName`** = `voucherName ?: campaignName` (tên ưu đãi) | `.title` → `tvContent` | `.description` → `descriptionLabel` |

> **Dòng to từng là số tiền giảm** — `discountPreview.estimatedDiscount` format thành "Giảm 50.000đ".
> Đã bỏ: user cần biết mình đang chọn ưu đãi **nào**. Hai hàm format đi kèm cũng xoá luôn
> (`formatEstimatedDiscount` trong `PromotionUiMapper.kt`, `MyPromotionCell.formatDiscount` bên iOS)
> — widget checkout format số tiền theo đường riêng, không dùng chung.
>
> `voucherName` **không phải field riêng** ở domain: `EligibleCampaignsMapper.toEligibleOffer` hợp
> nhất `campaignName = voucherName ?: campaignName`. Nhờ đó nhóm "của tôi" ra tên voucher, còn nhóm
> "Ưu đãi khác" (chưa nhận nên không có voucher, không có `voucherName`) ra tên campaign — đúng ngữ
> nghĩa. Muốn phân biệt hai thứ thì phải tách field ở `EligibleOffer` trước.

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

### Loading khi tải thêm trang

Chỉ nhóm **"Ưu đãi khác"** phân trang theo cuộn nên chỉ nhóm đó có chỉ báo; nhóm "Ưu đãi của tôi"
lấy trang kế bằng nút "Xem thêm" (`isLoadingMore` không được render ở đâu cả).

| | Android | iOS |
|---|---|---|
| Cờ state | `isLoadingMoreOther` | `isLoadingMoreOther` → `Display.isLoadingMoreOther` |
| Dựng | `ChoosePromotionListItem.Loading` nối cuối list trong `rebuildList()` | `tableFooterView = loadMoreSpinner`, `renderLoadMore()` |
| Giao diện | `prm_item_loading_notify_prm.xml` (ProgressBar 24sdp + chữ "Đang tải") — **dùng chung** với màn "Ưu đãi của tôi" | `UIActivityIndicatorView(.medium)` cao 44pt |

> Ba màn có phân trang dùng **hai** cơ chế khác nhau bên iOS: "Ưu đãi của tôi" đi qua
> `PRMRefreshTableView.startLoadingMore()` (table có sẵn refresh + infinite scroll), còn "Tìm ưu đãi"
> và màn này là `UITableView` thường nên tự gắn spinner vào `tableFooterView`. Android thì cả ba đều
> là một hàng cuối trong adapter.

### Tìm không ra kết quả

Dùng lại nguyên bộ mặt của màn "Tìm ưu đãi" (ảnh + "Không tìm thấy kết quả phù hợp" + "Khám phá
thêm các đề xuất phù hợp với bạn nhé.").

| | Android | iOS |
|---|---|---|
| Điều kiện | `state.keyword.isNotBlank() && !isLoading && isEmpty` (`ChoosePromotionFragment.observeData`) | `Display.showsNoResult`, cùng biểu thức trong `toDisplay()` |
| Dựng | `ctlNoResult` trong `prm_fragment_choose_promotion.xml` — chép từ `prm_fragment_search_my_promotion.xml` | `PromotionSearchNoResultView` (`PRMPromotionUI`) — **cùng class** với màn Tìm ưu đãi |
| Ảnh | `@drawable/prm_il_chua_co_giao_dich_mau` | asset `prm_ic_search_no_result` |
| Khi hiện | ẩn `rcvVoucher` | ẩn `promotionsTableView` |

> **List rỗng mà KHÔNG có từ khoá thì không hiện view này** — đó là "chưa có ưu đãi nào" chứ không
> phải "tìm không ra". Thanh đáy (nút "Áp dụng") vẫn hiện ở cả hai nền tảng; bên iOS view này được
> `insertSubview(_:belowSubview:)` dưới thanh đáy, cùng cách với shimmer.
>
> ⚠️ Ảnh hai bên **khác nhau** (nợ kỹ thuật có sẵn từ màn Tìm ưu đãi, không phải phát sinh ở đây).

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
| Truyền sang màn chọn | `PromotionSDK.openChoosePromotion` (widget tự gọi khi bấm) → `ChoosePromotionFragment.forEndowView` (internal) → `PreloadVouchers` | `PromotionSDKImpl.openChoosePromotion` → `ChoosePromotionBuilder.DataModel` |

Kết quả `findEligible` là `null` (API lỗi) → cả hai cờ về `true`, không mở đường gọi trang kế.
> - **Validate KHÔNG còn ở màn này.** Bấm "Áp dụng" chỉ **trả offers đang chọn** (`ApplySelectedOffers`);
>   validate & apply do **`EndowStore`** lo (xem [EndowView.md](./EndowView.md)).
## 1. Contract (MVI)

### 1.1. State — `ChoosePromotionState`

Ở lõi: `promotionLogic/…/presentation/choosepromotion/ChoosePromotionContract.kt`.

| Nhóm | Field |
|------|-------|
| Trạng thái tải | `hasLoadedInitial`, `isLoading`, `isRefreshing`, `isLoadingMore`, `isLoadingMoreOther`, `isEmpty` |
| Tab & tìm kiếm | `tabs` (`MyPromotionTab`), `selectedTabCode`, `keyword` |
| Phân trang "của tôi" | `myPage`, `mySize` (mặc định **20**), `myIsLastPage` |
| Phân trang "ưu đãi khác" | `otherPage`, `otherSize` (**20**), `otherIsLastPage` |
| Dữ liệu | `myOffers`, `otherOffers` — đều là `List<ChooseOffer>` |
| Chọn ưu đãi | `isMultiSelection`, `selectedIds`, `myExpanded` — **selection do store quản**, dùng chung 2 nền tảng |
| Lỗi | `errorCode` (một-lần) và `loadFailed` (**bền**) |
| Khoá nút "Áp dụng" | `isApplying` |
| Cảnh báo sắp hết hạn | `expireWarningDate` |

Hai field lỗi **không** thay thế nhau: `errorCode` là một-lần (hiện xong `dispatch(ConsumeError)`),
nên không dùng nó để quyết định "có hiện view rỗng không" — hiện được một nhịp rồi biến mất, màn
trắng trơn. `loadFailed` mới là cờ bền cho việc đó.

`isApplying` **chỉ để khoá nút**, không phải cờ loading của màn: shimmer và pull-to-refresh không đọc
nó. Việc validate chạy ở `EndowStore` của widget chứ không ở store này, nên native phải báo hai đầu
bằng `ApplyStarted` / `ApplyFinished` — **quên một nhánh kết thúc là nút chết luôn**.

### 1.2. Nguồn dữ liệu: `findEligible`

Màn này gọi `FindEligibleCampaignsUseCase` (`POST .../redemptions/eligible`), **không** phải
`searchVouchers`. Nhờ đó `otherOffers` (campaign công khai khách chưa nhận) mới có dữ liệu — trước
đây Android gọi `searchVouchers` nên section "Ưu đãi khác" luôn rỗng, lệch với iOS.

Tìm kiếm chạy **server-side** (parity Android ↔ iOS): `keyword` gửi kèm mỗi request `findEligible`
(v1.6 §7.3), server lọc cả `myOffers` lẫn `otherOffers`; đổi keyword → reload trang 0, keyword đi
kèm cả load-more. **Không** lọc client trong bộ nhớ — lọc client chỉ đúng trên trang đã tải và sẽ im
lặng trả sai khi danh sách dài hơn một trang (iOS trước đây lọc client qua `PRMOfferSearchFilter`, nay đã gỡ).

### 1.3. Intent — `ChoosePromotionIntent`
- `LoadInitial` — load lần đầu (cả hai nhóm, `section = null`).
- `Preload(myOffers, otherOffers, myIsLastPage, otherIsLastPage)` — **nhận data `PRMEndowView` đã tải** để khỏi gọi API lần hai. Mang `List<EligibleOffer>` (model lõi) chứ không phải model UI, giữ nguồn sự thật ở domain.
- `SeedOnce(preSelectedIds, myOffers, otherOffers, …)` — seed pre-select **+** preload, chỉ chạy **một lần cho cả vòng đời store**. Không có nó thì view dựng lại sẽ ghi đè `selectedIds` về bộ ban đầu (tick mới của user biến mất) và rewind `otherOffers` về trang đầu.
- `SetPreSelected(ids)` — seed riêng các voucher pre-select.
- `Refresh` — làm mới.
- `QueryChanged(keyword)` — gõ mỗi ký tự → debounce → reload server-side kèm `keyword`. Gõ trắng đã tự đi đúng đường `ClearKeyword`, native **không** cần rẽ nhánh `if keyword.isEmpty()`.
- `Search` — bấm Enter: chạy ngay, bỏ debounce.
- `ClearKeyword` — nút "X" xoá tường minh → nạp lại danh sách đầy đủ.
- `LoadMoreMyVouchers` / `LoadMoreOtherVouchers` — phân trang từng nhóm **độc lập** (`section = MY_OFFERS` / `OTHER_OFFERS`); response chỉ chứa nhóm được hỏi, nhóm kia là null.
- `ToggleSelection(id)` — chọn/bỏ chọn một ưu đãi.
- `SeeMoreMy` — bấm "Xem thêm/Thu gọn" nhóm của tôi.
- `ApplyStarted` / `ApplyFinished` — native báo hai đầu của lượt validate để khoá/mở khoá nút.
- `ConsumeError` — xoá `errorCode` sau khi đã hiển thị.

### 1.4. Lỗi và điều hướng — **không** có `Effect` riêng của màn

`ChoosePromotionEffect` / `ChoosePromotionAction` / `ChoosePromotionUiState` **đã bị bỏ**:

- **Lỗi** → `errorCode` trong state, rồi `PRMEffect.ShowError` dùng chung ở `PRMStore`.
- **Mở chi tiết** → tầng UI tự gọi `openPromotionDetail()`, vẫn gác bởi cờ `VOUCHER_DETAIL`.
- **Áp dụng** → validate chạy ở `EndowStore` của widget; màn này chỉ báo `ApplyStarted`/`ApplyFinished`.

---

## 2. Luồng apply (validate nằm ở `EndowStore`)

```
User bấm "Áp dụng" → Action.ValidateAndApply
  ViewModel: lọc offers theo store.selectedIds → sendEffect(ApplySelectedOffers(offers))
Fragment: onApplySelectedOffers(offers) { errorCode -> ... }
        → PRMEndowView.applySelectedOffers(offers, onSettled)
        → PRMEndowViewModel.validateAndApply(offers) { state -> onSettled(state.errorCode) }
        → EndowStore: isValidating=true → validateStackableDiscounts → isValidating=false
  errorCode != null → showErrorDialog(mapPromotionError(code)) — **popup**, **Ở LẠI** màn chọn (không áp)
  errorCode == null → goBack() (đóng màn; widget đã cập nhật qua state)
```

> **Màn chỉ đóng khi validate xong và không lỗi** — đối xứng `PromotionSDKImpl.openChoosePromotion`
> bên iOS (completion của `endowVM.validateAndApply`).

### 2.1. Chống spam nút "Áp dụng"

Lượt validate mất vài trăm ms tới vài giây; không khoá nút thì mỗi cú chạm là một lượt
`validateStackableDiscounts` nữa cho **cùng một bộ voucher**, rồi n callback cùng chạy về — n popup
lỗi, hoặc `goBack()`/`pop()` nhiều lần.

Cờ nằm ở state dùng chung: `ChoosePromotionState.isApplying`, bật/tắt bằng
`ChoosePromotionIntent.ApplyStarted` / `ApplyFinished`, và `canApply()` trả `false` khi nó bật —
nên **nút tự tắt ở cả hai nền tảng** mà không bên nào phải thêm rule riêng.

Vì sao native phải tự bắn hai intent (thay vì store tự biết): lượt validate **không chạy ở store
này** mà ở `EndowStore` của widget. Store màn Chọn chỉ giữ cờ.

| | Android | iOS |
|---|---|---|
| Bắn `ApplyStarted` | `ChoosePromotionFragment.onApplyClicked` | `ChoosePromotionViewController.didTapApplyButton` |
| Bắn `ApplyFinished` | callback `onSettled` của `PRMEndowView.applySelectedOffers` | closure `onSettled` truyền qua `vc.onApplyVoucher` |
| Chặn tức thời | `if (viewModel.state.value.isApplying) return` | `guard !viewModel.currentState.isApplying` |

Hai chỗ dễ làm hỏng:

1. **Quên `ApplyFinished` ở một nhánh** → nút khoá vĩnh viễn, user phải thoát màn. Mọi đường ra đều
   phải gọi: `applySelectedOffers` bên Android gọi `onSettled` cả khi widget đã detach;
   `PromotionSDKImpl` bên iOS gọi `onSettled()` cả trong nhánh `guard let self` hỏng.
2. **Chỉ dựa vào `isEnabled`/`alpha` của nút.** Cả hai nền tảng render nút từ state phát ra ở lượt
   sau (Android collect `StateFlow`, iOS `watchState`), nên vẫn hở một khung hình cho cú chạm thứ
   hai. Vì vậy handler đọc thẳng state đồng bộ (`state.value` / `currentState`) rồi mới chạy tiếp.

- Use case: `ValidateStackableDiscountsUseCase` (Domain) → repository → API, gọi từ `EndowStore`.
- `objectId` gửi lên là `EligibleOffer.id`: `voucherId` nếu khách đã sở hữu, ngược lại `campaignId`.
- Pre-select khi mở lại màn: **tất cả** `discountDetails` đang áp (kể cả item không còn hợp lệ) để user
  thấy và bỏ chọn được — khớp iOS.

---

## 3. Hiển thị 1 item (parity Android ↔ iOS)

Mọi **quyết định** đều lấy từ `ChooseOffer` do store dựng — native chỉ format, **không tự suy lại**:

| Quyết định | Nguồn | Android | iOS |
|---|---|---|---|
| Còn dùng được | `ChooseOffer.isUsable` = `usable` (server) **AND** chưa quá `expireDate` → `MyVoucherListItem.isEnabled` | mờ card + nhãn lý do, ẩn "Chi tiết" & checkbox | `isDisabled` (blur overlay) + `stateText` + `showsCheckbox` |
| **Hiện dải "Chưa đủ điều kiện áp dụng"** | `ChooseOffer.showsIneligibleWarning()` = `!source.usable` — **chỉ cờ của server** | `MyVoucherListItem.showsIneligibleWarning` → `ctlNotEnoughApplyVoucher` + `imgCircleNotEnoughApplyVoucher` | `MyPromotionCellViewModel.showsIneligibleWarning` → `warningView.isHidden` |
| Lý do không đủ điều kiện | `EligibleOffer.unmatchedRules.first`, dự phòng "Không đủ điều kiện" | `displayStatusLabel` → `txtExpired` | `stateText` |
| **Hết hạn** | `ChooseOffer.isExpired` (quá `expireDate`) — **kéo `isUsable` = false**, nhưng **không** kéo dải | nhãn "Đã hết hạn" (`prm_status_expired`) đè `displayStatusLabel` | `stateText: .expired` truyền từ `buildSections()` |
| Sắp hết hạn | `ChooseOffer.expiringInDays` (ngưỡng `expireWarningDate`, lùi về `ExpiryWarning.lastKnownDays` ở luồng preload) | "HSD còn X ngày" (màu cam `#F47527`), dự phòng "HSD: dd/MM/yyyy" (màu mặc định), không có HSD → "HSD: Không hết hạn" | như trên |
| Highlight từ khoá | `state.keyword.trim()` | `toHighlightedSpannable` | `PromotionCardModel.highlightKeyword` |

### 3.1. Dải "Chưa đủ điều kiện áp dụng" phải **luồn xuống dưới card**

> **Hết hạn KHÔNG hiện dải này.** Dải nói đúng một chuyện: *đơn hàng hiện tại* chưa thoả điều kiện
> của ưu đãi (chưa đủ giá trị tối thiểu, sai sản phẩm…) — sửa đơn là dùng được. Ưu đãi **hết hạn**
> thì sửa đơn kiểu gì cũng vô ích, treo "Chưa đủ điều kiện áp dụng" lên là sai nghĩa; ca đó chỉ mờ
> card + badge "Đã hết hạn".
>
> Bẫy: `ChooseOffer.isUsable` đã **gộp** hai lý do (`usable && !expired`), nên bám thẳng `isUsable`
> để bật dải là dính luôn ca hết hạn — cả hai nền tảng từng sai đúng kiểu đó. Dải hỏi **đúng cờ của
> server**: `ChooseOffer.showsIneligibleWarning()` = `!source.usable`, **một chỗ duy nhất**; Fragment
> và Cell chỉ đọc, không bên nào tự suy lại.
>
> Ô tick (ẩn cho **mọi** ca không dùng được, gồm cả hết hạn): Android `cbUseVoucher.visibility = INVISIBLE`, iOS `showsCheckbox: offer.isUsable`
> (`checkboxButton.isHidden`). **Ẩn chứ không bỏ chỗ** — checkbox không nằm trong `UIStackView` nên
> constraint "merchant kết thúc trước checkbox 8px" vẫn sống, khớp INVISIBLE bên Android. Để `true`
> cứng như trước thì ô tick vẫn lòi ra sau lớp phủ mờ, lệch Android.

Dải cao **30**, bị card đè **8**, chỉ lòi ra **22**; nội dung (icon + chữ) căn giữa theo phần lòi ra
chứ không theo cả dải. Không phải chi tiết trang trí: card là hình coupon — bo góc 12 + một khuyết
tròn ở cạnh đáy — nên dải mà chỉ nằm kề bên dưới thì mấy chỗ khuyết/bo đó **hở ra nền list**.

| | Android | iOS |
|---|---|---|
| Dải | `ctlNotEnoughApplyVoucher`, khai báo **trước** `ctlTop` (vẽ sau lưng card) + `layout_marginBottom="@dimen/_minus22sdp"` | `warningView`, `contentStackView.spacing = -8` + `sendSubviewToBack` |
| Bật/tắt | `voucher.showsIneligibleWarning` | `!viewModel.showsIneligibleWarning` |
| Khuyết đáy | khuyết là giả (ảnh tròn đè lên card): đổi `imgCircleBottom` xám → `imgCircleNotEnoughApplyVoucher` vàng | khuyết là thật (cắt trong path `CouponBackgroundView`) → màu vàng của dải tự lộ qua |
| Làm mờ card | `ctlTop.alpha = 0.6` | `blurOverlayView` **mask theo `CouponBackgroundView.cardPath(for:)`** |

Hai chỗ dễ làm hỏng lại:

1. **Đổi thứ tự z, không đổi thứ tự layout.** Bên iOS `sendSubviewToBack` chỉ động tới `subviews`,
   `arrangedSubviews` giữ nguyên nên stack vẫn xếp card → dải. Đảo `arrangedSubviews` là dải nhảy lên trên.
2. **Lớp phủ mờ phải cắt theo hình coupon.** `blurOverlayView` mà dùng `cornerRadius` (hình chữ nhật
   bo góc) thì trắng 60% tràn ra ngoài chỗ khuyết + chỗ bo, phủ bạc lên chính dải vàng nằm sau —
   nhìn y như bị hở. Vì vậy `CouponBackgroundView.cardPath(for:)` là hàm **thuần**, public, dựng path
   cho một khung bất kỳ để view khác mask theo.

Chiều cao cell: `warningView.isHidden` thì stack bỏ luôn khoảng cách âm, cell co đúng bằng card —
không cần constraint riêng cho ca "đủ điều kiện" (và cho ca **hết hạn**, cũng không có dải).

Khuyết đáy vàng (`imgCircleNotEnoughApplyVoucher` bên Android) bật/tắt **cùng cờ với dải**: không có
dải mà vẫn đổi khuyết sang vàng thì card hết hạn lòi một chấm vàng vô nghĩa ở cạnh dưới.

## 4. `serviceCode` đi vào request bằng đường nào

Spec `findEligible` (3.5.4 v19) **không có field `serviceCode`** ở bất kỳ cấp nào — body chỉ gồm
`customerInfo`, `orderInfo`, `filterOptions`, `scenario`, `sectionCode`, `keyword`, `pagination`.
Chiều dịch vụ đi qua **`orderInfo.items[].productId`**.

```
updateContext(serviceCode = "TKBAOVIET")
  → PromotionMutableContext.serviceCode
  → PromotionRequestContextProvider.getService()
  → ctx.eligibleOrderItems()        ← đổ vào productId của từng item còn trống
  → orderInfo.items[].productId
```

Dùng `ctx.eligibleOrderItems()`, **không** phải `getOrderItems()`, ở mọi chỗ dựng request findEligible
(`ChoosePromotionStore.buildRequest`, `EndowStore.loadInitial`). Hai chỗ lệch nhau là widget và màn
chọn hỏi server hai câu khác nhau rồi ra hai danh sách khác nhau.

Ba điều kèm theo:

- **Nguồn là `updateContext`, không phải `PromotionSDKConfig.serviceCode`.** Config là hằng số cả
  phiên; app có nhiều điểm mở màn chọn ưu đãi thì mỗi điểm `updateContext` trước khi mở là đủ, không
  phải re-init. Context được đọc lại ở **mỗi** request nên không có giá trị cũ kẹt lại.
- **`updateContext` ghi đè toàn bộ**, không merge: gọi `updateContext(serviceCode = …)` một mình là
  xoá sạch `orderId`/`orderValue`/`orderItems`. Luôn truyền đủ bộ.
- **Item tự khai `productId` thì giữ nguyên** — host biết dòng hàng của mình rõ hơn context cấp đơn.
- **`items[]` rỗng thì SDK không tự dựng item.** `skuSourceId` là required mà context cấp đơn không
  có SKU nào; rỗng → server chỉ chạy rule cấp đơn (spec §4.3).

`EndowStore.loadedOrderKey` gồm cả `getService()`: hai điểm vào cùng đơn nhưng khác dịch vụ là hai
danh sách khác nhau, thiếu nó thì widget giữ nguyên kết quả của dịch vụ trước.

---

> **Hai điểm từng sai, đừng làm lại:**
>
> 1. **Ngưỡng "sắp hết hạn" chỉ có ở response danh sách.** Luồng THƯỜNG của màn này là nhận preload
>    từ widget — không có response, nên `state.expireWarningDate` vẫn là default `null` và
>    `expiringInDays` ra `null` cho mọi item: dòng "HSD còn X ngày" **không bao giờ hiện**. Nay
>    `EndowStore.loadInitial` gọi `ExpiryWarning.remember(...)` khi nạp widget, và `toChooseOffer`
>    lùi về `ExpiryWarning.lastKnownDays`. Sửa một trong hai chỗ là chưa đủ.
> 2. **Hết hạn ≠ `usable = false`.** `EligibleOffer.usable` chỉ phản ánh `displayMode = "DISABLED"`
>    của server; ưu đãi quá hạn mà server chưa đánh cờ thì trước đây vẫn sáng và tick chọn được —
>    lệch "Ưu đãi của tôi", nơi `VoucherStatus.EXPIRED` cho `isUsable = false`. Nay `toChooseOffer`
>    tự đối chiếu `expireDate`. Mốc là `daysUntil < 0`, **không** `<= 0`: `daysUntil` làm tròn lên
>    nên 0 = "hết hạn trong hôm nay", vẫn dùng được và là ngày đầu của dải "sắp hết hạn".
>    `ChooseOffer.isExpired` mang **lý do** sang native để chọn nhãn "Đã hết hạn" thay vì câu
>    `unmatchedRules` (Android) / "Không đủ điều kiện" (iOS).

- **Không dùng `status` để suy trạng thái ở màn này**: `EligibleOffer.toMyVoucherListItem()` chỉ sinh
  `AVAILABLE` / `INELIGIBLE`, nên mọi nhánh theo `EXPIRED`/`REVOKED` sẽ **không bao giờ chạy**.
- **Ưu đãi không dùng được thì không chọn được** ở cả hai bên: Android chặn ở `root.setOnClickListener`,
  iOS chặn ở `ChoosePromotionItemCell.promotionCardViewDidTap` (tap thân card ở màn này = *chọn*, không
  phải xem chi tiết) — checkbox đã bị `PromotionCardView` chặn sẵn.
- Thanh "Đã chọn N voucher" chỉ hiện khi `isMultiSelection` **và** có item đang chọn — Android
  `updateApplyButtonState`, iOS `UiState.showsSelectedCount`. Không bên nào hiện số tiền giảm.
- Bấm "Áp dụng" khi **không chọn gì** → không làm gì (không đóng màn, không gỡ ưu đãi đang áp).
  Muốn gỡ thì bấm vào widget `PRMEndowView` (`clearApplied`).

## 5. Tái sử dụng

- Dùng lại `MyVoucherListItem`, `TabItem` từ feature **My Promotion** (không định nghĩa lại model voucher).
- `PreloadVouchers` là tối ưu quan trọng: dữ liệu được nạp ở `PRMEndowView` rồi truyền sang để **tránh double API call**.

---

## 6. Lưu ý khi sửa

- Hai danh sách (mine/other) có phân trang độc lập — giữ tách biệt `page` và `otherPage`.
- Mọi thay đổi cấu trúc request stackable discount → đồng bộ với `data/dto/stackablediscount/` và `NetworkingGuide.md`.
- Liên quan: [EndowView.md](./EndowView.md), [MyPromotion.md](./MyPromotion.md).
