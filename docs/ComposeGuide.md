# ComposeGuide — Jetpack Compose

## ⚠️ Trạng thái hiện tại: KHÔNG sử dụng Jetpack Compose

TTCN Promotion SDK xây dựng UI hoàn toàn bằng **XML View + Data Binding + View Binding**.
Tại thời điểm viết tài liệu, **module `vds-promotion` không khai báo và không dùng Jetpack Compose**.

- `build.gradle.kts` của `vds-promotion` bật `dataBinding` và `viewBinding`, **không** bật `buildFeatures.compose`.
- Không có dependency Compose nào trong `gradle/libs.versions.toml`.
- Hướng dẫn UI chính thức cho project là **[XMLViewGuide.md](./XMLViewGuide.md)**.

> File này tồn tại để ghi rõ **quyết định kiến trúc** và **quy tắc liên quan đến Compose**, tránh việc vô tình
> đưa Compose vào SDK.

---

## Quy tắc về Compose

1. **Không tự ý thêm Jetpack Compose** vào module `vds-promotion`.
   - Việc thêm Compose là **đổi kiến trúc + thêm thư viện mới** → vi phạm AI_AGENT_RULES điều 3 và điều 4.
2. Mọi UI mới của SDK phải làm bằng **XML View** theo `XMLViewGuide.md`.
3. Nếu trong tương lai có yêu cầu **chính thức** chuyển/bổ sung Compose:
   - Phải được người phụ trách project phê duyệt rõ ràng.
   - Phải cập nhật `Architecture.md`, `AndroidGuide.md`, và viết lại file này thành hướng dẫn Compose đầy đủ.
   - Cân nhắc tác động tới host app (kích thước SDK, xung đột version, interop View ↔ Compose).

---

## Nếu (trong tương lai) Compose được phê duyệt — khung quy tắc dự kiến

> Phần dưới chỉ là **định hướng**, **chưa áp dụng**. Không viết code Compose cho tới khi được phê duyệt.

- Đặt `@Composable` theo từng feature, tách `Screen` (stateless) và `Route/Host` (kết nối ViewModel).
- Vẫn giữ **MVI**: Composable nhận `state: State`, phát `(Action) -> Unit`; thu `Effect` qua `LaunchedEffect`.
- Tái dùng `PRMBaseViewModel<S, A, E>` (StateFlow/SharedFlow đã sẵn sàng cho Compose `collectAsStateWithLifecycle`).
- Theme Compose phải map từ `PromotionSDKTheme` để giữ khả năng tùy biến brand của host.
- Dùng interop (`AndroidView` / `ComposeView`) khi cần sống chung với màn hình XML hiện có.

---

## Tóm tắt

| Câu hỏi | Trả lời |
|---------|---------|
| Project có dùng Compose không? | **Không.** |
| UI làm bằng gì? | XML View + Data/View Binding — xem `XMLViewGuide.md`. |
| Được thêm Compose không? | Không, trừ khi có yêu cầu/phê duyệt chính thức. |
