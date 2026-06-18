# AI_AGENT_RULES — Quy tắc bắt buộc cho AI Agent

Tài liệu này quy định **luật làm việc bắt buộc** cho mọi AI agent (và cả người) khi đóng góp code vào
**TTCN Promotion Android SDK**. Mục tiêu: giữ kiến trúc nhất quán, tránh phá vỡ SDK và tránh rác code.

> Đây là tài liệu có mức ưu tiên **cao nhất** trong `/docs`. Khi có xung đột, tuân theo file này.

---

## 8 quy tắc cốt lõi

### 1. Luôn đọc `/docs` trước khi làm task
Trước khi viết hoặc sửa bất kỳ dòng code nào, đọc các tài liệu liên quan trong `/docs`:
ít nhất là `Architecture.md`, `ProjectStructure.md`, và guide chuyên đề tương ứng với task
(Networking, DI, Database, XMLView…).

### 2. Luôn kiểm tra source code hiện tại trước khi thêm file mới
- Tìm xem class/hàm/component cần dùng **đã tồn tại chưa** (`grep`, xem package tương ứng).
- Không tạo file mới nếu đã có file đảm nhiệm cùng vai trò.
- Tôn trọng quy ước đặt tên và vị trí package hiện có.

### 3. Không tự ý đổi kiến trúc
- Giữ nguyên Clean Architecture (Data / Domain / Presentation) và mô hình **MVI**.
- Không đổi sang mô hình khác (MVVM thuần, MVP, MVC…), không gộp/đảo chiều phụ thuộc giữa các layer.
- Domain **không** được phụ thuộc Android framework hay tầng Data cụ thể.

### 4. Không thêm thư viện mới nếu chưa được yêu cầu
- Không thêm dependency/plugin mới vào `libs.versions.toml` hoặc `build.gradle.kts` khi chưa được yêu cầu rõ ràng.
- Đặc biệt: **không** thêm Hilt/Koin (đã có Custom DI), **không** thêm Jetpack Compose (project dùng XML View).
- Ưu tiên dùng thư viện đã có trong version catalog.

### 5. Không duplicate code
- Trước khi viết logic mới, kiểm tra `core/utils`, `ui/utils`, các `extension`, base class (`PRM*`).
- Tách phần dùng chung thành hàm/extension/use case thay vì copy-paste.

### 6. Ưu tiên tái sử dụng component có sẵn
- Dùng lại `PRMBaseActivity` / `PRMBaseFragment` / `PRMBaseViewModel`, adapter, custom view, extension đã có.
- Dùng lại use case trong `core/domain/usecase` thay vì gọi thẳng repository/data source từ ViewModel.

### 7. Nếu sửa API, database, DI hoặc architecture thì phải cập nhật docs
Khi thay đổi một trong các phần sau, **bắt buộc** cập nhật file docs tương ứng trong cùng thay đổi:
- API / networking → `NetworkingGuide.md`
- Database / cache → `DatabaseGuide.md`
- DI / module đăng ký → `DependencyInjection.md`
- Kiến trúc / luồng dữ liệu → `Architecture.md` (và `ProjectStructure.md` nếu đổi cấu trúc thư mục)

### 8. Trước khi code phải tóm tắt rule liên quan và đưa kế hoạch triển khai
Mỗi task, **trước khi sửa code**, agent phải trình bày ngắn gọn:
1. **Tóm tắt rule liên quan** — những điều trong `/docs` ảnh hưởng tới task.
2. **Kế hoạch triển khai** — các bước, file dự kiến sửa/thêm, và lý do không vi phạm các rule trên.

---

## Checklist trước khi bắt đầu (Pre-flight)

- [ ] Đã đọc docs liên quan (điều 1).
- [ ] Đã `grep`/duyệt source để xác nhận chưa tồn tại thứ cần thêm (điều 2).
- [ ] Không phát sinh thay đổi kiến trúc (điều 3).
- [ ] Không thêm dependency mới ngoài yêu cầu (điều 4).
- [ ] Đã xác định component/use case tái sử dụng được (điều 5, 6).
- [ ] Đã trình bày tóm tắt rule + kế hoạch (điều 8).

## Checklist trước khi kết thúc (Pre-commit)

- [ ] Code tuân thủ `CodingStandards.md`.
- [ ] Lỗi được xử lý theo `ErrorHandling.md`.
- [ ] Đã cập nhật docs nếu chạm tới API/DB/DI/architecture (điều 7).
- [ ] Không còn code trùng lặp; tái dùng tối đa component có sẵn.
- [ ] Build pass (`./gradlew :vds-promotion:assemble`).

---

## Điều cấm tuyệt đối

- ❌ Thêm Jetpack Compose vào module SDK.
- ❌ Thêm Hilt/Koin hoặc framework DI khác.
- ❌ Để Domain layer phụ thuộc vào Android SDK hoặc Retrofit/Room trực tiếp.
- ❌ ViewModel gọi thẳng `RemoteDataSource`/`Repository` bỏ qua use case.
- ❌ Đổi public API của SDK (package `ui.entry`, `useCases`) mà không cập nhật docs + ghi chú breaking change.
