# Features — Tài liệu theo tính năng

Thư mục này mô tả từng **feature** trong module `vds-promotion`
(`ui/feature/...`). Mỗi feature tuân theo **MVI** (State / Action / Effect) trên nền `PRMBaseViewModel`.

> Đọc trước: [../Architecture.md](../Architecture.md) (luồng MVI) và [../ProjectStructure.md](../ProjectStructure.md).

---

## Danh sách feature

| Feature | Package | Tài liệu | Vai trò |
|---------|---------|----------|---------|
| My Promotion | `ui/feature/promotion/mypromotion` | [MyPromotion.md](./MyPromotion.md) | Danh sách voucher của khách (tab, phân trang, tìm kiếm) |
| Choose Promotion | `ui/feature/promotion/choosepromotion` | [ChoosePromotion.md](./ChoosePromotion.md) | Chọn + validate + áp dụng voucher (stackable discount) |
| Promotion Detail | `ui/feature/promotion/promotiondetail` | [PromotionDetail.md](./PromotionDetail.md) | Chi tiết voucher + tab nội dung |
| Search My Promotion | `ui/feature/promotion/searchmypromotion` | [SearchMyPromotion.md](./SearchMyPromotion.md) | Tìm kiếm voucher của khách |
| Endow View | `ui/feature/promotion/endowview` | [EndowView.md](./EndowView.md) | Custom View + `PromotionIntegrateManager` để nhúng vào màn thanh toán của host |
| Feature Flag | `ui/feature/featureflag` | [FeatureFlag.md](./FeatureFlag.md) | Bật/tắt tính năng (đang ở dạng scaffold) |

---

## Cấu trúc chuẩn của một feature

```
ui/feature/<nhóm>/<feature>/
├── XxxContract.kt        # data class XxxUiState + sealed XxxAction + sealed XxxEffect
├── XxxViewModel.kt       # PRMBaseViewModel<State, Action, Effect>, handleAction(...)
├── XxxFragment.kt        # render(state) + handleEffect(effect) + gửi Action
└── adapter/              # (nếu có danh sách) ListAdapter + DiffUtil
```

Khi thêm feature mới: tạo theo đúng khuôn trên, đăng ký ViewModel ở `ui/di/ViewModelModule.kt`,
và **thêm một file tài liệu** vào thư mục này + cập nhật bảng trên (AI_AGENT_RULES điều 2 & 7).
