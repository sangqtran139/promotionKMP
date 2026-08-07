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

Đừng dùng `SharedPreferences` hay `NSUserDefaults` thật trong `commonTest`. Cũng **đừng viết fake tay**
— dùng `MapSettings` (module `multiplatform-settings-test`) làm delegate cho chính `SettingsPreferences`
mà production dùng:

```kotlin
private fun testPreferences(): PromotionPreferences = SettingsPreferences(MapSettings())
```

Cách này hơn fake tay ở chỗ test đi qua **đúng đường code của app thật** — fake tự viết dễ có ngữ nghĩa
lệch (vd trả `""` thay vì `null` khi thiếu key) rồi giấu bug thay vì bắt nó.

Bản thân `SettingsPreferences` có test riêng ở `PromotionPreferencesTest.kt`.

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

## 5b. Coverage — Kover

Đo bằng [Kover](https://github.com/Kotlin/kotlinx-kover) (`org.jetbrains.kotlinx.kover`), khai ở
`promotionLogic/build.gradle.kts`.

```bash
./scripts/test-report.sh                    # chạy cả 2 target + sinh báo cáo gộp (khuyến nghị)
./scripts/test-report.sh --json             # kèm bản .json cho CI
./gradlew :promotionLogic:koverHtmlReport   # build/reports/kover/html/index.html
./gradlew :promotionLogic:koverXmlReport    # cho CI
./gradlew :promotionLogic:koverVerify       # gác ngưỡng, fail nếu tụt
```

**Đo trên target `android` (JVM).** Kover cần bytecode JVM nên không đo được Kotlin/Native — nhưng
test nằm ở `commonTest` và chạy trên **cả hai** target, nên số liệu vẫn phản ánh đúng `commonMain`.
Vẫn phải chạy `iosSimulatorArm64Test` trước khi commit (mục 5 điều 7).

**Ngưỡng:** LINE ≥ 93%, INSTRUCTION ≥ 92%, BRANCH ≥ 90% — đặt **sát dưới** mức hiện tại để PR làm
tụt coverage là fail ngay, không phải mục tiêu để phấn đấu. Nâng lên mỗi khi bộ test dày thêm.

**Cách đẩy BRANCH lên (kinh nghiệm thực tế đợt đưa 31% → 90%):** đừng đoán, hãy đọc **đúng số dòng**
còn thiếu nhánh từ `report.xml` rồi viết test nhắm thẳng:

```bash
python3 - <<'EOF'
import xml.etree.ElementTree as ET
r = ET.parse("promotionLogic/build/reports/kover/report.xml").getroot()
for pk in r.findall("package"):
    for sf in pk.findall("sourcefile"):
        miss = [l.get('nr') for l in sf.findall('line') if int(l.get('mb', 0)) > 0]
        if miss: print(f"{sf.get('name')}: {', '.join(miss)}")
EOF
```

Bốn nhóm chiếm gần hết phần thiếu, theo thứ tự ROI:
1. **`result?.x ?: default`** — chỉ chạy khi API trả `data: null`. Một `NullRepo` trả null cho mọi
   lời gọi phủ được rất nhiều nhánh cùng lúc.
2. **Payload tối thiểu vs đầy đủ** — mapper toàn `?:`; chỉ test payload đầy đủ thì nửa số nhánh
   không bao giờ chạy.
3. **Nhánh chỉ chạy khi sự cố** — timeout, 4xx không parse được body, `success:false`.
4. **Guard đồng thời** — latest-wins, load-more khi đang refresh. Cần `StandardTestDispatcher`
   (KHÔNG phải `Unconfined`) để coroutine xếp hàng và dựng được cảnh "response về muộn".

**Nhánh không thể phủ:** `suspend` biên dịch thành state machine (`Xxx$method$1`) với `when(label)`
dispatch + `throw IllegalStateException` cho label không hợp lệ. Đừng loại chúng khỏi phép đo — thân
`if`/`when` do ta viết trong `scope.launch { }` cũng nằm trong chính class đó, loại đi là giấu logic
thật. Cũng có **nhánh chết thật sự**: `ChoosePromotionStore` có `it.isMultiSelection -> cur + id`
mà không intent nào bật được `isMultiSelection` — đó là code chết, không phải thiếu test.

**Loại trừ khỏi phép đo** (`reports.filters.excludes`): DTO thuần dữ liệu (`*Dto`, `*Request`,
`*Response`, `$serializer`) và cầu nền tảng `SdkLockKt` — thân hàm nằm ở `androidMain`/`iosMain`,
không thuộc `commonMain`. Đo chúng chỉ làm nhiễu con số.

> `PromotionClockKt` **từng nằm trong danh sách này và đó là sai**: ngoài `currentEpochMillis()` thì
> file còn `isoDateToEpochMillis`, `daysUntil`, `daysFromCivil` — logic commonMain thật, bị che khỏi
> coverage. Đã gỡ. Bài học: chỉ exclude theo *class chỉ chứa* cầu nền tảng, đừng exclude cả file vì
> nó **có** một hàm `expect`.

> ⚠️ Kover **0.9.1 không chạy được** với plugin `androidLibrary` kiểu mới của KMP
> (`com.android.kotlin.multiplatform.library`): `Could not get unknown property 'compileJavaTaskProvider'`.
> Phải dùng **≥ 0.9.9**.

---

## 6. Test UI

`:promotionLogic` không có UI nên không có test UI ở đây.

- **Android** (`promotionSDK`): ViewModel test bằng JUnit + `kotlinx-coroutines-test`, fake `PromotionUseCases`.
  Instrumentation test cho Fragment nếu cần.
- **iOS** (`promotionSDK`): XCTest cho ViewModel, dùng `RxTest`/`RxBlocking` cho stream.

Khi hai module UI được tạo, bổ sung mục này (AI_AGENT_RULES điều 8).
