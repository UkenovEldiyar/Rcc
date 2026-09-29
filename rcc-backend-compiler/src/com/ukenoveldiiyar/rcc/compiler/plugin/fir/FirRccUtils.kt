package com.ukenoveldiiyar.rcc.compiler.plugin.fir

import com.ukenoveldiiyar.rcc.compiler.plugin.RccBackendCompilerAnnotations
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

context(context: CheckerContext)
fun FirDeclaration.hasEntryPointAnnotation(): Boolean = hasAnnotation(
    RccBackendCompilerAnnotations.entryPointAnnotationClassId,
    context.session
)

context(context: CheckerContext)
fun FirBasedSymbol<*>.hasEntryPointAnnotation(): Boolean = hasAnnotation(
    RccBackendCompilerAnnotations.entryPointAnnotationClassId,
    context.session
)

context(context: CheckerContext)
fun findOrNullParentEntryPoint(): FirNamedFunctionSymbol? =
    context.findClosest<FirNamedFunctionSymbol> {
        it.hasEntryPointAnnotation()
    }