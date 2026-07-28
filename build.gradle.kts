import org.gradle.api.publish.PublishingExtension

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

subprojects {
    pluginManager.withPlugin("maven-publish") {
        // Không có URL → không đăng ký repo. Cố tình: máy dev chưa có credentials vẫn build và
        // publishToMavenLocal bình thường, thay vì fail lúc cấu hình vì thiếu property.
        val baseUrl = artifactoryUrl.orNull?.trimEnd('/') ?: return@withPlugin

        extensions.configure<PublishingExtension> {
            repositories {
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
        }
    }
}