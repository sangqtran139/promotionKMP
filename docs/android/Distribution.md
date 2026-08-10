# Distribution (Android) — Maven

SDK Android phát hành bằng **Maven**: publish `com.ttcn.promotion:promotionSDK` (kèm POM + Gradle
Module Metadata), host khai **một** dòng. Trước đây là file AAR — host chép hai file vào `libs/` rồi
tự khai toàn bộ dependency; §1 giữ lại vì nó giải thích *vì sao* đổi.

Tài liệu này: cách phát hành, cách host tích hợp, và — phần quan trọng — **ràng buộc nào biến mất
(§4), ràng buộc nào vẫn còn (§5)**. Đừng kỳ vọng Maven sửa những thứ nó không sửa.

> Phát hành **iOS** (XCFramework) là kênh riêng — xem [../ios/Distribution.md](../ios/Distribution.md).

**Trạng thái:** `:promotionLogic` và `:AndroidPromotionSDK` đã có `maven-publish`, publish được vào
**cả hai** đích: `~/.m2` (vòng lặp dev) và **JFrog Artifactory** nội bộ (phát hành thật) — §3.4.
`:androidApp` tiêu thụ SDK bằng toạ độ Maven, ưu tiên `~/.m2` rồi mới tới Artifactory.

**Máy mới thì chạy một lệnh:**

```bash
./scripts/build-android.sh              # publish SDK vào ~/.m2 → build app demo
./scripts/build-android.sh --skip-app   # chỉ publish SDK vào ~/.m2
./scripts/build-android.sh --remote     # publish LÊN Artifactory (phát hành)
./scripts/build-android.sh --help       # các tuỳ chọn: --clean, --install, --version
```

Script ép đúng thứ tự **publish trước, build app sau** — bỏ bước publish thì Gradle báo
`Could not find com.ttcn.promotion:promotionSDK`. Tương đương chạy tay:

```bash
./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal
./gradlew :androidApp:assembleDebug
```

## 1. Trước đây: file AAR — và ba chỗ đau

`androidApp/build.gradle.kts` từng tiêu thụ SDK bằng file, y như host thật:

```kotlin
implementation(fileTree("libs") { include("*.aar") })   // promotionLogic.aar + AndroidPromotionSDK-1.0.0-release.aar

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
dependencies { implementation("com.ttcn.promotion:promotionSDK:1.0.0") }
```

Gradle đọc metadata → tự kéo `promotionLogic`, Ktor, coroutines, AppCompat, Glide, Gson… đúng
version SDK đã compile. Hai mươi dòng khai tay biến mất, và cùng với chúng là cả hai lớp lỗi runtime
ở §1.

**`implementation` vẫn là `implementation`.** Trong metadata, `implementation(projects.promotionLogic)`
xuất hiện ở scope **runtime**, không phải compile. Nghĩa là host kéo được `promotionLogic` để chạy
nhưng **không** thấy `com.ttcn.promotionsdk.*` trên compile classpath — đúng ranh giới mà
`AndroidPromotionSDK/build.gradle.kts` đang cố giữ. Cách file-AAR hiện tại thì ngược lại: host phải tự
`implementation(libs.ktor…)`, nên Ktor và coroutines **nằm luôn trên compile classpath của host** —
rò rỉ thứ đáng lẽ giấu.

---

## 3. Làm như thế nào

### 3.1. Toạ độ

Hai module cần `group` + `version` để Gradle biết dịch `projects.promotionLogic` thành toạ độ Maven:

| Module | groupId | artifactId | Đổi tên được? |
|---|---|---|---|
| `:promotionLogic` | `$SDK_GROUP` | `promotionLogic` | **Không** — xem cảnh báo dưới |
| `:AndroidPromotionSDK` | `$SDK_GROUP` | `promotionSDK` | Được — host khai thẳng toạ độ này |

Cả **groupId** và **version** đều là property trong `gradle.properties` — một nguồn cho bốn nơi đọc
(hai module SDK, `settings.gradle.kts`, `:androidApp`):

```properties
SDK_GROUP=com.ttcn.promotion
SDK_VERSION=1.0.0
```

Override khi build: `-PSDK_GROUP=… -PSDK_VERSION=…`.

> **Đổi `SDK_GROUP` là breaking.** Mọi host đang khai `com.ttcn.promotion:promotionSDK:x` sẽ nhận
> `Could not find` — Gradle không có cơ chế "đổi tên có chuyển hướng" cho toạ độ Maven. Đổi thì phải
> bump major, giữ bản group cũ trên Artifactory cho host chưa kịp chuyển, và báo đối tác.

> **artifactId của lõi phải trùng tên module.** `:AndroidPromotionSDK` khai
> `implementation(projects.promotionLogic)`, và Gradle ghi vào POM của nó toạ độ `group:<tên-module>`
> = `com.ttcn.promotion:promotionLogic`. Rename ở publication (thành `promotion-logic` chẳng hạn)
> **không** đổi được toạ độ trong POM đó: POM vẫn trỏ `promotionLogic`, mà trên repo chỉ có
> `promotion-logic` → host nhận `Could not find com.ttcn.promotion:promotionLogic`. Đã dính thật, và
> configuration cache còn giấu lỗi một lượt (build "xanh" nhờ POM cũ trong cache).
>
> `:AndroidPromotionSDK` **không** dính ràng buộc này — không module nào trỏ vào nó bằng
> `projects.…`, host gõ toạ độ bằng tay — nên nó publish dưới tên `promotionSDK` cho đối xứng với
> `promotionLogic`. Muốn đổi cả tên lõi thì phải đổi **tên module** trong `settings.gradle.kts`.

### 3.2. `:AndroidPromotionSDK` (thư viện Android thường)

```kotlin
plugins {
    id("com.android.library")
    id("maven-publish")
}

group = sdkGroup                  // SDK_GROUP, đọc từ gradle.properties
version = sdkVersion              // SDK_VERSION

android {
    publishing {
        // KHÔNG `withSourcesJar()`: sources jar cho host mở nguyên văn cả 98 file, đúng thứ mà
        // `internal` đang che. Xem PublicApi.md §6.1.
        singleVariant("release")
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
            artifactId = "promotionSDK"        // đổi tên ở đây an toàn — §3.1
        }
    }
    // Không khai `repositories` ở đây: repo đích nằm ở build.gradle.kts gốc (§3.4), còn
    // `publishToMavenLocal` là task built-in của maven-publish nên ~/.m2 không cần khai gì.
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

group = sdkGroup
version = sdkVersion

kotlin {
    // KMP mặc định publish kèm sources.jar. Đây là lõi nghiệp vụ, đẩy sources lên Artifactory là
    // dâng nguyên source cho host đọc. Vẫn sinh sources.jar cho build nội bộ, chỉ không publish.
    withSourcesJar(publish = false)
}

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
├── promotionSDK/1.0.0/promotionSDK-1.0.0-release.aar   ← host Android khai cái này
└── promotionLogic/1.0.0/promotionLogic-1.0.0.aar     ← lõi, promotionSDK tự kéo về
```

> Muốn publish cả iOS về sau (consumer Kotlin/Native) thì **bỏ** khối `afterEvaluate` này — KMP quay
> lại 5 package, và cấu trúc module gốc lúc đó là cần thiết.
>
> `afterEvaluate` không phải thừa: KMP đặt artifactId **sau** giai đoạn cấu hình, nên đụng vào
> publication sớm hơn thì sửa hụt (đổi được bản android, bỏ sót iOS — không lỗi, không cảnh báo).

### 3.4. Repo đích: `~/.m2` (dev) + JFrog Artifactory (phát hành)

Hai đích, **không** loại trừ nhau — chọn theo việc đang làm:

| Đích | Lệnh | Dùng khi |
|---|---|---|
| `~/.m2/repository` | `./scripts/build-android.sh` | Vòng lặp dev: sửa SDK → build app demo ngay |
| Artifactory | `./scripts/build-android.sh --remote` | Phát hành cho host/đối tác |
| Artifactory (có hỏi version) | `./scripts/publish-android.sh` | Phát hành thật — xem bên dưới |

Việc **phát hành** (khác vòng lặp dev) dùng `./scripts/publish-android.sh`: script hỏi version cần
publish (mặc định lấy `SDK_VERSION` trong `gradle.properties`), kiểm tra định dạng + credentials,
hỏi trước khi đẩy, và cảnh báo nếu version đó **đã có trên repo** — repo release bật "immutable" nên
đẩy đè sẽ bị từ chối *sau khi* đã build xong. Đích chọn bằng `--target`:
`viettelmoney` (mặc định, `vn.viettelpay.library:promotion`), `artifactory` (`$SDK_GROUP:promotionSDK`),
`local` (~/.m2, để thử trước). `--help` liệt kê đủ tuỳ chọn.

`~/.m2` không cần khai gì trong script Gradle: `publishToMavenLocal` là task **built-in** của
`maven-publish`. Repo Artifactory khai **một lần ở `build.gradle.kts` gốc** cho cả hai module —
chúng publish vào cùng một nơi, tách ra hai chỗ chỉ tạo cơ hội lệch nhau:

```kotlin
// build.gradle.kts (gốc)
subprojects {
    pluginManager.withPlugin("maven-publish") {
        // Thiếu artifactoryUrl → không đăng ký repo. Máy dev chưa có credentials vẫn build được.
        val baseUrl = artifactoryUrl.orNull?.trimEnd('/') ?: return@withPlugin
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "artifactory"          // → task publish…ToArtifactoryRepository
                    url = uri("$baseUrl/$artifactoryRepoKey")
                    isAllowInsecureProtocol = baseUrl.startsWith("http://")
                    credentials { username = …; password = … }
                }
            }
        }
    }
}
```

**Cấu hình — để ở `~/.gradle/gradle.properties`, KHÔNG commit:**

```properties
artifactoryUrl=https://<host>/artifactory
artifactoryUser=<user>
artifactoryPassword=<identity token — đừng dùng mật khẩu đăng nhập>
```

Trên CI dùng biến môi trường thay thế: `ARTIFACTORY_URL` / `ARTIFACTORY_USER` /
`ARTIFACTORY_PASSWORD` (property có độ ưu tiên cao hơn env).

Bên Artifactory cần **hai** repo kiểu Maven (mặc định script trỏ tới tên chuẩn của JFrog, đổi được
bằng `artifactoryReleasesRepo` / `artifactorySnapshotsRepo`):

| Repo | Version rơi vào đây khi | Ghi chú |
|---|---|---|
| `libs-release-local` | `SDK_VERSION` **không** kết thúc bằng `-SNAPSHOT` | Bật **immutable/không cho ghi đè** — đẩy trùng version bị từ chối, đúng như mong muốn |
| `libs-snapshot-local` | `SDK_VERSION` kết thúc bằng `-SNAPSHOT` | Cho ghi đè, dùng khi tích hợp thử với host |

Việc chọn repo đọc thẳng `SDK_VERSION` chứ **không** dùng `project.version`: callback
`pluginManager.withPlugin` chạy lúc module áp plugin, tức **trước** dòng `version = sdkVersion` trong
script của module đó — lúc ấy `project.version` vẫn là `"unspecified"` và sẽ chọn nhầm repo release
cho một bản snapshot.

**Vài cái bẫy đã biết:**

- **`http://`.** Gradle 7+ chặn repo http. Config trên tự bật `isAllowInsecureProtocol` **chỉ khi**
  URL thật sự là http — Artifactory nội bộ hay rơi vào trường hợp này (như Bitbucket của team).
- **URL kết thúc ở `/artifactory`**, không kèm tên repo — code tự nối `/<repo-key>`. Dán nhầm URL
  đầy đủ từ UI của JFrog thì thành `…/artifactory/libs-release-local/libs-release-local`.
- **401 mà credentials đúng**: Artifactory thường tắt basic auth bằng mật khẩu; phải dùng
  **identity token** (User Profile → Generate Identity Token).
- **SNAPSHOT bị cache 24 giờ** ở phía host. Ai tiêu thụ bản `-SNAPSHOT` phải thêm
  `configurations.all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }`.
- **Token nằm trong configuration cache.** Project bật `org.gradle.configuration-cache=true`, nên
  giá trị credentials đọc lúc cấu hình được ghi vào `.gradle/configuration-cache/` trên máy. Thư mục
  đó đã nằm trong `.gitignore` — nhưng đừng chép nguyên thư mục `.gradle` cho người khác, và trên
  CI thì dùng token có hạn/thu hồi được.

### 3.5. `androidApp` sau khi đổi

```kotlin
// settings.gradle.kts — cả hai repo đều CHỈ mở cho group của SDK. Thả rông thì chúng tranh resolve
// với mọi thư viện khác (repo nội bộ proxy thiếu một version androidx là build đứt).
dependencyResolutionManagement {
    repositories {
        // ~/.m2 đứng TRƯỚC: sửa SDK → publishToMavenLocal → build app, không phải đợi đẩy lên server.
        // Mặt trái: bản local cũ CHE bản trên Artifactory. Nghi ngờ thì xoá thư mục group trong ~/.m2.
        mavenLocal { content { includeGroup(sdkGroup) } }
        // Chỉ đăng ký khi có artifactoryUrl — thiếu thì im lặng bỏ qua, build vẫn chạy bằng ~/.m2.
        maven {
            name = "artifactory"
            url = uri("$artifactoryUrl/$repoKey")
            credentials { … }
            content { includeGroup(sdkGroup) }
        }
        google { … }
        mavenCentral()
    }
}
```
```kotlin
// androidApp/build.gradle.kts
dependencies {
    implementation("$sdkGroup:promotionSDK:$sdkVersion")   // hết fileTree, 20 dòng còn 4

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
+--- com.ttcn.promotion:promotionSDK:1.0.0
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

**Sửa tận gốc** = trong `:AndroidPromotionSDK`, đổi sang `api(...)` đúng những lib nằm trong signature
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
- **Cần hạ tầng.** `mavenLocal()` chỉ là cái máy của bạn — chia cho đối tác thì phải qua
  Artifactory (§3.4). Cấu hình Gradle đã xong, phần **còn lại là việc của hạ tầng**: tạo repo
  release/snapshot, cấp identity token cho từng dev/CI, và mở **quyền đọc** cho tài khoản của đối
  tác (họ cũng phải khai `artifactoryUrl` + credentials phía họ — repo nội bộ không ẩn danh được).
- **Cần kỷ luật version.** Maven không nghĩ hộ: bump version, changelog, và **breaking API vẫn là
  breaking** (AI_AGENT_RULES điều 7 — đổi public API thì cập nhật [PublicApi.md](../common/PublicApi.md)).
- **ProGuard/R8 không đổi.** `consumerProguardFiles("consumer-rules.pro")` đi kèm AAR ở **cả hai**
  cách. Đây không phải lý do để chuyển.
- **`minSdk`, `compileSdk`, `nonTransitiveRClass`, namespace** — không liên quan, giữ nguyên.
- **Không sửa được lỗi thiết kế.** Nếu SDK rò rỉ type nội bộ ra public API thì Maven vẫn ship y
  nguyên chỗ rò đó.

## 6. Liên quan

- [../common/PublicApi.md](../common/PublicApi.md) — bề mặt SDK cho host; đổi là breaking, ảnh hưởng chính sách version.
- [../common/ProjectStructure.md](../common/ProjectStructure.md) — vai trò `:promotionLogic` / `:AndroidPromotionSDK`.
- [../ios/Distribution.md](../ios/Distribution.md) — kênh phát hành iOS (XCFramework).
- [../AI_AGENT_RULES.md](../AI_AGENT_RULES.md) — điều 6 (thêm thư viện phải có lý do), điều 8 (đổi API thì cập nhật docs).
