plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ttcn.promotionsdk.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ttcn.promotionsdk.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(project(":vds-promotion"))
    implementation(libs.androidx.appcompat)

    implementation("com.mikepenz:fastadapter:5.7.0")
    implementation("com.mikepenz:fastadapter-extensions-binding:5.7.0")
}
