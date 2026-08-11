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

// Toạ độ SDK đọc từ gradle.properties — xem docs/android/Distribution.md §3.1. MỘT group duy nhất
// cho mọi đích: ~/.m2, Artifactory chung, Artifactory Viettelmoney.
val sdkGroup = providers.gradleProperty("SDK_GROUP").getOrElse("vn.viettelpay.library")
// HAI version độc lập — `promotion` theo SDK_VERSION, `promotionLogic` theo LOGIC_VERSION.
// Ở đây chúng chỉ dùng để chọn repo Artifactory (release hay snapshot); toạ độ thật thì
// `:androidApp` khai SDK_VERSION còn lõi về theo metadata.
val sdkVersion = providers.gradleProperty("SDK_VERSION").getOrElse("1.0.0")
val logicVersion = providers.gradleProperty("LOGIC_VERSION").getOrElse("1.0.0")

val viettelmoneyUrl = "https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney"

dependencyResolutionManagement {
    repositories {
        // ~/.m2 — TUỲ CHỌN, bật bằng `useMavenLocal=true`.
        //
        // Bật thì `~/.m2` đứng ĐẦU danh sách nên được hỏi trước; **không tìm thấy thì Gradle tự đi
        // tiếp** xuống Artifactory Viettelmoney rồi mới tới google()/mavenCentral(). Nghĩa là chỉ cần
        // publish MỘT module vào ~/.m2 cũng chạy được: module đó lấy bản local, module còn lại vẫn về
        // từ server. Chỉ khi repo có artifact nhưng SAI version/variant thì mới đứt hẳn, chứ "thiếu"
        // thì luôn rơi xuống repo dưới.
        //
        // Hai cách bật, `providers.gradleProperty` đọc được cả hai:
        //     ./gradlew :androidApp:assembleDebug -PuseMavenLocal=true     ← một lần
        //     useMavenLocal=true trong gradle.properties                   ← bật lâu dài cho máy dev
        // (gọn hơn cả: `./scripts/build-android.sh`, script tự truyền cờ này sau khi publish.)
        //
        // Mặc định TẮT là cố ý: bản local cũ trong ~/.m2 đứng trước sẽ CHE bản thật trên Artifactory
        // mà không báo gì, nên app demo "chạy được" bằng SDK cũ hàng tuần liền vẫn không ai biết.
        // Tắt đi thì `:androidApp` luôn lấy đúng thứ host thật lấy.
        //
        // Giới hạn đúng group của SDK — mavenLocal() thả rông sẽ tranh resolve với mọi thư viện khác
        // và cho ra build không tái lập được.
        val useMavenLocal = providers.gradleProperty("useMavenLocal").getOrElse("false").toBoolean()
        if (useMavenLocal) {
            mavenLocal {
                content { includeGroup(sdkGroup) }
            }
        }

        // Artifactory nội bộ Viettelmoney — NGUỒN THẬT của `:androidApp`, đối xứng với chiều publish
        // (`./gradlew publishSdkToViettelmoney`, repo cùng tên khai ở build.gradle.kts gốc).
        //
        // Credentials đọc từ `local.properties` (KHÔNG commit, đã gitignore) — cùng một chỗ cho cả
        // hai chiều resolve/publish, đúng convention app host Viettelmoney đang dùng:
        //     maven.username=<user>
        //     maven.password=<identity token>
        // Thiếu credentials thì Gradle báo 401 khi resolve `$sdkGroup:promotion`.
        val localProperties = java.util.Properties().apply {
            val file = rootDir.resolve("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        maven {
            name = "viettelmoney"
            url = uri(viettelmoneyUrl)
            credentials {
                username = localProperties.getProperty("maven.username", "")
                password = localProperties.getProperty("maven.password", "")
            }
            // Repo nội bộ KHÔNG được tranh resolve androidx/ktor với google()/mavenCentral().
            content { includeGroup(sdkGroup) }
        }
        // Artifactory: nguồn thật cho host. Chỉ đăng ký khi có artifactoryUrl (~/.gradle/gradle.properties
        // hoặc env ARTIFACTORY_*) — thiếu thì im lặng bỏ qua, build vẫn chạy bằng ~/.m2.
        val artifactoryUrl = providers.gradleProperty("artifactoryUrl")
            .orElse(providers.environmentVariable("ARTIFACTORY_URL"))
            .orNull
            ?.trimEnd('/')
        if (artifactoryUrl != null) {
            val snapshotsRepo = providers.gradleProperty("artifactorySnapshotsRepo")
                .getOrElse("libs-snapshot-local")
            val releasesRepo = providers.gradleProperty("artifactoryReleasesRepo")
                .getOrElse("libs-release-local")
            // Đăng ký repo cho CẢ hai module. Hai version độc lập nên chúng có thể nằm ở hai repo
            // khác nhau (lõi đang -SNAPSHOT trong khi UI đã release là chuyện thường) — chỉ đăng ký
            // theo mỗi SDK_VERSION thì Gradle không tìm ra `promotionLogic`. `distinct()` để trường
            // hợp thường gặp (cả hai cùng release) không đăng ký trùng một URL hai lần.
            val repoKeys = listOf(sdkVersion, logicVersion)
                .map { if (it.endsWith("SNAPSHOT")) snapshotsRepo else releasesRepo }
                .distinct()
            repoKeys.forEachIndexed { index, repoKey ->
                maven {
                    // Tên repo phải duy nhất trong cùng một RepositoryHandler.
                    name = if (index == 0) "artifactory" else "artifactory${repoKey.replaceFirstChar(Char::titlecase)}"
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