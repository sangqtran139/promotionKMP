# AI_AGENT_RULES — Quy tắc bắt buộc cho AI Agent

Luật làm việc bắt buộc cho mọi AI agent (và cả người) khi đóng góp code vào **TTCN Promotion SDK (KMP)**.
Mục tiêu: giữ kiến trúc nhất quán giữa lõi dùng chung và hai UI native, tránh phá vỡ SDK, tránh rác code.

> Đây là tài liệu có mức ưu tiên **cao nhất** trong `/docs`. Khi có xung đột, tuân theo file này.

> **Lịch sử:** bản trước của file này được sao chép nguyên xi từ repo `ttcn-promotion-android-sdk` và
> mô tả một project Android thuần XML/Retrofit/Room. Nó cấm thêm thư viện, cấm Compose và cấm đổi
> kiến trúc — ba điều mà việc chuyển sang KMP bắt buộc phải làm. Bản này viết lại cho đúng repo hiện tại.

---

## 10 quy tắc cốt lõi

### 1. Luôn đọc `/docs` trước khi làm task
Đọc ít nhất `Architecture.md`, `ProjectStructure.md`, và guide chuyên đề tương ứng
(`NetworkingGuide.md`, `DependencyInjection.md`, `StorageGuide.md`, `AndroidUIGuide.md`, `IosUIGuide.md`).

### 2. Luôn kiểm chứng bằng source code, đừng tin tài liệu một cách mù quáng
Tài liệu có thể lỗi thời. Trước khi kết luận "X không được dùng" hay "Y là stub", **mở file ra đọc**
và `grep` toàn repo. Nếu source mâu thuẫn với docs → **sửa docs** (điều 8), đừng sửa code cho khớp docs.

### 3. Luôn kiểm tra source hiện tại trước khi thêm file mới
- Tìm xem class/hàm/component đã tồn tại chưa.
- Không tạo file mới nếu đã có file cùng vai trò.
- Tôn trọng quy ước đặt tên và vị trí package hiện có.

### 4. Giữ đúng ranh giới "lõi dùng chung / UI riêng nền tảng"
- Business logic (data / domain / use case) **chỉ** nằm ở `:promotionLogic`, `commonMain`.
- `commonMain` **không** được import `android.*`, `platform.*`, hay bất kỳ API riêng nền tảng nào.
  Cần API nền tảng → dùng `expect`/`actual` (xem `SdkLock`, `PromotionPreferences`).
- UI **không** gọi thẳng Repository/DataSource; luôn đi qua use case.
- Domain **không** biết DTO. Data map DTO ↔ domain model trước khi trả lên.

### 5. Không tự ý đổi kiến trúc
- Giữ Clean Architecture (Data / Domain / Presentation).
- Android UI giữ **MVI** (`PRMBaseViewModel<S, A, E>`); iOS UI giữ **MVVM + Builder/Router + RxSwift**.
- Không hợp nhất hai mô hình UI, không đổi sang mô hình khác, trừ khi có yêu cầu rõ ràng.

### 6. Thêm thư viện phải có lý do và được yêu cầu
- Không thêm dependency/plugin mới vào `libs.versions.toml` khi chưa được yêu cầu rõ ràng.
- Ưu tiên `expect`/`actual` tự viết cho nhu cầu nhỏ thay vì kéo thêm thư viện
  (ví dụ `PromotionPreferences` thay cho `multiplatform-settings`).
- **Không** thêm Hilt/Koin/Dagger — đã có Custom DI.
- **Không** thêm annotation processor (kapt/KSP) vào `:promotionLogic` — sẽ vỡ target iOS.

### 7. Không duplicate code
Trước khi viết logic mới, kiểm tra `core/util`, các mapper, use case đã có. Tách phần dùng chung
thành hàm/extension/use case thay vì copy-paste.

### 8. Sửa API, storage, DI hoặc kiến trúc thì phải cập nhật docs trong cùng thay đổi
- API / networking → `NetworkingGuide.md` + `HeadlessAPI.md`
- Storage / cache → `StorageGuide.md`
- DI / module đăng ký → `DependencyInjection.md`
- Kiến trúc / luồng dữ liệu → `Architecture.md` (+ `ProjectStructure.md` nếu đổi cấu trúc thư mục)

### 9. Trước khi code phải tóm tắt rule liên quan và đưa kế hoạch
Mỗi task, **trước khi sửa code**, trình bày ngắn gọn:
1. **Tóm tắt rule liên quan** — những điều trong `/docs` ảnh hưởng tới task.
2. **Kế hoạch triển khai** — các bước, file dự kiến sửa/thêm, lý do không vi phạm rule.
3. **Hỏi ý kiến người phụ trách** trước khi làm thay đổi lớn (đổi public API, thêm thư viện, đổi kiến trúc).

### 10. Android và iOS phải đồng bộ về logic và kiến trúc

Hai nền tảng là **hai mặt của cùng một SDK**, không phải hai sản phẩm riêng. Bất cứ thứ gì host nhìn
thấy hoặc chi phối hành vi phải **song ánh** giữa Android và iOS:

- **Logic nghiệp vụ**: chỉ ở `:promotionLogic` (rule 4). Không viết lại bằng Kotlin-riêng-Android hay
  Swift. Kill-switch, gác cờ, chuẩn hoá lỗi, persistence… nằm ở lõi dùng chung.
- **Bề mặt công khai & model**: `PromotionSDKApi`, DTO, `PromotionSDKTheme` + token, `PromotionThemeJson`,
  `PromotionThemeDefaults`, `PromotionThemeDisplay`, error code, giá trị mặc định (kể cả **màu**) —
  phải **cùng tên type, cùng tên field/hàm, cùng thứ tự, cùng cách document, cùng cấu trúc thư mục**.
- **Sửa một bên = sửa bên kia trong cùng thay đổi.** Thêm field vào token → thêm cả hai + DTO JSON +
  test. Đổi tên hàm → đổi cả hai.
- **Khác biệt chỉ được phép khi *cố hữu* do nền tảng**, và **phải ghi rõ lý do trong docs**. Ví dụ đã
  chấp nhận: Android SDK là `object` (theme toàn cục) vs iOS là instance (`sdk.configure`); Android cần
  `Context` cho màu resource; registry đặt ở CoreUI bên iOS vì component VDS đọc nó; applier pattern chỉ
  có ở Android. Không được lấy "khác nền tảng" làm cớ cho lệch **tuỳ tiện** (tên, thứ tự, giá trị default).
- Kiến trúc UI giữ riêng theo rule 5 (MVI vs MVVM) — đó là cố hữu, đã ghi.

> Kiểm bằng cách diff tên type/field/hàm hai bên (xem `PublicApi.md` và `Theming.md`). Lệch tên hoặc
> lệch giá trị default = bug, không phải "đặc thù nền tảng".

---

## Checklist trước khi bắt đầu (Pre-flight)

- [ ] Đã đọc docs liên quan (điều 1).
- [ ] Đã đọc source để xác minh docs còn đúng (điều 2).
- [ ] Đã `grep`/duyệt source để xác nhận chưa tồn tại thứ cần thêm (điều 3).
- [ ] Thay đổi không phá ranh giới lõi/UI (điều 4) và không đổi kiến trúc (điều 5).
- [ ] Không thêm dependency mới ngoài yêu cầu (điều 6).
- [ ] Đã xác định component/use case tái sử dụng được (điều 7).
- [ ] Đã trình bày tóm tắt rule + kế hoạch và hỏi ý kiến (điều 9).

## Checklist trước khi kết thúc (Pre-commit)

- [ ] Code tuân thủ `CodingStandards.md`.
- [ ] Lỗi được xử lý theo `ErrorHandling.md`.
- [ ] Đã cập nhật docs nếu chạm tới API/storage/DI/architecture (điều 8).
- [ ] Không còn code trùng lặp.
- [ ] **Build và test xanh trên cả hai nền tảng**:
      `./gradlew :promotionLogic:testAndroidHostTest :promotionLogic:iosSimulatorArm64Test`
      và `./gradlew :promotionLogic:assemble`.
- [ ] Logic mới có test trong `commonTest` (chạy trên cả Android lẫn iOS).

---

## Điều cấm tuyệt đối

- ❌ Import `android.*` hoặc `platform.*` trong `commonMain`.
- ❌ Thêm kapt/KSP hoặc annotation processor vào `:promotionLogic`.
- ❌ Thêm Hilt/Koin hoặc DI framework khác.
- ❌ Để Domain layer phụ thuộc vào Ktor, kotlinx.serialization, hoặc DTO của Data layer.
- ❌ ViewModel gọi thẳng `RemoteDataSource`/`Repository` bỏ qua use case.
- ❌ Đổi public API (`PromotionContainer`, `PromotionUseCases`, `PromotionFeatureFlagUseCases`,
      `PromotionSDKConfig`) mà không cập nhật docs + ghi chú breaking change.
- ❌ Đặt chuỗi hiển thị (tiếng Việt) trong `:promotionLogic` — copy thuộc về tầng UI.

---

## Về Compose Multiplatform

Compose Multiplatform **được phép trong tương lai** và là mục tiêu mở rộng đã thống nhất, nhưng
**chưa dùng ở thời điểm này**. Xem `ComposeGuide.md`. UI hiện tại là native mỗi nền tảng:
XML View trên Android, UIKit trên iOS.
