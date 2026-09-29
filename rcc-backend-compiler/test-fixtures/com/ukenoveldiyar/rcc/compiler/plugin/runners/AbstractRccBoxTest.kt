package com.ukenoveldiyar.rcc.compiler.plugin.runners

import com.ukenoveldiyar.rcc.compiler.plugin.directives.RccTestDirectives
import com.ukenoveldiyar.rcc.compiler.plugin.dump.RccBytecodeDumpHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep

open class AbstractRccBoxTest : AbstractJvmBoxTest() {
    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        super.configure(this)

        defaultDirectives {
            +RccTestDirectives.DUMP_RCC
        }

        configureIrHandlersStep {
            useHandlers(::RccBytecodeDumpHandler)
        }
    }
}
