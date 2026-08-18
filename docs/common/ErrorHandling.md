# ErrorHandling — Quy ước xử lý lỗi

Phân loại, lan truyền và hiển thị lỗi trong TTCN Promotion SDK. Mục tiêu: lỗi rõ ràng, không bị nuốt
âm thầm, và host app nhận thông tin nhất quán trên cả hai nền tảng.

---

## 1. Phân loại exception

Đặt ở `domain/exception/`:

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
    const val TOKEN_EXPIRED = "TOKEN_EXPIRED"  // HTTP 401/403 — xem §3
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

### HTTP 401/403 — `TOKEN_EXPIRED` → `onExpireToken()`

`Throwable.toErrorCode()` (`domain/exception/ErrorCodeExtensions.kt`) ưu tiên kiểm tra
`PromotionException.httpStatus`: `401`/`403` → luôn trả `TOKEN_EXPIRED`, bất kể `errorCode` server gửi
kèm là gì. Đây là **điểm chặn duy nhất** — cả 5 store (`MyPromotion`/`SearchMyPromotion`/
`ChoosePromotion`/`PromotionDetail`/`Endow`) đi qua hàm này nên không phải sửa từng store.

Ở tầng Android UI, mã `TOKEN_EXPIRED` được hai nơi đọc thêm (ngoài đường Popup/Không-hiện-gì bình
thường ở §4) để bắn `PromotionSDKCallback.onExpireToken()` ra host:
- `PRMStoreViewModel.effects` — phủ 4 màn Fragment.
- `PRMEndowView.renderState` — widget Endow không đi qua `effects` nên tự bắt riêng.

`onExpireToken()` bắn **thêm**, không thay thế luồng báo lỗi hiện có (Popup/im lặng ở §4 vẫn chạy như
cũ) — host tự quyết định điều hướng (thường là refresh token/đưa user về màn login). **Chưa áp dụng
cho headless `PromotionSDKApi`** (xem `PromotionSDKError.SessionExpired`, hiện chưa wiring) — xem
[InitParity.md §3](./InitParity.md#3-promotionsdkcallback--hợp-nhất-theo-ios-6-sự-kiện-tên-trùng-cả-2-bên).

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
  rồi hiển thị. `state` không giữ lại lỗi.
- ViewModel `dispatch(ConsumeError)` ngay sau khi phát để store xoá cờ lỗi.

### Không còn toast — chỉ còn popup hoặc im lặng

SDK **đã bỏ hẳn toast** ở cả hai nền tảng. Trước đây có một cổng bật/tắt toast
(hai cờ `isEnabled` nay đã xoá cùng lớp toast) mặc định **TẮT**, nên phần lớn lỗi
nghiệp vụ bị nuốt im lặng mà nhìn code thì tưởng có báo. Nay chỉ còn hai lựa chọn, và phải chọn có
chủ ý:

| Cách | Dùng khi | Android | iOS |
|---|---|---|---|
| **Popup** | im lặng thì user không hiểu chuyện gì: vừa chủ động bấm và đang chờ kết quả, hoặc tính năng bị cờ chặn | `PRMBaseFragment.showErrorDialog(message)` → `PRMBaseConfirmDialog` | `PRMBaseViewController.showErrorDialog(_:)` → `PRMConfirmationDialog` |
| **Không hiện gì** | màn đã có empty-view / shimmer / list cũ nói thay | nhánh `is PRMEffect.ShowError -> Unit` | `_ = error` trong `handle(_:)` |

Hai hàm popup nằm ở **lớp base** của mỗi nền tảng, đừng gọi thẳng dialog ở call site. Chỗ nào không
phải Fragment/VC (ví dụ `PromotionSDK` gọi từ Activity, `PRMBaseRouter`) thì dùng
`PRMBaseConfirmDialog.showError(context, fm, message)` / `PRMConfirmationDialog.showError(_:in:)`.

Đang dùng popup ở: validate hỏng khi bấm "Áp dụng", kéo-để-tải-lại hỏng (màn "Chọn ưu đãi"), và mọi
đường bị feature flag chặn.

### PRM_MOB_021 — tính năng bị cờ chặn

**SDK tự hiện popup, host không phải lo câu chữ.** Đây là quyết định của SDK (chính SDK tắt tính
năng) nên thông báo cũng phải của SDK. Gom đúng một hàm mỗi nền tảng:

| Nền tảng | Hàm | Call site |
|---|---|---|
| Android | `PRMBaseConfirmDialog.showFeatureDisabled(context, fm)` | `PRMBaseFragment.openPromotionDetail`, `PromotionSDK.openMyPromotion`, `PromotionSDK.openPromotionDetail`, `PRMEndowView.confirmRedemption` |
| iOS | `PromotionSDKImpl.showFeatureDisabledToast(on:)` | `PRMBaseRouter.canOpenVoucherDetail()`, `PromotionSDKImpl.confirmRedemption` |

Host **vẫn** nhận lỗi qua `onError` — để **dừng luồng thanh toán**, không phải để hiện chữ. Callback
trả thẳng **kiểu công khai** `PromotionSDKError`, không phải mã thô:

```kotlin
onError = { error -> if (error is PromotionSDKError.FeatureDisabled) stopCheckout() }   // Android
```
```swift
onError: { error in if case .featureDisabled = error { stopCheckout() } }               // iOS
```

Lý do không trả `String`: hằng số mã lỗi nằm trong `promotionLogic` (`implementation`, **không** có
trên compile classpath của host), nên host chỉ còn cách hardcode `"PRM_MOB_021"`. Vẫn có
`PromotionSDKError.from(code)` cho ai nhận mã thô từ nguồn khác — nó dùng đúng bảng map mà
`PromotionSDKApi` (headless) đang dùng, một nguồn sự thật.

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
