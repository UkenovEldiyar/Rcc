package com.ukenoveldiiyar.rcc.compiler.plugin.ir.code

enum class RccStackValueTable(
    val slots: Int,
    val literal: Char,
) {
    INT(1, 'I'),
    LONG(1, 'J'),
    FLOAT(1, 'F'),
    DOUBLE(1, 'D'),
    REFERENCE(1, 'R'),
    REFERENCE_EXTERN(1, 'E');

    val isPrimitive get() = this != REFERENCE && this != REFERENCE_EXTERN

    val isSlot1: Boolean get() = slots == 1

    val isSlot2: Boolean get() = slots == 2
}