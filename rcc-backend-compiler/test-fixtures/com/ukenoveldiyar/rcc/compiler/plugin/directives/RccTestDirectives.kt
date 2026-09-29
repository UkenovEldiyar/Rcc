package com.ukenoveldiyar.rcc.compiler.plugin.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object RccTestDirectives : SimpleDirectivesContainer() {
    val WITH_COMPONENT_FIXTURES by directive("Register Rcc component fixtures")
    val DUMP_RCC by directive("Dump compiled .rcc bytecode and compare against <testFile>.rcc.txt")
}
