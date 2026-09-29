import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

group = "com.ukenoveldiyar.rcc"
version = libs.versions.rcc.version.get()

dependencies {
    implementation(libs.snakeyaml.engine)
    api(libs.kotlinx.serialization.protobuf)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.kotlin.test.junit5)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        freeCompilerArgs.add("-Xname-based-destructuring=only-syntax")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
