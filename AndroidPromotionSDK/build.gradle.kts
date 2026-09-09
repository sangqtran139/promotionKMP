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
val sdkGroup = (project.findProperty("SDK_GROUP") as String?) ?: "vn.viettelpay.library"

// Toạ độ Maven. Cần `group` + `version` để Gradle dịch `projects.promotionLogic` bên dưới thành
// toạ độ thật trong metadata — không có thì publish hỏng. Xem docs/android/Distribution.md.
group = sdkGroup
version = sdkVersion

android {
    // `R`, `BuildConfig` và `databinding.*` sinh ra ở **com.ttcn.prm** — đúng bằng giá trị dưới đây.
    // Comment cũ ghi `com.ttcn.promotionsdk.*`; đó là namespace của module LÕI (`:promotionLogic`),
    // không phải của module này. Ai đọc rồi đi tìm `com.ttcn.promotionsdk.R` sẽ không thấy gì.
    namespace = "com.ttcn.prm"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // Rule ở `lint.xml` cạnh file này. `checkDependencies = false`: chỉ soi module này, không lôi
    // `promotionLogic` (Kotlin thuần, không có resource) vào.
    lint {
        lintConfig = file("lint.xml")
        abortOnError = true
        // Bật sau khi hạ hết warning còn lại — bật ngay thì phải kèm baseline, mà baseline là chỗ
        // vi phạm đi vào rồi nằm đó mãi.
        // warningsAsErrors = true
    }

    buildFeatures {
        // `dataBinding` đã TẮT. 20/24 layout từng bọc thẻ `<layout>` — thứ bắt trình biên dịch Data
        // Binding xử lý cả 20 file — nhưng **không file nào có `<data>`/`<variable>`** và không chỗ
        // nào trong Kotlin dùng `DataBindingUtil`/`ViewDataBinding`. Toàn bộ code đi qua View
        // Binding (`PrmXxxBinding.inflate`). Tức là trả phí biên dịch cho một tính năng không dùng.
        //
        // Thứ tự bắt buộc khi gỡ: bỏ thẻ `<layout>` ở 20 file TRƯỚC, rồi mới tắt cờ này. Tắt trước
        // là 20 file thành XML không hợp lệ.
        dataBinding = false
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
 * Phát hành `$SDK_GROUP:promotion:<SDK_VERSION>` — cặp đôi với `promotionLogic`.
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
            artifactId = "promotion"
        }
    }
}

dependencies {
    // Nghiệp vụ đến từ đây. `implementation` chứ không `api`: host chỉ tích hợp AndroidPromotionSDK,
    // nên `com.ttcn.promotionsdk.*` phải nằm ngoài compile classpath của host. Mọi model của
    // lõi được map sang DTO public ở `ui/entry/api` — xem `PromotionSDKApi`.
    implementation(projects.promotionLogic)

    implementation(libs.kotlinx.coroutines.core)

    // ─── `api` vì type nằm trong CHỮ KÝ PUBLIC ────────────────────────────────────────────
    //
    // `implementation` đặt dependency vào scope **runtime** trong metadata, nên host KHÔNG thấy nó
    // lúc biên dịch. Với hai artifact dưới đây thì host bắt buộc phải thấy:
    //
    //  - `fragment`: `PromotionSDK.openMyPromotion(activity: FragmentActivity)` và 5 hàm khác nhận
    //    `FragmentActivity`. Host không có nó trên compile classpath thì không gọi được hàm nào.
    //  - `constraintlayout`: `PRMEndowView : ConstraintLayout`. Host đặt widget này vào layout XML
    //    **và** tham chiếu nó trong code; compiler cần cả chuỗi supertype để resolve member.
    //
    // Hệ quả trước khi sửa: host phải tự đoán ra và tự khai lại — đúng thứ `api` sinh ra để tránh.
    // Xem `docs/android/Distribution.md`.
    api(libs.androidx.fragment.ktx)
    api(libs.androidx.constraintlayout)

    // ─── `implementation` — chỉ dùng bên trong, host không thấy ───────────────────────────
    //
    // Không khai core-ktx: bản 1.19.0 trong catalog đòi AGP 9.1.0, còn project đang ở 9.0.1.
    // appcompat + fragment-ktx đã kéo androidx.core về ở version tương thích.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.shimmer)
    // `sdp-android` đã GỠ. Resource của thư viện Android merge thẳng vào app host, nên nó đổ
    // ~600 dimens × 29 bucket vào `R` của host — tên `_1sdp`, `_2sdp`… không prefix, không
    // namespace; host cũng dùng sdp mà khác version là xung đột, host không dùng thì vẫn phải mang.
    //
    // Thay bằng 55 giá trị SDK thật sự dùng, sinh vào `res/values-sw*/prm_sdp.xml` với tên
    // `prm_Xsdp`. Giữ nguyên giá trị theo TỪNG BUCKET, không quy về dp cố định — sdp là "scalable
    // dp", thay bằng số cứng là layout đổi trên tablet và máy màn nhỏ.
    // Theme của host truyền vào dạng JSON và được parse ở tầng UI (PromotionThemeJson).
    implementation(libs.gson)

    // `ThemeHex` + `PromotionThemeJson` là Kotlin thuần (không android.graphics) nên chạy được
    // bằng unit test JVM, không cần Robolectric.
    testImplementation(libs.junit)
}
