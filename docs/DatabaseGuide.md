# DatabaseGuide — Quy tắc Database & lưu trữ cục bộ

Lưu trữ cục bộ của SDK nằm ở **Data layer** (`core/data/local/`). Project khai báo **Room** và dùng
**SharedPreferences** cho cache nhẹ.

---

## 1. Trạng thái hiện tại

| Thành phần | File | Trạng thái |
|------------|------|-----------|
| Room runtime/ktx/compiler | `gradle/libs.versions.toml` (room 2.6.1) | ✅ Đã khai báo dependency, build với **kapt** |
| `PromotionDatabase` | `core/data/local/PromotionDatabase.kt` | 🟡 **Scaffold** (chưa khai báo `@Database`/entity) |
| `PromotionCacheDao`, `FeatureFlagDao` | `core/data/local/` | 🟡 **Scaffold** (chưa có `@Dao`/truy vấn) |
| `SharedPrefStorage` | `core/data/local/SharedPrefStorage.kt` | 🟡 **Scaffold** (sẵn sàng triển khai cache key-value) |

> ⚠️ Hiện các class local **đang ở dạng khung**. Khi triển khai thật, tuân theo quy tắc bên dưới
> và **cập nhật file này** (AI_AGENT_RULES điều 7).

---

## 2. Khi nào dùng Room, khi nào dùng SharedPreferences

| Nhu cầu | Dùng |
|---------|------|
| Cache dữ liệu **có cấu trúc, nhiều bản ghi, truy vấn** (danh sách voucher, feature flag chi tiết) | **Room** |
| Lưu **giá trị nhỏ, key-value** (cờ đơn giản, timestamp, session id, flag đã xem) | **SharedPreferences** (`SharedPrefStorage`) |
| Dữ liệu nhạy cảm (token) | Cân nhắc mã hoá; không lưu plain text nếu là dữ liệu bí mật |

---

## 3. Quy tắc Room

1. **Entity** đặt trong `core/data/local/` (vd `entity/`), tách biệt DTO mạng và domain model.
   - DTO (mạng) ≠ Entity (DB) ≠ Domain model. Map giữa các tầng, không tái dùng chéo bừa bãi.
2. **DAO** là `@Dao` interface; hàm truy vấn `suspend` (hoặc trả `Flow` cho luồng dữ liệu liên tục).
3. **Database** kế thừa `RoomDatabase`, khai báo `@Database(entities = [...], version = N)`; tạo qua
   `Room.databaseBuilder(context.applicationContext, ...)`.
4. **Đăng ký qua DI**: cung cấp `PromotionDatabase` và các `Dao` bằng `single { }` trong module local
   (tạo `LocalModule`/đặt trong module phù hợp), dùng `applicationContext`.
5. **Migration**: tăng `version` và cung cấp `Migration` tường minh khi đổi schema. **Không** dùng
   `fallbackToDestructiveMigration` ở môi trường production trừ khi chấp nhận mất cache.
6. **Truy cập DB không chạy main thread**: luôn qua coroutine (`suspend`/`Flow`), không block UI.
7. Room compiler dùng **kapt** (đã cấu hình). Không đổi sang KSP nếu chưa được yêu cầu (điều 4).

---

## 4. Quy tắc SharedPreferences (`SharedPrefStorage`)

- Tập trung mọi truy cập SharedPreferences qua `SharedPrefStorage`; **không** rải `getSharedPreferences()` khắp nơi.
- Đặt tên file pref có tiền tố SDK (vd `prm_promotion_prefs`) để tránh đụng host app.
- Key khai báo dưới dạng hằng số `const val`; không hardcode chuỗi key rải rác.
- Dùng `applicationContext`. Ghi giá trị nhỏ; dữ liệu lớn/cấu trúc → dùng Room.

---

## 5. Vị trí trong kiến trúc

```
Repository (data)
   ├─ RemoteDataSource (Retrofit)   ← nguồn mạng
   └─ Local (Room Dao / SharedPrefStorage)   ← cache cục bộ
        → map Entity/pref → Domain model
```

- **Repository** quyết định chiến lược cache (mạng trước/cache trước, đồng bộ…), không để ViewModel tự xử lý cache.
- Map **Entity → Domain model** trước khi trả lên Domain (không để Entity rò rỉ lên UI).
- Domain layer **không** import `androidx.room.*`.

---

## 6. Khi triển khai/đổi database — bắt buộc

- Cập nhật entity/dao/database + binding DI + migration.
- **Cập nhật file này**: trạng thái, danh sách entity, version, chiến lược cache.
- Không thêm thư viện DB khác (SQLDelight, ObjectBox…) — project đã chọn **Room** (AI_AGENT_RULES điều 4).
