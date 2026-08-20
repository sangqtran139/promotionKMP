# Features — Tài liệu theo tính năng

Mỗi tính năng có **một nghiệp vụ dùng chung** ở `:promotionLogic` và **hai hiện thực UI** —
Android (MVI, XML View) và iOS (MVVM + callback thuần, UIKit).

> Đọc trước: [Architecture.md](../common/Architecture.md) và [HeadlessAPI.md](../common/HeadlessAPI.md).

---

## 1. Ánh xạ màn hình Android ↔ iOS ↔ use case

Tên class đã được **đồng nhất giữa hai nền tảng** — iOS đổi theo tên Android.

| Tính năng | Màn Android | Màn iOS | Use case dùng |
|---|---|---|---|
| Ưu đãi của tôi | `MyPromotionFragment` | `MyPromotionViewController` | `searchVouchers` |
| Chọn ưu đãi (checkout) | `ChoosePromotionFragment` | `ChoosePromotionViewController` | `findEligible`, `validateDiscounts`, `createRedemption` |
| Chi tiết ưu đãi | `PromotionDetailFragment` | `PromotionDetailViewController` | `getVoucherDetail` |
| Tìm kiếm ưu đãi | `SearchMyPromotionFragment` | `SearchMyPromotionViewController` | `searchVouchers` (kèm `keyword`) |
| Widget nhúng | `PRMEndowView` | `PRMEndowView` | `findEligible` |
| Entry point | `PromotionSDK` | `PromotionSDK` | — |
| Callback host | `PromotionSDKCallback` | `PromotionSDKCallback` | — |
| Theme | `PromotionSDKTheme` | `PromotionSDKTheme` | — |
| Feature flag | `FeatureFlagViewModel` | `BaseRouter+FeatureFlag` | `featureFlags.*` |

> Bản iOS cũ tên `VDSPromotion`, `SelectPromotionViewController`, `SelectPromtionView` (thiếu chữ `o`).
> Đổi tên là **breaking change** với app host iOS — cần bật major version và báo đối tác.

> **Lệch nghiệp vụ cần biết:** `findEligible` (Find Eligible Campaigns) trước đây **chỉ có ở iOS**.
> Android dùng `searchVouchers` cho luồng chọn ưu đãi. Lõi KMP nay có cả hai — xem
> [HeadlessAPI.md §3](../common/HeadlessAPI.md) để chọn đúng hàm:
> `searchVouchers` = voucher **đã sở hữu**, không xét đơn.
> `findEligible` = ưu đãi **đủ điều kiện cho đơn**, trả hai nhóm `myOffers` + `otherOffers`.

---

## 2. Tài liệu chi tiết

| Tính năng | Tài liệu |
|---|---|
| Ưu đãi của tôi | [MyPromotion.md](./MyPromotion.md) |
| Chọn ưu đãi | [ChoosePromotion.md](./ChoosePromotion.md) |
| Chi tiết ưu đãi | [PromotionDetail.md](./PromotionDetail.md) |
| Tìm kiếm | [SearchMyPromotion.md](./SearchMyPromotion.md) |
| Widget nhúng | [EndowView.md](./EndowView.md) |
| Feature flag | [FeatureFlag.md](./FeatureFlag.md) |

> ⚠️ **Sáu file trên hiện mô tả kỹ phía Android**; phần triển khai iOS tương ứng **đã có trong repo**
> tại `iosPromotionSDK/PromotionSDKUI/` (không còn ở repo `ttcn-promotion-ios-sdk` riêng nữa). Bảng ánh xạ
> class iOS ↔ Android ở §1 là điểm tra cứu chính; xem thêm [ios/UIGuide.md](../ios/UIGuide.md) cho pattern
> Builder/Router/ViewModel/ViewController. Khi cập nhật từng file feature, bổ sung mục iOS ngay bên cạnh mục Android.
>
> `FeatureFlag.md` còn ghi *"đang ở dạng scaffold"* — **không còn đúng**. FeatureFlag đã là code thật
> ở cả hai nền tảng; xem [StorageGuide.md](../common/StorageGuide.md) và [HeadlessAPI.md §4](../common/HeadlessAPI.md).

---

## 3. Cấu trúc chuẩn của một feature

Một feature nằm ở **ba nơi**. Logic ở nơi thứ nhất, hai nơi còn lại chỉ render.

### Lõi dùng chung (`:promotionLogic`) — nơi đặt logic

```
presentation/<feature>/
├── XxxContract.kt        # data class XxxState + sealed interface XxxIntent
│                         #   + model hiển thị (XxxVoucher, XxxTab…) + mapper toXxx()
└── XxxStore.kt           # PRMStore<XxxState, XxxIntent> — gọi use case, phát State/Effect
```

### Android (`:AndroidPromotionSDK`)

```
ui/feature/<feature>/          # phẳng, KHÔNG có cấp <nhóm>
├── XxxFragment.kt         # render(state) + thu Effect + dispatch(Intent) thẳng lên store
├── XxxViewModel.kt        # lớp con 3 dòng của PRMStoreViewModel<XxxState, XxxIntent>
├── XxxUiModels.kt         # model thuần trình bày của riêng Android (nếu cần)
└── adapter/               # ListAdapter + DiffUtil (nếu có danh sách)
```

### iOS (`iosPromotionSDK`)

```
PromotionSDKUI/<Feature>/
├── XxxBuilder.swift          # lắp ráp VC + VM + Router
├── XxxRouter.swift           # điều hướng
├── XxxViewModel.swift        # lớp con của PRMStoreViewModel
├── XxxViewController.swift   # setupUI() + bindViewModel()
└── XxxViewController.xib     # cùng tên class
```

Hai tầng UI **không** chứa business logic, và cũng **không** gọi use case trực tiếp — chúng
`dispatch` Intent vào store dùng chung rồi render State nhận về.

> 📌 **Không** dựng lại `XxxUiState` / `XxxAction` riêng cho mỗi nền tảng. Bản trước có, chép gần 1-1
> `State`/`Intent` của store ở bốn file, và đã bị bỏ — xem KDoc `PRMStoreViewModel`.

---

## 4. Khi thêm feature mới

1. Nghiệp vụ trước: use case + repository + DTO ở `:promotionLogic`, kèm test `commonTest`.
2. **Store + Contract** ở `promotionLogic/…/presentation/<feature>/`, kèm test `commonTest`.
3. UI Android: Fragment + lớp con `PRMStoreViewModel`, đăng ký ở
   `ui/di/PromotionViewModelFactory.kt` (`promotionViewModelFactory()`).
4. UI iOS theo khuôn Builder/Router/ViewModel/ViewController — **cùng tên và cùng thứ tự hàm** với
   Android (parity, xem [`../common/InitParity.md`](../common/InitParity.md)).
5. Thêm một file tài liệu vào thư mục này + cập nhật bảng ở §1 và §2
   (AI_AGENT_RULES điều 3 & 8).
6. Đổi hành vi/public API → ghi vào `CHANGELOG.md`.
