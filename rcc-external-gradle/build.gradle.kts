import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.buildconfig)
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.ukenoveldiyar.rcc"
version = libs.versions.rcc.version.get()

gradlePlugin {
    plugins {
        create("RccComponentPlugin") {
            id = "com.ukenoveldiyar.rcc.component.gradle"
            implementationClass = "com.ukenoveldiyar.rcc.external.gradle.RccComponentGradlePlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.kotlin.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(project(":rcc-external"))
}


java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

buildConfig {
    buildConfigField("String", "VERSION", "\"${libs.versions.rcc.version.get()}\"")
}

