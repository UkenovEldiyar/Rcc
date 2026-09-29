package com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm

fun JvmType.toKotlinFqName(): String = when (this) {
    is JvmType.Primitive -> when (descriptor) {
        'Z' -> "kotlin.Boolean"; 'B' -> "kotlin.Byte"; 'C' -> "kotlin.Char"; 'S' -> "kotlin.Short"
        'I' -> "kotlin.Int"; 'J' -> "kotlin.Long"; 'F' -> "kotlin.Float"; 'D' -> "kotlin.Double"
        else -> throw IllegalArgumentException("Неизвестный примитив '$descriptor'")
    }
    JvmType.VoidType -> "kotlin.Unit"
    is JvmType.ArrayType -> "kotlin.Array"
    is JvmType.ObjectType -> internalName.toKotlinFqName()
}

fun String.toKotlinFqName(): String = knownJvmToKotlin[this] ?: replace('/', '.').replace('$', '.')

private val knownJvmToKotlin: Map<String, String> = buildMap {
    put("java/lang/Object", "kotlin.Any")
    put("java/lang/String", "kotlin.String")
    put("java/lang/CharSequence", "kotlin.CharSequence")
    put("java/lang/Number", "kotlin.Number")
    put("java/lang/Comparable", "kotlin.Comparable")
    put("java/lang/Throwable", "kotlin.Throwable")
    put("java/lang/Exception", "kotlin.Exception")
    put("java/lang/Boolean", "kotlin.Boolean")
    put("java/lang/Byte", "kotlin.Byte")
    put("java/lang/Character", "kotlin.Char")
    put("java/lang/Short", "kotlin.Short")
    put("java/lang/Integer", "kotlin.Int")
    put("java/lang/Long", "kotlin.Long")
    put("java/lang/Float", "kotlin.Float")
    put("java/lang/Double", "kotlin.Double")

    put("java/util/List", "kotlin.collections.List")

    for (arity in 0..22) {
        put("kotlin/jvm/functions/Function$arity", "kotlin.Function$arity")

        put("androidx/compose/runtime/internal/ComposableFunction$arity", "kotlin.Function$arity")
    }
}

fun JvmType.toHostBindingPath(): String = when (this) {
    is JvmType.Primitive -> when (descriptor) {
        'Z' -> "kotlin/Boolean"; 'B' -> "kotlin/Byte"; 'C' -> "kotlin/Char"; 'S' -> "kotlin/Short"
        'I' -> "kotlin/Int"; 'J' -> "kotlin/Long"; 'F' -> "kotlin/Float"; 'D' -> "kotlin/Double"
        else -> throw IllegalArgumentException("Неизвестный примитив '$descriptor'")
    }
    JvmType.VoidType -> "kotlin/Unit"
    is JvmType.ArrayType -> "kotlin/Array"
    is JvmType.ObjectType -> internalName.toHostBindingPath()
}

fun String.toHostBindingPath(): String = knownJvmToHostBindingPath[this] ?: replace('$', '.')

private val knownJvmToHostBindingPath: Map<String, String> = buildMap {
    put("java/lang/Object", "kotlin/Any")
    put("java/lang/String", "kotlin/String")
    put("java/lang/CharSequence", "kotlin/CharSequence")
    put("java/lang/Number", "kotlin/Number")
    put("java/lang/Comparable", "kotlin/Comparable")
    put("java/lang/Throwable", "kotlin/Throwable")
    put("java/lang/Exception", "kotlin/Exception")
    put("java/lang/Boolean", "kotlin/Boolean")
    put("java/lang/Byte", "kotlin/Byte")
    put("java/lang/Character", "kotlin/Char")
    put("java/lang/Short", "kotlin/Short")
    put("java/lang/Integer", "kotlin/Int")
    put("java/lang/Long", "kotlin/Long")
    put("java/lang/Float", "kotlin/Float")
    put("java/lang/Double", "kotlin/Double")
    for (arity in 0..22) {
        put("kotlin/jvm/functions/Function$arity", "kotlin/Function$arity")
        put("androidx/compose/runtime/internal/ComposableFunction$arity", "kotlin/Function$arity")
    }
}
