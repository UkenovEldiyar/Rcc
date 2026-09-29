import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

group = "com.ukenoveldiyar.rcc"
version = libs.versions.rcc.version.get()

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlin.poet)
    implementation(libs.kotlin.poet.ksp)
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
