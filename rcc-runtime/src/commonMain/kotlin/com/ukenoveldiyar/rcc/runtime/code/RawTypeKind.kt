package com.ukenoveldiyar.rcc.runtime.code

import com.ukenoveldiyar.rcc.runtime.code.type.Type

internal typealias RawType = Byte
internal typealias RawTypes = ByteArray

internal object RawTypeKind {
    const val UNIT: Type = 1
    const val INT: Type = 2
    const val LONG: Type = 3
    const val FLOAT: Type = 4
    const val DOUBLE: Type = 5
    const val OBJECT: Type = 6

    fun nameOf(
        type: Type
    ): String = Names.getOrElse(type.toInt()) { "<illegal type: $type>" }

    @JvmStatic
    private val Names by lazy(LazyThreadSafetyMode.NONE) {
        arrayOf(
            "Nothing",
            "Unit",
            "Int",
            "Long",
            "Float",
            "Double",
            "Object",
        )
    }
}