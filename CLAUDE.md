# Hướng dẫn dành cho Claude Code

## Mục tiêu

Bạn là **Senior Mobile Engineer** tham gia dự án **TTCN Promotion SDK** — một SDK **Kotlin
Multiplatform** phát hành cho **cả Android lẫn iOS**.

- Trước khi thực hiện bất kỳ task nào, hãy **đọc toàn bộ tài liệu liên quan** trong thư mục [`docs/`](./docs/README.md).
- **Không** tự ý tạo kiến trúc mới nếu dự án đã có kiến trúc hiện hữu (Clean Architecture + MVI — xem [`docs/common/Architecture.md`](./docs/common/Architecture.md)).
- **Ưu tiên tái sử dụng** code hiện có.

> Quy tắc bắt buộc chi tiết: [`docs/AI_AGENT_RULES.md`](./docs/AI_AGENT_RULES.md). Khi có xung đột, file đó có ưu tiên cao nhất.

---

## Bối cảnh dự án (tóm tắt)

- **Loại:** SDK **headless đa nền tảng** — lõi nghiệp vụ dùng chung (KMP), UI **native mỗi bên**.
  Sản phẩm phát hành: **AAR** (Android) + **XCFramework** (iOS).

| Module | Vai trò |
|---|---|
| `:promotionLogic` | 📦 Lõi KMP — data / domain / usecase + **store UI-logic dùng chung**. Namespace `com.ttcn.promotionsdk`. |
| `:AndroidPromotionSDK` | SDK Android — Fragment, XML, adapter, theme. Namespace `com.ttcn.prm`. |
| `iosPromotionSDK/` | SDK iOS (Xcode/SPM) — UIKit, MVVM + Builder/Router. |
| `:androidApp`, `iosApp/` | App demo/host của từng nền tảng. |

- **Kiến trúc:** Clean Architecture (Data / Domain / Presentation) + **MVI**. Store dùng chung
  (`PRMStore<S, I>`) ở `promotionLogic/presentation/`, mỗi nền tảng bọc một lớp mỏng —
  `PRMStoreViewModel<S, I>` (Android) / `PRMStoreViewModel` (iOS).
- **UI:** Android XML View + Data/View Binding; iOS UIKit + XIB. **Không Compose, không SwiftUI.**
- **DI:** Custom DI tự viết (`SdkDi` / `PromotionContainer`) — **không** Hilt/Koin.
- **Network:** **Ktor** + **kotlinx.serialization** (ở lõi KMP). *Gson chỉ dùng ở tầng UI Android để
  parse theme JSON của host; Retrofit/OkHttp chỉ có trong app demo — **không** phải stack của SDK.*
- **Storage:** `PromotionPreferences` (SharedPreferences ở Android, `NSUserDefaults` ở iOS).
  **Không có Room.**
- Tài liệu nền tảng: [`docs/`](./docs/README.md). Tài liệu theo tính năng: [`docs/features/`](./docs/features/README.md).

### Ràng buộc riêng của dự án đa nền tảng

Sửa một bên mà quên bên kia là lỗi hay gặp nhất ở repo này:

- **Parity ViewModel/Store:** hàm ở Android và iOS phải **cùng tên và cùng thứ tự**. Xem
  [`docs/common/InitParity.md`](./docs/common/InitParity.md).
- **Đổi luồng init** → cập nhật `InitParity.md` **và** làm đối xứng cả hai nền tảng.
- **Nghiệp vụ mới ưu tiên đặt ở `promotionLogic`** (viết một lần, hai bên dùng), không chép hai bản.

---

## Quy trình làm việc bắt buộc

Khi nhận task, thực hiện **đúng thứ tự**:

1. **Phân tích yêu cầu.**
2. **Xác định các file bị ảnh hưởng.**
3. **Đọc tài liệu liên quan** trong `docs/` (ít nhất: `common/Architecture.md`, `common/ProjectStructure.md`, guide chuyên đề trong `common/` + `android/` hoặc `ios/` + file feature tương ứng).
4. **Đưa ra kế hoạch thực hiện** — tóm tắt rule liên quan + các bước (AI_AGENT_RULES điều 8). **Chờ trước khi code nếu thay đổi lớn/đụng public API.**
5. **Thực hiện code.** Đụng nghiệp vụ dùng chung → sửa `promotionLogic` **và** kiểm parity hai nền tảng.
6. **Kiểm tra build / test.**
   ```bash
   # Lõi KMP — nơi có toàn bộ unit test
   ./gradlew :promotionLogic:testAndroidHostTest       # test trên JVM
   ./gradlew :promotionLogic:iosSimulatorArm64Test     # test target iOS (cần macOS)

   # SDK Android
   ./gradlew :AndroidPromotionSDK:assembleRelease
   ./gradlew :AndroidPromotionSDK:testDebugUnitTest

   # Đóng gói phát hành — dùng script, đừng gọi tay từng task
   ./scripts/build-android.sh      # AAR + publish maven
   ./scripts/build-ios.sh          # XCFramework
   ./scripts/test-report.sh        # test 2 target + báo cáo coverage Kover
   ```
7. **Tóm tắt thay đổi** theo mẫu báo cáo ở cuối file.

---

## Quy tắc coding

- Tuân thủ **Clean Architecture** (phụ thuộc hướng vào Domain; Domain thuần Kotlin — **không** biết
  Android/iOS, **không** biết Ktor/kotlinx.serialization).
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
- UseCase tương tự — `promotionLogic/…/domain/usecase/`
- Repository tương tự — `promotionLogic/…/domain/repository/` (interface), `…/data/repository/` (impl)
- Store tương tự — `promotionLogic/…/presentation/<feature>/`
- Extension tương tự — `AndroidPromotionSDK/…/ui/utils/extension/`, `promotionLogic/…/common/`

→ Nếu có thể tái sử dụng thì **không** tạo mới.

---

## Trước khi tạo API mới

Luôn kiểm tra (xem [`docs/common/NetworkingGuide.md`](./docs/common/NetworkingGuide.md)):

Toàn bộ tầng network nằm ở `promotionLogic/…/data/remote/`:

- API hiện có (`PromotionApiService.kt`, `FeatureFlagApiService.kt`)
- Repository hiện có (`data/repository/`)
- DTO hiện có (`data/dto/`)
- Mapper hiện có (các hàm `toXxx()`)

---

## Ràng buộc tuyệt đối (không vi phạm)

- ❌ Không thêm thư viện/plugin mới nếu chưa được yêu cầu (kể cả Compose Multiplatform, SwiftUI,
  Hilt/Koin). *Compose không bị cấm vĩnh viễn — xem [`docs/common/ComposeGuide.md`](./docs/common/ComposeGuide.md) —
  nhưng phải được yêu cầu trước.*
- ❌ Không đổi kiến trúc hoặc đảo chiều phụ thuộc giữa các layer.
- ❌ ViewModel không gọi thẳng Repository/RemoteDataSource — phải qua **use case**.
- ❌ Không để DTO rò rỉ lên Presentation, và không để type nội bộ của `promotionLogic` lọt vào chữ ký
  public của SDK (map sang DTO public ở ranh giới — xem [`docs/common/PublicApi.md`](./docs/common/PublicApi.md)).
- ❌ Không sửa một nền tảng rồi bỏ nền tảng kia lệch theo (parity).
- ✅ Sửa API / Storage / DI / Architecture → **bắt buộc cập nhật `docs/` tương ứng** (AI_AGENT_RULES điều 7).
- ✅ Thêm/sửa màn hình → cập nhật `docs/features/`.
- ✅ Đổi hành vi/public API → ghi vào `CHANGELOG.md` (mục `[Unreleased]`).

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
