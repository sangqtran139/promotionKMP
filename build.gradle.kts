import org.gradle.api.publish.PublishingExtension
import java.util.Properties

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

// ─── Repo phát hành: JFrog Artifactory ────────────────────────────────────────────────────────
//
// Khai MỘT chỗ cho mọi module có `maven-publish` (:promotionLogic, :AndroidPromotionSDK) — hai
// module publish vào cùng một nơi nên không có lý do gì để chúng lệch nhau.
// Chi tiết & cách tạo repo bên Artifactory: docs/android/Distribution.md §3.4.
//
//   ./gradlew :promotionLogic:publishAllPublicationsToArtifactoryRepository \
//             :AndroidPromotionSDK:publishAllPublicationsToArtifactoryRepository
//   (hoặc gọn hơn: ./scripts/build-android.sh --remote)
//
// `~/.m2` KHÔNG cần khai ở đây: `publishToMavenLocal` là task built-in của maven-publish.
val artifactoryUrl = providers.gradleProperty("artifactoryUrl")
    .orElse(providers.environmentVariable("ARTIFACTORY_URL"))
val artifactoryUser = providers.gradleProperty("artifactoryUser")
    .orElse(providers.environmentVariable("ARTIFACTORY_USER"))
val artifactoryPassword = providers.gradleProperty("artifactoryPassword")
    .orElse(providers.environmentVariable("ARTIFACTORY_PASSWORD"))

// Artifactory tách repo release/snapshot (repo release thường bật "immutable" — đẩy đè version cũ
// sẽ bị từ chối). Chọn theo hậu tố version, đọc thẳng property chứ KHÔNG dùng `project.version`:
// callback `withPlugin` chạy lúc subproject áp plugin, tức TRƯỚC dòng `version = …` trong script của
// nó — lúc đó `project.version` vẫn là "unspecified" và sẽ chọn nhầm repo.
//
// **Hai module hai version độc lập** (SDK_VERSION / LOGIC_VERSION — xem gradle.properties), nên repo
// đích phải tính RIÊNG cho từng module: bump `:promotionLogic` lên -SNAPSHOT trong khi
// `:AndroidPromotionSDK` vẫn là bản release là chuyện thường, mà một `artifactoryRepoKey` dùng chung
// sẽ đẩy cả hai vào cùng một repo — bản snapshot lọt vào repo release rồi kẹt luôn ở đó.
val sdkVersion = providers.gradleProperty("SDK_VERSION").getOrElse("1.0.0")
val logicVersion = providers.gradleProperty("LOGIC_VERSION").getOrElse("1.0.0")
val snapshotsRepo = providers.gradleProperty("artifactorySnapshotsRepo").getOrElse("libs-snapshot-local")
val releasesRepo = providers.gradleProperty("artifactoryReleasesRepo").getOrElse("libs-release-local")

fun artifactoryRepoKeyFor(moduleName: String): String {
    val version = if (moduleName == "promotionLogic") logicVersion else sdkVersion
    return if (version.endsWith("SNAPSHOT")) snapshotsRepo else releasesRepo
}

// ─── Repo phát hành: Artifactory nội bộ Viettelmoney ──────────────────────────────────────────
//
// Đích RIÊNG, song song với repo "artifactory" ở trên — không thay thế. Cả hai nhận **cùng một**
// publication ("release" ở `:AndroidPromotionSDK`, "android" ở `:promotionLogic`) với **cùng một** bộ
// toạ độ `$SDK_GROUP:promotion` / `$SDK_GROUP:promotionLogic`, POM + `module.json` do Gradle sinh từ
// dependency graph. Trước đây mỗi module còn một `MavenPublication("viettelmoney")` khai tay, toạ độ
// riêng, POM đọc nguyên văn từ `<module>/publishing/pom.xml` — đã bỏ, xem ghi chú trong
// `AndroidPromotionSDK/build.gradle.kts`.
//
// Credentials đọc từ `local.properties` (KHÔNG commit, đã gitignore) thay vì gradle property/env
// như repo "artifactory" — theo đúng convention app host Viettelmoney đang dùng để RESOLVE
// dependencies (đối chiếu snippet họ đưa), giữ nhất quán một chỗ đọc credentials cho cả hai chiều.
// `settings.gradle.kts` đọc đúng hai key này cho chiều resolve của `:androidApp`.
//
//   ./gradlew publishSdkToViettelmoney   (task gộp, định nghĩa cuối file)
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val viettelmoneyUrl = "https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney"
val viettelmoneyUser = localProperties.getProperty("maven.username", "")
val viettelmoneyPassword = localProperties.getProperty("maven.password", "")

subprojects {
    // Tên module, chốt ở đây rồi dùng bên trong: repo release/snapshot chọn theo version RIÊNG của
    // từng module (SDK_VERSION hay LOGIC_VERSION).
    val moduleName = name
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                // Không có URL → không đăng ký repo. Cố tình: máy dev chưa có credentials vẫn build và
                // publishToMavenLocal bình thường, thay vì fail lúc cấu hình vì thiếu property.
                val baseUrl = artifactoryUrl.orNull?.trimEnd('/')
                if (baseUrl != null) {
                    maven {
                        name = "artifactory"   // → task publish…ToArtifactoryRepository
                        url = uri("$baseUrl/${artifactoryRepoKeyFor(moduleName)}")
                        // Artifactory nội bộ hay chạy http (như Bitbucket của team). Gradle 7+ chặn
                        // http mặc định, chỉ mở đúng khi URL thật sự là http.
                        isAllowInsecureProtocol = baseUrl.startsWith("http://")
                        credentials {
                            username = artifactoryUser.orNull
                            password = artifactoryPassword.orNull
                        }
                    }
                }

                maven {
                    name = "viettelmoney"   // → task publish…ToViettelmoneyRepository
                    url = uri(viettelmoneyUrl)
                    credentials {
                        username = viettelmoneyUser
                        password = viettelmoneyPassword
                    }
                }
            }
        }
    }
}

// Gộp cả hai module trong một lệnh — credentials đã seed sẵn trong local.properties.
// `:promotionLogic` dùng publication "android" (KMP tự sinh), `:AndroidPromotionSDK` dùng "release"
// (AGP) — cùng bộ toạ độ, cùng POM auto-gen, chỉ khác repo đích so với lệnh publish Artifactory.
tasks.register("publishSdkToViettelmoney") {
    group = "publishing"
    description = "Build và đẩy AndroidPromotionSDK + promotionLogic (toạ độ \$SDK_GROUP) " +
        "lên Artifactory nội bộ Viettelmoney."
    dependsOn(
        ":AndroidPromotionSDK:publishReleasePublicationToViettelmoneyRepository",
        ":promotionLogic:publishAndroidPublicationToViettelmoneyRepository",
    )
}