# Feature: Feature Flag

Cơ chế **bật/tắt tính năng** của SDK theo cấu hình từ xa. Hiện đang ở **dạng scaffold** (khung), chưa hoàn thiện.

- **Package:** `ui/feature/featureflag`
- **Thành phần:** `FeatureFlagViewModel`, `FeatureFlagUIState`, `FeatureFlagUIAction`
- **Hỗ trợ (Domain/Data):** `IsFeatureEnabledUseCase`, `GetFeatureFlagsUseCase`, `FeatureFlagRepository(Impl)`, `FeatureFlagApiService`, `FeatureFlagDao`, `core/di/FeatureFlagModule`, `core/domain/exception/FeatureFlagException`

---

## 1. Trạng thái hiện tại

| Thành phần | Trạng thái |
|------------|-----------|
| `FeatureFlagUIState` | 🟡 `sealed class FeatureFlagUIState` — **khung**, chưa định nghĩa biến thể |
| `FeatureFlagUIAction` | 🟡 `sealed class FeatureFlagUIAction` — **khung** |
| `FeatureFlagViewModel` | 🟡 Khung |
| `FeatureFlagDao` | 🟡 `interface FeatureFlagDao` — **khung** (xem `../DatabaseGuide.md`) |
| UseCase / Repository / ApiService | ✅ Có khung Domain/Data để triển khai |

> ⚠️ Khi triển khai thật, hoàn thiện State/Action theo chuẩn MVI và **cập nhật file này**.

---

## 2. Định hướng triển khai (khi được yêu cầu)

- **State**: chuyển `sealed class FeatureFlagUIState` thành các biến thể rõ ràng (vd `Loading`, `Loaded(flags)`, `Error(code)`), hoặc dùng `data class` nếu phù hợp khuôn MVI hiện tại.
- **Use case**:
  - `GetFeatureFlagsUseCase` — lấy toàn bộ cờ (mạng + cache qua `FeatureFlagRepository`).
  - `IsFeatureEnabledUseCase` — kiểm tra một cờ cụ thể đang bật/tắt.
- **Cache**: cờ lưu cục bộ (Room `FeatureFlagDao` hoặc SharedPreferences) — quyết định chiến lược cache ở Repository (xem `../DatabaseGuide.md`).
- **DI**: binding khai báo trong `core/di/FeatureFlagModule`.
- **Lỗi**: dùng `FeatureFlagException` + mã trong `ErrorCodes` (xem `../ErrorHandling.md`).

---

## 3. Nguyên tắc dùng feature flag

- Gọi `IsFeatureEnabledUseCase` để bật/tắt nhánh tính năng; **không** rải cờ hardcode trong UI.
- Có giá trị **mặc định an toàn** khi chưa lấy được cờ (fail-safe), không để crash/treo nếu thiếu cờ.
- Đặt khoá cờ tập trung (hằng số), tránh chuỗi key trùng lặp.
