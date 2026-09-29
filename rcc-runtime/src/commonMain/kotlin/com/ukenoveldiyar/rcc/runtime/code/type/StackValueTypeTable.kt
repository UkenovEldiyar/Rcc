package com.ukenoveldiyar.rcc.runtime.code.type

internal typealias StackValueType = Byte
internal typealias StackValueTypes = ByteArray

/**
 * Table containing information about the types that can be on the stack.
 */
internal object StackValueTypeTable {
    const val INT: StackValueType = 0
    const val LONG: StackValueType = 1
    const val FLOAT: StackValueType = 2
    const val DOUBLE: StackValueType = 3
    const val REFERENCE: StackValueType = 4
    const val REFERENCE_EXTERNAL: StackValueType = 5
}