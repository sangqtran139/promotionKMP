# Promotion SDK — Hướng dẫn tích hợp

Tài liệu tích hợp đã chuyển hẳn vào [`docs/`](./docs/README.md). File này chỉ còn là trang điện để
link cũ không chết.

| Bạn cần | Đọc |
|---|---|
| Tích hợp **Android** (dependency, khởi tạo, mở màn, widget checkout) | [`docs/AndroidIntegrationGuide.md`](./docs/AndroidIntegrationGuide.md) |
| Tích hợp **iOS** | [`docs/IosIntegrationGuide.md`](./docs/IosIntegrationGuide.md) |
| Toàn bộ bề mặt public + song ánh Android↔iOS | [`docs/common/PublicApi.md`](./docs/common/PublicApi.md) |
| **Headless** — tự dựng UI, gọi API qua `PromotionSDK.api` | [`docs/common/HeadlessAPI.md`](./docs/common/HeadlessAPI.md) |
| Feature flag / kill-switch | [`docs/features/FeatureFlag.md`](./docs/features/FeatureFlag.md) |
| Widget checkout `PRMEndowView` + `confirmRedemption` | [`docs/features/EndowView.md`](./docs/features/EndowView.md) |
| Mã lỗi & cách hiển thị | [`docs/common/ErrorHandling.md`](./docs/common/ErrorHandling.md) |
| Theme | [`docs/common/Theming.md`](./docs/common/Theming.md) |
| Phát hành | [`docs/android/Distribution.md`](./docs/android/Distribution.md) · [`docs/ios/Distribution.md`](./docs/ios/Distribution.md) |

---

> **Vì sao rút gọn (2026-08-06).** Bản cũ ~460 dòng mô tả một API đã không còn: `PromotionSDK.useCases`
> (nay là `PromotionSDK.api` trả `PromotionApiResult`), `PromotionSDKOptions(config = PromotionSDKConfig(…))`
> (nay là `session = PromotionSessionConfig(…)`; `PromotionSDKConfig` thuộc `promotionLogic`, không nằm
> trên compile classpath của host), callback `onVoucherApplied(List<AppliedDiscount>)` / `onError` /
> `onSDKClosed` (nay là 6 sự kiện khác), và AAR `vds-promotion` (nay là Maven
> `vn.viettelpay.library:promotion`). Nó cũng không hề nhắc feature flag hay `openPromotionDetail`.
>
> Giữ hai bản hướng dẫn tích hợp song song là lý do khiến bản này trôi khỏi code mà không ai nhận ra —
> nên từ nay **chỉ sửa `docs/`**, đừng viết lại nội dung vào file này.
