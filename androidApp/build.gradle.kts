import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

// AGP 8.x không tự căn JVM target Kotlin theo Java như AGP 9 → set tay khớp compileOptions (17).
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

android {
    namespace = "com.ttcn.promotionsdk.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.ttcn.promotionsdk.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

val sdkVersion = (project.findProperty("SDK_VERSION") as String?) ?: "1.0.0"

dependencies {
    // ─── SDK: nạp từ Maven, đúng như app host của đối tác ────────────────────────────────
    // Artifact mang theo POM/metadata → Gradle tự kéo promotion-logic, Ktor, AppCompat, Glide…
    // đúng version SDK đã compile. Host **không** phải khai tay dependency nào của SDK.
    //
    // Trước khi build app, phải có artifact trong ~/.m2:
    //     ./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionUI:publishToMavenLocal
    //
    // Sửa SDK xong mà quên publish thì app vẫn build với bản cũ — im lặng, y như "quên syncSdkAars"
    // ngày trước. Vòng lặp dev nhanh thì dùng `implementation(projects.androidPromotionUI)`;
    // để nghiệm thu bộ artifact như host thật thì giữ dòng dưới. Xem docs/Distribution.md §5.
    implementation("com.ttcn.promotion:promotionUI:$sdkVersion")

    // ─── androidx/material: HOST vẫn phải khai ───────────────────────────────────────────
    // Hai lý do khác nhau, đừng lẫn:
    //
    // 1. App demo TỰ dùng: layout của nó có MaterialButton, ConstraintLayout, TabLayout…
    //    Host thật cũng phải khai những gì chính app mình dùng — chuyện bình thường.
    //
    // 2. SDK **phơi androidx ra public API** nhưng khai `implementation`: `PRMBaseFragment<VB> :
    //    Fragment()`, `PRMBaseActivity<VB> : AppCompatActivity()`, `openMyPromotion(FragmentActivity)`.
    //    `implementation` → metadata đặt chúng ở scope **runtime** → host KHÔNG thấy lúc compile,
    //    nên `class X : PRMBaseFragment<B>()` không biên dịch được nếu thiếu dòng fragment/appcompat
    //    dưới đây. Muốn host khỏi phải đoán thì SDK đổi sang `api(...)` cho đúng những lib nằm trong
    //    signature public — xem docs/Distribution.md §5.
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // ─── Của riêng app demo ──────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    // PromotionTestLoginManager gọi API lấy token. SDK **không** dùng Retrofit.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
}
