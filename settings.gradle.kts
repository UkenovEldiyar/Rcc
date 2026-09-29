rootProject.name = "Rcc"

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
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()
    }
}

//plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
//}

include(":rcc-backend-annotation")
include(":rcc-backend-compiler")
include(":rcc-backend-gradle")
include(":rcc-external")
include(":rcc-external-processor")
include(":rcc-external-gradle")
include(":rcc-runtime")

include("sample:androidApp")
include("sample:androidAppRcc")
include("sample:shared")
