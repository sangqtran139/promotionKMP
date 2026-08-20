# Feature: Endow View (điểm tích hợp vào màn thanh toán)

Đây là feature **tích hợp quan trọng nhất** cho host app: một **custom View** (`PRMEndowView`) cùng
**`PRMEndowView.confirmRedemption`** để nhúng phần "ưu đãi/voucher" trực tiếp vào màn hình thanh toán của đối tác.

- **Package public:** `entry/endowview/` — `PRMEndowView`, `AppliedDiscount`. Trạng thái widget dùng
  `EndowWidgetState` của `promotionLogic` (không còn bản sao `EndowViewState` bên Android).
  Đây là **bề mặt host**, nên nó ở trong `entry` như mọi thứ host chạm tới (xem [PublicApi.md](../common/PublicApi.md)).
- **Package nội bộ:** `ui/feature/promotion/endowview/` — `EndowViewModel` (lớp bọc mỏng, **không**
  còn state model riêng).

> **Cập nhật (tầng UI-logic dùng chung):** Toàn bộ nghiệp vụ widget — `findEligible`, **validate & apply**,
> và quyết định **widget-state** (`EMPTY`/`NOT_APPLIED`/`APPLIED`/`UNAVAILABLE`) — nay nằm ở
> **`EndowStore`** (`promotionLogic/presentation/endow`), dùng chung Android & iOS.
> `PRMEndowViewModel` (Android) và `EndowViewModel` (iOS) đều là **lớp bọc mỏng** quanh store.
> - Validate&apply đi qua `EndowStore.ValidateAndApply(offers)`. Màn "Chọn ưu đãi" chỉ **trả offers
>   đang chọn** (`ApplySelectedOffers` /
>   `onApplySelectedOffers` → `PRMEndowView.applySelectedOffers`), store lo validate.
> - Kết quả validate dùng model shared `EndowAppliedDiscount`. Bên Android `AppliedDiscount` nay là
>   **`typealias` trỏ thẳng vào nó** (không còn data class chép lại + hai hàm map qua lại): host giữ
>   nguyên import `com.ttcn.prm.ui.feature.endowview.AppliedDiscount` và vẫn dựng `AppliedDiscount(...)`
>   như cũ. Host viết bằng **Java** thì không thấy typealias — phải dùng `EndowAppliedDiscount`.
> - **iOS widget reactive off store** (parity Android): `endowVM.observe { render(EndowState) }` +
>   `endowVM.loadInitial()`; `render` map `EndowStore.widgetState` → `PRMEndowView.setState`, callback
>   host qua `EndowHostNotifier` (`promotionLogic/presentation/endow`, dùng chung 2 nền tảng) — bắn
>   `onVoucherApplied(voucherId)` khi widget **vừa vào APPLIED**, hoặc đang APPLIED mà id voucher đầu
>   danh sách đổi khác (vd host tự gọi lại `openChoosePromotion` để "Chọn lại" khi voucher cũ vẫn còn
>   hợp lệ) — không chỉ theo transition trạng thái như trước.
> - **Order items dùng chung**: request `findEligible` lấy `items` từ
>   `PromotionRequestContextProvider.getOrderItems()` (iOS: `PromotionMutableContext`).

---

## 1. `PRMEndowView` — custom View

- Kế thừa `ConstraintLayout`, inflate `PrmViewEndowBinding` (View Binding).
- Tự gắn vòng đời qua `findViewTreeLifecycleOwner()` + coroutine scope nội bộ (`SupervisorJob`).
- Dùng `ApplyPromotionAdapter` để hiển thị các voucher đã áp dụng.
- Theme hoá qua `PromotionThemeRegistry` / `DiscountBadgeToken`.

### Nền của widget — host quyết, SDK chỉ đỡ trường hợp trống

`prm_view_endow.xml` không khai `android:background`, còn chữ bên trong thì cứng ở màu sáng
(`prm_color_222_cep` = #222222). Trên host dùng theme **DayNight**, ở dark mode nền tối của host lộ
ra sau widget và chữ #222222 nằm đè lên → nhìn như widget bị dark theme.

`ContextThemeWrapper(R.style.PRMForceLight)` ở chỗ inflate **không** cứu được: nó chỉ đổi cách
resolve *attribute* lúc inflate, mà ở đây không có attribute nền nào để resolve. Khác iOS —
`overrideUserInterfaceStyle = .light` (`PRMEndowView.swift`) ép cả subtree gồm cả nền.

Cách xử lý: `PRMEndowView` đặt nền `@color/prm_white` **chỉ khi host chưa đặt gì**.

```kotlin
private fun applyDefaultBackgroundIfHostDidNotSetOne() {
    if (background == null) {
        setBackgroundColor(ContextCompat.getColor(context, R.color.prm_white))
    }
}
```

`android:background` khai trong XML của host đã được constructor `View` đọc vào **trước** khối `init`
này, nên `background != null` nghĩa là host đã có ý — SDK giữ nguyên, không đè. Host đặt nền bằng
code sau khi view dựng xong thì lệnh của host chạy sau, cũng thắng.

```xml
<!-- host muốn nền khác / trong suốt: khai tường minh, SDK im lặng -->
<com.ttcn.prm.ui.feature.endowview.PRMEndowView
    android:id="@+id/endowView"
    android:background="@android:color/transparent"
    … />
```

> `applyTokenInternal()` **không** được đụng tới nền. Trước đây nó có `binding.root.background = null`
> chạy lại mỗi lần render — vô tác dụng vì không ai đặt nền cho `viewContainer`, nhưng là cái bẫy
> nếu sau này nền mặc định chuyển sang `binding.root`.

### State — `EndowState` (dùng chung, ở `promotionLogic`)

Android **không còn state model riêng**. `EndowViewModel.uiState` chính là `EndowStore.state`, và
`PRMEndowView.renderState(state: EndowState)` đọc thẳng — cùng một object với iOS.

`internal` vì nó mang `EligibleOffer` — type của `:promotionLogic`, không được lọt ra API public.
Cùng lý do, `PRMEndowView.myVouchers` / `otherVouchers` cũng là `internal`; widget tự đưa chúng cho
`PromotionSDK.openChoosePromotion(activity, endowView)` khi user bấm — host không chạm tới.

| Field | Ý nghĩa |
|-------|---------|
| `myVouchers` / `otherVouchers` | `List<EligibleOffer>` đã nạp sẵn (truyền sang Choose Promotion để tránh gọi API lại) |
| `discountDetails` | Chi tiết giảm giá sau khi áp dụng — mỗi `AppliedDiscount` có thêm `tags: List<String>` (nhãn hiển thị server gửi kèm `discountDetails[]`, `tags[0]` là chữ hiện lên voucher; rỗng → fallback format `calculatedDiscount`, xem `ApplyPromotionAdapter.bind`) |
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
nên voucher tự-áp-dụng **không chạy** ở luồng checkout — trên cả Android lẫn iOS.
`validateAndAutoApply` vẫn nằm đó, chờ backend bổ sung field.
Xem `TODO(auto-apply)` ở `PromotionUiMapper.kt` và `PRMEndowViewModel.kt`.

### `EndowWidgetState` (trạng thái hiển thị — dùng chung 2 nền tảng)
- `EMPTY` — chưa có voucher.
- `NOT_APPLIED` — có voucher nhưng chưa áp dụng.
- `APPLIED` — đã áp dụng.
- `UNAVAILABLE` — có voucher nhưng không đủ điều kiện (hết hạn/không hợp lệ với đơn).

---

## 2. `confirmRedemption` — nút thanh toán của host

```kotlin
// Android — gọi thẳng trên widget
btnConfirmPayment.setOnClickListener {
    binding.endowView.confirmRedemption(
        onSuccess = { proceedPayment() },
        onError   = { errorCode -> showError(errorCode) },
    )
}
```
```swift
// iOS — qua facade, vì widget trả về UIView trần
PromotionSDK.confirmRedemption(
    onSuccess: { self.proceedPayment() },
    onError: { code in self.showError(code) }
)
```

Nghiệp vụ nằm ở **`EndowStore.confirmRedemption`** (`promotionLogic`) nên hai nền tảng chạy một
đường. Thứ tự xử lý:

1. Không áp ưu đãi nào → `onSuccess` ngay, **không gọi mạng**, không phụ thuộc cờ.
2. Cờ `VOUCHER_REDEEM` tắt → `onError("PRM_MOB_021")`, không gọi mạng, **không popup** — xem
   [FeatureFlag.md](FeatureFlag.md): luồng thanh toán là của host, SDK không chen thông báo vào.
3. `INSUFFICIENT_BUDGET` (trong body **hoặc** HTTP 422) → validate lại (gác riêng bằng
   `VOUCHER_APPLY`) → cập nhật state → widget hiện giá mới → `onError("INSUFFICIENT_BUDGET")`.

> **Trước đây:** Android có class `PromotionIntegrateManager` giữ nguyên luồng này — nghĩa là logic
> đụng tiền **chưa từng dùng chung**, iOS không hề có. Nó còn bắt host nhớ gọi `clear()` (quên là rò
> scope) và tự nhân bản phép map kết quả validate. Đã bỏ; luồng về `EndowStore`, có 6 test ở
> `EndowStoreTest`.

---

## 3. Luồng tích hợp end-to-end

```
Host nhúng <PRMEndowView/> vào layout thanh toán
   → PRMEndowView nạp voucher (myVouchers/otherVouchers)
   → User mở Choose Promotion (nhận PreloadVouchers từ Endow để tránh double API)
   → chọn & validate → ApplyValidatedVouchers → Endow cập nhật discountDetails + EndowWidgetState.APPLIED
Host bấm thanh toán
   → PRMEndowView.confirmRedemption() / PromotionSDK.confirmRedemption()
        → EndowStore.confirmRedemption()
             → createRedemptionSession
             → nếu INSUFFICIENT_BUDGET: validateStackableDiscounts (revalidate) + cập nhật state
        → onSuccess(proceed) / onError(errorCode)
```

---

## 4. Lưu ý khi sửa (quan trọng — đây là public-facing)

- `PRMEndowView` là **bề mặt tích hợp với host** — đổi API = breaking. Cập nhật [`AndroidIntegrationGuide.md`](../AndroidIntegrationGuide.md) §6.2 + [`PublicApi.md`](../common/PublicApi.md) (AI_AGENT_RULES điều 7). `INTEGRATION.md` ở gốc repo chỉ là trang điện, **đừng** viết nội dung vào đó.
- Giữ tối ưu `PreloadVouchers` để không gọi API trùng giữa Endow và Choose Promotion.
- Mã lỗi trả về `onError` lấy từ `ErrorCodes` / `PromotionException` (xem `../ErrorHandling.md`).
- Liên quan: [ChoosePromotion.md](./ChoosePromotion.md).
