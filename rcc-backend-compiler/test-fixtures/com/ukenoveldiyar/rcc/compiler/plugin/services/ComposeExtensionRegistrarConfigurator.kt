package com.ukenoveldiyar.rcc.compiler.plugin.services

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.ukenoveldiiyar.rcc.compiler.plugin.RccCompilerPluginComponentRegistrar
import com.ukenoveldiyar.rcc.compiler.plugin.directives.ComposeTestDirectives
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

fun TestConfigurationBuilder.configureComposePlugin() {
    useDirectives(ComposeTestDirectives)
    useConfigurators(::RccAndComposeExtensionRegistrarConfigurator)
    useComposeRuntimeClasspath()
    configureAnnotations()
    configureComponentFixtures()
}

private class RccAndComposeExtensionRegistrarConfigurator(
    testServices: TestServices
) : EnvironmentConfigurator(testServices) {
    private val rccRegistrar = RccCompilerPluginComponentRegistrar()
    private val composeRegistrar = ComposePluginRegistrar()

    override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        val registrars = if (ComposeTestDirectives.COMPOSE_FIRST in module.directives) {
            listOf(composeRegistrar, rccRegistrar)
        } else {
            listOf(rccRegistrar, composeRegistrar)
        }
        registrars.forEach { registrar -> with(registrar) { registerExtensions(configuration) } }
    }
}
