plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    id("kotlin-parcelize")
}

val sdkVersion = (project.findProperty("SDK_VERSION") as String?) ?: "1.0.0"

android {
    namespace = "com.ttcn.promotionsdk"
    compileSdk = 35

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 35
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"$sdkVersion\"")
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            // KT-73255: annotation trên constructor param (vd @ColorInt) áp cho cả param + field —
            // opt-in hành vi mặc định tương lai, tắt cảnh báo một lần cho toàn module.
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }
}

// Đặt tên file AAR theo phiên bản: vds-promotion-<version>.aar
base {
    archivesName = "vds-promotion-$sdkVersion"
}

dependencies {
    // Feature flag
    implementation(libs.unleash.android)

    // Core dependencies
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.timber)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // UI dependencies
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.appcompat)
    implementation(libs.fragment.ktx)
    implementation(libs.recyclerview)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.swiperefreshlayout)
    implementation(libs.sdp.android)
    implementation(libs.shimmer)
    implementation(libs.shapeofview)
}
