# TestingGuide — Quy ước Testing

Hướng dẫn viết test cho TTCN Promotion SDK. Kiến trúc Clean + MVI giúp phần lớn logic test được ở mức **unit**
mà không cần thiết bị/UI.

---

## 1. Phân tầng test (ưu tiên từ trên xuống)

| Loại | Vị trí | Test gì |
|------|--------|---------|
| **Unit test** (JVM) | `vds-promotion/src/test/` | UseCase, Repository (mock data source), mapping DTO↔domain, reducer/`handleAction` của ViewModel |
| **Instrumentation test** | `vds-promotion/src/androidTest/` | Room DAO thật, custom view, luồng UI quan trọng |
| **Tích hợp qua app demo** | `app/` | Smoke test tích hợp SDK end-to-end (mock API có sẵn trong `app/mock/`) |

> Ưu tiên unit test vì Domain thuần Kotlin, không phụ thuộc Android.

---

## 2. Trọng tâm cần test

1. **UseCase** — logic nghiệp vụ, mapping request → DTO (vd `PromotionUseCases.toDto()`), xử lý kết quả/null.
2. **Repository** — đúng gọi data source và **map DTO → domain** (mock `RemoteDataSource`).
3. **Mapping** — các hàm `toVoucherDetail()`, `toMyVoucherListItem()`… (đầu vào null/thiếu field → giá trị mặc định đúng).
4. **ViewModel (MVI)** — gửi `Action` → kiểm tra `uiState` thay đổi đúng và `uiEffect` phát đúng.
5. **Xử lý lỗi** — `onError` tắt loading + phát `Effect.ShowError` với mã đúng (xem `ErrorHandling.md`).

---

## 3. Test ViewModel MVI

Vì `PRMBaseViewModel` dùng `StateFlow`/`SharedFlow` + `viewModelScope`:

- Đặt `Dispatchers.Main` test (vd `Dispatchers.setMain(testDispatcher)`), dùng coroutines-test (`runTest`).
- Mô hình: **Given** state đầu → **When** `handleAction(...)` → **Then** assert `uiState.value` và effect thu được.
- Inject **fake/mock use case** vào ViewModel (constructor injection), không gọi mạng thật.

```
Given: state mặc định
When : viewModel.handleAction(Action.Refresh)
Then : uiState.value.isLoading == true (lúc bắt đầu)
       sau khi use case trả về → vouchers cập nhật, isLoading == false
```

---

## 4. Test Repository / Networking

- **Mock `PromotionRemoteDataSource`** để test `PromotionRepositoryImpl` (kiểm tra gọi đúng tham số + map đúng).
- Nếu cần test tầng Retrofit thật: dùng `MockWebServer` (OkHttp) — **chỉ thêm dependency test khi được yêu cầu**.
- App demo đã có sẵn cơ chế **mock API** (`app/mock/promotion/`) cho test tích hợp thủ công.

---

## 5. Test Room (khi triển khai DB)

- Test DAO bằng **instrumentation test** với `Room.inMemoryDatabaseBuilder(...)`.
- Kiểm tra insert/query/update + migration khi đổi schema (xem `DatabaseGuide.md`).

---

## 6. Quy ước viết test

- Đặt tên test rõ ràng: `methodName_condition_expectedResult` hoặc backtick mô tả (vi/eng nhất quán theo file hiện có).
- Mỗi test một hành vi; arrange-act-assert rõ ràng.
- Không phụ thuộc thứ tự test, không dùng dữ liệu thật/PII.
- Dùng test double (fake/mock) cho dependency ngoài; tái dùng builder/fixture chung thay vì lặp.

---

## 7. Chạy test

```bash
# Unit test module SDK
./gradlew :vds-promotion:testDebugUnitTest

# Instrumentation test (cần thiết bị/emulator)
./gradlew :vds-promotion:connectedDebugAndroidTest
```

---

## 8. Lưu ý về dependency test

- Hiện version catalog tập trung vào runtime. **Không tự thêm** thư viện test mới (JUnit5, MockK, Turbine,
  coroutines-test, MockWebServer…) nếu chưa được yêu cầu (AI_AGENT_RULES điều 4). Khi được yêu cầu, thêm vào
  `libs.versions.toml` và cập nhật file này.
