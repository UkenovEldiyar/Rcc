package com.ukenoveldiiyar.rcc.compiler.plugin

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.RccClassGeneratorExtension
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.RccIrGenerationExtension
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.HostBindingIndex
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.RccReachabilityIndex
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults.DefaultArgumentResolver
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.write.RccModuleWriter
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

class RccCompilerPluginComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String get() = BuildConfig.KOTLIN_PLUGIN_ID
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        configuration[RccCompilerConfigurationKeys.ENABLED, true].let { enabled -> if (!enabled) return }
        val compileOutDir = configuration[RccCompilerConfigurationKeys.OUTPUT_DIR]!!

        val componentContainer = configuration[RccCompilerConfigurationKeys.COMPONENTS]
            .orEmpty()
            .let { RccCompilerComponentContainer.load(it) }

        val reachabilityIndex = RccReachabilityIndex()

        IrGenerationExtension.registerExtension(
            RccIrGenerationExtension(reachabilityIndex)
        )

        val moduleWriter = RccModuleWriter(
            reachabilityIndex = reachabilityIndex,
            outputFile = File(compileOutDir, "module.rcc"),
        )

        ClassGeneratorExtension.registerExtension(
            RccClassGeneratorExtension(
                hostBindingIndex = HostBindingIndex(componentContainer),
                reachabilityIndex = reachabilityIndex,
                moduleWriter = moduleWriter,
                defaultArgumentResolver = DefaultArgumentResolver(configuration.jvmClasspathRoots),
            )
        )

    }
}
