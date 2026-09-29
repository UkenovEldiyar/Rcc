plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.buildconfig)
    `java-test-fixtures`
    `maven-publish`
    idea
}

group = "com.ukenoveldiyar.rcc"
version = libs.versions.rcc.version.get()

buildConfig {
    packageName("com.ukenoveldiiyar.rcc.compiler.plugin")
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"com.ukenoveldiyar.rcc.backend.compiler\"")
}

val testDataDir = layout.projectDirectory.dir("testData")
val testGenDirectory = layout.buildDirectory.dir("test-gen")

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    testFixtures {
        java.setSrcDirs(listOf("test-fixtures"))
    }
    test {
        java.setSrcDirs(listOf("test", testGenDirectory))
        resources.setSrcDirs(listOf(testDataDir))
    }
}

idea {
    // This is needed until IDEA fixes IDEA-339729.
    module.generatedSourceDirs.add(testGenDirectory.get().asFile)
}

val testArtifacts: Configuration = configurations.create("testArtifact")

val annotationsRuntimeClasspath by configurations.dependencyScope("annotationsRuntimeClasspath") {
    isTransitive = false
}

val annotationsJvmRuntimeClasspath by configurations.resolvable("annotationsJvmRuntimeClasspath") {
    extendsFrom(annotationsRuntimeClasspath)
}

// Compose runtime needs its full transitive graph (unlike the annotations jar above), so this
// stays transitive.
val composeRuntimeClasspath by configurations.dependencyScope("composeRuntimeClasspath")

val composeRuntimeJvmClasspath by configurations.resolvable("composeRuntimeJvmClasspath") {
    extendsFrom(composeRuntimeClasspath)
}

dependencies {
    compileOnly(libs.kotlin.compiler)

    implementation(projects.rccExternal)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlin.io)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.kotlin.test.junit5)
    testFixturesApi(libs.kotlin.test.framework)
    testFixturesApi(libs.kotlin.compiler)
    testFixturesApi(libs.kotlin.compose.compiler.plugin)
    testFixturesRuntimeOnly(libs.junit)

    // Нужны, чтобы test-fixtures мог сериализовать component-фикстуры (.pb) для HostBindingIndex —
    // main зависит от них через implementation, что не расшаривается на testFixtures автоматически.
    testFixturesImplementation(projects.rccExternal)
    testFixturesImplementation(libs.kotlinx.serialization.protobuf)
    // Для RccModuleReader (test-fixtures/.../dump) — зеркалит RccModuleWriter/ModuleProgramLoader.
    testFixturesImplementation(libs.kotlin.io)

    annotationsRuntimeClasspath(projects.rccBackendAnnotation)
    composeRuntimeClasspath(libs.compose.runtime)
    composeRuntimeClasspath(libs.compose.foundation)

    testArtifacts(libs.kotlin.stdlib)
    testArtifacts(libs.kotlin.stdlib.jdk8)
    testArtifacts(libs.kotlin.reflect)
    testArtifacts(libs.kotlin.test)
    testArtifacts(libs.kotlin.script.runtime)
    testArtifacts(libs.kotlin.annotations.jvm)
}

tasks.test {
    dependsOn(testArtifacts)
    dependsOn(annotationsJvmRuntimeClasspath)
    dependsOn(composeRuntimeJvmClasspath)

    useJUnitPlatform()
    workingDir = rootDir

    systemProperty("annotationsRuntime.jvm.classpath", annotationsJvmRuntimeClasspath.asPath)
    systemProperty("composeRuntime.jvm.classpath", composeRuntimeJvmClasspath.asPath)

    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
    setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

    systemProperty("idea.ignore.disabled.plugins", "true")
    systemProperty("idea.home.path", rootDir)
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
        freeCompilerArgs.add("-Xname-based-destructuring=only-syntax")
    }
}


val generateTests = tasks.register<JavaExec>("generateTests") {
    inputs.dir(testDataDir)
        .withPropertyName("testData")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    outputs.dir(testGenDirectory)
        .withPropertyName("generatedTests")

    classpath = sourceSets.testFixtures.get().runtimeClasspath
    mainClass.set("com.ukenoveldiyar.rcc.compiler.plugin.GenerateTestsKt")
    workingDir = rootDir
    args(
        listOf(
            testGenDirectory.get().asFile.absolutePath,
            testDataDir.asFile.absolutePath,
        )
    )
}

tasks.compileTestKotlin {
    dependsOn(generateTests)
}

fun Test.setLibraryProperty(propName: String, jarName: String) {
    val path = testArtifacts.files
        .find { """$jarName-\d.*""".toRegex().matches(it.name) }
        ?.absolutePath
        ?: return

    systemProperty(propName, path)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
