import org.gradle.kotlin.dsl.android

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    `maven-publish`
}

group = "com.ukenoveldiyar.rcc"
version = "0.1.0"

kotlin {
    jvm()

    android {
        namespace = "com.ukenoveldiyar.rcc.backend.compiler.annotation"

        compileSdk {
            version = release(36)
        }
    }
}
