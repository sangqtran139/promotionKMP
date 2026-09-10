# Feature: Offer Widget (điểm tích hợp vào màn thanh toán)

Đây là feature **tích hợp quan trọng nhất** cho host app: một **custom View** (`PRMOfferWidget`) cùng
**`PRMOfferWidget.confirmRedemption`** để nhúng phần "ưu đãi/voucher" trực tiếp vào màn hình thanh toán của đối tác.

- **Package public:** `entry/offerwidget/` — `PRMOfferWidget`, `AppliedDiscount`. Trạng thái widget dùng
  `OfferWidgetDisplayState` của `promotionLogic` (không còn bản sao `OfferWidgetViewState` bên Android).
  Đây là **bề mặt host**, nên nó ở trong `entry` như mọi thứ host chạm tới (xem [PublicApi.md](../common/PublicApi.md)).
- **Package nội bộ:** `ui/feature/promotion/offerwidget/` — `OfferWidgetViewModel` (lớp bọc mỏng, **không**
  còn state model riêng).

> **Cập nhật (tầng UI-logic dùng chung):** Toàn bộ nghiệp vụ widget — `findEligible`, **validate & apply**,
> và quyết định **widget-state** (`EMPTY`/`NOT_APPLIED`/`APPLIED`/`UNAVAILABLE`) — nay nằm ở
> **`OfferWidgetStore`** (`promotionLogic/presentation/offerwidget`), dùng chung Android & iOS.
> `PRMOfferWidgetViewModel` (Android) và `OfferWidgetViewModel` (iOS) đều là **lớp bọc mỏng** quanh store.
> - Validate&apply đi qua `OfferWidgetStore.validateAndApply(offers)`. Màn "Chọn ưu đãi" chỉ **trả offers
>   đang chọn** (`onApplySelectedOffers` → `PRMOfferWidget.applySelectedOffers`), store lo validate và
>   **trả về `OfferWidgetApplyOutcome`** (`Applied` / `Rejected(items)` / `Failed(errorCode)`) — xem
>   [1.6](#16-offerwidgetapplyoutcome--kết-cục-một-lượt-áp).
> - Kết quả validate dùng model shared `OfferWidgetAppliedDiscount`. Bên Android `AppliedDiscount` nay là
>   **`typealias` trỏ thẳng vào nó** (không còn data class chép lại + hai hàm map qua lại): host giữ
>   nguyên import `com.ttcn.prm.ui.feature.offerwidget.AppliedDiscount` và vẫn dựng `AppliedDiscount(...)`
>   như cũ. Host viết bằng **Java** thì không thấy typealias — phải dùng `OfferWidgetAppliedDiscount`.
> - **iOS widget reactive off store** (parity Android): `offerWidgetVM.observe { render(OfferWidgetState) }` +
>   `offerWidgetVM.loadInitial()`; `render` map `OfferWidgetStore.widgetState` → `PRMOfferWidget.setState`, callback
>   host qua `OfferWidgetHostNotifier` (`promotionLogic/presentation/offerwidget`, dùng chung 2 nền tảng) — bắn
>   `onVoucherApplied(voucherId)` khi widget **vừa vào APPLIED hoặc UNAVAILABLE**, hoặc đang ở một
>   trong hai trạng thái đó mà id voucher đầu danh sách đổi khác (vd host tự gọi lại
>   `openChoosePromotion` để "Chọn lại" khi voucher cũ vẫn còn hợp lệ) — không chỉ theo transition
>   trạng thái như trước. Bắn **cả khi `valid = false`** (UNAVAILABLE) để host luôn biết id voucher đã
>   áp; muốn biết còn hợp lệ hay không thì tự đọc `OfferWidgetDisplayState`/gọi `validateDiscounts`.
> - **Order items dùng chung**: request `findEligible` lấy `items` từ
>   `PromotionRequestContextProvider.getOrderItems()` (iOS: `PromotionMutableContext`).

## Mục lục

<!-- toc -->
- [1. `PRMOfferWidget` — custom View](#1-prmofferwidget--custom-view)
  - [1.1. Nền của widget — host quyết, SDK chỉ đỡ trường hợp trống](#11-nền-của-widget--host-quyết-sdk-chỉ-đỡ-trường-hợp-trống)
  - [1.2. State — `OfferWidgetState` (dùng chung, ở `promotionLogic`)](#12-state--offerwidgetstate-dùng-chung-ở-promotionlogic)
  - [1.3. Nguồn dữ liệu và feature flag](#13-nguồn-dữ-liệu-và-feature-flag)
  - [1.4. Auto-apply hiện đang tắt](#14-auto-apply-hiện-đang-tắt)
  - [1.5. `OfferWidgetDisplayState` (trạng thái hiển thị — dùng chung 2 nền tảng)](#15-offerwidgetdisplaystate-trạng-thái-hiển-thị--dùng-chung-2-nền-tảng)
  - [1.6. `OfferWidgetApplyOutcome` — kết cục một lượt áp](#16-offerwidgetapplyoutcome--kết-cục-một-lượt-áp)
- [2. `confirmRedemption` — nút thanh toán của host](#2-confirmredemption--nút-thanh-toán-của-host)
- [3. Luồng tích hợp end-to-end](#3-luồng-tích-hợp-end-to-end)
- [4. Lưu ý khi sửa (quan trọng — đây là public-facing)](#4-lưu-ý-khi-sửa-quan-trọng--đây-là-public-facing)
<!-- /toc -->

---

## 1. `PRMOfferWidget` — custom View

- Kế thừa `ConstraintLayout`, inflate `PrmViewOfferWidgetBinding` (View Binding).
- Tự gắn vòng đời qua `findViewTreeLifecycleOwner()` + coroutine scope nội bộ (`SupervisorJob`).
- Dùng `ApplyPromotionAdapter` để hiển thị các voucher đã áp dụng.
- Theme hoá qua `PromotionThemeRegistry` / `PRMDiscountBadgeToken`.

### 1.1. Nền của widget — host quyết, SDK chỉ đỡ trường hợp trống

`prm_view_offer_widget.xml` không khai `android:background`, còn chữ bên trong thì cứng ở màu sáng
(`prm_color_222_cep` = #222222). Trên host dùng theme **DayNight**, ở dark mode nền tối của host lộ
ra sau widget và chữ #222222 nằm đè lên → nhìn như widget bị dark theme.

`ContextThemeWrapper(R.style.PRMForceLight)` ở chỗ inflate **không** cứu được: nó chỉ đổi cách
resolve *attribute* lúc inflate, mà ở đây không có attribute nền nào để resolve. Khác iOS —
`overrideUserInterfaceStyle = .light` (`PRMOfferWidget.swift`) ép cả subtree gồm cả nền.

Cách xử lý: `PRMOfferWidget` đặt nền `@color/prm_white` **chỉ khi host chưa đặt gì**.

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
<com.ttcn.prm.ui.feature.offerwidget.PRMOfferWidget
    android:id="@+id/offerWidget"
    android:background="@android:color/transparent"
    … />
```

> `applyTokenInternal()` **không** được đụng tới nền. Trước đây nó có `binding.root.background = null`
> chạy lại mỗi lần render — vô tác dụng vì không ai đặt nền cho `viewContainer`, nhưng là cái bẫy
> nếu sau này nền mặc định chuyển sang `binding.root`.

### 1.2. State — `OfferWidgetState` (dùng chung, ở `promotionLogic`)

Android **không còn state model riêng**. `OfferWidgetViewModel.uiState` chính là `OfferWidgetStore.state`, và
`PRMOfferWidget.renderState(state: OfferWidgetState)` đọc thẳng — cùng một object với iOS.

`internal` vì nó mang `EligibleOffer` — type của `:promotionLogic`, không được lọt ra API public.
Cùng lý do, `PRMOfferWidget.myVouchers` / `otherVouchers` cũng là `internal`; widget tự đưa chúng cho
`PromotionSDK.openChoosePromotion(activity, offerWidget)` khi user bấm — host không chạm tới.

| Field | Ý nghĩa |
|-------|---------|
| `myVouchers` / `otherVouchers` | `List<EligibleOffer>` đã nạp sẵn (truyền sang Choose Promotion để tránh gọi API lại) |
| `discountDetails` | Chi tiết giảm giá sau khi áp dụng — mỗi `AppliedDiscount` có thêm `tags: List<String>` (nhãn hiển thị server gửi kèm `discountDetails[]`, `tags[0]` là chữ hiện lên voucher; rỗng → fallback format `calculatedDiscount`, xem `ApplyPromotionAdapter.bind`) |
| `discountUnavailable` | Có voucher nhưng không đủ điều kiện áp dụng |
| `totalVoucherCount` | Tổng số ưu đãi = `myTotalElements + otherTotalElements` |
| `hasLoadedInitial` | Đã nạp lần đầu |
| `error` | Lỗi (nếu có) |

### 1.3. Nguồn dữ liệu và feature flag

Widget gọi `FindEligibleCampaignsUseCase` (giống `PromotionSDKImpl` bên iOS), **không** phải
`searchVouchers`. Nó tự ẩn (`isVisible = false`) nếu cờ `VOUCHER_SELECTION` tắt: áp cache hiện có
ngay khi attach, rồi `PromotionFeatureGate.refresh()` và áp lại nếu giá trị đổi — đúng thứ tự của
`applyFlag` bên iOS. Cờ tắt thì **không** gọi API.

### 1.4. Auto-apply hiện đang tắt

`findEligible` chưa trả `isAutoApplied` (không có ở `EligibleOfferDto` lẫn các DTO lồng bên trong),
nên voucher tự-áp-dụng **không chạy** ở luồng checkout — trên cả Android lẫn iOS.
`validateAndAutoApply` vẫn nằm đó, chờ backend bổ sung field.
Xem `TODO(auto-apply)` ở `PromotionUiMapper.kt` và `PRMOfferWidgetViewModel.kt`.

### 1.5. `OfferWidgetDisplayState` (trạng thái hiển thị — dùng chung 2 nền tảng)
- `EMPTY` — chưa có voucher.
- `NOT_APPLIED` — có voucher nhưng chưa áp dụng.
- `APPLIED` — đã áp dụng.
- `UNAVAILABLE` — voucher **đang áp** nhưng không còn hợp lệ với đơn.

> ⚠️ `UNAVAILABLE` nay chỉ đến từ **hai** đường: ưu đãi đang áp hỏng giữa chừng
> (`revalidateAfterBudgetError`, khi `createRedemption` báo hết ngân sách) và host tự đưa kết quả vào
> (`OfferWidgetIntent.SetApplied` / `MarkUnavailable`, tức `PRMOfferWidget.setDiscountDetails` /
> `markAppliedVoucherUnavailable`).
>
> Đường thứ ba **đã bỏ**: trước đây bấm "Áp dụng" ở màn chọn mà server trả `valid = false` thì
> `validateAndApply` vẫn ghi `appliedDiscounts` + `discountUnavailable = true`, widget nhảy sang
> `UNAVAILABLE` còn màn chọn thì đóng như đã áp xong. Nay nhánh đó là
> `OfferWidgetApplyOutcome.Rejected` — **không commit gì**, và màn chọn ở lại để user chọn cái khác (xem
> [ChoosePromotion.md §2.2](./ChoosePromotion.md#22-server-từ-chối-ưu-đãi--disable-tại-chỗ)).

### 1.6. `OfferWidgetApplyOutcome` — kết cục một lượt áp

`OfferWidgetStore.validateAndApply(offers)` là `suspend` và trả **kết cục của đúng lượt gọi đó**, không phải
`OfferWidgetState`:

| Kết cục | Khi nào | `appliedDiscounts` | Nơi gọi làm gì |
|---|---|---|---|
| `Applied` | tất cả `valid = true` (hoặc `offers` rỗng = xoá áp) | **ghi bộ mới** | đóng màn chọn |
| `Rejected(items)` | có ít nhất một `valid = false` | **giữ nguyên** | ở lại + disable ưu đãi + popup câu server |
| `Failed(errorCode)` | mạng/HTTP hỏng, hoặc HTTP 200 mà `data` rỗng (`NO_RESULT`) | giữ nguyên | ở lại + popup câu lỗi chung |

`items` là `List<RejectedOffer>` (`presentation/common`) — `objectId` + `message` lấy từ
`ValidateDiscountsResult.reasonFor()` (ưu tiên `validationMessages` cấp dòng, lùi về
`businessRuleViolations` cấp đơn). `message` **có thể rỗng** khi server từ chối mà không nói lý do;
lõi không dựng chuỗi tiếng Việt, native tự lùi về câu lỗi chung.

`Rejected` cố ý **không** set `errorCode`: đây không phải lỗi kỹ thuật, và câu giải thích đi kèm
outcome chứ không qua bảng mã lỗi.

---

## 2. `confirmRedemption` — nút thanh toán của host

```kotlin
// Android — gọi thẳng trên widget
btnConfirmPayment.setOnClickListener {
    binding.offerWidget.confirmRedemption(
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

Nghiệp vụ nằm ở **`OfferWidgetStore.confirmRedemption`** (`promotionLogic`) nên hai nền tảng chạy một
đường. Thứ tự xử lý:

1. Không áp ưu đãi nào → `onSuccess` ngay, **không gọi mạng**, không phụ thuộc cờ.
2. Cờ `VOUCHER_REDEEM` tắt → `onError("PRM_MOB_021")`, không gọi mạng, **không popup** — xem
   [FeatureFlag.md](FeatureFlag.md): luồng thanh toán là của host, SDK không chen thông báo vào.
3. `INSUFFICIENT_BUDGET` (trong body **hoặc** HTTP 422) → validate lại (gác riêng bằng
   `VOUCHER_APPLY`) → cập nhật state → widget hiện giá mới → `onError("INSUFFICIENT_BUDGET")`.

> **Trước đây:** Android có class `PromotionIntegrateManager` giữ nguyên luồng này — nghĩa là logic
> đụng tiền **chưa từng dùng chung**, iOS không hề có. Nó còn bắt host nhớ gọi `clear()` (quên là rò
> scope) và tự nhân bản phép map kết quả validate. Đã bỏ; luồng về `OfferWidgetStore`, có 6 test ở
> `OfferWidgetStoreTest`.

---

## 3. Luồng tích hợp end-to-end

```
Host nhúng <PRMOfferWidget/> vào layout thanh toán
   → PRMOfferWidget nạp voucher (findEligible qua OfferWidgetStore)
   → User mở Choose Promotion (chỉ nhận preSelectedVoucherIds; màn đó TỰ gọi findEligible)
   → chọn & bấm "Áp dụng" → OfferWidgetStore.validateAndApply → OfferWidgetApplyOutcome
        Applied  → widget ưu đãi cập nhật discountDetails + OfferWidgetDisplayState.APPLIED, màn chọn đóng
        Rejected → widget ưu đãi KHÔNG đổi, màn chọn ở lại và disable ưu đãi đó
        Failed   → widget ưu đãi KHÔNG đổi, màn chọn ở lại + popup lỗi
Host bấm thanh toán
   → PRMOfferWidget.confirmRedemption() / PromotionSDK.confirmRedemption()
        → OfferWidgetStore.confirmRedemption()
             → createRedemptionSession
             → nếu INSUFFICIENT_BUDGET: validateStackableDiscounts (revalidate) + cập nhật state
        → onSuccess(proceed) / onError(errorCode)
```

---

## 4. Lưu ý khi sửa (quan trọng — đây là public-facing)

- `PRMOfferWidget` là **bề mặt tích hợp với host** — đổi API = breaking. Cập nhật [`AndroidIntegrationGuide.md`](../AndroidIntegrationGuide.md) §6.2 + [`PublicApi.md`](../common/PublicApi.md) (AI_AGENT_RULES điều 7). `INTEGRATION.md` ở gốc repo chỉ là trang điện, **đừng** viết nội dung vào đó.
- **Tối ưu `PreloadVouchers` đã bỏ**: màn "Chọn ưu đãi" luôn tự gọi `findEligible` khi mở, nên
  `PRMOfferWidget.myVouchers`/`otherVouchers`/`myIsLastPage`/`otherIsLastPage` (đều `internal`) cũng đã
  xoá. Widget vẫn giữ danh sách trong `OfferWidgetState` cho lượt nạp của chính nó.
- Thêm nhánh mới cho `OfferWidgetApplyOutcome` → cập nhật cả `ChoosePromotionFragment.onApplyClicked`
  (Android) lẫn `ChoosePromotionViewController.handleApplyOutcome` (iOS); bên Swift dùng `as?` nên
  **không** báo lỗi biên dịch khi thiếu nhánh.
- Mã lỗi trả về `onError` lấy từ `ErrorCodes` / `PromotionException` (xem `../ErrorHandling.md`).
- Liên quan: [ChoosePromotion.md](./ChoosePromotion.md).
