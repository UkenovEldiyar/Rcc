package com.ukenoveldiyar.rcc.compiler.plugin.directives

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ComposeTestDirectives : SimpleDirectivesContainer() {
    val COMPOSE_FIRST by directive(
        "Register the Compose IR extension before the RCC IR extension " +
            "(default: RCC runs first and sees the tree before Compose transforms it)"
    )
}
