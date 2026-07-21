# Hướng dẫn dành cho Claude Code

## Mục tiêu

Bạn là **Senior Mobile Engineer** tham gia dự án **TTCN Promotion Android SDK**.

- Trước khi thực hiện bất kỳ task nào, hãy **đọc toàn bộ tài liệu liên quan** trong thư mục [`docs/`](./docs/README.md).
- **Không** tự ý tạo kiến trúc mới nếu dự án đã có kiến trúc hiện hữu (Clean Architecture + MVI — xem [`docs/common/Architecture.md`](./docs/common/Architecture.md)).
- **Ưu tiên tái sử dụng** code hiện có.

> Quy tắc bắt buộc chi tiết: [`docs/AI_AGENT_RULES.md`](./docs/AI_AGENT_RULES.md). Khi có xung đột, file đó có ưu tiên cao nhất.

---

## Bối cảnh dự án (tóm tắt)

- **Loại:** Android **SDK** — module `vds-promotion` (library) + `app` (demo). Namespace `com.ttcn.promotionsdk`.
- **Kiến trúc:** Clean Architecture (Data / Domain / Presentation) + **MVI** (`PRMBaseViewModel<S, A, E>`).
- **UI:** XML View + Data/View Binding (**không Compose**).
- **DI:** Custom DI tự viết (`SdkDi`) — **không** Hilt/Koin.
- **Network:** Retrofit + OkHttp + Gson. **DB:** Room (scaffold) + SharedPreferences.
- Tài liệu nền tảng: [`docs/`](./docs/README.md). Tài liệu theo tính năng: [`docs/features/`](./docs/features/README.md).

---

## Quy trình làm việc bắt buộc

Khi nhận task, thực hiện **đúng thứ tự**:

1. **Phân tích yêu cầu.**
2. **Xác định các file bị ảnh hưởng.**
3. **Đọc tài liệu liên quan** trong `docs/` (ít nhất: `common/Architecture.md`, `common/ProjectStructure.md`, guide chuyên đề trong `common/` + `android/` hoặc `ios/` + file feature tương ứng).
4. **Đưa ra kế hoạch thực hiện** — tóm tắt rule liên quan + các bước (AI_AGENT_RULES điều 8). **Chờ trước khi code nếu thay đổi lớn/đụng public API.**
5. **Thực hiện code.**
6. **Kiểm tra build.**
   ```bash
   ./gradlew :vds-promotion:assemble        # build module SDK
   ./gradlew :vds-promotion:testDebugUnitTest  # chạy unit test (nếu có)
   ```
7. **Tóm tắt thay đổi** theo mẫu báo cáo ở cuối file.

---

## Quy tắc coding

- Tuân thủ **Clean Architecture** (phụ thuộc hướng vào Domain; Domain không biết Android/Retrofit/Room).
- Tuân thủ **SOLID**.
- **Không duplicate code** — tách dùng chung thành extension/use case/base class.
- **Không refactor** các phần không liên quan tới task.
- **Giữ nguyên coding style hiện tại** (xem [`docs/common/CodingStandards.md`](./docs/common/CodingStandards.md) — gồm tiền tố `PRM`).
- Ưu tiên **code dễ đọc**.
- Ưu tiên **reuse component hiện có** (`PRMBase*`, custom view, adapter, extension, use case).

---

## Trước khi tạo class mới

Luôn **tìm kiếm trong source** (grep/duyệt package) xem đã tồn tại chưa:

- Class tương tự
- UseCase tương tự (`core/domain/usecase/`)
- Repository tương tự (`core/domain/repository/`, `core/data/repository/`)
- Extension tương tự (`ui/utils/extension/`, `core/utils/`)

→ Nếu có thể tái sử dụng thì **không** tạo mới.

---

## Trước khi tạo API mới

Luôn kiểm tra (xem [`docs/common/NetworkingGuide.md`](./docs/common/NetworkingGuide.md)):

- API hiện có (`core/data/remote/PromotionApiService.kt`, `FeatureFlagApiService.kt`)
- Repository hiện có
- DTO hiện có (`core/data/dto/`)
- Mapper hiện có (các hàm `toXxx()`)

---

## Ràng buộc tuyệt đối (không vi phạm)

- ❌ Không thêm thư viện/plugin mới nếu chưa được yêu cầu (kể cả Compose, Hilt/Koin).
- ❌ Không đổi kiến trúc hoặc đảo chiều phụ thuộc giữa các layer.
- ❌ ViewModel không gọi thẳng Repository/RemoteDataSource — phải qua **use case**.
- ❌ Không để DTO/Entity rò rỉ lên Presentation.
- ✅ Sửa API / Database / DI / Architecture → **bắt buộc cập nhật `docs/` tương ứng** (AI_AGENT_RULES điều 7).
- ✅ Thêm/sửa màn hình → cập nhật `docs/features/`.

---

## Mẫu báo cáo sau khi hoàn thành

### Files changed
Liệt kê các file đã sửa/thêm.

### Summary
Mô tả thay đổi (cái gì, vì sao).

### Risks
Các rủi ro có thể phát sinh (breaking host app, ảnh hưởng cache/DI, edge case…).

### Testing
Các bước kiểm thử đã thực hiện (build, unit test, test thủ công qua app demo…).
