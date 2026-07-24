# Feature: Feature Flag

Cơ chế **bật/tắt tính năng** của SDK theo cấu hình từ xa — kill-switch, không phải A/B test.

- **Lõi (`:promotionLogic`):** `PromotionFeatureFlagUseCases()` — `refresh()`, `isEnabled(name)`,
  `flagsOf(names)`, `all()`. Dựng thẳng sau `PromotionContainer.initialize(...)`.
- **Hỗ trợ:** `FetchFeatureFlagsUseCase`, `IsFeatureEnabledUseCase`, `GetFeatureFlagsUseCase`,
  `GetPromotionFeatureFlagsUseCase`, `FeatureFlagRepository(Impl)`, `FeatureFlagApiService`,
  `FeatureFlagLocalDataSource`, `core/di/FeatureFlagModule`, `FeatureFlagException`
- **Gate (dùng chung):** `PromotionFeatureGate` trong `core/domain/usecase/` — **một object Kotlin duy
  nhất** cho cả hai nền tảng. Android gọi `PromotionFeatureGate.canOpenVoucherDetail()`, iOS gọi
  `PromotionFeatureGate.shared.canOpenVoucherDetail()`.
- **Tên tính năng:** chỉ có ở `PromotionFeatureFlag` (hằng chuỗi Kotlin). Enum Swift
  `PromotionSDKFeature` đã bị xoá; không có enum thay thế ở bất kỳ ngôn ngữ nào.
- **Phần riêng mỗi nền tảng:** chỉ còn *chỗ gọi* hiển thị thông báo khi bị chặn —
  `PRMBaseFragment.openPromotionDetail()` và `BaseRouter.canOpenVoucherDetail()`; **cả hai đều toast**
  (`Toast` / `PRMToast`).

> **SDK không phơi API hỏi cờ ra host.** App đối tác không cần biết cờ nào đang bật: mọi điểm vào đều
> tự gác, và khi bị chặn thì SDK hiện thông báo PRM_MOB_021 (iOS còn báo qua
> `onAvailabilityChanged(enabled:)`). `PromotionSDK.featureFlags` (Android) và
> `PromotionSDK.isEnabled(feature:)` (iOS) đã bị gỡ — không nơi nào dùng chúng.

---

## 1. Các cờ

`PromotionFeatureFlag`: `ENABLE_ALL`, `VOUCHER_LIST`, `VOUCHER_DETAIL`, `VOUCHER_SELECTION`,
`VOUCHER_APPLY`, `VOUCHER_REDEEM`.

Ba hành vi cần nhớ:

1. **`ENABLE_ALL` là công tắc tổng.** Tắt nó thì mọi cờ con đều tắt, bất kể giá trị riêng
   (`if (!enableAll) return false` trong `PromotionFeatureFlags.isEnabled`).
2. **Fail-open.** Chưa có cache → bật hết. SDK không tự khoá tính năng khi chưa gọi được API lần nào.
3. **`refresh()` không ném lỗi.** Gọi API thất bại thì giữ nguyên cờ đang cache.

> **Bug đã sửa.** `when (flag)` trong `PromotionFeatureFlags.isEnabled` không có nhánh `ENABLE_ALL`
> nên hỏi thẳng công tắc tổng luôn rơi vào `else -> false` — tức `isEnabled(ENABLE_ALL)` **luôn trả
> `false`**, kể cả khi nó đang bật. Cờ vẫn gác được cờ con (dòng `if (!enableAll)`), nên bug này ẩn.
> Nay có nhánh riêng và ba test trong `EnableAllFlagTest`.

Cờ ghi xuống `KeyValueStorage` (SharedPreferences / NSUserDefaults) nên lần mở app sau không phải
chờ API. Xem `../StorageGuide.md`.

---

## 2. Gác ở hai tầng, một nguồn sự thật

Quyết định "có được chạy không" **luôn** đến từ `PromotionFeatureGate` ở lõi Kotlin. Nhưng nó được
hỏi ở hai chỗ, vì có hai đường vào khác nhau:

| Tầng | Ai hỏi gate | Gác cái gì | Cờ tắt thì |
|---|---|---|---|
| Facade headless | `PromotionUseCases` | 5 hàm nghiệp vụ | `Failure(FEATURE_DISABLED)`, **không gọi mạng** |
| UI native | Điểm điều hướng | Mở màn danh sách / chi tiết, hiện widget | Thông báo `PRM_MOB_021`, không điều hướng |

ViewModel của UI native dựng thẳng use case đơn lẻ (`SearchCustomerVouchersUseCase()`) nên **không**
đi qua facade — đó là lý do phải hỏi gate lần nữa ở điểm điều hướng, chứ không phải gác hai lần thừa.

Điểm điều hướng, đối xứng từng cặp:

| Việc | Android | iOS |
|---|---|---|
| Mở màn "Ưu đãi của tôi" | `PromotionSDK.openMyPromotion()` | `PromotionSDK.openMyPromotion(from:)` |
| Mở màn "Chi tiết ưu đãi" | `PRMBaseFragment.openPromotionDetail()` | `BaseRouter.canOpenVoucherDetail()` |
| Hiện widget checkout | `PRMEndowView.applyFeatureFlag()` | `PromotionSDKImpl.applyFlag()` |
| Nạp cờ lúc init | `PromotionSDK.initialize` → `gate.refresh()` | `PromotionSDKImpl.init` → `gate.refresh()` |

Cả hai nền tảng đều **không** phơi API hỏi cờ cho host — xem ghi chú ở đầu file.

`PromotionFeatureGate` có sẵn `isSdkEnabled()`, `canApplyVoucher()`, `canRedeemVoucher()` nhưng
**chưa nơi nào gọi**: công tắc tổng đã áp ngầm trong `PromotionFeatureFlags.isEnabled`, còn hai cờ
`APPLY`/`REDEEM` thì `PromotionUseCases` tự gác bên trong. Giữ lại cho các màn sắp tới.

Ánh xạ cờ ↔ hàm (giống hệt hai nền tảng):

| Hàm | Cờ |
|---|---|
| `searchVouchers` | `VOUCHER_LIST` |
| `getVoucherDetail` | `VOUCHER_DETAIL` |
| `findEligible` | `VOUCHER_SELECTION` |
| `validateDiscounts` | `VOUCHER_APPLY` |
| `createRedemption` | `VOUCHER_REDEEM` |

Mã lỗi chung: `PromotionErrorCodes.FEATURE_DISABLED` = `"PRM_MOB_021"`.

> **Cạm bẫy đã gặp.** Android từng nạp cờ lúc `PromotionSDK.initialize()` rồi **không đọc lại ở đâu cả** —
> tắt `VOUCHER_DETAIL` trên server thì iOS chặn màn chi tiết, Android vẫn vào bình thường. Thêm màn
> mới thì phải gác ở tầng UI; facade không thấy được điều hướng.

Test: `promotionLogic/src/commonTest/.../FeatureFlagGateTest.kt` khẳng định cờ tắt thì chặn **trước
khi** chạm mạng, và một cờ tắt không chặn nhầm hàm khác.

---

## 3. Nguyên tắc dùng feature flag

- Luôn hỏi `PromotionFeatureGate`; **không** gọi thẳng `PromotionFeatureFlagUseCases()` từ tầng UI.
  Thêm màn mới thì thêm một hàm `canOpen…` vào gate, đừng lặp `isEnabled("PROMOTION.…")` tại chỗ.
- Giữ **fail-open**: thiếu cờ không được làm treo hay khoá tính năng.
- Đặt tên cờ tập trung ở `PromotionFeatureFlag` trong lõi Kotlin — iOS đọc lại chính hằng số đó
  (`PromotionFeature.flagName`), không định nghĩa lại chuỗi.

---

## 4. Việc còn treo

**`ui/feature/featureflag/` (Android) là code chết** — `FeatureFlagViewModel`, `FeatureFlagUIState`,
`FeatureFlagUIAction` không có nơi nào dùng (0 tham chiếu). Chúng là khung sót lại từ bản gốc.
Xoá được, chờ xác nhận.

**TODO(feature-flag): schema chưa chốt.** Lõi Kotlin và bản iOS cũ parse **hai schema khác nhau**
cho cùng endpoint `POST api/v1/vtm/feature-flag/list`:

```
iOS    → data.enableSdk + data.features[] { featureCode: "voucher_detail", allowed }
Kotlin → data[] { flagName: "PROMOTION.VOUCHER_DETAIL", enabled }
```

Nếu server dùng schema iOS thì Kotlin parse hỏng, `refresh()` nuốt lỗi, cache giữ mặc định bật-hết →
mọi gate cho qua (đúng hành vi cũ, nhưng kill-switch **không hoạt động**). Khi backend xác nhận
schema, sửa `FeatureFlagItemResponse` trong `promotionLogic`; các file gác không phải đổi.
