package com.ukenoveldiyar.rcc.non

import com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint

// RUN_PIPELINE_TILL: FRONTEND
// WITH_COMPONENT_FIXTURES

fun entryPointNon() {
    val component = ComponentNon()
}

class ComponentNon()