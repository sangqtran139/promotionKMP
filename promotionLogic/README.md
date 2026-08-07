# :promotionLogic — Promotion SDK business logic (KMP)

Module Kotlin Multiplatform đóng gói **data + domain + use case** của Promotion SDK, build được cho
Android và iOS. Không chứa UI, không chứa chuỗi hiển thị — dùng được cho cả XML View (Android),
UIKit (iOS), lẫn Compose Multiplatform về sau.

Package gốc `com.ttcn.promotionsdk.*`, chia theo tầng Clean Architecture:
`config/` · `common/` · `data/` · `domain/` · `di/` · `presentation/`
(xem [ProjectStructure.md §2](../docs/common/ProjectStructure.md)).

> Tài liệu đầy đủ ở [`/docs`](../docs/README.md). Bề mặt API: [`HeadlessAPI.md`](../docs/HeadlessAPI.md).

---

## Dùng như thế nào

```kotlin
// Android — BẮT BUỘC overload có Context (nạp SharedPreferences + suy ra isDebug)
PromotionContainer.init(context, PromotionSDKConfig(apiKey = "...", baseUrl = "..."))

// Common / iOS
PromotionContainer.init(PromotionSDKConfig(apiKey = "...", baseUrl = "...", isDebug = false))

when (val r = PromotionContainer.useCases.searchVouchers(request)) {
    is PromotionResult.Success -> render(r.data)
    is PromotionResult.Failure -> showError(r.errorCode)
}

PromotionContainer.featureFlags.refresh()
if (PromotionContainer.featureFlags.isEnabled(PromotionFeatureFlag.VOUCHER_LIST)) { /* … */ }
```

`PromotionUseCases` không ném exception — mọi lỗi trả qua `PromotionResult.Failure`.

### Năm use case nghiệp vụ

`searchVouchers` · `getVoucherDetail` · `findEligible` · `validateDiscounts` · `createRedemption`

Lõi này là **hợp** của hai SDK gốc: `findEligible` đến từ bản iOS (Android chưa có),
FeatureFlag đến từ bản Android.

---

## Build & test

```bash
./gradlew :promotionLogic:assemble                      # Android AAR + iOS framework
./gradlew :promotionLogic:testAndroidHostTest           # test trên JVM
./gradlew :promotionLogic:iosSimulatorArm64Test         # test trên iOS simulator
./gradlew :promotionLogic:linkDebugFrameworkIosArm64    # framework cho iOS device
```

Test nằm ở `commonTest` nên chạy trên **cả hai** nền tảng. Luôn chạy cả hai trước khi commit.

---

## Khác biệt so với SDK Android gốc

| Bản gốc | Bản KMP | Lý do |
|---|---|---|
| Retrofit + interface `@GET`/`@POST` | Ktor `HttpClient` + `KtorPromotionApiService` | Kotlin/Native không có dynamic proxy nên phải viết tay impl |
| Gson `@SerializedName` | kotlinx.serialization `@SerialName` | Gson là JVM-only |
| OkHttp `ApiInterceptor` | Ktor `defaultRequest` | Header (Bearer, `X-Request-ID`, `Accept-Language`) giữ nguyên |
| `java.util.UUID` | `kotlin.uuid.Uuid` | stdlib, không cần dependency |
| `ConcurrentHashMap` + `synchronized` | `SdkLock` (expect/actual) | `ReentrantLock` (Android) / `NSRecursiveLock` (iOS). Phải reentrant vì `resolve()` gọi đệ quy |
| `SharedPrefStorage` | `PromotionPreferences` + `SettingsPreferences` | Thân dùng chung ở `commonMain` (multiplatform-settings); `actual` chỉ dựng delegate `SharedPreferences` / `NSUserDefaults` |
| `Context` trong `NetworkModule` | `PromotionSDKConfig.isDebug` | `Context` chỉ còn dùng để mở `SharedPreferences` |
| `RetrofitClient` | `PromotionHttpClient` | Engine tự chọn theo classpath: OkHttp (Android), Darwin (iOS) |

Ba thay đổi có ảnh hưởng tới payload, đều đã có test bao:

- `metadata: Map<String, Any>` và `alternativeStacks: List<Any>` chuyển thành `JsonObject` /
  `List<JsonElement>` — kotlinx.serialization không serialize `Any`. JSON gửi/nhận không đổi.
- `Json { encodeDefaults = true; explicitNulls = false }` để payload khớp hành vi Gson.
  Bỏ `encodeDefaults` thì request thiếu `sessionOptions` — code vẫn compile, test map-xuống vẫn xanh.
- Một số field response bắt buộc được cho giá trị mặc định (`createdAt`, `expiresAt`, `canStack`…).
  Gson gán null kể cả với kiểu non-null; kotlinx.serialization sẽ ném `MissingFieldException`.

---

## Không port sang

`PromotionDatabase`, `PromotionCacheDao`, `FeatureFlagDao` — stub rỗng 4 dòng, không nơi nào tham chiếu,
và repo gốc có 0 dòng `import androidx.room`. Module này **không** cần Room, SQLDelight hay kapt.

`DateUtils`, `CoroutineUtils`, `PromotionConfig` — cũng là stub rỗng.

`PRMSimpleSpanBuilder` dùng `android.text.Spannable` và chỉ được `PrmContentDetailEndowFragment` gọi,
nên nó thuộc tầng UI.
