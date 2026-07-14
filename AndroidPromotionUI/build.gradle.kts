plugins {
    // AGP 9 đã tích hợp sẵn Kotlin — thêm `org.jetbrains.kotlin.android` sẽ lỗi.
    // Không khai version vì AGP đã có trên classpath từ :androidApp.
    id("com.android.library")
    `maven-publish`
}

val sdkVersion = (project.findProperty("SDK_VERSION") as String?) ?: "1.0.0"

// Toạ độ Maven. Cần `group` + `version` để Gradle dịch `projects.promotionLogic` bên dưới thành
// toạ độ thật trong metadata — không có thì publish hỏng. Xem docs/Distribution.md.
group = "com.ttcn.promotion"
version = sdkVersion

android {
    // Giữ đúng namespace của SDK gốc: `R` và `databinding.*` sinh ra ở com.ttcn.promotionsdk.*
    namespace = "com.ttcn.promotionsdk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    buildFeatures {
        // SDK gốc dùng cả hai: layout bọc `<layout>` sinh binding kiểu Data Binding.
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        // Chỉ phát hành bản release. Kèm sources để host debug vào trong SDK được.
        singleVariant("release") { withSourcesJar() }
    }
}

// AAR đặt tên theo phiên bản: AndroidPromotionUI-<version>.aar
base {
    archivesName = "AndroidPromotionUI-$sdkVersion"
}

/**
 * Phát hành `com.ttcn.promotion:promotionUI:<SDK_VERSION>` — cặp đôi với `promotionLogic`.
 *
 * Khác hẳn cách ship file AAR: artifact đi kèm POM + Gradle Module Metadata, nên host khai một dòng
 * và Gradle tự kéo `promotionLogic`, Ktor, Glide… đúng version. Các `implementation` dưới đây vào
 * metadata ở scope **runtime** → host không thấy chúng trên compile classpath.
 *
 * Đổi artifactId ở đây **an toàn** vì host khai thẳng toạ độ này. Ngược lại, artifactId của
 * `:promotionLogic` thì **không** được đổi — xem ghi chú trong `promotionLogic/build.gradle.kts`.
 *
 * Chạy: `./gradlew :AndroidPromotionUI:publishToMavenLocal` (xem docs/Distribution.md §3.4).
 */
publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
            artifactId = "promotionUI"
        }
    }
    repositories {
        // Bước 1: ~/.m2. Đổi sang Nexus/Artifactory khi luồng đã thông — Distribution.md §3.4.
        mavenLocal()
    }
}

dependencies {
    // Nghiệp vụ đến từ đây. `implementation` chứ không `api`: host chỉ tích hợp AndroidPromotionUI,
    // nên `com.ttcn.promotionsdk.core.*` phải nằm ngoài compile classpath của host. Mọi model của
    // lõi được map sang DTO public ở `ui/entry/api` — xem `PromotionSDKApi`.
    implementation(projects.promotionLogic)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.appcompat)
    // Không khai core-ktx: bản 1.19.0 trong catalog đòi AGP 9.1.0, còn project đang ở 9.0.1.
    // appcompat + fragment-ktx đã kéo androidx.core về ở version tương thích.
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.shimmer)
    implementation(libs.shapeofview)
    implementation(libs.sdp.android)
    implementation(libs.timber)
    // Theme của host truyền vào dạng JSON và được parse ở tầng UI (PromotionThemeJson).
    implementation(libs.gson)

    // `ThemeHex` + `PromotionThemeJson` là Kotlin thuần (không android.graphics) nên chạy được
    // bằng unit test JVM, không cần Robolectric.
    testImplementation(libs.junit)
}
