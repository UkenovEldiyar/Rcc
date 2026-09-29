package com.ukenoveldiyar.rcc

import com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint

// RUN_PIPELINE_TILL: FRONTEND
// WITH_COMPONENT_FIXTURES

@RccEntryPoint
fun entryPointCallHost() {
    val component = Component(0, 0)
    val companion = Component

    companion
    component.value1
    component.value2 = 0

    component.member()
    component.member(0)

    <!RCC_NOT_COMPONENT!>with<!>(component) {
        "".member(0)
    }
}

class Component(
    val value1: Int,
    var value2: Int,
) {

    fun member() {

    }

    fun member(
        paremeter1: Int
    ) {

    }

    fun String.member(
        paremeter1: Int
    ) {

    }

    companion object
}