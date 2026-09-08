# Feature: My Promotion

Màn hình hiển thị **danh sách voucher của khách hàng** với tab, phân trang và tìm kiếm.
Đây là màn hình UI mode chính được host mở qua `PromotionSDK` (`MyPromotionFragment`).

- **Package:** `ui/feature/promotion/mypromotion`
- **Thành phần:** `MyPromotionFragment`, `MyPromotionViewModel`, `MyPromotionContract`, `adapter/`

## Mục lục

<!-- toc -->
- [1. Contract (MVI)](#1-contract-mvi)
  - [1.1. State — `MyPromotionUiState`](#11-state--mypromotionuistate)
  - [1.2. Action — `MyPromotionAction`](#12-action--mypromotionaction)
  - [1.3. Effect — `MyPromotionEffect`](#13-effect--mypromotioneffect)
- [2. Luồng dữ liệu](#2-luồng-dữ-liệu)
- [3. UI item & mapping](#3-ui-item--mapping)
- [4. API backend](#4-api-backend)
- [5. 4b. Cache & prefetch tab (`MyPromotionStore`)](#5-4b-cache--prefetch-tab-mypromotionstore)
- [6. Lưu ý khi sửa](#6-lưu-ý-khi-sửa)
<!-- /toc -->

---

## 1. Contract (dùng chung 2 nền tảng)

Contract nằm ở **lõi**, không ở tầng UI:
`promotionLogic/…/presentation/mypromotion/MyPromotionContract.kt`. Android và iOS đọc cùng một
`State` và phát cùng một `Intent`.

### 1.1. State — `MyPromotionState`
| Field | Ý nghĩa |
|-------|---------|
| `hasLoadedInitial` | Đã load lần đầu hay chưa (tránh load lại) |
| `isLoading` / `isRefreshing` / `isRefreshingTab` / `isLoadingMore` | Các trạng thái tải |
| `isEmpty` | Danh sách rỗng |
| `tabs` / `selectedTabCode` | Danh sách tab (`MyPromotionTab`) và tab đang chọn |
| `keyword` | Từ khoá tìm kiếm hiện tại |
| `page` / `size` / `isLastPage` | Trạng thái phân trang |
| `vouchers` | Danh sách `MyPromotionVoucher` để render |
| `errorCode` | Mã lỗi **một-lần**; native hiển thị rồi `dispatch(ConsumeError)` để xoá |

### 1.2. Intent — `MyPromotionIntent`
- `LoadInitialIfNeeded` — load lần đầu nếu chưa có.
- `Refresh` — kéo làm mới.
- `SelectTab(tabCode)` — đổi tab.
- `Search(keyword)` — lọc theo từ khoá.
- `LoadMore` — tải trang kế tiếp.
- `ConsumeError` — xoá `errorCode` sau khi đã hiển thị.

### 1.3. Lỗi và điều hướng — **không** có `Effect` riêng của màn

`MyPromotionEffect` / `MyPromotionAction` / `MyPromotionUiState` **đã bị bỏ**. Thay vào đó:

- **Lỗi** đi qua `errorCode` trong state; `PRMStore.effects` suy ra `PRMEffect.ShowError(errorCode)`
  dùng chung mọi màn (`presentation/base/PRMStore.kt`).
- **Điều hướng** do tầng UI tự làm ngay tại chỗ bấm — store không phát sự kiện mở màn.

---

## 2. Luồng dữ liệu

```
Fragment/VC → dispatch(LoadInitialIfNeeded)
  MyPromotionStore: state = state.copy(isLoading = true)
                  → SearchCustomerVouchersUseCase(request)   // :promotionLogic — Domain → Data → Ktor
                  → map sang MyPromotionVoucher / MyPromotionTab
                  → state = state.copy(vouchers = …, tabs = …, isLoading = false)
  Lỗi → state.copy(isLoading = false, errorCode = …)
Android: collect viewModel.state → render; collect viewModel.effects → popup lỗi
iOS:     onState → render;  onEffect → popup lỗi  (rồi dispatch(ConsumeError))
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
- **Request params:** `keyword`, `serviceCode`, `tab`, `page` (0-based), `size`. Định danh khách lấy từ JWT `sub`, **không** truyền lên.
- **Response:** Spring Page phẳng — `content[]` + `number`, `size`, `last`, `totalElements`; kèm `tabs[]` động (mỗi tab có `code`, `label`, `count`, `order`, `default`).
- **Không còn:** `myVouchers`/`otherVouchers`/`sectionCode` (đã bỏ từ v1.3).
- Search keyword: free search, không giới hạn độ dài tối thiểu; rỗng/whitespace = không filter.

## 5. 4b. Cache & prefetch tab (`MyPromotionStore`)

Đổi tab **không gọi API** nếu cache còn tươi. Cơ chế nằm trọn ở store dùng chung nên Android và iOS
hưởng như nhau, native không phải sửa gì.

| Thành phần | Vai trò |
|---|---|
| `tabCaches: Map<tabCode, TabCache>` | RAM, sống cùng store (= vòng đời màn). Giữ `vouchers` + `page` + `isLastPage` + `fetchedAt`. **Rỗng suốt** nếu server không trả `tabs[]` — không có tab thì không có gì để cache |
| `TAB_CACHE_TTL_MS` | **60s**. Trong TTL → dùng cache thẳng; hết TTL → hiện cache ngay rồi refresh ngầm |
| `prefetchOtherTabs()` | Sau mỗi lần load `reset = true`, nạp trước trang đầu của **các tab chưa xem** |
| `markAllCachesStale()` | Kéo làm mới ép mọi tab thành ôi (giữ dữ liệu, chỉ hạ cờ tươi) |

**Luồng:** vào màn → 1 request (tab active) → response trả `tabs[]` → prefetch song song N-1 tab còn
lại → user bấm tab nào cũng có sẵn.

⚠️ **Prefetch KHÔNG được đi qua `loadVouchers()`.** Hàm đó lọc response bằng `shouldApplyResponse()`,
mà điều kiện "tab đã đổi" sẽ vứt sạch response của tab đang không được chọn — tức đúng mọi response
của prefetch. Đường prefetch riêng vì thế không đụng `latestTabRequestId`, không set cờ loading, chỉ
ghi `tabCaches`, và **nuốt lỗi im lặng** (tab user chưa bấm vào thì không được bắn toast).

**Đánh đổi:** mở màn bắn N request thay vì 1 (N = số tab, server trả động). User mở rồi thoát ngay là
phí N-1; đổi lại đổi tab thành miễn phí.

## 6. Lưu ý khi sửa

- Đổi tham số tìm kiếm/phân trang → đồng bộ qua use case `SearchCustomerVouchersUseCase`, **không** gọi thẳng repository.
- Thêm trạng thái UI → thêm field vào `MyPromotionState` (immutable, có default) — ở **lõi**, để cả
  hai nền tảng cùng có.
- Click voucher → tầng UI tự mở màn chi tiết; store **không** phát sự kiện điều hướng.
- Model hiển thị riêng của Android (`MyVoucherListItem`, `TabItem` ở `*UiModels.kt`) vẫn giữ — đó là
  cách RecyclerView muốn nhìn dữ liệu, Fragment map tại chỗ dùng.
- **Tab user vừa bấm luôn thắng.** Store lấy tab active theo thứ tự: tab client vừa yêu cầu
  (`requestTabCode`) → `SearchCustomerVouchersResult.resolveActiveTab()` → tab đầu danh sách.
  `resolveActiveTab` (rule dùng chung Android & iOS: `selectedTab` → `defaultTab` → `requestedTab` →
  tab đầu theo `order`) chỉ trả lời câu **"đáp xuống tab nào"**, nên store chỉ hỏi nó ở lần load đầu
  và **không** truyền `requestedTab` vào. Trước đây store truyền vào, tức để `selectedTab` của
  response đè lên tab user vừa bấm: server echo lệch (`tab=used` mà trả `selectedTab=all`) là tab
  sáng nhảy về "Tất cả" trong khi list hiện ra lại là của "Đã dùng". UI chỉ đọc, không hardcode "all".
- **Đổi tab mà API hỏng → phải về empty, KHÔNG giữ list tab cũ.** `onTabSelected` cố tình giữ tạm
  danh sách tab trước trong lúc load (`keepCurrentListWhileLoading`) cho đỡ nháy, nên nhánh
  `onFailure` phân biệt bằng **chính cờ đó**:
  - `keepCurrentListWhileLoading` (vừa đổi sang tab chưa có cache) → xoá `vouchers`, `isEmpty = true`,
    reset `page`/`isLastPage`;
  - còn lại (kéo làm mới / nạp lại chính tab đang đứng) → giữ list cũ, chỉ set `errorCode`.

  ⚠️ **Đừng quay lại dùng "tab này có cache không" làm điều kiện.** Hai chuyện đó không đồng nghĩa:
  server không trả `tabs[]` thì `tabCaches` rỗng suốt vòng đời màn → mọi lần `reset` hỏng đều rơi
  vào nhánh xoá, tức kéo-làm-mới ngay trên tab đang đứng mà rớt mạng là **danh sách bị xoá trắng**.

  Sửa nhánh này nhớ chạy `MyPromotionStoreTest.selectTab_apiFails_clearsList_insteadOfKeepingPreviousTab`,
  `refreshTab_apiFails_keepsCachedListOfSameTab` và
  `StoreEdgeBranchTest.failureAfterCacheExists_keepsListAndReportsError`.
- **Badge trạng thái (`txtExpired` / `stateText`)** — ĐÃ DÙNG và HẾT HẠN **luôn dùng chuỗi của SDK**
  ("Đã sử dụng" / "Đã hết hạn"), **không** lấy `displayStatusLabel`: field đó map thẳng
  `metadata.disabledReason` của server, tức mã enum (`REDEEMED`, `EXPIRED`) chứ không phải chuỗi
  hiển thị — ưu tiên nó thì badge lòi chữ tiếng Anh. Nhánh không đủ điều kiện mới giữ nhãn server
  (đã là câu đọc được). Rule này áp cả 2 nền tảng: `MyPromotionAdapter` ↔ `MyPromotionCellViewModel`.
- **Dòng HSD** — thứ tự ưu tiên, **giống hệt 2 nền tảng** (list, search, choose, detail):
  1. `expiringInDays != null` → "HSD còn X ngày", màu cam `#F47527` (`tokenCarrotOrange100`);
  2. parse được ngày → "HSD: dd/MM/yyyy", màu mặc định `tokenDark60`;
  3. **API không trả HSD (null/rỗng)** → "HSD: Không hết hạn" (`prm_expiry_never` /
     `PromotionUIStrings.expiryNever`);
  4. có chuỗi ngày nhưng **parse hỏng** → ẩn hẳn dòng (không dám khẳng định vô hạn).

  Phân biệt (3) và (4) là chủ đích: cả hai đều cho `date == nil`, nên iOS mang thêm cờ
  `MyPromotionCellViewModel.neverExpires` (`PRMPromotionDate.isMissing`), Android kiểm
  `expirationDate.isBlank()` trên chuỗi thô. Android phải set màu ở **mọi nhánh** vì ViewHolder tái sử dụng.

  Rule này áp **cả màn Chi tiết** (TLNV MOB_002 2.4 refer MOB_001 #4). Ngưỡng cảnh báo chỉ có ở
  response danh sách nên `ExpiryWarning` (promotionLogic) nhớ lại giá trị gần nhất cho store chi tiết
  dùng; bỏ được nhánh nhớ này khi BE trả `expireWarningDate` ở API detail.
- **Tab "(số lượng)"** — chỉ hiện khi **đã load xong** danh sách (TLNV MOB_001 control #3):
  `submitTabs(..., showCount = !isLoading)` / `renderTabs(..., showCount: !state.isLoading)`.
- **Hàng HSD trong card (iOS)** — `PromotionCardView.footerStackView` =
  `[dateLabel, spacerView, actionButton, stateContainerView]`: nút "Sử dụng + icon" và badge trạng
  thái **ghim cứng** bên phải (hugging + resistance `.required`), HSD dùng `PRMMarqueeLabel` với
  resistance `.defaultLow` nên bị bóp trước và **tự chạy chữ** khi tràn; khe giữa hai bên tối thiểu
  **5px** (`spacerView` width ≥ 5, stack `spacing = 0`).
  ⚠️ Nút và badge phải là **arranged subview riêng**, đừng bọc chung một container: chúng loại trừ
  nhau nhưng constraint của view thường vẫn sống khi `isHidden`, nên bọc chung sẽ ép
  `button.width == stateLabel.width + 16` → badge ẩn (rỗng) kéo nút còn 16px, chữ "Sử dụng" ra "…".
  Stack thì tự thu hồi chỗ của arranged subview bị ẩn nên không dính.
  ⚠️ **Lệch Android có chủ đích:** `tvEndDate` (`prm_item_promotion.xml`) chỉ xuống dòng, chưa marquee.
- Liên quan: [ChoosePromotion.md](./ChoosePromotion.md) (dùng lại `MyVoucherListItem`, `TabItem`).
