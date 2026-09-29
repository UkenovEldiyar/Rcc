import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.kotlin.serialization)
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.ukenoveldiyar.rcc.backend.gradle"
version = libs.versions.rcc.version.get()

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

gradlePlugin {
    plugins {
        create("rccPlugin") {
            id = "com.ukenoveldiyar.rcc.backend.gradle"
            displayName = "RccBackendCompilerGradlePlugin"
            implementationClass =
                "com.ukenoveldiyar.rcc.backend.gradle.RccBackendCompilerGradlePlugin"
        }
    }
}

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin.api)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotaml)
    implementation(project(":rcc-external"))
//    implementation(project(":rcc-compiler"))
}

buildConfig {
    packageName("com.ukenoveldiyar.rcc.backend.gradle")

    useKotlinOutput {
        topLevelConstants = true
    }

    buildConfigField("String", "PLUGIN_ID", "\"com.ukenoveldiyar.rcc.backend.compiler\"")
    buildConfigField("String", "PLUGIN_GROUP", "\"com.ukenoveldiyar.rcc\"")
    buildConfigField("String", "PLUGIN_ARTIFACT", "\"rcc-backend-compiler\"")
    buildConfigField("String", "PLUGIN_VERSION", "\"${libs.versions.rcc.version.get()}\"")
}