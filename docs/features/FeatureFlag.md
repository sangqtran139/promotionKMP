# Feature: Feature Flag

Cơ chế **bật/tắt tính năng** của SDK theo cấu hình từ xa, lấy từ REST API.

- **Package:** `ui/feature/featureflag`
- **Thành phần:** `FeatureFlagViewModel`, `FeatureFlagUIState`, `FeatureFlagUIAction`
- **Hỗ trợ (Domain/Data):** `FetchFeatureFlagsUseCase`, `IsFeatureEnabledUseCase`, `GetFeatureFlagsUseCase`, `GetPromotionFeatureFlagsUseCase`, `FeatureFlagRepository(Impl)`, `FeatureFlagApiService`, `FeatureFlagRemoteDataSource`, `FeatureFlagDao`, `core/di/FeatureFlagModule`, `core/domain/exception/FeatureFlagException`

---

## 1. Trạng thái hiện tại

| Thành phần | Trạng thái |
|------------|-----------|
| `FeatureFlagUIState` | 🟡 `sealed class` — **khung**, chưa định nghĩa biến thể |
| `FeatureFlagUIAction` | 🟡 `sealed class` — **khung** |
| `FeatureFlagViewModel` | 🟡 Khung — chưa implement |
| `FeatureFlagDao` | 🟡 `interface` — **khung** (xem `../DatabaseGuide.md`) |
| `FeatureFlagApiService` | ✅ Hoàn thiện — endpoint `POST /feature-flag/list` |
| `FeatureFlagRemoteDataSource` | ✅ Hoàn thiện — xử lý lỗi qua `FeatureFlagException` / `NetworkException` |
| `FeatureFlagRepositoryImpl` | ✅ Hoàn thiện — cache in-memory `PromotionFeatureFlags`, fetch ngay khi khởi tạo |
| Use case | ✅ `FetchFeatureFlagsUseCase`, `IsFeatureEnabledUseCase`, `GetFeatureFlagsUseCase`, `GetPromotionFeatureFlagsUseCase` |

> ⚠️ Khi triển khai ViewModel/UI thật, hoàn thiện State/Action theo chuẩn MVI và **cập nhật file này**.

---

## 2. Kiến trúc luồng dữ liệu

```
PromotionContainer.init()
  └─ RepositoryModule đăng ký FeatureFlagRepositoryImpl
       └─ CoroutineScope(IO).launch { repo.fetchFlags() }  ← fetch ngầm ngay lúc init
            └─ FeatureFlagRemoteDataSource.getFeatureFlags()
                 └─ POST /feature-flag/list
            ← List<FeatureFlagItemResponse>
       ← toPromotionFeatureFlags() → cache vào PromotionFeatureFlags

PromotionSDK.featureFlags  ← đọc đồng bộ từ cache
  └─ GetPromotionFeatureFlagsUseCase()
       └─ FeatureFlagRepository.getPromotionFeatureFlags()
```

---

## 3. API endpoint

| Field | Giá trị |
|-------|---------|
| Method | `POST` |
| Path | `promotion/promotion-vtm-bff/api/v1/vtm/feature-flag/list` |
| Body | `{}` |
| Response | `{"status":200,"code":"SUCCESS","data":[{"flagName":"PROMOTION.VOUCHER_DETAIL","enabled":true},...]}` |

---

## 4. Cờ hiện tại (`PromotionFeatureFlag`)

| Hằng số | flagName API |
|---------|-------------|
| `ENABLE_ALL` | `PROMOTION.ENABLE_ALL` |
| `VOUCHER_APPLY` | `PROMOTION.VOUCHER_APPLY` |
| `VOUCHER_REDEEM` | `PROMOTION.VOUCHER_REDEEM` |
| `VOUCHER_SELECTION` | `PROMOTION.VOUCHER_SELECTION` |
| `VOUCHER_DETAIL` | `PROMOTION.VOUCHER_DETAIL` |
| `VOUCHER_LIST` | `PROMOTION.VOUCHER_LIST` |

`ENABLE_ALL` là cờ cha — nếu tắt thì tất cả cờ con đều bị tắt (`PromotionFeatureFlags.isEnabled()`).

---

## 5. Xử lý lỗi & fallback

- Lỗi fetch (mất mạng, server lỗi) → `runCatching` trong `fetchFlags()` hấp thụ toàn bộ.
- Mặc định khi chưa fetch xong hoặc fetch thất bại: **tất cả flag = `false`** (fail-safe).
- Không crash, không ảnh hưởng luồng chính của SDK.

---

## 6. Nguyên tắc dùng feature flag

- Gọi `IsFeatureEnabledUseCase` để bật/tắt nhánh tính năng; **không** rải cờ hardcode trong UI.
- Có giá trị **mặc định an toàn** khi chưa lấy được cờ (fail-safe), không để crash/treo nếu thiếu cờ.
- Đặt khoá cờ tập trung trong `PromotionFeatureFlag` (hằng số), tránh chuỗi key trùng lặp.
