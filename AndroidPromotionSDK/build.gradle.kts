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
        // Chỉ phát hành bản release. Kèm sources để host debug vào trong SDK được.
        singleVariant("release") { withSourcesJar() }
    }
}

// AAR đặt tên theo phiên bản: AndroidPromotionSDK-<version>.aar
base {
    archivesName = "AndroidPromotionSDK-$sdkVersion"
}

/**
 * Phát hành `com.ttcn.promotion:promotionSDK:<SDK_VERSION>` — cặp đôi với `promotionLogic`.
 *
 * Khác hẳn cách ship file AAR: artifact đi kèm POM + Gradle Module Metadata, nên host khai một dòng
 * và Gradle tự kéo `promotionLogic`, Ktor, Glide… đúng version. Các `implementation` dưới đây vào
 * metadata ở scope **runtime** → host không thấy chúng trên compile classpath.
 *
 * Đổi artifactId ở đây **an toàn** vì host khai thẳng toạ độ này. Ngược lại, artifactId của
 * `:promotionLogic` thì **không** được đổi — xem ghi chú trong `promotionLogic/build.gradle.kts`.
 *
 * Chạy: `./gradlew :AndroidPromotionSDK:publishToMavenLocal` (xem docs/android/Distribution.md §3.4).
 *
 * Repo đích (Artifactory) khai ở **build.gradle.kts gốc** cho cả hai module. `publishToMavenLocal`
 * là task built-in nên không cần khai `mavenLocal()` ở đây.
 */
publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
            artifactId = "promotionSDK"
        }

        /**
         * Bản đẩy riêng cho Artifactory nội bộ Viettelmoney — toạ độ `vn.viettelpay.library:promotion`,
         * KHÁC hẳn publication "release" ở trên (`com.ttcn.promotion:promotionSDK`, POM Gradle tự sinh
         * từ dependency graph). Ở đây POM lấy nguyên văn từ `publishing/pom.xml` (viết tay, dependencies
         * scope `runtime`, trỏ `vn.viettelpay.library:promotion-logic`) — [pom.withXml] xoá sạch nội
         * dung Gradle tự sinh rồi ghép nguyên cây XML đọc từ file đó vào, KHÔNG suy từ
         * `implementation(...)` bên dưới. Sửa dependency thật thì sửa ở `publishing/pom.xml`, sửa ở
         * đây không có tác dụng.
         *
         * Artifact lấy thẳng file .aar đã build (`archivesName` = `AndroidPromotionSDK-$sdkVersion-release`)
         * thay vì `from(components["release"])`, để không bị Gradle tự sinh lại POM đè lên [pom.withXml].
         *
         * Chạy gộp cả 2 module: `./gradlew publishSdkToViettelmoney` (task ở build.gradle.kts gốc).
         * Repo "viettelmoney" (URL + credentials từ `local.properties`) khai ở đó.
         */
        create<MavenPublication>("viettelmoney") {
            groupId = "vn.viettelpay.library"
            artifactId = "promotion"
            version = sdkVersion

            // AGP tạo task `assembleRelease` sau giai đoạn cấu hình (giống lý do publication
            // "release" ở trên cũng phải đợi `afterEvaluate` mới `from(components["release"])`).
            afterEvaluate {
                artifact(layout.buildDirectory.file("outputs/aar/AndroidPromotionSDK-$sdkVersion-release.aar")) {
                    extension = "aar"
                    builtBy(tasks.named("assembleRelease"))
                }
            }

            // File plain + version plain (không phải `file(...)`/tham chiếu `sdkVersion` GỌI TRONG
            // lambda) — `withXml` chạy lúc thực thi task, đóng gói tham chiếu `Project`/script object
            // bên trong lambda phá configuration cache (`DefaultProject` không serialize được).
            // `handWrittenPom`/`resolvedVersion` chỉ là `File`/`String`, serialize bình thường.
            val handWrittenPom = file("$projectDir/publishing/pom.xml")
            val resolvedVersion = sdkVersion
            pom.withXml {
                // `@sdkVersion@` trong publishing/pom.xml: version của "promotion" và của dependency
                // "promotion-logic" (hai module bump song song) — thay bằng SDK_VERSION thật (`-PSDK_VERSION=x.y.z`,
                // mặc định "1.0.0") để đường dẫn upload (groupId/artifactId/**version**) và nội dung
                // POM khớp nhau, không lệch như hardcode cứng "1.0.0".
                val xmlText = handWrittenPom.readText().replace("@sdkVersion@", resolvedVersion)
                val handWritten = groovy.xml.XmlParser().parseText(xmlText)
                val root = asNode()
                root.children().toList().forEach { root.remove(it as groovy.util.Node) }
                handWritten.children().forEach { root.append(it as groovy.util.Node) }
            }
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
    implementation(libs.shapeofview)
    implementation(libs.sdp.android)
    implementation(libs.timber)
    // Theme của host truyền vào dạng JSON và được parse ở tầng UI (PromotionThemeJson).
    implementation(libs.gson)

    // `ThemeHex` + `PromotionThemeJson` là Kotlin thuần (không android.graphics) nên chạy được
    // bằng unit test JVM, không cần Robolectric.
    testImplementation(libs.junit)
}
