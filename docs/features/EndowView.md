# Feature: Endow View (điểm tích hợp vào màn thanh toán)

Đây là feature **tích hợp quan trọng nhất** cho host app: một **custom View** (`PRMEndowView`) cùng
**`PromotionIntegrateManager`** để nhúng phần "ưu đãi/voucher" trực tiếp vào màn hình thanh toán của đối tác.

- **Package:** `ui/feature/promotion/endowview` (+ `ui/feature/promotion/PromotionIntegrateManager.kt`)
- **Thành phần:** `PRMEndowView`, `PRMEndowViewModel`, `PRMEndowUiState`, `PromotionIntegrateManager`

> **Cập nhật (tầng UI-logic dùng chung):** Toàn bộ nghiệp vụ widget — `findEligible`, **validate & apply**,
> và quyết định **widget-state** (`EMPTY`/`NOT_APPLIED`/`APPLIED`/`UNAVAILABLE`) — nay nằm ở
> **`EndowStore`** (`promotionLogic/presentation/endow`), dùng chung Android & iOS.
> `PRMEndowViewModel` (Android) và `EndowViewModel` (iOS, mới — trước đây iOS không có VM cho widget,
> logic dồn ở `PromotionSDKImpl`) đều là **lớp bọc mỏng** quanh store.
> - Validate&apply đi qua `EndowStore.ValidateAndApply(offers)` (trước: Android ở `ChoosePromotionViewModel`,
>   iOS ở `PromotionSDKImpl`). Màn "Chọn ưu đãi" nay chỉ **trả offers đang chọn** (`ApplySelectedOffers` /
>   `onApplySelectedOffers` → `PRMEndowView.applySelectedOffers`), store lo validate.
> - Kết quả validate dùng model shared `EndowAppliedDiscount`; mỗi nền tảng map sang model public riêng
>   (`AppliedDiscount` bên Android).
> - **iOS widget reactive off store** (parity Android): `PromotionSDKImpl` bỏ `loadVouchers`/`cachedListModel`/
>   `setState` tay — nay `endowVM.observe { render(EndowState) }` + `endowVM.loadInitial()`; `render` map
>   `EndowStore.widgetState` → `PRMEndowView.setState`, callback host (count/applied) theo transition.
> - **Order items dùng chung**: request `findEligible` lấy `items` từ `PromotionRequestContextProvider.getOrderItems()`
>   (iOS: `PromotionMutableContext`), thay cho `items = emptyList()` — đồng bộ campaign theo SKU.

---

## 1. `PRMEndowView` — custom View

- Kế thừa `ConstraintLayout`, inflate `PrmViewEndowBinding` (View Binding).
- Tự gắn vòng đời qua `findViewTreeLifecycleOwner()` + coroutine scope nội bộ (`SupervisorJob`).
- Dùng `ApplyPromotionAdapter` để hiển thị các voucher đã áp dụng.
- Theme hoá qua `PromotionThemeRegistry` / `DiscountBadgeToken`.

### State — `PRMEndowUiState` (`internal`)

`internal` vì nó mang `EligibleOffer` — type của `:promotionLogic`, không được lọt ra API public.
Cùng lý do, `PRMEndowView.myVouchers` / `otherVouchers` cũng là `internal`; host lấy chúng gián tiếp
qua `ChoosePromotionFragment.forEndowView(endowView)`.

| Field | Ý nghĩa |
|-------|---------|
| `myVouchers` / `otherVouchers` | `List<EligibleOffer>` đã nạp sẵn (truyền sang Choose Promotion để tránh gọi API lại) |
| `discountDetails` | Chi tiết giảm giá sau khi áp dụng |
| `discountUnavailable` | Có voucher nhưng không đủ điều kiện áp dụng |
| `totalVoucherCount` | Tổng số ưu đãi = `myTotalElements + otherTotalElements` |
| `hasLoadedInitial` | Đã nạp lần đầu |
| `error` | Lỗi (nếu có) |

### Nguồn dữ liệu và feature flag

Widget gọi `FindEligibleCampaignsUseCase` (giống `PromotionSDKImpl` bên iOS), **không** phải
`searchVouchers`. Nó tự ẩn (`isVisible = false`) nếu cờ `VOUCHER_SELECTION` tắt: áp cache hiện có
ngay khi attach, rồi `PromotionFeatureGate.refresh()` và áp lại nếu giá trị đổi — đúng thứ tự của
`applyFlag` bên iOS. Cờ tắt thì **không** gọi API.

### Auto-apply hiện đang tắt

`findEligible` chưa trả `isAutoApplied` (không có ở `EligibleOfferDto` lẫn các DTO lồng bên trong),
nên voucher tự-áp-dụng **không chạy** ở luồng checkout — trên cả Android lẫn iOS. Trước đây Android
lấy cờ này từ `searchVouchers` và nhánh auto-apply có chạy; đổi sang `findEligible` là đánh đổi có
chủ đích để hai nền tảng khớp nhau. `validateAndAutoApply` vẫn nằm đó, chờ backend bổ sung field.
Xem `TODO(auto-apply)` ở `PromotionUiMapper.kt` và `PRMEndowViewModel.kt`.

### `EndowViewState` (trạng thái hiển thị)
- `EMPTY` — chưa có voucher.
- `NOT_APPLIED` — có voucher nhưng chưa áp dụng.
- `APPLIED` — đã áp dụng.
- `UNAVAILABLE` — có voucher nhưng không đủ điều kiện (hết hạn/không hợp lệ với đơn).

---

## 2. `PromotionIntegrateManager` — SDK manager cho đối tác

Đối tác **khởi tạo 1 lần** và gọi `confirmRedemption()` khi user bấm thanh toán. Toàn bộ logic
createRedemption / revalidate / cập nhật UI được ẩn bên trong; DI resolve tự động (`PromotionIntegrateManager.create(...)`).

```kotlin
// Khởi tạo (trong Fragment.setupUI)
val promotionManager = PromotionIntegrateManager.create(binding.endowView)

// Khi bấm thanh toán
btnConfirmPayment.setOnClickListener {
    promotionManager.confirmRedemption(
        onSuccess = { proceedPayment() },
        onError   = { errorCode -> showError(errorCode) },
    )
}

// Giải phóng khi Fragment destroy
override fun onDestroyView() {
    super.onDestroyView()
    promotionManager.clear()
}
```

Phụ thuộc bên trong (resolve qua DI): `CreateRedemptionSessionUseCase`, `ValidateStackableDiscountsUseCase`,
`PromotionRequestContextProvider`; chạy trên scope riêng (`SupervisorJob + Dispatchers.Main.immediate`).

---

## 3. Luồng tích hợp end-to-end

```
Host nhúng <PRMEndowView/> vào layout thanh toán
   → PRMEndowView nạp voucher (myVouchers/otherVouchers)
   → User mở Choose Promotion (nhận PreloadVouchers từ Endow để tránh double API)
   → chọn & validate → ApplyValidatedVouchers → Endow cập nhật discountDetails + EndowViewState.APPLIED
Host bấm thanh toán
   → PromotionIntegrateManager.confirmRedemption()
        → validateStackableDiscounts (revalidate)
        → createRedemptionSession
        → onSuccess(proceed) / onError(errorCode)
```

---

## 4. Lưu ý khi sửa (quan trọng — đây là public-facing)

- `PromotionIntegrateManager` và `PRMEndowView` là **bề mặt tích hợp với host** — đổi API = breaking. Cập nhật [`AndroidIntegrationGuide.md`](../AndroidIntegrationGuide.md) §6.2 + [`PublicApi.md`](../common/PublicApi.md) (AI_AGENT_RULES điều 7). `INTEGRATION.md` ở gốc repo chỉ là trang điện, **đừng** viết nội dung vào đó.
- **Luôn** gọi `clear()` khi view/Fragment huỷ để giải phóng scope (tránh leak).
- Giữ tối ưu `PreloadVouchers` để không gọi API trùng giữa Endow và Choose Promotion.
- Mã lỗi trả về `onError` lấy từ `ErrorCodes` / `PromotionException` (xem `../ErrorHandling.md`).
- Liên quan: [ChoosePromotion.md](./ChoosePromotion.md).
