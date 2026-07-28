rootProject.name = "PromotionSDK"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Toạ độ SDK đọc từ gradle.properties — xem docs/android/Distribution.md §3.1.
val sdkGroup = providers.gradleProperty("SDK_GROUP").getOrElse("com.ttcn.promotion")
val sdkVersion = providers.gradleProperty("SDK_VERSION").getOrElse("1.0.0")

dependencyResolutionManagement {
    repositories {
        // SDK được :androidApp tiêu thụ dạng artifact Maven (docs/android/Distribution.md).
        // ~/.m2 đứng TRƯỚC Artifactory: vòng lặp dev sửa SDK → publishToMavenLocal → build app,
        // không phải đợi đẩy lên server. Mặt trái: bản local cũ sẽ CHE bản trên Artifactory — nghi
        // ngờ thì `rm -rf ~/.m2/repository/${sdkGroup.replace('.', '/')}`.
        //     ./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal
        // Giới hạn đúng group của SDK — mavenLocal() thả rông sẽ tranh resolve với mọi thư viện khác
        // và cho ra build không tái lập được.
        mavenLocal {
            content { includeGroup(sdkGroup) }
        }
        // Artifactory: nguồn thật cho host. Chỉ đăng ký khi có artifactoryUrl (~/.gradle/gradle.properties
        // hoặc env ARTIFACTORY_*) — thiếu thì im lặng bỏ qua, build vẫn chạy bằng ~/.m2.
        val artifactoryUrl = providers.gradleProperty("artifactoryUrl")
            .orElse(providers.environmentVariable("ARTIFACTORY_URL"))
            .orNull
            ?.trimEnd('/')
        if (artifactoryUrl != null) {
            val repoKey = if (sdkVersion.endsWith("SNAPSHOT")) {
                providers.gradleProperty("artifactorySnapshotsRepo").getOrElse("libs-snapshot-local")
            } else {
                providers.gradleProperty("artifactoryReleasesRepo").getOrElse("libs-release-local")
            }
            maven {
                name = "artifactory"
                url = uri("$artifactoryUrl/$repoKey")
                isAllowInsecureProtocol = artifactoryUrl.startsWith("http://")
                credentials {
                    username = providers.gradleProperty("artifactoryUser")
                        .orElse(providers.environmentVariable("ARTIFACTORY_USER")).orNull
                    password = providers.gradleProperty("artifactoryPassword")
                        .orElse(providers.environmentVariable("ARTIFACTORY_PASSWORD")).orNull
                }
                // Cũng giới hạn theo group SDK: repo nội bộ KHÔNG được tranh resolve androidx/ktor
                // với google()/mavenCentral() — nếu nó proxy thiếu một version thì build đứt.
                content { includeGroup(sdkGroup) }
            }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":promotionLogic")
include(":AndroidPromotionSDK")