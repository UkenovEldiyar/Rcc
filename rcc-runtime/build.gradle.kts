plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlin.io)
            implementation(libs.androidx.collection)
            implementation(libs.compose.runtime)
        }
    }
}