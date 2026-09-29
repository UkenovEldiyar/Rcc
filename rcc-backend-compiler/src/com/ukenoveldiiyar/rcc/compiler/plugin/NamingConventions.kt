package com.ukenoveldiiyar.rcc.compiler.plugin

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

object RccBackendCompilerAnnotations {
    val entryPointAnnotationFqName = FqName("com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint")
    val composableAnnotationFqName = FqName("androidx.compose.runtime.Composable")
    val entryPointAnnotationClassId = ClassId.topLevel(FqName("com.ukenoveldiyar.rcc.backend.compiler.annotation.RccEntryPoint"))
}