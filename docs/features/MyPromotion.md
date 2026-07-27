# Feature: My Promotion

Màn hình hiển thị **danh sách voucher của khách hàng** với tab, phân trang và tìm kiếm.
Đây là màn hình UI mode chính được host mở qua `PromotionSDK` (`MyPromotionFragment`).

- **Package:** `ui/feature/promotion/mypromotion`
- **Thành phần:** `MyPromotionFragment`, `MyPromotionViewModel`, `MyPromotionContract`, `adapter/`

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
- **Request params:** `keyword`, `serviceCode`, `tab`, `page` (0-based), `size`. Định danh khách lấy từ JWT `sub`, **không** truyền lên.
- **Response:** Spring Page phẳng — `content[]` + `number`, `size`, `last`, `totalElements`; kèm `tabs[]` động (mỗi tab có `code`, `label`, `count`, `order`, `default`).
- **Không còn:** `myVouchers`/`otherVouchers`/`sectionCode` (đã bỏ từ v1.3).
- Search keyword: free search, không giới hạn độ dài tối thiểu; rỗng/whitespace = không filter.

## 4b. Cache & prefetch tab (`MyPromotionStore`)

Đổi tab **không gọi API** nếu cache còn tươi. Cơ chế nằm trọn ở store dùng chung nên Android và iOS
hưởng như nhau, native không phải sửa gì.

| Thành phần | Vai trò |
|---|---|
| `tabCaches: Map<tabCode, TabCache>` | RAM, sống cùng store (= vòng đời màn). Giữ `vouchers` + `page` + `isLastPage` + `fetchedAt` |
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

## 5. Lưu ý khi sửa

- Đổi tham số tìm kiếm/phân trang → đồng bộ qua use case `SearchCustomerVouchersUseCase`, **không** gọi thẳng repository.
- Thêm trạng thái UI → thêm field vào `MyPromotionUiState` (immutable, có default).
- Click voucher → phát `Effect.OpenVoucherDetail`, không tự điều hướng trong ViewModel.
- Tab active xác định qua `SearchCustomerVouchersResult.resolveActiveTab(requestedTab)` — rule **dùng chung Android & iOS** (`selectedTab` → `defaultTab` → tab client yêu cầu → tab đầu theo `order`); UI chỉ đọc, không hardcode "all".
- **Đổi tab mà API hỏng → phải về empty, KHÔNG giữ list tab cũ.** `onTabSelected` cố tình giữ tạm
  danh sách tab trước trong lúc load (`keepCurrentListWhileLoading`) cho đỡ nháy, nên nhánh
  `onFailure` phải phân biệt theo cache của **tab được yêu cầu**:
  - có cache (refresh lại chính tab đó) → giữ list cũ, chỉ set `errorCode`;
  - không có cache (vừa đổi sang tab mới) → xoá `vouchers`, `isEmpty = true`, reset `page`/`isLastPage`.

  Sửa nhánh này nhớ chạy `MyPromotionStoreTest.selectTab_apiFails_clearsList_insteadOfKeepingPreviousTab`
  và `refreshTab_apiFails_keepsCachedListOfSameTab` — hai test khoá đúng cặp hành vi đối lập này.
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
