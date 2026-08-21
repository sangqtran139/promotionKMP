# Feature: Promotion Detail

Màn hình **chi tiết một voucher** — hiển thị thông tin, trạng thái và nút hành động (áp dụng…).
Có thêm tab nội dung qua `PrmContentDetailEndowFragment` + `PrmCustomFragmentPagerAdapter`.

- **Package:** `ui/feature/promotion/promotiondetail`
- **Thành phần:** `PromotionDetailFragment`, `PromotionDetailViewModel`, `PromotionDetailContract`, `PrmContentDetailEndowFragment`, `adapter/PrmCustomFragmentPagerAdapter`

> **KHÔNG seed từ ngoài — quy tắc chốt, áp dụng cả Android & iOS (2026-07-23).**
> Màn chi tiết **chỉ hiển thị khi `getCustomerVoucherDetail` trả về**. Navigation chỉ mang `voucherId`;
> dữ liệu từ màn danh sách (tên, logo, HSD, trạng thái) **không** được dùng để dựng card/nút — tránh
> hai nguồn sự thật lệch nhau (list có thể cũ hơn detail).
> Skeleton phải khớp từng con số với UI thật và khớp giữa 2 nền tảng — bảng số chuẩn ở
> [android/UIGuide.md §12](../android/UIGuide.md).
>
> Trong lúc chờ: shimmer toàn màn (Android `showDetailLoading`, iOS `PromotionDetailShimmerView` +
> `Display.empty`). Vì vậy `PromotionDetailStore` **không có** intent seed.

---

## 1. Contract

Màn này **không còn `UiState`/`Action`/`Effect` riêng** — nó đọc thẳng `PromotionDetailState` và phát
thẳng `PromotionDetailIntent` của `PromotionDetailStore` (dùng chung với iOS). Xem
[android/UIGuide.md §4](../android/UIGuide.md).

### State — `PromotionDetailState` (`promotionLogic`)
| Field | Ý nghĩa |
|-------|---------|
| `isLoading` | Đang tải chi tiết |
| `detail` | `VoucherDetail?` (domain model) |
| `status` | `VoucherStatus` (UNKNOWN/…)|
| `actionVisible` | Có hiện nút hành động không |
| `actionEnabled` | Nút có cho bấm không |
| `actionLabel` | Nhãn nút từ server (`VoucherDetail.displayStatusLabel`) — ⚠️ **native đang không đọc**, nút dùng chuỗi cứng "Sử dụng ngay" |

### Intent — `PromotionDetailIntent` (`promotionLogic`)
- `LoadDetail(voucherId)` — tải chi tiết voucher.
- `ConsumeError` — xoá lỗi sau khi đã báo (Fragment không gọi tay: `viewModel.errors` tự lo).

### Phần thuần Android
- Lỗi: thu effect nhưng **không hiện gì** (`is PRMEffect.ShowError -> Unit`) — màn đã có shimmer/empty-view nói thay. SDK đã bỏ toast; cần báo thì dùng `showErrorDialog(...)`.
- Bottom sheet "Chọn dịch vụ": `viewModel.serviceOptions()` trả thẳng list, Fragment tự mở sheet —
  không đi vòng qua effect.

---

## 2. Luồng dữ liệu

```
Fragment(voucherId) → handleAction(LoadDetail(voucherId))
  ViewModel: setState(isLoading=true)
           → launch { getCustomerVoucherDetail(...) }   // GetCustomerVoucherDetailUseCase
           → DTO map toVoucherDetail() → domain VoucherDetail
           → tính status/actionVisible/actionEnabled/actionLabel theo VoucherStatus
           → setState(detail=..., isLoading=false)
  Lỗi → sendEffect(ShowError)
Fragment: render thông tin + cấu hình nút theo state
```

- Use case: `GetCustomerVoucherDetailUseCase`.
- Hiện/ẩn + cho bấm (`actionVisible`, `actionEnabled`) suy ra từ `status` **ở store** (`displayState().isUsable`),
  không hardcode ở Fragment/VC.
- **Nhãn nút màn Chi tiết: chuỗi cứng "Sử dụng ngay"** (`R.string.prm_use_now` / `PromotionUIStrings.useNow`),
  **không** đọc `actionLabel`. Store vẫn gán `actionLabel = VoucherDetail.displayStatusLabel` để bật lại
  nhãn server không phải sửa store, nhưng **hai màn native đang bỏ qua** — BE trả "Sử dụng" cho mọi voucher,
  còn màn này muốn "Sử dụng ngay". Card ở màn danh sách thì **vẫn theo nhãn server**.

  > **Nguồn nhãn:** BE trả `voucher.displayStatusLabel` ("Sử dụng") **trong object `voucher`** ở cả
  > API list lẫn detail. Mapper: `displayStatusLabel = voucher.displayStatusLabel ?: metadata.disabledReason`
  > (`VoucherMapper.displayLabelOrReason`). `metadata.disabledReason` là **mã enum**
  > (`EXPIRED`/`REDEEMED`/`SERVICE_NOT_APPLICABLE`) và chỉ có khi `usable="false"`.
  >
  > **Ngoại lệ:** badge trạng thái ĐÃ DÙNG / HẾT HẠN vẫn dùng chuỗi của SDK ("Đã sử dụng" / "Đã hết hạn"),
  > **không** lấy nhãn server — vì BE đang trả "Sử dụng" cho mọi voucher trong danh sách, tin nhãn đó thì
  > voucher hết hạn cũng hiện "Sử dụng". Xem `MyPromotionAdapter` / `MyPromotionCellViewModel`.
  >
  > `voucher.status` ("ACTIVE") cũng đã khai trong DTO nhưng **chưa dùng**: trạng thái vẫn suy từ
  > `metadata.usable`. Đổi nguồn là đụng rule fail-closed nên tách thành quyết định riêng.

---

## 3. Tab nội dung

- `PrmContentDetailEndowFragment` hiển thị nội dung mô tả/điều khoản.
- `PrmCustomFragmentPagerAdapter` quản lý các tab nội dung.
- Nguồn dữ liệu 2 tab (`VoucherMapper.toVoucherDetail`, `promotionLogic/.../dto/voucher/VoucherMapper.kt`):
  - **"Thông tin chi tiết"** ← `VoucherDetail.description` ← `voucher.content`.
  - **"Hướng dẫn sử dụng"** ← `VoucherDetail.guideline` ← `voucher.guideline`.
  Test khoá: `VoucherDetailFieldBranchTest.detail_fullVoucher_*` / `detail_guidelineMissing_isNull`.

**Vòng đời pager — ba ràng buộc, đừng phá (sửa 2026-08-07):**

| Ràng buộc | Vì sao |
|---|---|
| Adapter nhận **`Fragment`** (`PrmCustomFragmentPagerAdapter(this)`), không phải `requireActivity()` | Truyền Activity thì tab rơi vào FM của Activity host: tab là `PRMBaseFragment` nên tự đăng ký lớp chặn back của host dù không phải "màn", và còn sót lại sau khi màn chi tiết đóng |
| Adapter dựng **một lần / một view** (`setupDetailTabs`), nội dung đổi qua `setTabs` | Gán adapter mới vào cùng `ViewPager2` + cùng `childFragmentManager` thì adapter mới không biết state adapter cũ đã lưu |
| `createFragment()` **dựng instance mới**, adapter chỉ giữ `PrmPagerTab` (nhãn + `contentKey` + lambda dựng) | Giữ sẵn `List<Fragment>` rồi trả lại chính nó là sai hợp đồng `FragmentStateAdapter`: sau khi khôi phục từ saved state, pager gọi `createFragment` cho vị trí đã có fragment → `IllegalStateException: Fragment already added` |

`contentKey` chính là HTML của tab: đổi nội dung thì `getItemId` đổi theo nên pager tự dựng lại tab;
nội dung y nguyên thì tab được dùng lại, không nạp lại WebView.

---

## 4. API backend

- **Endpoint:** `GET /promotion/promotion-vtm-bff/api/v1/vtm/customer-vouchers/{voucherId}`
- **Query params:** `service` (tuỳ chọn — dịch vụ đang thanh toán). Định danh khách lấy từ JWT `sub`, **không** truyền lên.
- **Response fields mới (v1.1):** `campaignId`, `campaignType`, `campaignStatus`, `applicableProducts[]` (gồm `productId`, `sku`, `name`, `image`, `type=INCLUDED|EXCLUDED`).

> **`applicableProducts` — bug đã sửa.** Sau khi làm phẳng DTO ở v1.3, field này **không được khai**
> trong `CustomerVoucherDetail` / `VoucherListItem`, mà Json bật `ignoreUnknownKeys = true` nên nó bị
> nuốt **im lặng**: mapper trả `emptyList()` ⇒ `servicesForApplicableProducts` giao với tập rỗng ⇒
> bottom sheet "Chọn dịch vụ" **luôn trống**, dù host khai đủ `availableServices`. Không có log, không
> có lỗi parse — rất dễ đổ nhầm cho config phía host.
>
> Nay DTO khai ở **cả hai vị trí** (ngang hàng `metadata`, và lồng trong `voucher`), mapper lấy bên nào
> có dữ liệu: `applicableProducts.ifEmpty { voucher.applicableProducts }`. Khi chốt được BE trả ở đâu
> thì bỏ nhánh dự phòng. Test khoá: `VoucherMappingBranchTest.detail_applicableProducts_*`.
>
> Mapper **giữ nguyên `type = EXCLUDED`** — `servicesForApplicableProducts` hiện chỉ so `productId`,
> chưa đọc `type`, nên SKU bị loại trừ vẫn hiện như dịch vụ hợp lệ. Cần lọc thì sửa ở tầng lọc.

### Nút hành động + điều hướng (TLNV MOB_002 control #5)

> **Lệch nhãn/hành vi (cố ý theo yêu cầu sau này):** cột "Nhãn nút" dưới đây đã bị **đảo ngược** so
> với bản gốc TLNV MOB_002 control #5 — nhãn nay chỉ đổi theo `returnVoucherOnApply` (bật →
> "Sử dụng ngay", tắt → "Áp dụng"), còn cột "Bấm thì" (hành vi thật khi bấm, ở `onActionClick`)
> **không đổi**. Kết quả: nhánh `returnVoucherOnApply = true` hiện nhãn "Sử dụng ngay" nhưng bấm vào
> vẫn **áp dụng voucher và đóng màn ngay**, không mở bottomsheet chọn dịch vụ — nhãn không còn mô tả
> đúng hành vi. Xem code: `PromotionDetailFragment.bindDetailContent` (nhãn) /
> `PromotionDetailFragment.onActionClick` (hành vi).

| Vào màn từ | Nhãn nút | Bấm thì |
|---|---|---|
| "Ưu đãi của tôi" / Tìm kiếm | "Áp dụng" (`prm_apply`) | Chọn dịch vụ (xem dưới) |
| Luồng thanh toán ("Chọn ưu đãi" → Chi tiết) | "Sử dụng ngay" (`prm_use_now`) | Quay lại "Chọn ưu đãi", voucher **đã tick** |
| **Host gọi thẳng** `openPromotionDetail(..., returnVoucherOnApply = true)` — mặc định | "Sử dụng ngay" | Trả **object `PromotionVoucherDetail`** về `onVoucherApplied` của chính lời gọi đó, rồi đóng màn |
| Host gọi thẳng với `returnVoucherOnApply = false` | "Áp dụng" | Chọn dịch vụ, như hàng đầu |

Ai đóng màn sau khi "Áp dụng" do `hostHandlesDismiss` quyết (mặc định `false` = SDK tự pop). Bật `true`
thì SDK báo xong **để nguyên màn**, host tự đóng — Android `PromotionDetailFragment.onActionClick`
bỏ `goBack()`, iOS `PromotionDetailViewController.didTapApplyButton` bỏ `routeToParent()`.
Callback chạy **trước** bước pop ở cả hai bên, chính là điều kiện để chế độ này dùng được.

Cờ đi kèm navigation là **một boolean duy nhất, không có enum ở nền tảng nào**:
`returnVoucherOnApply` — Android là arg của `newInstance` (lưu vào `arguments`), iOS là field của
`PromotionDetailBuilder.DataModel`. Đặt tên theo *hành vi* chứ không theo *màn gọi*, vì nơi mở màn có
thể là "Chọn ưu đãi" nội bộ **hoặc** màn bất kỳ của host; tên kiểu `checkout` sẽ sai nghĩa ngay.
Xem [PublicApi.md](../common/PublicApi.md).

Đường về khi "Áp dụng" có **hai người nhận**, tuỳ ai mở màn:

| Ai mở | Android | iOS |
|---|---|---|
| `ChoosePromotionFragment` (nội bộ) | `setFragmentResult(RESULT_APPLY_VOUCHER)` → `listenApplyFromDetail` | closure `onVoucherApplied` → `ChoosePromotionViewController.selectVoucherFromDetail` |
| Host qua `openPromotionDetail` | `PromotionDetailFragment.onVoucherApplied` | cùng closure `onVoucherApplied`, do `PromotionSDKImpl` truyền vào |

Android bắn **cả hai** trong `onActionClick` (mỗi lần mở chỉ một bên có người nghe); iOS dùng chung
một closure nên không có chuyện đó.

Kiểu dữ liệu closure: tầng UI nội bộ chuyền **`VoucherDetail` domain** (`ChoosePromotionRouter` chỉ
lấy `.voucherId`), rồi `PromotionSDK` (Android) / `PromotionSDKImpl` (iOS) map sang DTO public
`PromotionVoucherDetail` ngay tại ranh giới. Bắt buộc phải vậy ở iOS: `PRMPromotionUI` nằm **dưới**
`Entry/API` trong chiều phụ thuộc nên builder không thể khai type public. Android làm y hệt cho đối xứng. Luồng nội bộ dùng `SetPreSelected` (không phải toggle) để kết quả
luôn là "đúng voucher này được chọn".

Khi bấm "Sử dụng ngay", số dịch vụ khả dụng quyết định hành vi:

| Số dịch vụ | Hành vi |
|---|---|
| 1 | **Chọn thẳng, không mở sheet** — effect `ServiceChosen` / `.serviceChosen` |
| 0 hoặc >1 | Mở bottom sheet (0 → sheet hiện "Không có dịch vụ thoả mãn") |

Nút bị **ẩn** khi voucher REDEEMED / EXPIRED / REVOKED / SUSPENDED (`actionVisible` từ store).

### Bottom sheet "Chọn dịch vụ" — bố cục & chiều cao

**Lưới 3 cột, cuộn dọc, sheet cao tối đa 60% màn hình.** Dịch vụ dư ra thì xuống hàng; vượt trần thì
cuộn trong lưới.

| | Android | iOS |
|---|---|---|
| Bố cục | `GridLayoutManager(context, SPAN_COUNT = 3)` | `flowLayout.scrollDirection = .vertical`, `spanCount = 3` |
| Bề rộng ô | do `GridLayoutManager` tự chia — item layout khai `match_parent` là vừa khít, **không** set bằng code | `sizeForItemAt` chia `floor((bounds - inset) / 3)` |
| Chiều cao | nội dung thật; trần `behavior.maxHeight = 60% heightPixels` | `min(header + rowCount * itemHeight + inset, screen * 0.6)` |
| Cuộn | RecyclerView tự cuộn khi bị AT_MOST cắt (`clipToPadding=false` cho padding thành content inset) | `collectionView` cuộn dọc, `contentInset` 16/24 |
| Trạng thái mở | `skipCollapsed = true` + `STATE_EXPANDED` | luôn mở đúng chiều cao đã tính |

> **Đừng bỏ `skipCollapsed`/`maxHeight` bên Android.** Mặc định `BottomSheetDialogFragment` mở ở
> `STATE_COLLAPSED` với peek auto (~9/16 bề ngang, cỡ 230dp) → **từ 4 dịch vụ (2 hàng) là hàng dưới đã
> khuất**; và không có trần thì content cao hơn màn hình sẽ tràn xuống dưới, kéo trong lưới chỉ làm
> **đóng sheet** chứ không cuộn → item cuối không cách nào chạm tới.

> **Lệch TLNV.** MOB_002 item #6 ghi "danh sách hiển thị trên 1 dòng, tối đa 3 dịch vụ, vuốt trái/phải
> để xem thêm". Bản hiện tại đổi sang **lưới cuộn dọc** theo yêu cầu sản phẩm (2026-08-04): nhiều dịch
> vụ thì vuốt ngang khó thấy hết. Cần chốt lại với BA nếu TLNV không được cập nhật theo.

### Back khi host dùng Navigation Component (bug đã sửa 2026-08-07)

**Triệu chứng:** host mở màn chi tiết bằng `PromotionSDK.openPromotionDetail(...)` từ một màn nằm
trong `NavHostFragment`; bấm back thì **màn chi tiết ở lại**, còn màn host phía dưới lùi một nấc.

**Gốc** ở `PRMBaseFragment.closeTopSdkScreen()` (trước tên là `popOwnBackStack`):
`FragmentManager.popBackStackImmediate()` bản **không tham số** không pop stack của chính nó trước —
nó uỷ quyền xuống primary navigation fragment:

```java
if (mPrimaryNav != null && id < 0 && name == null) {
    if (mPrimaryNav.getChildFragmentManager().popBackStackImmediate()) return true;
}
```

`NavHostFragment` tự đặt mình làm primary nav fragment, nên lời gọi đó pop **destination của host**
rồi trả `true`. SDK tưởng đã đóng màn của mình nên nuốt luôn phím back.

**Sửa:** pop đích danh entry trên cùng — `popBackStackImmediate(topEntryId, POP_BACK_STACK_INCLUSIVE)`.
Truyền `id` thì điều kiện uỷ quyền (`id < 0 && name == null`) không còn đúng. Thiếu cờ
`POP_BACK_STACK_INCLUSIVE` thì nó chỉ pop những entry nằm *trên* entry đó, tức không pop gì.

Kiểm chứng: mở màn chi tiết từ một màn trong NavHost rồi bấm back — phải đóng đúng màn chi tiết, màn
host giữ nguyên nấc.

## 5. Lưu ý khi sửa

- Mapping/field mới của chi tiết voucher → cập nhật `VoucherDetail` (domain) + `toVoucherDetail()` (data).
- Logic enable/label nút theo trạng thái mới → bổ sung vào `VoucherStatus` và phần suy luận trong ViewModel.
- `campaignStatus` ảnh hưởng tới khả năng dùng voucher (DISABLED/EXPIRED/DELETED → không dùng được dù `status=ACTIVE`).
