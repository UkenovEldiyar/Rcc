package com.ukenoveldiyar.rcc.error

import com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint

// RUN_PIPELINE_TILL: FRONTEND
// WITH_COMPONENT_FIXTURES

@RccEntryPoint
fun entryPointCallHost() {

    val componentError = <!RCC_NOT_COMPONENT!>ComponentError()<!>
    val nonCompanion = <!RCC_NOT_COMPONENT!>ComponentError<!>

    componentError.<!RCC_NOT_COMPONENT!>value1Error<!>
    componentError.<!RCC_NOT_COMPONENT!>value2Error<!> = 10

    componentError.<!RCC_NOT_COMPONENT!>memberError()<!>
    componentError.<!RCC_NOT_COMPONENT!>memberError(10)<!>

    <!RCC_NOT_COMPONENT!>with<!>(nonComponent) {
        "".<!RCC_NOT_COMPONENT!>memberError<!>(10)
    }
}

class ComponentError(
    val value1Error: Int,
    var value2Error: Int,
) {

    fun memberError() {

    }

    fun memberError(
        paremeter1: Int
    ) {

    }

    fun String.memberError(
        paremeter1: Int
    ) {

    }

    companion object
}