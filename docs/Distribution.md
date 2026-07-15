
# Distribution (Android) — Maven

SDK Android phát hành bằng **Maven**: publish `com.ttcn.promotion:promotionUI` (kèm POM + Gradle
Module Metadata), host khai **một** dòng. Trước đây là file AAR — host chép hai file vào `libs/` rồi
tự khai toàn bộ dependency; §1 giữ lại vì nó giải thích *vì sao* đổi.

Tài liệu này: cách phát hành, cách host tích hợp, và — phần quan trọng — **ràng buộc nào biến mất
(§4), ràng buộc nào vẫn còn (§5)**. Đừng kỳ vọng Maven sửa những thứ nó không sửa.

**Trạng thái:** `:promotionLogic` và `:AndroidPromotionUI` đã có `maven-publish`; `:androidApp` tiêu
thụ SDK bằng toạ độ Maven từ `mavenLocal()`. Repo thật (Nexus/Artifactory) là bước còn lại — §3.4.

**Máy mới thì chạy một lệnh:**

```bash
./scripts/build-android.sh              # publish SDK vào ~/.m2 → build app demo
./scripts/build-android.sh --skip-app   # chỉ publish SDK
./scripts/build-android.sh --help       # các tuỳ chọn: --clean, --install, --version
```

Script ép đúng thứ tự **publish trước, build app sau** — bỏ bước publish thì Gradle báo
`Could not find com.ttcn.promotion:promotionUI`. Tương đương chạy tay:

```bash
./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionUI:publishToMavenLocal
./gradlew :androidApp:assembleDebug
```

> Chỉ nói về Android. iOS phát hành bằng XCFramework và **không** hưởng lợi gì ở đây (§6).

---

## 1. Trước đây: file AAR — và ba chỗ đau

`androidApp/build.gradle.kts` từng tiêu thụ SDK bằng file, y như host thật:

```kotlin
implementation(fileTree("libs") { include("*.aar") })   // promotionLogic.aar + AndroidPromotionUI-1.0.0-release.aar

// AAR không mang theo dependency → khai tay 20+ dòng:
implementation(libs.ktor.client.core)
implementation(libs.kotlinx.coroutines.core)
implementation(libs.androidx.appcompat)
implementation(libs.glide)
// … và ~16 dòng nữa
```

Hai file được làm mới bằng `./gradlew syncSdkAars` (task `Copy`, nay đã xoá).

Ba chỗ đau, đều **im lặng** — không có cái nào báo lỗi lúc build:

1. **AAR không có POM.** Nó là file zip chứa class + res, không mô tả nó cần gì. Host thiếu một
   dependency → app build xong, chạy đến màn dùng tới thì `NoClassDefFoundError`.
2. **Version dependency do host tự chọn.** SDK compile với Glide X, host khai Glide Y. Không ai đối
   chiếu. Lệch API → `NoSuchMethodError` lúc runtime.
3. **Quên `syncSdkAars`.** Sửa SDK xong, quên chạy → app demo vẫn build với AAR cũ, không cảnh báo.

Cộng thêm: hai file `.aar` (~2,5 MB) sống trong cây source, phải chuyển cho đối tác bằng đường thủ
công (drive/mail), và dễ vô tình lọt vào commit.

---

## 2. Maven sửa cái gì — bản chất

Publish lên Maven repository thì cùng với AAR có thêm **POM** (và **Gradle Module Metadata**, `.module`,
chính xác hơn POM). File này khai đúng thứ SDK cần, kèm version. Host viết **một dòng**:

```kotlin
repositories { mavenLocal() }                                  // hoặc Nexus nội bộ
dependencies { implementation("com.ttcn.promotion:promotionUI:1.0.0") }
```

Gradle đọc metadata → tự kéo `promotionLogic`, Ktor, coroutines, AppCompat, Glide, Gson… đúng
version SDK đã compile. Hai mươi dòng khai tay biến mất, và cùng với chúng là cả hai lớp lỗi runtime
ở §1.

**`implementation` vẫn là `implementation`.** Trong metadata, `implementation(projects.promotionLogic)`
xuất hiện ở scope **runtime**, không phải compile. Nghĩa là host kéo được `promotionLogic` để chạy
nhưng **không** thấy `com.ttcn.promotionsdk.core.*` trên compile classpath — đúng ranh giới mà
`AndroidPromotionUI/build.gradle.kts` đang cố giữ. Cách file-AAR hiện tại thì ngược lại: host phải tự
`implementation(libs.ktor…)`, nên Ktor và coroutines **nằm luôn trên compile classpath của host** —
rò rỉ thứ đáng lẽ giấu.

---

## 3. Làm như thế nào

### 3.1. Toạ độ

Hai module cần `group` + `version` để Gradle biết dịch `projects.promotionLogic` thành toạ độ Maven:

| Module | groupId | artifactId | Đổi tên được? |
|---|---|---|---|
| `:promotionLogic` | `com.ttcn.promotion` | `promotionLogic` | **Không** — xem cảnh báo dưới |
| `:AndroidPromotionUI` | `com.ttcn.promotion` | `promotionUI` | Được — host khai thẳng toạ độ này |

`SDK_VERSION` đã có sẵn (property, mặc định `1.0.0`) — dùng lại làm `version`.

> **artifactId của lõi phải trùng tên module.** `:AndroidPromotionUI` khai
> `implementation(projects.promotionLogic)`, và Gradle ghi vào POM của nó toạ độ `group:<tên-module>`
> = `com.ttcn.promotion:promotionLogic`. Rename ở publication (thành `promotion-logic` chẳng hạn)
> **không** đổi được toạ độ trong POM đó: POM vẫn trỏ `promotionLogic`, mà trên repo chỉ có
> `promotion-logic` → host nhận `Could not find com.ttcn.promotion:promotionLogic`. Đã dính thật, và
> configuration cache còn giấu lỗi một lượt (build "xanh" nhờ POM cũ trong cache).
>
> `:AndroidPromotionUI` **không** dính ràng buộc này — không module nào trỏ vào nó bằng
> `projects.…`, host gõ toạ độ bằng tay — nên nó publish dưới tên `promotionUI` cho đối xứng với
> `promotionLogic`. Muốn đổi cả tên lõi thì phải đổi **tên module** trong `settings.gradle.kts`.

### 3.2. `:AndroidPromotionUI` (thư viện Android thường)

```kotlin
plugins {
    id("com.android.library")
    id("maven-publish")
}

group = "com.ttcn.promotion"
version = sdkVersion              // biến đã có trong file

android {
    publishing {
        singleVariant("release") { withSourcesJar() }   // sources jar: host debug dễ hơn
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
            artifactId = "promotionUI"        // đổi tên ở đây an toàn — §3.1
        }
    }
    repositories {
        mavenLocal()              // bước 1 — xem 3.4
    }
}
```

### 3.3. `:promotionLogic` (Kotlin Multiplatform)

KMP tự tạo publication cho mọi target khi thêm plugin — **không** `create<MavenPublication>` bằng tay:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
}

group = "com.ttcn.promotion"
version = sdkVersion

afterEvaluate {
    // Variant android lấy thẳng tên module (`promotionLogic`) — không có module trung gian.
    publishing.publications.withType<MavenPublication>().configureEach {
        if (name == "android") artifactId = project.name
    }
    // Chỉ publish target android. `enabled` (lúc cấu hình), không `onlyIf { … }` — lambda của onlyIf
    // giữ tham chiếu script object, configuration cache không serialize được → build fail.
    tasks.withType<AbstractPublishToMaven>().configureEach {
        enabled = publication.name == "android"
    }
}
```

**Mặc định KMP publish 5 package**: một cho mỗi target (`promotionLogic-android`,
`promotionLogic-iosarm64`, `-iosx64`, `-iossimulatorarm64`) cộng một **module gốc**
`promotionLogic` chỉ chứa metadata trỏ sang chúng. Cơ chế đó có ý nghĩa khi consumer nhiều nền
tảng cùng khai một toạ độ và để Gradle tự chọn variant.

Ở đây **không** cần: iOS link `PromotionLogic.xcframework`, không đi qua Maven (§6). Còn đúng một
target, nên module gốc chỉ là một lớp trỏ vô ích. Bỏ nó, cho android lấy thẳng tên — repo còn **hai
package, mỗi cái là một AAR thật**:

```
com/ttcn/promotion/
├── promotionUI/1.0.0/promotionUI-1.0.0-release.aar   ← host Android khai cái này
└── promotionLogic/1.0.0/promotionLogic-1.0.0.aar     ← lõi, promotionUI tự kéo về
```

> Muốn publish cả iOS về sau (consumer Kotlin/Native) thì **bỏ** khối `afterEvaluate` này — KMP quay
> lại 5 package, và cấu trúc module gốc lúc đó là cần thiết.
>
> `afterEvaluate` không phải thừa: KMP đặt artifactId **sau** giai đoạn cấu hình, nên đụng vào
> publication sớm hơn thì sửa hụt (đổi được bản android, bỏ sót iOS — không lỗi, không cảnh báo).

### 3.4. Repo đích — đi hai bước

**Bước 1 — `mavenLocal()`** (`~/.m2/repository`). Không cần hạ tầng, không credentials:

```bash
./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionUI:publishToMavenLocal
```

Đủ để chứng minh luồng chạy và để `androidApp` tiêu thụ như host thật.

**Bước 2 — repo nội bộ** (Nexus/Artifactory/GitHub Packages) khi đã thông:

```kotlin
repositories {
    maven {
        url = uri(providers.gradleProperty("promotionRepoUrl").get())
        credentials {
            username = providers.gradleProperty("promotionRepoUser").get()
            password = providers.gradleProperty("promotionRepoPassword").get()
        }
    }
}
```

Credentials để ở `~/.gradle/gradle.properties` hoặc biến môi trường — **không** commit.

### 3.5. `androidApp` sau khi đổi

```kotlin
// settings.gradle.kts — mavenLocal CHỈ cho group của SDK. Thả rông thì nó tranh resolve với mọi
// thư viện khác và cho ra build không tái lập được.
dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroup("com.ttcn.promotion") } }
        google { … }
        mavenCentral()
    }
}
```
```kotlin
// androidApp/build.gradle.kts
dependencies {
    implementation("com.ttcn.promotion:promotionUI:$sdkVersion")   // hết fileTree, 20 dòng còn 4

    // androidx/material vẫn phải khai — xem §5.1, đây KHÔNG phải thừa:
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // của riêng app demo:
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
}
```

`syncSdkAars` đã xoá. Thư mục `androidApp/libs/` không còn ai đọc — xoá được.

**Đã kiểm chứng trên máy** (`./gradlew :androidApp:dependencies`):

```
+--- com.ttcn.promotion:promotionUI:1.0.0
|    +--- com.ttcn.promotion:promotionLogic:1.0.0
|    |    \--- com.ttcn.promotion:promotionLogic-android:1.0.0
|    |         +--- io.ktor:ktor-client-core:3.3.0        ← tự kéo, host không khai
|    +--- androidx.appcompat:appcompat:1.7.1
|    +--- com.github.bumptech.glide:glide:4.16.0
```

Và `debugCompileClasspath` **không** chứa dòng nào của `io.ktor` hay `promotionLogic` — chúng chỉ
có ở runtime, đúng như §2 nói.

---

## 4. Ràng buộc **mất đi**

| Ràng buộc hôm nay | Sau khi lên Maven |
|---|---|
| Host khai tay 20+ dependency của SDK | Còn 4 dòng androidx/material (§5.1); Ktor, coroutines, Glide, Gson, lifecycle… POM tự mang |
| Thiếu dependency → `NoClassDefFoundError` lúc chạy | Không xảy ra: Gradle tự resolve |
| Host tự chọn version dependency, lệch với version SDK compile → `NoSuchMethodError` | Gradle resolve version theo metadata; xung đột với thư viện của host được **hoà giải và báo cáo** (`./gradlew :app:dependencies`) |
| Ktor/coroutines nằm trên compile classpath host (do phải khai tay) | Chỉ ở **runtime** — lõi thật sự bị giấu, đúng thiết kế seal API |
| Hai file `.aar` 2,5 MB trong cây source, chuyển tay qua drive/mail | Artifact nằm trên repo, kéo bằng toạ độ |
| Không biết đang chạy bản nào (`promotionLogic.aar` không có version trong tên) | Version nằm trong toạ độ; đổi bản = sửa một số |
| Rollback = đi xin lại file cũ | Rollback = hạ số version |

---

## 5. Ràng buộc **vẫn còn** (đừng kỳ vọng sai)

### 5.1. Host **vẫn** phải khai androidx — vì SDK phơi androidx ra public API

Không phải "một dòng là xong". Sự thật đo được khi build app demo lần đầu qua Maven: **9 class không
truy cập được**, `:androidApp:compileDebugKotlin` chết.

```
Cannot access class 'androidx.fragment.app.Fragment'
Cannot access class 'androidx.fragment.app.FragmentActivity'
Cannot access class 'androidx.appcompat.widget.AppCompatRadioButton'
Cannot access class 'MaterialButton' / 'ConstraintLayout' / 'TabLayout' / 'CardView' …
```

Hai nguyên nhân khác nhau, đừng lẫn:

1. **App tự dùng** MaterialButton/ConstraintLayout/TabLayout trong layout của nó. Host thật cũng
   phải khai thứ chính mình dùng — bình thường, không phải khuyết điểm của Maven.
2. **SDK phơi androidx ra public API nhưng khai `implementation`.** `PRMBaseFragment<VB> : Fragment()`,
   `PRMBaseActivity<VB> : AppCompatActivity()`, `PromotionSDK.openMyPromotion(activity: FragmentActivity)`
   — host **buộc phải** thấy `Fragment`/`AppCompatActivity` lúc compile để kế thừa. Nhưng
   `implementation` đẩy chúng xuống scope **runtime** trong metadata, nên host không thấy.

Cách ship file AAR ngày trước **che** lỗi này: host khai tay tất cả nên tình cờ có đủ. Maven làm nó
lộ ra — đó là tin tốt, nhưng bản thân Maven **không sửa** nó.

**Sửa tận gốc** = trong `:AndroidPromotionUI`, đổi sang `api(...)` đúng những lib nằm trong signature
public:

```kotlin
api(libs.androidx.appcompat)      // PRMBaseActivity : AppCompatActivity
api(libs.androidx.fragment.ktx)   // PRMBaseFragment : Fragment, openMyPromotion(FragmentActivity)
implementation(libs.glide)        // nội bộ — cứ để runtime
```

Chưa làm: hiện `androidApp` khai lại 4 dòng androidx/material cho qua. Khi nào có host thật kế thừa
`PRMBaseFragment`, họ sẽ vấp đúng lỗi trên và phải tự đoán ra `androidx.fragment` — nên đây là **nợ
kỹ thuật đã biết**, không phải trạng thái mong muốn.

- **Vẫn phải publish lại sau khi sửa SDK.** "Quên `syncSdkAars`" chỉ đổi hình dạng thành "quên
  `publishToMavenLocal`" — vẫn im lặng chạy bản cũ. Cách chữa thật là *vòng lặp dev không đi qua
  artifact*: dùng `implementation(projects.androidPromotionUI)` hoặc composite build khi phát triển,
  và chỉ tiêu thụ Maven khi **nghiệm thu**. Nếu dùng version `-SNAPSHOT`, nhớ
  `configurations.all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }` — mặc định Gradle
  cache SNAPSHOT **24 giờ**.
- **Cần hạ tầng.** `mavenLocal()` chỉ là cái máy của bạn. Chia cho đối tác thì phải có Nexus/
  Artifactory/GitHub Packages, kèm quản lý credentials và quyền đọc.
- **Cần kỷ luật version.** Maven không nghĩ hộ: bump version, changelog, và **breaking API vẫn là
  breaking** (AI_AGENT_RULES điều 7 — đổi public API thì cập nhật [PublicApi.md](./PublicApi.md)).
- **ProGuard/R8 không đổi.** `consumerProguardFiles("consumer-rules.pro")` đi kèm AAR ở **cả hai**
  cách. Đây không phải lý do để chuyển.
- **`minSdk`, `compileSdk`, `nonTransitiveRClass`, namespace** — không liên quan, giữ nguyên.
- **Không sửa được lỗi thiết kế.** Nếu SDK rò rỉ type nội bộ ra public API thì Maven vẫn ship y
  nguyên chỗ rò đó.

---

## 6. iOS thì sao

**Không đi qua Maven.** Host iOS nhận thẳng **một** `PromotionSDKUI.xcframework`; kênh phát hành là
SPM/CocoaPods/xcframework rời. Sau khi chuyển, hai nền tảng có hai kênh phát hành khác nhau — bình
thường, không phải thiếu sót.

Cũng một lệnh, đối xứng với Android:

```bash
./scripts/build-ios.sh              # dựng XCFramework → build app demo (simulator)
./scripts/build-ios.sh --skip-app   # chỉ dựng PromotionSDKUI.xcframework
./scripts/build-ios.sh --run        # build xong cài + mở trên simulator
```

Chuỗi phụ thuộc mà script ép đúng thứ tự:

```
:promotionLogic (Kotlin) ──gradle──▶ PromotionLogic.xcframework   (lõi, static)
             └─ link tĩnh vào ─────▶ PromotionSDKUI.xcframework   (UI, Swift)
                                              └─ iosApp link + embed
```

Bước 1 do `iosPromotionUI/scripts/build-xcframework.sh` lo (nó tự gọi Gradle dựng lõi rồi copy vào
`iosPromotionUI/Frameworks/` **trước khi** archive Swift — dùng header cũ thì lỗi hiện ra tận
`SwiftCompile` với "cannot find … in scope", rất khó lần).

> **Scheme `iosApp` phải là shared scheme** (`xcshareddata/xcschemes`, đã commit). Để trong
> `xcuserdata` thì chỉ máy của người tạo mới thấy — máy khác clone về, `xcodebuild -scheme iosApp`
> không tìm ra. Và `-derivedDataPath` bắt buộc đi kèm `-scheme`, không dùng được với `-target`.

### 6.1. Mỗi bản phát hành phải **lưu dSYM lại**

Target framework đặt `STRIP_STYLE = non-global` (Release), nên binary giao cho host **không còn
local symbol**. Đây là chủ ý: 30.500 local symbol — 8.680 trong đó là `kfun:`/`ktype:` của
Kotlin/Native link tĩnh vào — làm `__LINKEDIT` phình 2,6MB trên tổng 9,5MB của slice device. Strip
xong slice device còn 7,3MB, và các global symbol host cần để link vẫn nguyên vẹn (đã kiểm bằng cách
link thật một host giả lập vào framework đã strip).

Cái giá: **crash report chỉ symbolicate được bằng dSYM.** Không có nó thì stack trace chỉ còn địa
chỉ trần — kể cả phần Kotlin.

`build-xcframework.sh` đặt dSYM **cạnh** xcframework:

```
iosPromotionUI/build/
├── PromotionSDKUI.xcframework          ← giao cho host
└── PromotionSDKUI.framework.dSYM       ← GIỮ LẠI, đừng để rơi
```

> ⚠️ Thư mục `build/` bị `rm -rf` ở **đầu** mỗi lần chạy script. dSYM không được archive đi nơi khác
> thì lần build sau là mất vĩnh viễn, và mọi crash report của bản đã phát hành thành vô dụng —
> không dựng lại được, vì UUID của bản build mới sẽ khác.

Vì sao để cạnh chứ không nhét vào trong bằng `create-xcframework -debug-symbols` (cách "đúng sách"):
riêng dSYM device đã **26MB**, cộng bản simulator thì gói phân phối phồng từ ~35MB lên hơn 100MB.
Host không cần dSYM để build — chỉ người giữ bản phát hành cần, lúc đọc crash report. Nên nó là
**artifact của quy trình release**, không phải thứ gửi cho host.

Kiểm tra dSYM đúng với binary đang phát hành (hai UUID phải trùng):

```bash
dwarfdump --uuid iosPromotionUI/build/PromotionSDKUI.framework.dSYM/Contents/Resources/DWARF/PromotionSDKUI
dwarfdump --uuid iosPromotionUI/build/PromotionSDKUI.xcframework/ios-arm64/PromotionSDKUI.framework/PromotionSDKUI
```

### 6.2. Slice — host không phải khai gì

XCFramework có hai slice, cùng cách phân phối như bản `VDSPromotionSDK.xcframework` cũ:

| Slice | Dùng khi nào |
|---|---|
| `ios-arm64` | Bản lên App Store — **chỉ slice này ship** |
| `ios-arm64_x86_64-simulator` | Dev của app host chạy simulator (cả Apple Silicon lẫn Mac Intel) |

Slice simulator **không** ship, nhưng vẫn bắt buộc phải có: thiếu nó thì dev bên host bấm Run với
simulator là ăn lỗi link *"building for iOS Simulator, but linking in object file built for iOS"*.

> **Đã cân nhắc bỏ `x86_64`** (simulator trên Mac Intel) để cắt ~9,5MB slice simulator. Không làm,
> vì một xcframework **không thể tiêm build setting vào project host**: bỏ `x86_64` thì app host
> **buộc** phải tự khai `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64` — nếu không, CI build kiểu
> `xcodebuild -sdk iphonesimulator` (không kèm `-destination`) sẽ fail *"Unable to find module
> dependency"*. Đẩy một dòng cấu hình bắt buộc sang phía host không đáng đổi lấy 9,5MB, nhất là khi
> ba tối ưu còn lại (§6, asset/strip/abi.json) đã giảm gói mà host không phải làm gì.

---

## 7. Liên quan

- [PublicApi.md](./PublicApi.md) — bề mặt SDK cho host; đổi là breaking, ảnh hưởng chính sách version.
- [ProjectStructure.md](./ProjectStructure.md) — vai trò `:promotionLogic` / `:AndroidPromotionUI`.
- [AI_AGENT_RULES.md](./AI_AGENT_RULES.md) — điều 6 (thêm thư viện phải có lý do), điều 8 (đổi API thì cập nhật docs).
