# Feature: Endow View (điểm tích hợp vào màn thanh toán)

Đây là feature **tích hợp quan trọng nhất** cho host app: một **custom View** (`PRMEndowView`) cùng
**`PromotionIntegrateManager`** để nhúng phần "ưu đãi/voucher" trực tiếp vào màn hình thanh toán của đối tác.

- **Package:** `ui/feature/promotion/endowview` (+ `ui/feature/promotion/PromotionIntegrateManager.kt`)
- **Thành phần:** `PRMEndowView`, `PRMEndowViewModel`, `PRMEndowUiState`, `PromotionIntegrateManager`

---

## 1. `PRMEndowView` — custom View

- Kế thừa `ConstraintLayout`, inflate `PrmViewEndowBinding` (View Binding).
- Tự gắn vòng đời qua `findViewTreeLifecycleOwner()` + coroutine scope nội bộ (`SupervisorJob`).
- Dùng `ApplyPromotionAdapter` để hiển thị các voucher đã áp dụng.
- Theme hoá qua `PromotionThemeRegistry` / `DiscountBadgeToken`.

### State — `PRMEndowUiState`
| Field | Ý nghĩa |
|-------|---------|
| `myVouchers` / `otherVouchers` | Danh sách voucher đã nạp sẵn (truyền sang Choose Promotion để tránh gọi API lại) |
| `discountDetails` | Chi tiết giảm giá sau khi áp dụng |
| `discountUnavailable` | Có voucher nhưng không đủ điều kiện áp dụng |
| `totalVoucherCount` | Tổng số voucher |
| `hasLoadedInitial` | Đã nạp lần đầu |
| `error` | Lỗi (nếu có) |

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

- `PromotionIntegrateManager` và `PRMEndowView` là **bề mặt tích hợp với host** — đổi API = breaking. Cập nhật `INTEGRATION.md` + docs (AI_AGENT_RULES điều 7).
- **Luôn** gọi `clear()` khi view/Fragment huỷ để giải phóng scope (tránh leak).
- Giữ tối ưu `PreloadVouchers` để không gọi API trùng giữa Endow và Choose Promotion.
- Mã lỗi trả về `onError` lấy từ `ErrorCodes` / `PromotionApiException` (xem `../ErrorHandling.md`).
- Liên quan: [ChoosePromotion.md](./ChoosePromotion.md).
