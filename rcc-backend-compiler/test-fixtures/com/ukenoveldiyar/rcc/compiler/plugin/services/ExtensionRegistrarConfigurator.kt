package com.ukenoveldiyar.rcc.compiler.plugin.services

import com.ukenoveldiiyar.rcc.compiler.plugin.RccCompilerConfigurationKeys
import com.ukenoveldiiyar.rcc.compiler.plugin.RccCompilerPluginComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.getOrCreateTempDirectory

fun TestConfigurationBuilder.configurePlugin() {
    useConfigurators(::ExtensionRegistrarConfigurator)
    configureAnnotations()
    configureComponentFixtures()
}

private class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    private val registrar = RccCompilerPluginComponentRegistrar()
    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {

        val outputDir = testServices.getOrCreateTempDirectory("rcc-out")
        configuration.put(RccCompilerConfigurationKeys.OUTPUT_DIR, outputDir.absolutePath)

        with(registrar) { registerExtensions(configuration) }
    }
}
