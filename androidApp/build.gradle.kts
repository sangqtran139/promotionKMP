plugins {
    // AGP 9 đã tích hợp sẵn Kotlin — không khai `org.jetbrains.kotlin.android`.
    alias(libs.plugins.androidApplication)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ─── SDK: nạp dạng FILE AAR, đúng như app host của đối tác ───────────────────────────
    // Không dùng `implementation(projects.androidPromotionUI)`: mục đích của module này là
    // nghiệm thu bộ AAR chạy được ngoài đời. Chạy `./gradlew syncSdkAars` để làm mới hai file.
    implementation(fileTree("libs") { include("*.aar") })

    // AAR **không mang theo dependency** (không có POM). Host phải khai báo tay toàn bộ
    // dependency của SDK — nếu thiếu sẽ crash `NoClassDefFoundError` lúc runtime, không lỗi build.
    //
    // Dependency của promotionLogic.aar:
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    // Dependency của AndroidPromotionUI.aar:
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.shimmer)
    implementation(libs.shapeofview)
    implementation(libs.sdp.android)
    implementation(libs.timber)
    implementation(libs.gson)

    // ─── Của riêng app demo ──────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    // PromotionTestLoginManager gọi API lấy token. SDK **không** dùng Retrofit.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
}

/**
 * Làm mới hai file AAR trong `libs/` từ output của `:promotionLogic` và `:AndroidPromotionUI`.
 *
 * Đây là điểm yếu cố hữu của cách tích hợp bằng file: sửa SDK xong mà quên chạy task này thì
 * app demo vẫn build với AAR cũ, **không có cảnh báo nào**.
 */
tasks.register<Copy>("syncSdkAars") {
    dependsOn(":promotionLogic:assemble", ":AndroidPromotionUI:assembleRelease")
    from(rootProject.file("promotionLogic/build/outputs/aar/promotionLogic.aar"))
    from(rootProject.file("AndroidPromotionUI/build/outputs/aar")) {
        include("AndroidPromotionUI-*-release.aar")
    }
    into(layout.projectDirectory.dir("libs"))
}
