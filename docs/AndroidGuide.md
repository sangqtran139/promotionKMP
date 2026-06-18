# AndroidGuide — Quy ước Android chung

Quy ước nền tảng Android cho module SDK `vds-promotion`. Đây là **thư viện**, nên ưu tiên an toàn khi nhúng vào host app.

---

## 1. Cấu hình nền tảng

| Mục | Giá trị |
|-----|---------|
| Loại module SDK | `com.android.library` |
| Namespace | `com.ttcn.promotionsdk` |
| `minSdk` | 24 |
| `compileSdk` | 35 |
| JVM toolchain | 17 |
| Ngôn ngữ | Kotlin 2.2.0 |
| Build features | `dataBinding = true`, `viewBinding = true` |
| Plugin | android-library, kotlin-android, kotlin-kapt, kotlin-parcelize |

> Vì `minSdk = 24`, không dùng API yêu cầu min cao hơn nếu không có guard (`Build.VERSION.SDK_INT`).

---

## 2. Vòng đời Android & SDK

- SDK khởi tạo qua `PromotionSDK.init(context, options)` (xem `ui/entry/PromotionSDK.kt`).
  - Lưu theme, callback; gọi `PromotionContainer.init(...)` để dựng DI; nạp UI DI một lần (`ensureUiDiLoaded`).
  - Luôn dùng `context.applicationContext` để tránh leak Activity (DI đăng ký `context.applicationContext`).
- Hai chế độ:
  - **UI mode**: host mở Fragment của SDK (vd `MyPromotionFragment`).
  - **Headless mode**: host gọi `PromotionSDK.useCases` (không nhúng UI).
- `PromotionSDK.useCases` chỉ dùng **sau** `init()`.

---

## 3. Activity / Fragment

- Kế thừa base có sẵn: `PRMBaseActivity`, `PRMBaseFragment`. **Không** tạo base mới khi base hiện tại đủ dùng.
- ViewModel lấy qua `PromotionViewModelFactory` / `ViewModelModule` (DI), không `new` thủ công.
- Quan sát state/effect trong vòng đời an toàn (`viewLifecycleOwner` + `repeatOnLifecycle(STARTED)`).
- Fragment chỉ render state và gửi `Action` về ViewModel; **không** chứa business logic.

---

## 4. Resource & UI

- UI dùng **XML View + Data Binding + View Binding** (chi tiết `XMLViewGuide.md`).
- Kích thước responsive dùng `sdp-android` (`@dimen/_8sdp`…); tránh hardcode.
- Ảnh dùng **Glide**; hiệu ứng loading dùng **Shimmer**; bo góc/shape dùng **ShapeOfView**.
- Theme của SDK cấu hình qua `PromotionSDKTheme` / `PromotionThemeRegistry` (`ui/theme/`), cho phép host tùy biến màu/brand. Chi tiết token, applier và **quy tắc thứ tự cấu hình**: xem [Theming.md](./Theming.md).

---

## 5. Logging

- Dùng **Timber** cho log nội bộ SDK. Không dùng `Log.d` rải rác, không log dữ liệu nhạy cảm (token, thông tin khách hàng).
- HTTP log (OkHttp `HttpLoggingInterceptor`) chỉ bật mức `BODY` khi app ở chế độ debuggable (xem `NetworkModule` / `RetrofitClient`).

---

## 6. Quy tắc dành cho SDK (quan trọng)

- **Tránh phụ thuộc nặng & trùng**: dùng lại thư viện đã có trong version catalog; không thêm dependency mới (AI_AGENT_RULES điều 4).
- **Không giữ tham chiếu Activity/Context tĩnh** gây leak.
- **Public API tối thiểu**: chỉ expose qua `ui/entry/`. Mọi thay đổi public API là breaking — cân nhắc kỹ và cập nhật docs.
- **ProGuard**: SDK có `consumer-rules.pro`; nếu thêm class cần giữ (reflection, Gson model) → bổ sung consumer rules.
- **Đa luồng**: coroutines cho tác vụ IO; UI cập nhật trên main thread.

---

## 7. Permission & Manifest

- Hạn chế tối đa permission trong manifest của SDK; chỉ khai báo thứ thật sự cần (vd INTERNET cho networking).
- Không yêu cầu host cấp quyền nhạy cảm trừ khi tính năng bắt buộc — và phải ghi rõ trong tài liệu tích hợp (`INTEGRATION.md`).
