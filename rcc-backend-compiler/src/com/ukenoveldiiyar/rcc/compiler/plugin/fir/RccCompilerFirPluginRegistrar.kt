package com.ukenoveldiiyar.rcc.compiler.plugin.fir

import com.ukenoveldiiyar.rcc.compiler.plugin.RccCompilerComponentContainer
import com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers.FirRccErrors
import com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers.RccFirCheckersExtension
import com.ukenoveldiiyar.rcc.compiler.plugin.fir.services.RccSessionProvider
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class RccCompilerFirPluginRegistrar(
    private val componentContainer: RccCompilerComponentContainer,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {

        +::RccFirCheckersExtension
        +RccSessionProvider(componentContainer)

        registerDiagnosticContainers(FirRccErrors)
    }
}