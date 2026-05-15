plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    id("kotlin-parcelize")
}

android {
    namespace = "com.ttcn.promotionsdk"
    compileSdk = 35

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
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
