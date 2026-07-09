plugins {
    // AGP 9 đã tích hợp sẵn Kotlin — thêm `org.jetbrains.kotlin.android` sẽ lỗi.
    // Không khai version vì AGP đã có trên classpath từ :androidApp.
    id("com.android.library")
}

val sdkVersion = (project.findProperty("SDK_VERSION") as String?) ?: "1.0.0"

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
}

// AAR đặt tên theo phiên bản: AndroidPromotionUI-<version>.aar
base {
    archivesName = "AndroidPromotionUI-$sdkVersion"
}

dependencies {
    // Nghiệp vụ đến từ đây. `api` để host thấy PromotionResult / domain model qua chữ ký public.
    api(projects.promotionLogic)

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
}
