package com.ukenoveldiiyar.rcc.compiler.plugin.fir.services

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

val ARITHMETIC_OPERATOR_NAMES: Set<Name> = with(OperatorNameConventions) {
    setOf(PLUS, MINUS, TIMES, DIV, REM, UNARY_MINUS, UNARY_PLUS, COMPARE_TO)
}
