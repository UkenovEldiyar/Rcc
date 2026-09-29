package com.ukenoveldiiyar.rcc.compiler.plugin.ir

import com.ukenoveldiiyar.rcc.compiler.plugin.RccBackendCompilerAnnotations
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.interpreter.hasAnnotation

fun IrAnnotationContainer.hasAnnotationEntryPoint(): Boolean =
    hasAnnotation(RccBackendCompilerAnnotations.entryPointAnnotationFqName)

fun IrAnnotationContainer.hasAnnotationComposable(): Boolean =
    hasAnnotation(RccBackendCompilerAnnotations.composableAnnotationFqName)