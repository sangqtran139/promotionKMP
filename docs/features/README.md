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
> tại `iosPromotionUI/PromotionSDKUI/` (không còn ở repo `ttcn-promotion-ios-sdk` riêng nữa). Bảng ánh xạ
> class iOS ↔ Android ở §1 là điểm tra cứu chính; xem thêm [ios/UIGuide.md](../ios/UIGuide.md) cho pattern
> Builder/Router/ViewModel/ViewController. Khi cập nhật từng file feature, bổ sung mục iOS ngay bên cạnh mục Android.
>
> `FeatureFlag.md` còn ghi *"đang ở dạng scaffold"* — **không còn đúng**. FeatureFlag đã là code thật
> ở cả hai nền tảng; xem [StorageGuide.md](../common/StorageGuide.md) và [HeadlessAPI.md §4](../common/HeadlessAPI.md).

---

## 3. Cấu trúc chuẩn của một feature

### Android (`:promotionUI`)

```
feature/<nhóm>/<feature>/
├── XxxContract.kt        # data class XxxUiState + sealed XxxAction + sealed XxxEffect
├── XxxViewModel.kt       # PRMBaseViewModel<State, Action, Effect>, handleAction(...)
├── XxxFragment.kt        # render(state) + handleEffect(effect) + gửi Action
└── adapter/              # ListAdapter + DiffUtil (nếu có danh sách)
```

### iOS (`promotionUI`)

```
<Feature>/
├── XxxBuilder.swift          # lắp ráp VC + VM + Router
├── XxxRouter.swift           # điều hướng
├── XxxViewModel.swift        # ViewModelType: transform(input:) -> Output
├── XxxViewController.swift   # setupUI() + bindViewModel()
└── XxxViewController.xib     # cùng tên class
```

Cả hai đều **không** chứa business logic — chúng gọi use case của `:promotionLogic`.

---

## 4. Khi thêm feature mới

1. Nghiệp vụ trước: use case + repository + DTO ở `:promotionLogic`, kèm test `commonTest`.
2. UI Android theo khuôn MVI, đăng ký ViewModel ở `ViewModelModule`.
3. UI iOS theo khuôn Builder/Router/ViewModel/ViewController.
4. Thêm một file tài liệu vào thư mục này + cập nhật bảng ở §1 và §2
   (AI_AGENT_RULES điều 3 & 8).
