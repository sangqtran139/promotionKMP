import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    alias(libs.plugins.kotlinAndroid)
    `maven-publish`
}

// AGP 8.x không tự căn JVM target Kotlin theo Java như AGP 9 → set tay khớp compileOptions (17).
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

val sdkVersion = (project.findProperty("SDK_VERSION") as String?) ?: "1.0.0"
val sdkGroup = (project.findProperty("SDK_GROUP") as String?) ?: "com.ttcn.promotion"

// Toạ độ Maven. Cần `group` + `version` để Gradle dịch `projects.promotionLogic` bên dưới thành
// toạ độ thật trong metadata — không có thì publish hỏng. Xem docs/android/Distribution.md.
group = sdkGroup
version = sdkVersion

android {
    // Giữ đúng namespace của SDK gốc: `R` và `databinding.*` sinh ra ở com.ttcn.promotionsdk.*
    namespace = "com.ttcn.prm"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        // Chỉ phát hành bản release, **không** kèm sources.jar: gửi kèm là dâng nguyên văn tầng UI
        // cho host đọc, đúng thứ `internal` đang che. Xem PublicApi.md §6.1.
        singleVariant("release")
    }
}

// AAR đặt tên theo phiên bản: AndroidPromotionSDK-<version>.aar
base {
    archivesName = "AndroidPromotionSDK-$sdkVersion"
}

/**
 * Phát hành `$SDK_GROUP:promotionSDK:<SDK_VERSION>` — cặp đôi với `promotionLogic`.
 *
 * Khác hẳn cách ship file AAR: artifact đi kèm POM + Gradle Module Metadata, nên host khai một dòng
 * và Gradle tự kéo `promotionLogic`, Ktor, Glide… đúng version. Các `implementation` dưới đây vào
 * metadata ở scope **runtime** → host không thấy chúng trên compile classpath.
 *
 * **Một publication cho mọi đích** (~/.m2, Artifactory chung, Artifactory Viettelmoney). Trước đây
 * có thêm publication "viettelmoney" khai tay, toạ độ khác, POM đọc nguyên văn từ
 * `publishing/pom.xml` — đã bỏ. Lý do: publication đó gắn AAR bằng `artifact(file)` nên KHÔNG có
 * SoftwareComponent, Gradle không sinh nổi POM lẫn `module.json`, và POM tay của nó không khai một
 * dependency nào → host build xanh rồi chết `NoClassDefFoundError`. `withXml` cũng không cứu được:
 * consumer Gradle đọc `module.json` trước, POM chỉ là bản dự phòng.
 *
 * Đổi artifactId ở đây **an toàn** vì host khai thẳng toạ độ này. Ngược lại, artifactId của
 * `:promotionLogic` thì **không** được đổi — xem ghi chú trong `promotionLogic/build.gradle.kts`.
 *
 * Chạy: `./gradlew :AndroidPromotionSDK:publishToMavenLocal` (xem docs/android/Distribution.md §3.4).
 *
 * Repo đích (Artifactory chung + Viettelmoney) khai ở **build.gradle.kts gốc** cho cả hai module.
 * `publishToMavenLocal` là task built-in nên không cần khai `mavenLocal()` ở đây.
 */
publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
            artifactId = "promotionSDK"
        }
    }
}

dependencies {
    // Nghiệp vụ đến từ đây. `implementation` chứ không `api`: host chỉ tích hợp AndroidPromotionSDK,
    // nên `com.ttcn.promotionsdk.*` phải nằm ngoài compile classpath của host. Mọi model của
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
    implementation(libs.sdp.android)
    implementation(libs.timber)
    // Theme của host truyền vào dạng JSON và được parse ở tầng UI (PromotionThemeJson).
    implementation(libs.gson)

    // `ThemeHex` + `PromotionThemeJson` là Kotlin thuần (không android.graphics) nên chạy được
    // bằng unit test JVM, không cần Robolectric.
    testImplementation(libs.junit)
}
