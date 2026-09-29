package com.ukenoveldiyar.rcc.compiler.plugin.runners

import com.ukenoveldiyar.rcc.compiler.plugin.services.configureComposePlugin
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractJvmBlackBoxCodegenTestBase
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider

open class AbstractJvmComposeBoxTest : AbstractJvmBlackBoxCodegenTestBase(FirParser.LightTree) {
    override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
        return EnvironmentBasedStandardLibrariesPathProvider
    }

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        super.configure(this)
        defaultDirectives {
            +CodegenTestDirectives.DUMP_IR
            +FirDiagnosticsDirectives.FIR_DUMP
            +JvmEnvironmentConfigurationDirectives.FULL_JDK

            +CodegenTestDirectives.IGNORE_DEXING
        }

        configureComposePlugin()
    }
}
