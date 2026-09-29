package com.ukenoveldiyar.rcc.compiler.plugin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform
import java.io.File

fun TestConfigurationBuilder.useComposeRuntimeClasspath() {
    useConfigurators(::ComposeRuntimeEnvironmentConfigurator)
    useCustomRuntimeClasspathProviders(::ComposeRuntimeClasspathProvider)
}

private class ComposeRuntimeEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (module.targetPlatform(testServices).isJvm()) {
            configuration.addJvmClasspathRoots(composeRuntimeJvmClasspath)
        }
    }
}

private class ComposeRuntimeClasspathProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {
    override fun runtimeClassPaths(module: TestModule): List<File> {
        return if (module.targetPlatform(testServices).isJvm()) composeRuntimeJvmClasspath else emptyList()
    }
}

private val composeRuntimeJvmClasspath = classpathFiles("composeRuntime.jvm.classpath")

private fun classpathFiles(property: String): List<File> {
    val value = System.getProperty(property) ?: error("Unable to get a valid classpath from '$property' property")
    return value.split(File.pathSeparator).map(::File)
}
