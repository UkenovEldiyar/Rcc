package com.ukenoveldiiyar.rcc.compiler.plugin.ir.write

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.JvmType

private const val WIRE_UNIT: Byte = 1
private const val WIRE_INT: Byte = 2
private const val WIRE_LONG: Byte = 3
private const val WIRE_FLOAT: Byte = 4
private const val WIRE_DOUBLE: Byte = 5
private const val WIRE_OBJECT: Byte = 6

fun JvmType.toRawTypeWire(): Byte = when (this) {
    is JvmType.Primitive -> when (descriptor) {
        'Z', 'B', 'C', 'S', 'I' -> WIRE_INT
        'J' -> WIRE_LONG
        'F' -> WIRE_FLOAT
        'D' -> WIRE_DOUBLE
        else -> throw IllegalArgumentException("Неизвестный примитив '$descriptor'")
    }
    JvmType.VoidType -> WIRE_UNIT
    is JvmType.ObjectType, is JvmType.ArrayType -> WIRE_OBJECT
}
