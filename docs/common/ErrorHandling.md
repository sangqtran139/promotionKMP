# ErrorHandling — Quy ước xử lý lỗi

Phân loại, lan truyền và hiển thị lỗi trong TTCN Promotion SDK. Mục tiêu: lỗi rõ ràng, không bị nuốt
âm thầm, và host app nhận thông tin nhất quán trên cả hai nền tảng.

---

## 1. Phân loại exception

Đặt ở `core/domain/exception/`:

| Class | Dùng cho |
|------|----------|
| `PromotionException` | Lỗi nghiệp vụ/HTTP từ server — mang `errorCode` / `message` / `httpStatus` |
| `NetworkException` | Lỗi transport (mất mạng, timeout) — mang `errorCode` (`NETWORK_ERROR` / `TIMEOUT`) |
| `FeatureFlagException` | Lỗi khi lấy cờ tính năng — **không** mang error code, không hiển thị cho user |
| `ErrorCodes` | Tập hằng số mã lỗi dùng chung |

```kotlin
internal object ErrorCodes {
    const val MISSING_CUSTOMER_ID = "missing_customer_id"
    const val NO_RESULT = "no_result"
    const val INSUFFICIENT_BUDGET = "INSUFFICIENT_BUDGET"
    const val GENERAL = "error_general"
    const val NETWORK_ERROR = "network_error"
    const val TIMEOUT = "timeout"
}
```

Nhờ chuẩn hoá này, Presentation đọc `errorCode` mà **không** phụ thuộc kiểu transport
(Ktor / OkHttp / NSURLSession).

---

## 2. Bốn tầng lỗi

```
Ktor  →  ResponseException | HttpRequestTimeoutException | IOException
   │
   │  PromotionRemoteDataSource.apiCall  ── map ──▶  PromotionException | NetworkException
   │                                                       (exception domain)
   ▼
Repository & UseCase  ── truyền thẳng, không bắt ──▶
   │
   │  PromotionUseCases.headlessCall  ── bọc ──▶  PromotionResult.Failure
   ▼                                              (không còn exception)
UI  →  when (result) { Success → render; Failure → hiển thị theo errorCode }
```

**Ranh giới quan trọng:** `PromotionUseCases` là nơi exception dừng lại. Public API **không ném**
exception — trừ `CancellationException`, luôn được `throw` lại để coroutine huỷ đúng cách.

```kotlin
private suspend fun <T : Any> headlessCall(block: suspend () -> T?): PromotionResult<T> =
    try {
        block()?.let { PromotionResult.Success(it) } ?: PromotionResult.Failure(ErrorCodes.NO_RESULT)
    } catch (e: CancellationException) {
        throw e                                    // BẮT BUỘC rethrow
    } catch (e: PromotionException) {
        PromotionResult.Failure(e.errorCode ?: ErrorCodes.GENERAL, e.message, e.httpStatus)
    } catch (e: NetworkException) {
        PromotionResult.Failure(e.errorCode, e.message)
    } catch (e: Throwable) {
        PromotionResult.Failure(ErrorCodes.GENERAL, e.message)
    }
```

---

## 3. Ánh xạ lỗi ở data source

| Bắt được | Ném ra |
|---|---|
| `PromotionException` | giữ nguyên |
| `ResponseException` (4xx/5xx) | `PromotionException` — parse error body lấy `code`/`message` server |
| `HttpRequestTimeoutException` | `NetworkException(TIMEOUT)` |
| `ConnectTimeoutException` | `NetworkException(TIMEOUT)` |
| `SocketTimeoutException` | `NetworkException(TIMEOUT)` |
| `IOException` khác | `NetworkException(NETWORK_ERROR)` |

⚠️ **Thứ tự `catch` quan trọng.** Cả ba loại timeout của Ktor đều kế thừa `IOException`.
Bắt `IOException` trước sẽ nuốt mất timeout và báo sai mã lỗi.

### Lỗi nghiệp vụ ẩn trong HTTP 200

Server có thể trả HTTP 200 nhưng envelope báo lỗi. `requireData()` xử lý:

```kotlin
val isHttpSuccess = status == null || status in 200..299
if (success == false || !isHttpSuccess) {
    throw PromotionException(errorCode = code, message = message, httpStatus = status)
}
```

### Feature flag là ngoại lệ có chủ đích

`FeatureFlagRemoteDataSource` gộp mọi lỗi HTTP về `FeatureFlagException` trống, và
`FeatureFlagRepositoryImpl.fetchFlags()` **nuốt luôn** lỗi đó:

```kotlin
runCatching { remoteDataSource.getFeatureFlags(...) }.getOrNull()?.let { /* cập nhật cache */ }
```

Lý do: cờ tính năng không phải thứ hiển thị lỗi cho người dùng. API hỏng → giữ cờ đang cache;
chưa từng có cache → bật hết. `refresh()` không bao giờ ném.

---

## 4. Quy tắc ở UI

### Android (MVI)

- Tác vụ async chạy trong `launch { }` của `PRMBaseViewModel` (đã gắn `CoroutineExceptionHandler`).
- Override `onError(throwable)` để tắt loading + bắn `Effect` lỗi.
- Lỗi hiển thị đi qua **Effect** (one-shot), không nhồi vào state vĩnh viễn.
- **Không** `catch {}` rỗng.

### iOS (MVVM + callback thuần)

- `PromotionResult.Failure` map sang `PromotionSDKError` ở tầng facade.
- Lỗi đi qua **`onEffect(.showError(code))`** — kênh riêng, một-lần, **không** trộn vào `onState`
  (đối ứng `uiEffect` bên Android). VC map `code` → chuỗi bằng `PromotionUIStrings.errorMessage`
  rồi gọi **`PromotionToast.show(...)`**; state không giữ lại lỗi. (Toast là idiom **dùng chung 2 nền
  tảng** — `PRMConfirmationDialog` không còn được dùng ở luồng lỗi nào.)
- ViewModel `dispatch(ConsumeError)` ngay sau khi phát để store xoá cờ lỗi.

### Cổng bật/tắt toast (cả hai nền tảng)

Toàn bộ hiển thị toast **gom sau một cờ**, mặc định **TẮT** — SDK vẫn **bắt lỗi như cũ** (effect
`ShowError` vẫn phát, ViewModel vẫn `ConsumeError`), chỉ **không hiển thị** gì. Đổi cờ `true` để bật lại.

| Nền tảng | Cờ | Điểm gác |
|---|---|---|
| Android | `PromotionToastGate.isEnabled` (`ui/base/`) | trong `PRMBaseFragment.showToast` / `PRMBaseActivity.showToast` |
| iOS | `PromotionToast.isEnabled` (`PromotionSDKUI/Base/`) | trong `PromotionToast.show(_:in:)` — mọi call site đi qua đây |

Đổi một bên thì đổi bên kia (giữ đối xứng).

**Ngoại lệ duy nhất — PRM_MOB_021 (tính năng bị cờ chặn): LUÔN hiện, không qua cổng.** Các toast lỗi
khác còn state thay thế (empty / shimmer / list cũ) nên tắt đi vẫn hiểu được; còn ở đây user bấm mà
màn không mở, im lặng thì thành "app đơ". Ngoại lệ này gom đúng **một hàm mỗi nền tảng**, đừng rải
`PRMToast.show` / `Toast.makeText` trực tiếp ở call site:

| Nền tảng | Hàm bỏ qua cổng | Call site |
|---|---|---|
| Android | `PromotionToastGate.showFeatureDisabled(context)` | `PRMBaseFragment.openPromotionDetail`, `PromotionSDK.openMyPromotion`, `PromotionSDK.openPromotionDetail` |
| iOS | `PromotionToast.showAlways(_:in:)` | `PRMBaseRouter.canOpenVoucherDetail()`, `PromotionSDKImpl.showFeatureDisabledToast(on:)` |

### Cả hai

Tra message theo `errorCode`, **không** hiển thị thẳng `message` từ server nếu đã có bản dịch cục bộ.
`:promotionLogic` không chứa chuỗi tiếng Việt (AI_AGENT_RULES).

---

## 5. Quy tắc

1. Data source **không** để lọt exception của Ktor lên Domain.
2. Domain/Repository **không** bắt lỗi — để `PromotionUseCases` bọc.
3. Luôn rethrow `CancellationException`.
4. Không bắt `Throwable` chung ở data source; bắt đúng loại.
5. Thêm mã lỗi mới → thêm vào `ErrorCodes` + cập nhật file này (AI_AGENT_RULES điều 8).
