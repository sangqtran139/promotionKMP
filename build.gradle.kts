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
// sẽ bị từ chối). Chọn theo hậu tố version, đọc thẳng SDK_VERSION chứ KHÔNG dùng `project.version`:
// callback `withPlugin` chạy lúc subproject áp plugin, tức TRƯỚC dòng `version = sdkVersion` trong
// script của nó — lúc đó `project.version` vẫn là "unspecified" và sẽ chọn nhầm repo.
val sdkVersion = providers.gradleProperty("SDK_VERSION").getOrElse("1.0.0")
val artifactoryRepoKey = if (sdkVersion.endsWith("SNAPSHOT")) {
    providers.gradleProperty("artifactorySnapshotsRepo").getOrElse("libs-snapshot-local")
} else {
    providers.gradleProperty("artifactoryReleasesRepo").getOrElse("libs-release-local")
}

// ─── Repo phát hành: Artifactory nội bộ Viettelmoney ──────────────────────────────────────────
//
// Đích RIÊNG, song song với repo "artifactory" ở trên — không thay thế. Publication đẩy vào đây là
// `MavenPublication("viettelmoney")` khai thủ công ở từng module (toạ độ `vn.viettelpay.library:…`,
// POM viết tay ở `<module>/publishing/pom.xml`), khác hẳn publication "release"/KMP mặc định
// (toạ độ `com.ttcn.promotion:…`, POM Gradle tự sinh từ dependency graph) đẩy vào repo "artifactory".
//
// Credentials đọc từ `local.properties` (KHÔNG commit, đã gitignore) thay vì gradle property/env
// như repo "artifactory" — theo đúng convention app host Viettelmoney đang dùng để RESOLVE
// dependencies (đối chiếu snippet họ đưa), giữ nhất quán một chỗ đọc credentials cho cả hai chiều.
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
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            repositories {
                // Không có URL → không đăng ký repo. Cố tình: máy dev chưa có credentials vẫn build và
                // publishToMavenLocal bình thường, thay vì fail lúc cấu hình vì thiếu property.
                val baseUrl = artifactoryUrl.orNull?.trimEnd('/')
                if (baseUrl != null) {
                    maven {
                        name = "artifactory"   // → task publish…ToArtifactoryRepository
                        url = uri("$baseUrl/$artifactoryRepoKey")
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

// Gộp cả hai module trong một lệnh — trùng tên đã seed sẵn trong local.properties.
// Chỉ đụng tới publication "viettelmoney" của từng module, không đả động publication "release".
tasks.register("publishSdkToViettelmoney") {
    group = "publishing"
    description = "Build và đẩy AndroidPromotionSDK + promotionLogic (toạ độ vn.viettelpay.library) " +
        "lên Artifactory nội bộ Viettelmoney."
    dependsOn(
        ":AndroidPromotionSDK:publishViettelmoneyPublicationToViettelmoneyRepository",
        ":promotionLogic:publishViettelmoneyPublicationToViettelmoneyRepository",
    )
}