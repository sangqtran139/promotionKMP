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

    // ─── Môi trường: staging / uat / product ─────────────────────────────────────────────────
    //
    // Chọn lúc BUILD, không phải lúc chạy: mỗi flavor sinh một `BuildConfig.DEMO_BASE_URL` riêng.
    // Trong Android Studio đổi bằng Build Variants; ngoài dòng lệnh thì đọc tên task:
    //
    //     ./gradlew :androidApp:assembleStagingDebug    # hoặc Uat / Product
    //     ./gradlew :androidApp:installProductDebug
    //     ./scripts/build-android.sh local --env product
    //
    // `buildConfig = true` là BẮT BUỘC từ AGP 8: mặc định đã tắt, thiếu dòng đó thì
    // `buildConfigField` không sinh ra gì và `BuildConfig.DEMO_BASE_URL` không biên dịch được.
    //
    // KHÔNG đặt `applicationIdSuffix`: ba flavor dùng chung một applicationId nên cài cái này là
    // đè cái kia. Đổi lại `scripts/build-android.sh` (và lệnh `adb am start`) không phải suy ra
    // app id theo flavor. Cần cài song song để so hai môi trường thì thêm
    // `applicationIdSuffix = ".uat"` vào từng flavor và sửa `APP_ID` trong script cho khớp.
    // `DEMO_ENV` (tên môi trường) tách khỏi `DEMO_BASE_URL` (địa chỉ): có chỗ cần rẽ nhánh theo
    // MÔI TRƯỜNG chứ không theo URL — vd staging không cấp `requestId` nên client phải tự sinh
    // (xem `LoginService.requestOtp`). Suy ngược môi trường từ chuỗi URL là so sánh chuỗi mong manh,
    // đổi URL một chữ là nhánh đó im lặng ngừng chạy.
    flavorDimensions += "env"

    productFlavors {
        create("staging") {
            dimension = "env"
            // BẮT BUỘC, đừng bỏ. Không flavor nào `isDefault` thì AGP chọn cái **đầu tiên theo
            // alphabet** — `product` < `staging` < `uat` — nên Android Studio tự chọn `productDebug`
            // sau mỗi lần sync và bấm Run là build thẳng PRODUCTION mà không báo gì.
            isDefault = true
            buildConfigField("String", "DEMO_ENV", "\"staging\"")
            // HTTP trần, không phải HTTPS — nên `res/xml/network_security_config.xml` mới phải mở
            // cleartext riêng cho host `125.235.38.229`. Xoá flavor này thì file đó cũng hết lý do
            // tồn tại; ngược lại, sửa IP ở đây mà quên file kia là mọi request staging bị Android
            // chặn thẳng với `CLEARTEXT communication not permitted`.
            buildConfigField("String", "DEMO_BASE_URL", "\"http://125.235.38.229:8080\"")
            resValue("string", "app_name", "Promotion SDK (STG)")
            versionNameSuffix = "-staging"
        }
        create("uat") {
            dimension = "env"
            buildConfigField("String", "DEMO_ENV", "\"uat\"")
            buildConfigField("String", "DEMO_BASE_URL", "\"https://api24cdn.vtmoney.vn/uatmm\"")
            resValue("string", "app_name", "Promotion SDK (UAT)")
            versionNameSuffix = "-uat"
        }
        create("product") {
            dimension = "env"
            buildConfigField("String", "DEMO_ENV", "\"product\"")
            // Cùng host với UAT, **bỏ đoạn `/uatmm`** — đó là toàn bộ khác biệt giữa hai môi trường.
            buildConfigField("String", "DEMO_BASE_URL", "\"https://api24cdn.vtmoney.vn\"")
            // Có hậu tố như hai môi trường kia. Trước đây để trống cho "giống bản thật", nhưng đây
            // là app DEMO — không có bản thật nào để giống, và tên trơn khiến bản production nhìn
            // hệt bản mặc định. Muốn biết mình đang cầm gì thì phải đọc được nó trên màn hình.
            resValue("string", "app_name", "Promotion SDK (PRODUCT)")
            versionNameSuffix = "-product"
        }
    }

    buildFeatures {
        buildConfig = true
        // `dataBinding` TẮT — cùng lý do với `:AndroidPromotionSDK`: 7 layout của app demo bọc thẻ
        // `<layout>` nhưng **0 file có `<data>`/`<variable>`** và **0 chỗ** dùng
        // `DataBindingUtil`/`ViewDataBinding`. Tất cả đi qua View Binding.
        dataBinding = false
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
val sdkGroup = (project.findProperty("SDK_GROUP") as String?) ?: "vn.viettelpay.library"

dependencies {
    // ─── SDK: nạp từ Artifactory Viettelmoney, ĐÚNG toạ độ host thật khai ─────────────────
    // Artifact đi kèm POM + Gradle Module Metadata do Gradle sinh từ dependency graph → tự kéo
    // `promotionLogic`, Ktor, Glide, Timber… đúng version SDK đã compile. Host **không** phải khai
    // tay dependency nào của SDK — trừ mấy lib rò rỉ ra public API, xem khối androidx bên dưới.
    //
    // Mặc định app kéo từ SERVER (repo "viettelmoney" đăng ký ở `settings.gradle.kts`, credentials
    // trong `local.properties`), nên **sửa SDK xong phải publish mới thấy thay đổi**. Vòng lặp dev
    // nhanh thì publish vào ~/.m2 rồi bật cờ:
    //     ./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionSDK:publishToMavenLocal
    //     ./gradlew :androidApp:assembleDebug -PuseMavenLocal=true
    // (gọn hơn: `./scripts/build-android.sh`). Xem docs/android/Distribution.md §5.
    implementation("$sdkGroup:promotion:$sdkVersion")

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
    // `sdp-android`: layout của CHÍNH app demo dùng `@dimen/_16sdp` và `@dimen/_120sdp`.
    //
    // Trước đây app không khai dòng này mà vẫn build được — nó **ăn ké** resource mà SDK kéo theo.
    // Đó chính là vấn đề AND-4 nói: resource của library merge thẳng vào app host, nên host vô tình
    // phụ thuộc vào một thư viện mình không hề khai. SDK vừa nội bộ hoá sdp (`prm_Xsdp`) là app này
    // gãy ngay — đúng thứ sẽ xảy ra với host thật nếu không phát hiện sớm.
    implementation(libs.sdp.android)
    implementation(libs.kotlinx.coroutines.android)
    // PromotionTestLoginManager gọi API lấy token. SDK **không** dùng Retrofit.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
}
