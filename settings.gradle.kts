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
// MỘT version cho cả hai module (`promotion` + `promotionLogic`) — xem gradle.properties.
// Ở đây nó chỉ dùng để chọn repo Artifactory (release hay snapshot); toạ độ thật thì
// `:androidApp` khai SDK_VERSION còn lõi về theo metadata.
val sdkVersion = providers.gradleProperty("SDK_VERSION").getOrElse("1.0.0")

val viettelmoneyUrl = "https://mobile-data.viettelmoney.vn/artifactory/gradle-viettelmoney"

// ─── `:androidApp` lấy SDK từ đâu ─────────────────────────────────────────────────────────────
//
// MỘT field quyết định, khai ở gradle.properties (hoặc `-PuseMavenLocal=…` cho một lần chạy):
//
//     useMavenLocal=true    → ~/.m2                      (vòng lặp dev, mặc định)
//     useMavenLocal=false   → Artifactory Viettelmoney   (nghiệm thu đúng thứ đối tác nhận)
//
// Trước đây phải comment/bỏ-comment cả khối repo trong file này — sai một dấu `//` là build đứt mà
// không rõ vì sao, và không ai nhớ nổi trạng thái hiện tại là gì. Nay đọc một dòng trong
// gradle.properties là biết.
//
// **Chỉ đăng ký MỘT repo cho `$sdkGroup`, không phải hai xếp chồng.** Có cả hai thì repo đứng trước
// lặng lẽ che repo sau: bản `~/.m2` cũ vẫn build xanh trong khi bạn tưởng đang test bản trên server.
// Thà thiếu hẳn và báo `Could not find` còn hơn đúng nhầm.
//
// Parse tay thay vì `String.toBoolean()`: hàm đó trả `false` cho MỌI thứ không phải "true", nên gõ
// nhầm `useMavenLocal=yes` sẽ lặng lẽ chuyển sang lấy từ server — sai kiểu khó lần nhất.
val useMavenLocalRaw = providers.gradleProperty("useMavenLocal").getOrElse("true").trim().lowercase()
require(useMavenLocalRaw == "true" || useMavenLocalRaw == "false") {
    "useMavenLocal phải là 'true' hoặc 'false' (nhận được: '$useMavenLocalRaw') — xem gradle.properties."
}
val useMavenLocal = useMavenLocalRaw == "true"

dependencyResolutionManagement {
    repositories {
        // Nguồn SDK — đúng MỘT trong hai, theo `useMavenLocal` (xem khối giải thích ở đầu file).
        // Cả hai đều `content { includeGroup(sdkGroup) }`: repo chỉ nhận group của SDK, không được
        // tranh resolve androidx/ktor với google()/mavenCentral().
        if (useMavenLocal) {
            // Hệ quả: **phải publish vào ~/.m2 trước khi build app**, không có đường lùi ra server.
            // `./scripts/build-android.sh local` ép sẵn đúng thứ tự publish → build.
            mavenLocal {
                content { includeGroup(sdkGroup) }
            }
        } else {
            // Credentials đọc từ `local.properties` (KHÔNG commit, đã gitignore) — cùng một chỗ với
            // chiều publish, đúng convention app host Viettelmoney đang dùng:
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
                content { includeGroup(sdkGroup) }
            }
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
            // Hai module dùng CHUNG một version nên luôn nằm cùng một repo — chỉ cần một key.
            // (Bản cũ tách `LOGIC_VERSION` nên phải đăng ký hai repo phòng khi lõi đang -SNAPSHOT
            // còn UI đã release; giờ không có tình huống đó nữa.) Giữ nguyên dạng danh sách để vòng
            // lặp đặt tên repo bên dưới không phải sửa.
            val repoKeys = listOf(
                if (sdkVersion.endsWith("SNAPSHOT")) snapshotsRepo else releasesRepo
            )
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
include(":networkKit")