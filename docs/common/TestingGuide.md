# TestingGuide — Quy ước Testing

Nguyên tắc chính: **logic dùng chung thì test dùng chung**. Test của `:promotionLogic` viết một lần
trong `commonTest` và chạy trên **cả** JVM lẫn Kotlin/Native. Một test chỉ pass trên JVM không chứng
minh được gì về iOS.

---

## 1. Chạy test

```bash
./gradlew :promotionLogic:testAndroidHostTest      # JVM (Android host)
./gradlew :promotionLogic:iosSimulatorArm64Test    # Kotlin/Native (iOS simulator)
```

> Tên task đến từ AGP KMP plugin (`com.android.kotlin.multiplatform.library`), **không** phải
> `testDebugUnitTest` như module Android thường.

### Luôn kiểm số lượng test, đừng tin "BUILD SUCCESSFUL"

Gradle báo thành công cả khi không có test nào chạy (up-to-date, hoặc bị lọc hết).

```bash
grep -ho 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
  promotionLogic/build/test-results/testAndroidHostTest/*.xml
```

---

## 2. Cấu trúc hiện tại

| File (`commonTest`) | Bao gì |
|---|---|
| `PromotionPipelineTest` | JSON → DTO → domain; header; lỗi HTTP; lỗi nghiệp vụ; payload `createRedemption` |
| `EligibleCampaignsTest` | `findEligible`: map hai nhóm, sort tab, payload pagination/section |
| `FeatureFlagTest` | cache, `ENABLE_ALL` là công tắc tổng, `refresh()` không ném khi API lỗi |
| `PromotionContainerTest` | DI: init, singleton, fallback provider, clear, re-init |

---

## 3. Test networking bằng `MockEngine`

`PromotionHttpClient.configure(...)` được tách khỏi `create(...)` để test dựng **cùng cấu hình** trên
`MockEngine` — nếu test dùng client khác cấu hình thì nó không kiểm chứng được gì.

```kotlin
val engine = MockEngine { request ->
    captured.add(request)
    respond(responseJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
}
val client = HttpClient(engine) {
    with(PromotionHttpClient) { configure(BASE_URL, provider, isDebug = false) }
}
```

### Kiểm cả hai chiều

Một test API tốt kiểm **payload gửi lên** lẫn **kết quả map xuống**:

```kotlin
// chiều lên — bắt lỗi encodeDefaults / explicitNulls
val body = (captured.single().body as TextContent).text
assertTrue("\"timeoutSeconds\":300" in body)
assertTrue("sectionCode" !in body)          // null phải bị bỏ

// chiều xuống — bắt lỗi mapper
assertEquals("v-1", data.myOffers.single().id)
```

Payload gửi lên đặc biệt quan trọng: nếu ai đó bỏ `encodeDefaults = true` trong `Json`, code vẫn
compile và test map-xuống vẫn xanh, nhưng **server nhận thiếu field**. Chỉ test payload bắt được.

---

## 4. Test storage

Đừng dùng `SharedPreferences` hay `NSUserDefaults` thật trong `commonTest` — dùng fake:

```kotlin
private class InMemoryStorage : KeyValueStorage {
    private val map = mutableMapOf<String, Boolean>()
    override fun putBoolean(key: String, value: Boolean) { map[key] = value }
    override fun getBoolean(key: String, default: Boolean) = map[key] ?: default
    override fun contains(key: String) = map.containsKey(key)
    override fun remove(key: String) { map.remove(key) }
    override fun clear() = map.clear()
}
```

---

## 5. Quy tắc

1. **Test đặt ở `commonTest`**, không phải `androidHostTest`, trừ khi test đúng phần `actual` của Android.
2. Mọi endpoint mới → một test `MockEngine` kiểm payload + mapping.
3. Mọi hành vi lỗi (timeout, 4xx, `success=false`) → một test riêng, khẳng định đúng `errorCode`.
4. Dùng `runTest` của `kotlinx-coroutines-test` cho hàm `suspend`.
5. Không test qua network thật. Không test phụ thuộc thời gian thực.
6. Đặt tên test theo `hànhVi_điềuKiện_kếtQuả`, ví dụ
   `refresh_onHttpError_keepsPreviousFlags_andDoesNotThrow`.
7. **Chạy cả hai nền tảng trước khi commit.** Kotlin/Native có khác biệt về freeze, thread và
   khởi tạo lazy mà JVM không lộ ra.

---

## 6. Test UI

`:promotionLogic` không có UI nên không có test UI ở đây.

- **Android** (`promotionUI`): ViewModel test bằng JUnit + `kotlinx-coroutines-test`, fake `PromotionUseCases`.
  Instrumentation test cho Fragment nếu cần.
- **iOS** (`promotionUI`): XCTest cho ViewModel, dùng `RxTest`/`RxBlocking` cho stream.

Khi hai module UI được tạo, bổ sung mục này (AI_AGENT_RULES điều 8).
