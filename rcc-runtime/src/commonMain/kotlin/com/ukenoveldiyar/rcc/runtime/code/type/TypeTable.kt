package com.ukenoveldiyar.rcc.runtime.code.type

internal typealias Type = Byte
internal typealias Types = ByteArray

/**
 * Table containing information about basic types
 */
internal object TypeTable {
    const val UNIT: Type = 0
    const val BOOLEAN: Type = 1
    const val CHAR: Type = 2
    const val BYTE: Type = 3
    const val SHORT: Type = 4
    const val INT: Type = 5
    const val LONG: Type = 6
    const val FLOAT: Type = 7
    const val DOUBLE: Type = 8
    const val OBJECT: Type = 9
    const val OBJECT_EXTERNAL: Type = 10

    fun nameOf(type: Type): String = NAMES.getOrElse(type.toInt()) { error("<illegal type: $type>") }

    @JvmStatic
    private val NAMES = arrayOf(
        "Unit",
        "Boolean",
        "Char",
        "Byte",
        "Short",
        "Int",
        "Long",
        "Float",
        "Double",
        "Reference",
        "Object",
        "Object-External"
    )
}