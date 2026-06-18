# ErrorHandling — Quy ước xử lý lỗi

Quy ước phân loại, lan truyền và hiển thị lỗi trong TTCN Promotion SDK. Mục tiêu: lỗi rõ ràng, không bị nuốt
âm thầm, và host app/người dùng nhận thông tin nhất quán.

---

## 1. Phân loại exception

Đặt trong `core/domain/exception/`:

| File | Dùng cho |
|------|----------|
| `NetworkException` | Lỗi mạng/kết nối/timeout/HTTP |
| `PromotionException` | Lỗi nghiệp vụ promotion (voucher không hợp lệ, hết hạn…) |
| `FeatureFlagException` | Lỗi liên quan feature flag |
| `ErrorCodes` | Tập hằng số mã lỗi dùng chung |
| `PromotionApiException` (`core/data/remote`) | Lỗi tầng API thô (status, body lỗi) trước khi map lên domain |

Nguyên tắc: lỗi **tầng Data/API** (`PromotionApiException`) được chuyển thành **exception domain**
(`NetworkException`/`PromotionException`) kèm **mã trong `ErrorCodes`** trước khi lên Domain/Presentation.

---

## 2. Luồng lan truyền lỗi

```
ApiService / RemoteDataSource  → PromotionApiException (data)
        │ Repository map
        ▼
NetworkException / PromotionException (domain, có errorCode)
        │ UseCase trả lên (ném hoặc trả kết quả lỗi)
        ▼
ViewModel.launch { } → CoroutineExceptionHandler → onError(throwable)
        │
        ├─ setState { copy(isLoading = false, ...) }
        └─ sendEffect(Effect.ShowError(errorCode))
        ▼
UI: handleEffect → tra cứu message theo errorCode → hiển thị
```

---

## 3. Quy tắc ở ViewModel

- Mọi tác vụ async chạy trong `launch { }` của `PRMBaseViewModel` (đã gắn `CoroutineExceptionHandler`).
- Override `onError(throwable)` để: tắt loading trong state + bắn `Effect` lỗi.
- **Không** nuốt exception (`catch {}` rỗng). Nếu bắt cục bộ, phải xử lý hoặc phát Effect lỗi rõ ràng.
- Lỗi hiển thị cho người dùng đi qua **Effect** (one-shot), không nhồi message lỗi vào state vĩnh viễn nếu là sự kiện tức thời.

```kotlin
override fun onError(throwable: Throwable) {
    setState { copy(isLoading = false, isRefreshing = false) }
    val code = (throwable as? PromotionException)?.errorCode ?: ErrorCodes.UNKNOWN
    sendEffect(MyPromotionEffect.ShowError(code))
}
```

---

## 4. Quy tắc ở Repository / UseCase

- Repository bắt lỗi từ data source, **map sang exception domain** có `errorCode`; không để DTO/HTTP raw rò lên Domain.
- UseCase giữ logic nghiệp vụ; có thể chuyển exception thành kết quả domain (vd `null`, sealed result) nếu phù hợp contract.
- Không log dữ liệu nhạy cảm khi xử lý lỗi (token, thông tin khách hàng).

---

## 5. Hiển thị lỗi ở UI

- Fragment nhận `Effect.ShowError(errorCode)` → map `errorCode` → chuỗi trong `strings.xml` (không hardcode).
- Cách hiển thị nhất quán với UX hiện tại (toast/snackbar/empty state). Tái dùng helper hiển thị có sẵn nếu có.
- Trạng thái rỗng/loading/lỗi nên phản ánh trong `UiState` (vd `isEmpty`, `isLoading`) để render đúng.

---

## 6. Mã lỗi & callback host

- Mã lỗi tập trung ở `ErrorCodes`; thêm mã mới ở đây, không rải hằng số khắp code.
- Nếu cần báo lỗi ra ngoài host, dùng `PromotionSDKCallback` (`ui/entry/`) — giữ contract ổn định.

---

## 7. Checklist khi thêm/xử lý lỗi mới

- [ ] Có mã lỗi tương ứng trong `ErrorCodes` (thêm nếu thiếu).
- [ ] Lỗi data/API đã map sang exception domain.
- [ ] ViewModel tắt loading + phát Effect lỗi (không nuốt lỗi).
- [ ] UI hiển thị message từ `strings.xml` theo mã lỗi.
- [ ] Không log thông tin nhạy cảm.
- [ ] Nếu đổi cấu trúc lỗi/mã → cập nhật file này.
