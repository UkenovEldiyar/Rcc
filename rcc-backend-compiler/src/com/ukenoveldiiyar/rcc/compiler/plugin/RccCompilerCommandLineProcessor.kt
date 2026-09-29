package com.ukenoveldiiyar.rcc.compiler.plugin

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor

object RccCompilerConfigurationKeys {
    val ENABLED = CompilerConfigurationKey<Boolean>("enabled")
    val COMPONENTS = CompilerConfigurationKey<List<String>>("components")
    val OUTPUT_DIR = CompilerConfigurationKey<String>("outputDir")
}

@Suppress("unused")
class RccCompilerCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String get() = BuildConfig.KOTLIN_PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = buildList {
        add(
            CliOption(
                optionName = ENABLED_OPTION_NAME,
                valueDescription = "<true|false>",
                description = "Whether the RCC compiler plugin is enabled",
                required = false,
                allowMultipleOccurrences = false,
            )
        )

        add(
            CliOption(
                optionName = COMPONENTS_OPTION_NAME,
                valueDescription = "<file>",
                description = "Component descriptor file",
                required = false,
                allowMultipleOccurrences = true,
            )
        )

        add(
            CliOption(
                optionName = OUTPUT_DIR_OPTION_NAME,
                valueDescription = "<path>",
                description = "Output directory for generated .rcc files",
                required = false,
                allowMultipleOccurrences = false,
            )
        )
    }

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            ENABLED_OPTION_NAME -> configuration.put(
                RccCompilerConfigurationKeys.ENABLED,
                value.toBoolean()
            )

            COMPONENTS_OPTION_NAME -> configuration.appendList(
                RccCompilerConfigurationKeys.COMPONENTS,
                value
            )

            OUTPUT_DIR_OPTION_NAME -> configuration.put(
                RccCompilerConfigurationKeys.OUTPUT_DIR,
                value
            )

            else -> {
                throw CliOptionProcessingException("Unknown plugin option: ${option.optionName}")
            }
        }
    }

    private companion object {
        const val ENABLED_OPTION_NAME = "enabled"
        const val COMPONENTS_OPTION_NAME = "components"
        const val OUTPUT_DIR_OPTION_NAME = "outputDir"
    }
}
