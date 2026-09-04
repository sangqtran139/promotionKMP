import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
}

// Vòng đời version RIÊNG với promotionLogic/AndroidPromotionSDK (`SDK_VERSION`) — module này không
// phải phần của Promotion SDK, mục tiêu dùng chung cho nhiều SDK Viettel khác nhau, nên không thể
// khoá chung nhịp phát hành với Promotion. Xem docs/common/SharedNetworkKit.md §Toạ độ phát hành.
val networkKitVersion = (project.findProperty("NETWORK_KIT_VERSION") as String?) ?: "0.1.0"
val sdkGroup = (project.findProperty("SDK_GROUP") as String?) ?: "vn.viettelpay.library"

group = sdkGroup
version = networkKitVersion

// Repo đích (Artifactory/Viettelmoney) đã khai chung cho mọi module có plugin `maven-publish` ở
// build.gradle.kts gốc — không cần khai lại ở đây.
//
// Chỉ publish publication "android": trên iOS, module này không đi qua Maven — nó được
// `promotionLogic` (hoặc consumer KMP khác trong cùng repo) thêm bằng `implementation(projects.networkKit)`,
// Kotlin/Native tự link tĩnh klib vào framework của consumer. Xem lý do đầy đủ (và tiền lệ) ở
// promotionLogic/build.gradle.kts — cùng một ràng buộc, cùng một cách xử lý.
afterEvaluate {
    publishing.publications.withType<MavenPublication>().configureEach {
        if (name == "android") artifactId = project.name
    }
    tasks.withType<AbstractPublishToMaven>().configureEach {
        enabled = publication.name == "android"
    }
}

kotlin {
    withSourcesJar(publish = false)

    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        // Cùng ràng buộc minOS 13.0 như promotionLogic (khớp deployment target của iosPromotionSDK) —
        // đặt sẵn từ bây giờ để không vỡ lúc UC10 link module này vào PromotionLogic.xcframework.
        iosTarget.compilations.all {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add(
                    "-Xoverride-konan-properties=osVersionMin.ios_arm64=13.0;" +
                        "osVersionMin.ios_x64=13.0;osVersionMin.ios_simulator_arm64=13.0",
                )
            }
        }
    }

    androidLibrary {
        namespace = "vn.viettelpay.networkkit"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.kotlinx.serialization.json)
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
