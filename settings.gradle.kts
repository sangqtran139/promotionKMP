rootProject.name = "MyApplication"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // SDK được :androidApp tiêu thụ dạng artifact Maven (docs/Distribution.md). Bước 1 là ~/.m2:
        //     ./gradlew :promotionLogic:publishToMavenLocal :AndroidPromotionUI:publishToMavenLocal
        // Giới hạn đúng group của SDK — mavenLocal() thả rông sẽ tranh resolve với mọi thư viện khác
        // và cho ra build không tái lập được.
        mavenLocal {
            content { includeGroup("com.ttcn.promotion") }
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":androidApp")
include(":promotionLogic")
include(":AndroidPromotionUI")