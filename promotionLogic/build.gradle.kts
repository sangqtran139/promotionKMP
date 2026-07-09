import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        // SdkLock là expect/actual class; cảnh báo Beta không có giá trị ở đây.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Gom ba target thành một PromotionLogic.xcframework để iosPromotionUI link vào.
    // iosX64 có mặt để slice simulator bao được cả Mac Intel, khớp với VDSPromotionSDK.xcframework hiện có.
    val xcf = XCFramework("PromotionLogic")

    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        // SDK iOS khai minos 13.0. Mặc định Kotlin/Native build cho iOS 15, khiến linker cảnh báo
        // "object file was built for newer iOS version (15.0) than being linked (13.0)".
        iosTarget.compilations.all {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add(
                    "-Xoverride-konan-properties=osVersionMin.ios_arm64=13.0;" +
                        "osVersionMin.ios_x64=13.0;osVersionMin.ios_simulator_arm64=13.0"
                )
            }
        }

        iosTarget.binaries.framework {
            baseName = "PromotionLogic"
            // Static: framework này được link tĩnh vào VDSPromotionSDK.framework,
            // nên host iOS chỉ nhận đúng MỘT xcframework.
            isStatic = true
            xcf.add(this)
        }
    }

    androidLibrary {
        // Nhường namespace `com.ttcn.promotionsdk` cho AndroidPromotionUI, để `R` và
        // `databinding.*` của UI resolve đúng như trong SDK gốc, không phải sửa import.
        namespace = "com.ttcn.promotionsdk.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}
