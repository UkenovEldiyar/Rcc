package com.ukenoveldiyar.rcc.external.ksp

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

object RccComponentFqName {
    const val COMPOSABLE_ANNOTATION_FQNAME = "androidx.compose.runtime.Composable"
}

private object Pkg {
    const val RUNTIME = "com.ukenoveldiyar.rcc.runtime"
    const val RUNTIME_EXTERNAL = "com.ukenoveldiyar.rcc.runtime.external"
}

object RccComponentEntityNames {
    val HOST_MODULE_CLASS = ClassName(Pkg.RUNTIME, "HostModule")
    val hostModuleFun = MemberName(Pkg.RUNTIME, "hostModule")
    val bindNamed = MemberName(Pkg.RUNTIME, "bindNamed", isExtension = true)
    val bindComposableNamed = MemberName(Pkg.RUNTIME, "bindComposableNamed", isExtension = true)
    // popObject<T>() живёт в rcc-runtime's external-пакете (ExternalScope.kt), не в com.ukenoveldiyar.
    // rcc.runtime напрямую — единственная реальная разница с остальными именами в этом объекте.
    val popObject = MemberName(Pkg.RUNTIME_EXTERNAL, "popObject", isExtension = true)
}
