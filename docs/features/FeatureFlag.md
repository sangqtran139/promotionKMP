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
- **Tên tính năng:** nguồn sự thật là `PromotionFeatureFlag` (hằng chuỗi Kotlin). Host **không** gõ
  chuỗi đó — bề mặt public dùng enum `PromotionFeature` ở mỗi nền tảng (xem §3).
- **Phần riêng mỗi nền tảng:** chỉ còn *chỗ gọi* hiển thị thông báo khi bị chặn —
  `PRMBaseFragment.openPromotionDetail()` và `BaseRouter.canOpenVoucherDetail()`; **cả hai đều toast**
  (`Toast` / `PRMToast`) và toast này **luôn hiện, bỏ qua cổng bật/tắt toast chung** —
  `PromotionToastGate.showFeatureDisabled()` / `PromotionToast.showAlways()`, xem
  [ErrorHandling.md](../common/ErrorHandling.md).

> **SDK vừa tự gác, vừa cho host hỏi.** Mọi điểm vào vẫn tự gác và hiện thông báo PRM_MOB_021 khi bị
> chặn — kill-switch không phụ thuộc vào việc host có kiểm tra hay không. Ngoài ra, từ 2026-08-04 host
> **hỏi trước được** qua bốn hàm ở §3 để ẩn entry point của mình thay vì để user bấm rồi ăn toast.

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
| Xác nhận thanh toán | `PromotionIntegrateManager.confirmRedemption()` | qua facade `PromotionSDKApi.createRedemption` |
| Nạp cờ lúc init | `PromotionSDK.initialize` → `gate.refresh()` | `PromotionSDKImpl.init` → `gate.refresh()` |

Ngoài hai tầng trên còn **tầng thứ ba, tuỳ chọn**: host tự hỏi để ẩn entry point của chính mình — §3.
Nó **không** thay thế hai tầng kia; host bỏ qua thì kill-switch vẫn hoạt động đầy đủ.

`isSdkEnabled()` có người dùng — chính là `PromotionSDK.isSdkEnabled()` ở §3.

> **Lỗ đã vá (2026-08-06).** Trước đây tài liệu này ghi "`canApplyVoucher()`/`canRedeemVoucher()` chưa
> nơi nào gọi trực tiếp vì `PromotionUseCases` đã tự gác" — **sai**. Luồng checkout Android không đi qua
> facade đó: `PromotionIntegrateManager.confirmRedemption` gọi thẳng `CreateRedemptionSessionUseCase`,
> nên hai cờ `APPLY`/`REDEEM` **không có hiệu lực nào** trên Android, kể cả khi tắt `ENABLE_ALL` — đây
> là chỗ duy nhất công tắc tổng chặn không được. iOS không dính vì `PromotionSDKApi.createRedemption`
> đi qua facade. Nay `confirmRedemption` hỏi `canRedeemVoucher()` và `revalidateAndUpdate` hỏi
> `canApplyVoucher()`; cờ tắt → `onError("PRM_MOB_021")`, không gọi mạng.
>
> Thứ tự trong `confirmRedemption` quan trọng: gác nằm **sau** nhánh `discountDetails.isEmpty()`.
> Không có voucher nào thì đơn hàng không dính tới SDK — kill-switch tắt ưu đãi, không được tắt thanh
> toán của host. Có voucher rồi thì bắt buộc `onError`, vì giá ở `PRMEndowView` đang là giá đã giảm.

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

## 3. API cho host

Bốn hàm, đối xứng hai nền tảng (cùng tên, **cùng thứ tự** khai báo trong `PromotionSDK`):

| Android | iOS | Làm gì |
|---|---|---|
| `featureFlags(): PromotionFeatureFlagsSnapshot` | `featureFlags() -> PromotionFeatureFlagsSnapshot` | Chụp toàn bộ cờ từ cache, **đồng bộ** |
| `isFeatureEnabled(feature: PromotionFeature): Boolean` | `isFeatureEnabled(_ feature: PromotionFeature) -> Bool` | Tra một cờ |
| `isSdkEnabled(): Boolean` | `isSdkEnabled() -> Bool` | Công tắc tổng `ENABLE_ALL` |
| `refreshFeatureFlags(onComplete)` | `refreshFeatureFlags(completion:)` | Nạp lại từ server rồi trả snapshot mới trên **main thread** |

DTO public, cũng là **song ánh** hai file (sửa một bên thì sửa cả hai):

| Android `entry/api/` | iOS `Entry/API/` |
|---|---|
| `PromotionFeatureModels.kt` — `PromotionFeature`, `PromotionFeatureFlagsSnapshot` | `PromotionFeatureModels.swift` |
| `PromotionFeatureMapper.kt` (`internal`) | phần `MARK: - Feature flag` trong `PromotionSDKImpl.swift` |

Vì sao phải có DTO riêng thay vì trả thẳng `PromotionFeatureFlags` của lõi: host chỉ tích hợp
`AndroidPromotionSDK` / `PRM.framework`, không có type của `promotionLogic` trên compile classpath —
đúng lý do đã có `PromotionApiModels`. Xem [PublicApi.md](../common/PublicApi.md).

```kotlin
// Android — ẩn entry point trước khi user kịp bấm
PromotionSDK.refreshFeatureFlags { flags ->
    binding.btnMyVoucher.isVisible = flags.voucherList
    binding.groupPromotion.isVisible = flags.all
}
```

```swift
// iOS
PromotionSDK.refreshFeatureFlags { flags in
    self.myVoucherButton.isHidden = !flags.voucherList
}
```

Ba điều phải nhớ:

1. **Mọi field đã áp sẵn công tắc tổng.** `all == false` → mọi field còn lại `false`. Mapper đi qua
   `PromotionFeatureFlags.isEnabled(...)` chứ **không** đọc thẳng field lõi, vì luật `if (!enableAll)`
   chỉ nằm trong hàm đó — đọc thẳng field sẽ trả `voucherList = true` khi công tắc tổng đang tắt.

   `isFeatureEnabled(feature)` còn viết thẳng luật này ra ở bề mặt public — hai vế phải **song song
   đúng**: `isSdkEnabled() && gate.isEnabled(<cờ riêng>)`. `&&` đó **không đổi kết quả** (lõi đã áp
   `ENABLE_ALL` rồi) và **không phá fail-open** (chưa `initialize()` thì cả hai vế `true`); nó tồn tại
   để người đọc hàm host-facing thấy ngay "SDK tắt ⇒ tính năng tắt", khỏi lần vào lõi. Cả hai nền tảng
   viết giống nhau: `PromotionSDK.kt` (Android) / `PromotionSDKImpl.swift` (iOS).
   Đây là **ngoại lệ duy nhất** được phép lặp điều kiện `ENABLE_ALL`; các call site khác vẫn chỉ hỏi
   `PromotionFeatureGate.isEnabled(...)`, đừng nhân bản `isSdkEnabled() &&` đi khắp nơi.
2. **Fail-open, không ném.** Chưa `initialize()` / chưa có cache → trả bật hết. Không hàm nào ném lỗi
   hay dừng chương trình; cờ hỏng không được phép làm chết màn hình của host.
3. **Đây là tầng tuỳ chọn.** Host bỏ qua hoàn toàn thì SDK vẫn tự gác như cũ (§2).

**`onAvailabilityChanged(enabled:)` giờ bắn cả `true` lẫn `false`** — trước đây chỉ bắn `false` khi
user đã bấm và bị chặn, nên host ẩn entry point rồi thì không có đường hiện lại. Nay nó bắn ở:

| Lúc | Giá trị | Android | iOS |
|---|---|---|---|
| Nạp cờ xong sau `initialize` / login lại | `ENABLE_ALL` | `PromotionSDK.notifyAvailability()` | `PromotionSDKImpl.notifyAvailabilityAfterInitialLoad()` |
| Mỗi lần `refreshFeatureFlags` | `ENABLE_ALL` | `PromotionSDK.refreshFeatureFlags` | idem |
| Widget checkout đổi trạng thái | `VOUCHER_SELECTION` | `PRMEndowView.applyFeatureFlag()` | `PromotionSDKImpl.applyFlag` |
| User bấm mà bị chặn | luôn `false` | `openMyPromotion` / `openPromotionDetail` | idem |

> iOS phải đợi `wireCallbacks` chạy xong mới báo được lần đầu: `PromotionSDKImpl.init` bắn Task nạp cờ,
> nhưng `onAvailabilityUpdate` chỉ được nối **sau khi** `init` trả về. Vì thế Task được giữ lại ở
> `initialFlagLoad` và `PromotionSDK.initialize` `await` nó sau `wireCallbacks`. Android không vướng:
> `callback` được gán trước khi `launch`.

Test: `AndroidPromotionSDK/src/test/.../entry/api/PromotionFeatureMapperTest.kt` — ánh xạ enum ↔ hằng
số lõi, và khẳng định công tắc tổng tắt thì snapshot tắt hết.

---

## 4. Nguyên tắc dùng feature flag

- Luôn hỏi `PromotionFeatureGate`; **không** gọi thẳng `PromotionFeatureFlagUseCases()` từ tầng UI.
  Thêm màn mới thì thêm một hàm `canOpen…` vào gate, đừng lặp `isEnabled("PROMOTION.…")` tại chỗ.
- Giữ **fail-open**: thiếu cờ không được làm treo hay khoá tính năng.
- Đặt tên cờ tập trung ở `PromotionFeatureFlag` trong lõi Kotlin. Cả hai nền tảng đọc lại **chính
  hằng số đó** khi map từ enum public — `PromotionFeature.flagName()` (Android) /
  `PromotionSDKImpl.flagName(for:)` (iOS); không nơi nào định nghĩa lại chuỗi.
- **Thêm cờ mới thì sửa đủ 5 chỗ:** hằng số ở `PromotionFeatureFlag` + field ở `PromotionFeatureFlags`
  (lõi), rồi case ở `PromotionFeature` + field ở `PromotionFeatureFlagsSnapshot` + nhánh ở mapper
  (mỗi nền tảng). Thiếu một chỗ thì host không hỏi được cờ vừa thêm.

---

## 5. Việc còn treo

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
