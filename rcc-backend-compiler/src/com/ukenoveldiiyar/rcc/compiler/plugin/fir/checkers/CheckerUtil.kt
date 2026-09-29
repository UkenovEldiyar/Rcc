package com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers

import com.ukenoveldiiyar.rcc.compiler.plugin.fir.services.rccSessionResolveStorage
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext

val CheckerContext.rccComponents
    get() = session.rccSessionResolveStorage
        .componentContainer
        .components

fun String.toRccComponentFqName(): String = replace('/', '.')