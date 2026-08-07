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
val sdkGroup = (project.findProperty("SDK_GROUP") as String?) ?: "com.ttcn.promotion"

// Toạ độ Maven: `$SDK_GROUP:promotionLogic:<SDK_VERSION>` (cả hai từ gradle.properties).
// KMP **tự sinh publication** cho mọi target khi có plugin maven-publish — không tạo tay
// MavenPublication như bên :AndroidPromotionSDK. Xem docs/android/Distribution.md §3.3.
group = sdkGroup
version = sdkVersion

// Repo đích (Artifactory) khai ở **build.gradle.kts gốc** cho cả hai module — Distribution.md §3.4.
// `publishToMavenLocal` là task built-in, không cần khai `mavenLocal()` ở đây.

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

    // Tắt publication của các target còn lại (kotlinMultiplatform + 3 bản iOS). "viettelmoney" là
    // publication khai TAY bên dưới (không phải KMP tự sinh) — phải giữ nguyên, không nằm trong diện
    // bị tắt này.
    // Dùng `enabled` (đánh giá lúc cấu hình) chứ **không** `onlyIf { … }`: lambda của onlyIf giữ
    // tham chiếu tới script object, configuration cache không serialize được → build fail.
    tasks.withType<AbstractPublishToMaven>().configureEach {
        enabled = publication.name == "android" || publication.name == "viettelmoney"
    }
}

/**
 * Bản đẩy riêng cho Artifactory nội bộ Viettelmoney — toạ độ `vn.viettelpay.library:promotion-logic`,
 * cặp đôi với `AndroidPromotionSDK/build.gradle.kts` (publication cùng tên ở đó, toạ độ `…:promotion`).
 * Cùng cơ chế: [pom.withXml] thay nguyên cây XML bằng nội dung đọc từ `publishing/pom.xml` (viết tay,
 * dependencies scope `runtime`, đủ cả Ktor + kotlinx-serialization mà target android thật sự cần —
 * xem `sourceSets.commonMain`/`androidMain` ở trên) — KHÔNG suy tự động như publication "android" ở
 * KMP. Artifact lấy thẳng AAR đã build (`bundleAndroidMainAar`), không qua `from(components[...])`.
 *
 * Chạy gộp cả 2 module: `./gradlew publishSdkToViettelmoney` (task ở build.gradle.kts gốc, repo
 * "viettelmoney" + credentials từ `local.properties` cũng khai ở đó).
 */
publishing {
    publications {
        create<MavenPublication>("viettelmoney") {
            groupId = "vn.viettelpay.library"
            artifactId = "promotion-logic"
            version = sdkVersion

            // Plugin android-multiplatform-library tạo `bundleAndroidMainAar` sau giai đoạn cấu hình
            // (cùng lý do khối `afterEvaluate` phía trên phải đợi mới sửa được `artifactId`/`enabled`).
            afterEvaluate {
                artifact(layout.buildDirectory.file("outputs/aar/promotionLogic.aar")) {
                    extension = "aar"
                    builtBy(tasks.named("bundleAndroidMainAar"))
                }
            }

            // File plain + version plain — xem giải thích ở publication cùng tên trong
            // AndroidPromotionSDK/build.gradle.kts (configuration cache + template `@sdkVersion@`).
            val handWrittenPom = file("$projectDir/publishing/pom.xml")
            val resolvedVersion = sdkVersion
            pom.withXml {
                val xmlText = handWrittenPom.readText().replace("@sdkVersion@", resolvedVersion)
                val handWritten = groovy.xml.XmlParser().parseText(xmlText)
                val root = asNode()
                root.children().toList().forEach { root.remove(it as groovy.util.Node) }
                handWritten.children().forEach { root.append(it as groovy.util.Node) }
            }
        }
    }
}

kotlin {
    withSourcesJar(publish = false)

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
        //
        // Hậu tố `.logic` khớp tên module. Namespace Android **độc lập** với package Kotlin — module
        // này không có resource nên nó chỉ là package attribute của AAR, không sinh `R` cho ai.
        namespace = "com.ttcn.promotionsdk.logic"
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
            // `implementation` chứ không `api`: kiểu `Settings` KHÔNG được lọt ra bề mặt public của
            // SDK — host và SKIE bridge chỉ thấy `PromotionPreferences` do ta sở hữu.
            implementation(libs.multiplatform.settings)
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
            // MapSettings: bản Settings chạy trên RAM, thay cho InMemoryStorage viết tay trước đây.
            implementation(libs.multiplatform.settings.test)
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
                // DTO thuần dữ liệu: `equals`/`hashCode`/`copy`/`componentN` do COMPILER sinh, mỗi
                // field một nhánh — đo chúng là đo Kotlin compiler, không phải code ta viết. Một mình
                // EligibleCustomerProfile đã "thiếu" 74 nhánh dù không có dòng logic nào.
                //
                // Lọc theo @Serializable (chính xác) thay vì đoán theo tên: trong repo này CHỈ có DTO
                // dùng annotation đó — domain model, presentation store và mapper đều không.
                annotatedBy("kotlinx.serialization.Serializable")
                classes("*${'$'}serializer")
                // Cầu sang nền tảng: thân hàm nằm ở androidMain/iosMain, không phải commonMain.
                //
                // `PromotionClockKt` ĐÃ ĐƯỢC GỠ khỏi danh sách này. Nó chưa bao giờ là cầu nền tảng
                // thuần: ngoài `currentEpochMillis()` thì file còn `isoDateToEpochMillis`, `daysUntil`,
                // `daysFromCivil` — logic commonMain thật, bị exclude che mất khỏi coverage. Và từ khi
                // `currentEpochMillis()` chuyển sang `kotlin.time.Clock` thì file không còn `actual` nào.
                classes("*.SdkLockKt")
            }
        }
        verify {
            // Ngưỡng đặt SÁT dưới mức hiện tại để mọi PR làm tụt coverage là fail ngay, chứ không
            // phải mục tiêu để phấn đấu. Nâng lên mỗi khi bộ test dày thêm.
            rule("Line coverage của commonMain") {
                bound {
                    minValue = 93
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                }
            }
            rule("Instruction coverage của commonMain") {
                bound {
                    minValue = 92
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.INSTRUCTION
                }
            }
            // BRANCH thấp hơn hai chỉ số trên một cách CỐ HỮU: `suspend` biên dịch thành state
            // machine (`Xxx$method$1`) với `when(label)` dispatch + nhánh `throw IllegalStateException`
            // cho label không hợp lệ — không test nào chạm tới được. Đo riêng: class thường đạt ~84%,
            // class `$` (lambda coroutine) chỉ ~71%.
            rule("Branch coverage của commonMain") {
                bound {
                    minValue = 90
                    coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.BRANCH
                }
            }
        }
    }
}
