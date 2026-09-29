package com.ukenoveldiyar.rcc.compiler.plugin.services

import com.ukenoveldiiyar.rcc.compiler.plugin.RccCompilerConfigurationKeys
import com.ukenoveldiyar.rcc.compiler.plugin.directives.RccTestDirectives
import com.ukenoveldiyar.rcc.external.config.ExternalConfig
import com.ukenoveldiyar.rcc.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.external.declarations.ExternalConstructor
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.external.declarations.ExternalProperty
import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.schema.ExternalConfigProto
import com.ukenoveldiyar.rcc.external.schema.toProto
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.source.SourceRange
import com.ukenoveldiyar.rcc.external.types.ExternalType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.getOrCreateTempDirectory
import java.io.File

fun TestConfigurationBuilder.configureComponentFixtures() {
    useDirectives(RccTestDirectives)
    useConfigurators(::ComponentFixturesConfigurator)
}

private val NO_SOURCE = SourceElement(SourceRange(0, 0))

private fun externalType(fqName: String) = ExternalType(fqName = FqName(fqName), sourceElement = NO_SOURCE)

@OptIn(ExperimentalSerializationApi::class)
private class ComponentFixturesConfigurator(testServices: TestServices) :
    EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(
        configuration: CompilerConfiguration,
        module: TestModule
    ) {
        if (RccTestDirectives.WITH_COMPONENT_FIXTURES !in module.directives) return

        val fixtureDir = testServices.getOrCreateTempDirectory("rccComponents")
        val fixtureFile = File(fixtureDir, "fixtures.pb")

        val file = ExternalFile(
            sourceElement = NO_SOURCE,
            annotations = emptyList(),
            name = "Fixture",
            declarations = listOf(
                ExternalClass(
                    fqName = FqName("com.ukenoveldiyar.rcc.Component"),
                    sourceElement = NO_SOURCE,
                    declarations = listOf(
                        ExternalConstructor(
                            fqName = FqName("com.ukenoveldiyar.rcc.Component.Component"),
                            parameters = listOf(externalType("kotlin.Int"), externalType("kotlin.Int")),
                            sourceElement = NO_SOURCE,
                        ),
                        ExternalProperty(
                            fqName = FqName("com.ukenoveldiyar.rcc.Component.value1"),
                            type = externalType("kotlin.Int"),
                            mutable = false,
                            sourceElement = NO_SOURCE,
                        ),
                        ExternalProperty(
                            fqName = FqName("com.ukenoveldiyar.rcc.Component.value2"),
                            type = externalType("kotlin.Int"),
                            mutable = true,
                            sourceElement = NO_SOURCE,
                        ),
                        ExternalFunction(
                            fqName = FqName("com.ukenoveldiyar.rcc.Component.member"),
                            parameters = emptyList(),
                            sourceElement = NO_SOURCE,
                        ),
                        ExternalFunction(
                            fqName = FqName("com.ukenoveldiyar.rcc.Component.member"),
                            parameters = listOf(externalType("kotlin.Int")),
                            sourceElement = NO_SOURCE,
                        ),
                        ExternalProperty(
                            fqName = FqName("com.ukenoveldiyar.rcc.Component.Companion"),
                            type = externalType("kotlin.Int"),
                            mutable = false,
                            sourceElement = NO_SOURCE,
                        ),
                    ),
                ),
                ExternalFunction(
                    fqName = FqName("com.ukenoveldiyar.rcc.Component.member"),
                    extension = externalType("kotlin.String"),
                    parameters = listOf(externalType("kotlin.Int")),
                    sourceElement = NO_SOURCE,
                ),
            ),
        )

        val config = ExternalConfig(
            packageName = "com.ukenoveldiyar.rcc",
            name = "Fixture",
            element = file,
        )

        fixtureFile.writeBytes(
            ProtoBuf.encodeToByteArray(ExternalConfigProto.serializer(), config.toProto())
        )

        configuration.put(RccCompilerConfigurationKeys.COMPONENTS, listOf(fixtureDir.absolutePath))
    }
}
