# ComposeGuide — Compose Multiplatform

## Trạng thái hiện tại: chưa dùng, nhưng đã sẵn sàng

UI của Promotion SDK hiện là **native mỗi nền tảng**: XML View trên Android
([AndroidUIGuide.md](../android/UIGuide.md)), UIKit trên iOS ([IosUIGuide.md](../ios/UIGuide.md)).

Compose Multiplatform **được phép áp dụng trong tương lai** — đây là mục tiêu mở rộng đã thống nhất,
không phải điều cấm.

> Bản trước của file này (sao chép từ SDK Android) ghi *"Không tự ý thêm Jetpack Compose… vi phạm
> AI_AGENT_RULES điều 3 và 4"*. Điều đó đúng với repo Android cũ, **sai** với repo này.

---

## Vì sao nền tảng đã sẵn sàng

Việc tách `:promotionLogic` thành lõi headless chính là điều kiện cần cho Compose Multiplatform:

- Business logic đã ở `commonMain`, không phụ thuộc Android/iOS.
- `PromotionUseCases` trả `PromotionResult` — không ném exception, dễ map sang state.
- Không có chuỗi hiển thị trong lõi, nên UI nào cũng dựng được copy của mình.
- Project đã có sẵn module `sharedUI` cấu hình Compose Multiplatform (`org.jetbrains.compose`,
  `compose-material3`, `lifecycle-viewmodel-compose`).

Một màn hình Compose Multiplatform chỉ cần `implementation(projects.promotionLogic)`.

---

## Điều kiện áp dụng

Trước khi viết màn hình Compose Multiplatform đầu tiên:

1. **Được người phụ trách phê duyệt rõ ràng** (AI_AGENT_RULES điều 9).
2. Cập nhật `Architecture.md`, `ProjectStructure.md` và file này.
3. Cân nhắc tác động tới host app: kích thước SDK, xung đột version Compose với host,
   interop `AndroidView` ↔ Compose cho màn hình XML còn lại.
4. Quyết định phạm vi: làm **một màn hình mẫu** trước (đề xuất: `MyPromotion`), chạy được trên cả hai
   nền tảng, rồi mới nhân rộng. Không big-bang.

---

## Khung quy tắc dự kiến

> Phần dưới là **định hướng**, chưa áp dụng.

- Đặt `@Composable` theo feature; tách `Screen` (stateless) và `Route/Host` (kết nối ViewModel).
- Giữ hướng dữ liệu một chiều: Composable nhận `state: State`, phát `(Action) -> Unit`;
  thu `Effect` qua `LaunchedEffect`.
- ViewModel dùng `androidx.lifecycle.ViewModel` bản multiplatform
  (`lifecycle-viewmodel-compose` đã có trong version catalog).
- Theme Compose phải map từ token của `PromotionSDKTheme` để giữ khả năng
  tùy biến brand của host. Xem [Theming.md](./Theming.md).
- Ảnh dùng Coil 3 (multiplatform) thay cho Glide.
- Dùng interop (`AndroidView` / `ComposeView`, `UIKitView`) khi cần sống chung với màn hình native.

---

## Cái gì sẽ biến mất

Khi một màn hình chuyển sang Compose Multiplatform, các thư viện Android-only sau không còn dùng cho
màn đó: `sdp-android`, `shapeofview`, `shimmer` (Facebook), `material`, `recyclerview`,
`constraintlayout`, `glide`. Không có bản KMP cho chúng — phải thay bằng Composable tương đương.

---

## Tóm tắt

| Câu hỏi | Trả lời |
|---------|---------|
| Project có dùng Compose Multiplatform không? | **Chưa.** |
| UI hiện làm bằng gì? | XML View (Android) + UIKit (iOS). |
| Có được thêm Compose Multiplatform không? | **Có**, sau khi được phê duyệt và cập nhật docs. |
| Cần đổi gì ở `:promotionLogic`? | Không. Lõi đã sẵn sàng. |
