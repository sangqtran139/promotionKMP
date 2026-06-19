# Architecture — Kiến trúc tổng thể

TTCN Promotion SDK áp dụng **Clean Architecture** kết hợp mô hình **MVI** cho tầng giao diện.
Mục tiêu: tách bạch trách nhiệm, giúp test dễ, và giữ public API của SDK ổn định.

---

## 1. Tổng quan 3 layer

```
┌───────────────────────────────────────────────────────────┐
│ PRESENTATION (ui/)                                         │
│   Activity / Fragment / Custom View  ←→  ViewModel (MVI)   │
│   - PRMBaseActivity / PRMBaseFragment / PRMBaseViewModel   │
│   - State / Action / Effect                                │
└───────────────▲───────────────────────────────────────────┘
                │ gọi UseCase, nhận domain model
┌───────────────┴───────────────────────────────────────────┐
│ DOMAIN (core/domain/)                                      │
│   UseCase  →  Repository (interface)  →  Domain Model      │
│   - Thuần Kotlin, KHÔNG phụ thuộc Android / Retrofit / Room│
└───────────────▲───────────────────────────────────────────┘
                │ implement interface, map DTO → domain
┌───────────────┴───────────────────────────────────────────┐
│ DATA (core/data/)                                          │
│   RepositoryImpl → RemoteDataSource (Retrofit) / Local     │
│   - DTO, ApiService, RetrofitClient, Dao, SharedPref       │
└───────────────────────────────────────────────────────────┘
```

**Quy tắc phụ thuộc (Dependency Rule):** phụ thuộc luôn hướng **vào trong** (Presentation → Domain ← Data).
Domain là trung tâm, **không biết** gì về Android, Retrofit hay Room.

---

## 2. Mô tả từng layer

### 2.1. Data layer — `core/data/`
Chịu trách nhiệm lấy/lưu dữ liệu và chuyển đổi dữ liệu thô (DTO) sang model domain.

- `data/dto/` — Data Transfer Object khớp với JSON từ API (voucher, redemption, stackablediscount…). Có hàm mapping `toXxx()` sang domain model.
- `data/remote/` — `PromotionApiService` (Retrofit interface), `PromotionRemoteDataSource`, `RetrofitClient`, `ApiInterceptor`. Khi response lỗi, data source **ném `PromotionException` (domain exception)** mang theo `errorCode`/`status` — không định nghĩa exception riêng ở data layer.
- `data/local/` — `PromotionDatabase`, các `Dao`, `SharedPrefStorage` (cache cục bộ).
- `data/repository/` — `PromotionRepositoryImpl`, `FeatureFlagRepositoryImpl` — **implement** interface của Domain, gọi data source và map DTO → domain.

### 2.2. Domain layer — `core/domain/`
Logic nghiệp vụ thuần, độc lập framework. **Không** import Android/Retrofit/Room.

- `domain/model/` — model nghiệp vụ, chia sub-package theo feature: `voucher/` (`VoucherDetail`, `SearchCustomerVouchersResult`, `SearchCustomerVouchersRequest`, `VoucherStatus`), `redemption/`, `stackablediscount/`, `featureflag/`.
- `domain/repository/` — **interface** repository (`PromotionRepository`, `FeatureFlagRepository`).
- `domain/usecase/` — use case đơn nhiệm (`SearchCustomerVouchersUseCase`, `CreateRedemptionSessionUseCase`…) và `PromotionUseCases` gom nhóm cho headless API.
- `domain/exception/` — exception và error code nghiệp vụ. `PromotionException` mang `errorCode`/`message`/`status`, do data layer ném ra và Presentation đọc trực tiếp (không phụ thuộc kiểu exception của data layer).

### 2.3. Presentation layer — `ui/`
Hiển thị và xử lý tương tác người dùng theo **MVI**.

- `ui/base/` — `PRMBaseActivity`, `PRMBaseFragment`, `PRMBaseViewModel<S, A, E>`.
- `ui/feature/` — từng màn hình (mypromotion, choosepromotion, promotiondetail, searchmypromotion, endowview, featureflag). Mỗi feature có Fragment + ViewModel + Contract (State/Action/Effect) + Adapter.
- `ui/entry/` — **public API** của SDK: `PromotionSDK`, `PromotionSDKOptions`, `PromotionSDKConfig`, `PromotionSDKCallback`, theme.
- `ui/di/` — đăng ký ViewModel (`ViewModelModule`, `PromotionViewModelFactory`).
- `ui/utils/`, `ui/theme/` — extension, custom view, theme.

---

## 3. Mô hình MVI — luồng dữ liệu

Base: `PRMBaseViewModel<S, A, E>` với 3 generic:
- **S = State** — `data class …UiState` (immutable, nguồn sự thật duy nhất cho UI).
- **A = Action** — `sealed interface …Action` (ý định người dùng / sự kiện).
- **E = Effect** — `sealed interface …Effect` (sự kiện một lần: điều hướng, toast, lỗi).

```
   User tương tác
        │  gửi Action
        ▼
  ViewModel.handleAction(action)
        │
        ├─ launch { useCase(...) }        // gọi Domain, IO an toàn
        │        │
        │        ▼
        │   Domain → Data → trả domain model
        │
        ├─ setState { copy(...) }          // cập nhật State (StateFlow)
        └─ sendEffect(Effect.Xxx)          // bắn Effect (SharedFlow, one-shot)
        ▼
  UI collect uiState  → render
  UI collect uiEffect → điều hướng / hiển thị thông báo
```

Cơ chế trong `PRMBaseViewModel`:
- `uiState: StateFlow<S>` — UI quan sát để render; cập nhật qua `setState { ... }`.
- `uiEffect: SharedFlow<E>` — sự kiện một lần; phát qua `sendEffect(...)`.
- `handleAction(action: A)` — điểm vào duy nhất xử lý Action (UI **không** gọi business logic trực tiếp).
- `launch { }` — coroutine có sẵn `CoroutineExceptionHandler` → gọi `onError(throwable)`.

> Ví dụ contract thực tế: `MyPromotionUiState` / `MyPromotionAction` / `MyPromotionEffect`
> trong `ui/feature/promotion/mypromotion/MyPromotionContract.kt`.

---

## 4. Luồng một request điển hình (ví dụ: tìm voucher)

1. Fragment gửi `MyPromotionAction.LoadInitialIfNeeded` → `viewModel.handleAction(...)`.
2. ViewModel `setState { copy(isLoading = true) }` rồi `launch { searchVouchersUseCase(request) }`.
3. `SearchCustomerVouchersUseCase` gọi `PromotionRepository.searchCustomerVouchers(...)`.
4. `PromotionRepositoryImpl` gọi `PromotionRemoteDataSource` → `PromotionApiService` (Retrofit).
5. DTO trả về được map `toSearchCustomerVouchersResult()` → domain model.
6. ViewModel `setState { copy(isLoading = false, vouchers = ...) }`; nếu lỗi → `sendEffect(ShowError(code))`.
7. Fragment render danh sách / hiển thị lỗi.

---

## 5. Hai chế độ sử dụng SDK

- **UI mode** — host nhúng Fragment/màn hình của SDK (`PromotionSDK.init(...)` rồi mở UI).
- **Headless mode** — host tự dựng UI, chỉ gọi nghiệp vụ qua `PromotionSDK.useCases` (`PromotionUseCases`).

Cả hai dùng chung Domain + Data, chia sẻ qua Custom DI (xem `DependencyInjection.md`).

---

## 6. Ràng buộc kiến trúc (không vi phạm)

- Presentation **chỉ** gọi Domain qua **use case**, không gọi thẳng Repository/DataSource.
- Domain **không** import `android.*`, `retrofit2.*`, `androidx.room.*`, **và không import `core.data.dto.*`**.
  → Mọi method của `PromotionRepository` (kể cả validate/redemption) nhận & trả **domain model**, không phải DTO.
- Data map DTO ↔ domain model **trước khi** trả lên Domain. Mỗi feature có sub-package riêng trong
  `core/data/dto/` gồm Request/Response + Mapper: `voucher/` (`VoucherSearchResponse`,
  `VoucherDetailResponse`, `VoucherMapper`), `stackablediscount/`, `redemption/` — hàm `toXxx()`.
  Envelope chung `ApiResponseTemplate` đặt ở `core/data/remote/ApiResponse.kt`.
- **Public model cho discount:** callback `PromotionSDKCallback.onVoucherApplied` và `PRMEndowView` dùng
  model công khai `AppliedDiscount` (`ui/entry/`) — **không** phải DTO data layer. Map domain
  `DiscountItemResult` → `AppliedDiscount` ở **presentation** (`ui/feature/promotion/ext/PromotionUiMapper.kt`).
  DTO `DiscountDetail` chỉ còn dùng nội bộ data layer (response).
- Mọi thay đổi kiến trúc/luồng dữ liệu phải cập nhật file này (AI_AGENT_RULES điều 3 & 7).
