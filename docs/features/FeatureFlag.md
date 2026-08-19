# Feature: Feature Flag

Cơ chế **bật/tắt tính năng** của SDK theo cấu hình từ xa — kill-switch, không phải A/B test.

- **Lõi (`:promotionLogic`):** `PromotionFeatureFlagUseCases()` — `refresh()`, `isEnabled(name)`,
  `flagsOf(names)`, `all()`. Dựng thẳng sau `PromotionContainer.initialize(...)`.
- **Hỗ trợ:** `FetchFeatureFlagsUseCase`, `IsFeatureEnabledUseCase`, `GetFeatureFlagsUseCase`,
  `GetPromotionFeatureFlagsUseCase`, `FeatureFlagRepository(Impl)`, `FeatureFlagApiService`,
  `FeatureFlagLocalDataSource`, `FeatureFlagException` (binding DI nằm rải ở `NetworkModule` /
  `LocalModule` / `RepositoryModule` / `UseCaseModule` — xem DependencyInjection.md §5)
- **Gate (dùng chung):** `PromotionFeatureGate` trong `domain/usecase/` — **một object Kotlin duy
  nhất** cho cả hai nền tảng. Android gọi `PromotionFeatureGate.canOpenVoucherDetail()`, iOS gọi
  `PromotionFeatureGate.shared.canOpenVoucherDetail()`.
- **Tên tính năng:** nguồn sự thật là `PromotionFeatureFlag` (hằng chuỗi Kotlin). Host **không** gõ
  chuỗi đó — bề mặt public dùng enum `PromotionFeature` ở mỗi nền tảng (xem §3).
- **Phần riêng mỗi nền tảng:** chỉ còn *chỗ gọi* hiển thị thông báo khi bị chặn —
  `PRMBaseFragment.openPromotionDetail()` và `BaseRouter.canOpenVoucherDetail()`; **cả hai đều toast**
  (`Toast` / `PRMToast`) và toast này **luôn hiện, bỏ qua cổng bật/tắt toast chung** —
  `PRMBaseConfirmDialog.showFeatureDisabled()` / `PromotionSDKImpl.showFeatureDisabledToast(on:)` (đều là **popup**, toast đã bỏ), xem
  [ErrorHandling.md](../common/ErrorHandling.md).

> **SDK vừa tự gác, vừa cho host hỏi.** Mọi điểm vào vẫn tự gác và hiện thông báo PRM_MOB_021 khi bị
> chặn — kill-switch không phụ thuộc vào việc host có kiểm tra hay không. Ngoài ra host **hỏi trước
> được** qua bốn hàm ở §3 để ẩn entry point của mình thay vì để user bấm rồi ăn toast.

---

## 1. Các cờ

`PromotionFeatureFlag`: `ENABLE_ALL`, `VOUCHER_LIST`, `VOUCHER_DETAIL`, `VOUCHER_SELECTION`,
`VOUCHER_APPLY`, `VOUCHER_REDEEM`.

Ba hành vi cần nhớ:

1. **`ENABLE_ALL` là công tắc tổng.** Tắt nó thì mọi cờ con đều tắt, bất kể giá trị riêng
   (`if (!enableAll) return false` trong `PromotionFeatureFlags.isEnabled`).
2. **Fail-open.** Chưa có cache → bật hết. SDK không tự khoá tính năng khi chưa gọi được API lần nào.
3. **`refresh()` không ném lỗi.** Gọi API thất bại thì giữ nguyên cờ đang cache.

Instance `PromotionFeatureFlags` **thoát ra khỏi repository đã được chuẩn hoá** bằng
`normalized()` (`FeatureFlagRepositoryImpl.getPromotionFeatureFlags`), nên đọc thẳng field cũng đúng
luật: `all().voucherList == isEnabled(VOUCHER_LIST)` cho cả sáu cờ. Riêng bản đem **lưu cache** vẫn là
giá trị thô — bật lại `ENABLE_ALL` thì các cờ con phải trở về giá trị riêng, không được kẹt `false`.

Cờ ghi xuống `PromotionPreferences` (SharedPreferences / NSUserDefaults) nên lần mở app sau không phải
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
| Xác nhận thanh toán | `EndowStore.confirmRedemption()` — dùng chung 2 nền tảng | (như Android) |
| Nạp cờ lúc init | `PromotionSDK.initialize` → `gate.refresh()` | `PromotionSDKImpl.init` → `gate.refresh()` |

Ngoài hai tầng trên còn **tầng thứ ba, tuỳ chọn**: host tự hỏi để ẩn entry point của chính mình — §3.
Nó **không** thay thế hai tầng kia; host bỏ qua thì kill-switch vẫn hoạt động đầy đủ.

`isSdkEnabled()` có người dùng — chính là `PromotionSDK.isSdkEnabled()` ở §3.

`EndowStore.confirmRedemption` hỏi `canRedeemVoucher()`, `revalidateAfterBudgetError` hỏi
`canApplyVoucher()`; cờ tắt → `onError("PRM_MOB_021")`, không gọi mạng. Gác nằm **sau** nhánh
`discountDetails.isEmpty()`: đơn không có voucher nào thì `onSuccess` chạy bất kể cờ.

> **Nhánh thanh toán KHÔNG hiện popup PRM_MOB_021**, khác mọi điểm gác còn lại. Ở các điểm kia user
> vừa bấm để MỞ một tính năng, im lặng thì màn hình đứng im vô lý nên SDK tự hiện thông báo. Còn
> `confirmRedemption` nằm giữa luồng thanh toán của **host** — chen popup của SDK vào là cướp quyền
> điều khiển, trong khi host mới là bên biết phải dừng hay đi tiếp và hiện gì. Cờ tắt vẫn không gọi
> API và vẫn trả `PRM_MOB_021` ra `onError`; chỉ bỏ phần hiển thị.

Ánh xạ cờ ↔ hàm (giống hệt hai nền tảng):

| Hàm | Cờ |
|---|---|
| `searchVouchers` | `VOUCHER_LIST` |
| `getVoucherDetail` | `VOUCHER_DETAIL` |
| `findEligible` | `VOUCHER_SELECTION` |
| `validateDiscounts` | `VOUCHER_APPLY` |
| `createRedemption` | `VOUCHER_REDEEM` |

Mã lỗi chung: `PromotionErrorCodes.FEATURE_DISABLED` = `"PRM_MOB_021"`.

Thêm màn mới thì phải gác ở tầng UI — facade không thấy được điều hướng.

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

1. **Mọi field đã áp sẵn công tắc tổng.** `all == false` → mọi field còn lại `false`. Luật chốt ở
   **hai lớp**: repository trả bản `normalized()` (§1), và mapper đi qua
   `PromotionFeatureFlags.isEnabled(...)` thay vì đọc field thô.

   `isFeatureEnabled(feature)` viết thẳng luật ra ở bề mặt public: `isSdkEnabled() &&
   gate.isEnabled(<cờ riêng>)` — không đổi kết quả và không phá fail-open. Cả hai nền tảng viết giống
   nhau: `PromotionSDK.kt` (Android) / `PromotionSDKImpl.swift` (iOS). Đây là **ngoại lệ duy nhất**
   được phép lặp điều kiện `ENABLE_ALL`; các call site khác chỉ hỏi
   `PromotionFeatureGate.isEnabled(...)`.
2. **Fail-open, không ném.** Chưa `initialize()` / chưa có cache → trả bật hết. Không hàm nào ném lỗi
   hay dừng chương trình; cờ hỏng không được phép làm chết màn hình của host.
3. **Đây là tầng tuỳ chọn.** Host bỏ qua hoàn toàn thì SDK vẫn tự gác như cũ (§2).

**`onAvailabilityChanged` đã bị bỏ** khỏi `PromotionSDKCallback` — host không cần biết trạng thái
bật/tắt để làm gì, SDK đã tự gác ở mọi điểm vào.

Trước đây nó bắn ở bốn chỗ (nạp cờ xong sau `initialize`, mỗi lần `refreshFeatureFlags`, widget
checkout đổi trạng thái, và khi user bấm mà bị chặn) — kèm một cái bẫy thứ tự chỉ iOS mới có: phải
đợi `wireCallbacks` chạy xong mới báo được lần đầu, nên `PromotionSDKImpl` phải giữ Task nạp cờ lại ở
`initialFlagLoad`. Bỏ callback là bỏ luôn cả lớp phức tạp đó.

Host muốn tự ẩn entry point thì hỏi chủ động bằng `isSdkEnabled()` / `isFeatureEnabled(...)` mỗi khi
dựng UI (§3) — đừng cache lại, cờ đổi được giữa phiên. Còn khi user đã bấm mà bị chặn thì SDK tự hiện
popup, hoặc gọi `onFeatureDisabled` nếu host truyền vào hàm `open…`.

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
