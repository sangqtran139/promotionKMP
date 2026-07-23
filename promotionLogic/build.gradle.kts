import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
    // Bridge Kotlin StateFlow/suspend/sealed → Swift cho tầng UI-logic dùng chung (MyPromotionStore).
    // Hỗ trợ Kotlin 2.0.0–2.4.0 (đang 2.2.0). Chỉ tác động framework iOS, Android không đổi.
    alias(libs.plugins.skie)
    alias(libs.plugins.kover)
    `maven-publish`
}

val sdkVersion = (project.findProperty("SDK_VERSION") as String?) ?: "1.0.0"

// Toạ độ Maven: `com.ttcn.promotion:promotion-logic:<SDK_VERSION>`.
// KMP **tự sinh publication** cho mọi target khi có plugin maven-publish — không tạo tay
// MavenPublication như bên :AndroidPromotionSDK. Xem docs/Distribution.md §3.3.
group = "com.ttcn.promotion"
version = sdkVersion

publishing {
    repositories {
        // Bước 1: ~/.m2. Đổi sang Nexus/Artifactory khi luồng đã thông — Distribution.md §3.4.
        mavenLocal()
    }
}

// Chỉ phát hành **một** package Android: `com.ttcn.promotion:promotionLogic` = AAR của target
// android. Không có module trung gian, và **không đổi tên** — artifactId giữ đúng tên module.
//
// Mặc định KMP publish 5 package: mỗi target một cái (`promotionLogic-android`,
// `promotionLogic-iosarm64`…) cộng một "module gốc" `promotionLogic` chỉ chứa metadata trỏ sang
// chúng. Ở đây iOS **không** đi qua Maven (nó link `PromotionLogic.xcframework`, xem khối
// `XCFramework` bên dưới) nên chỉ còn một target — bỏ module gốc, cho variant android lấy thẳng
// tên module.
//
// > **Artifact này phải giữ đúng tên module** — khác `:AndroidPromotionSDK` (publish dưới tên
// > `promotionUI`, đổi thoải mái vì host khai thẳng toạ độ đó). Lý do: `:AndroidPromotionSDK` khai
// > `implementation(projects.promotionLogic)`, và Gradle ghi vào POM của nó toạ độ
// > `group:<tên-module>` = `com.ttcn.promotion:promotionLogic`. Rename ở publication **không** đổi
// > được toạ độ đó → POM trỏ một đằng, repo có một nẻo, host nhận
// > `Could not find com.ttcn.promotion:promotionLogic`. Đã dính thật. Muốn tên khác thì phải đổi
// > tên module trong `settings.gradle.kts`.
//
// `afterEvaluate` là bắt buộc: KMP đặt artifactId sau giai đoạn cấu hình.
afterEvaluate {
    publishing.publications.withType<MavenPublication>().configureEach {
        if (name == "android") artifactId = project.name
    }

    // Tắt publication của các target còn lại (kotlinMultiplatform + 3 bản iOS).
    // Dùng `enabled` (đánh giá lúc cấu hình) chứ **không** `onlyIf { … }`: lambda của onlyIf giữ
    // tham chiếu tới script object, configuration cache không serialize được → build fail.
    tasks.withType<AbstractPublishToMaven>().configureEach {
        enabled = publication.name == "android"
    }
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
        // Nhường namespace `com.ttcn.promotionsdk` cho AndroidPromotionSDK, để `R` và
        // `databinding.*` của UI resolve đúng như trong SDK gốc, không phải sửa import.
        namespace = "com.ttcn.promotionsdk.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
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

// ─── Coverage (Kover) ─────────────────────────────────────────────────────────
//
// Đo trên target **android** (JVM) — Kover cần bytecode JVM nên không đo được Kotlin/Native.
// Test ở `commonTest` chạy trên CẢ hai target, nên số liệu này phản ánh đúng `commonMain`.
//
//   ./gradlew :promotionLogic:koverHtmlReport   → build/reports/kover/html/index.html
//   ./gradlew :promotionLogic:koverXmlReport    → cho CI
//   ./gradlew :promotionLogic:koverVerify       → gác ngưỡng
kover {
    reports {
        filters {
            excludes {
                // DTO thuần dữ liệu do kotlinx.serialization sinh accessor — đo không có ý nghĩa.
                classes("*.*Dto", "*.*Response", "*.*Request", "*${'$'}serializer")
                // Cầu sang nền tảng: thân hàm nằm ở androidMain/iosMain, không phải commonMain.
                classes("*.SdkLockKt", "*.PromotionClockKt")
            }
        }
        verify {
            // Ngưỡng đặt SÁT dưới mức hiện tại (LINE 81%) để mọi PR làm tụt coverage là fail ngay,
            // chứ không phải mục tiêu để phấn đấu. Nâng dần khi bộ test dày lên.
            rule("Line coverage của commonMain") {
                bound {
                    minValue = 80
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
        }
    }
}
