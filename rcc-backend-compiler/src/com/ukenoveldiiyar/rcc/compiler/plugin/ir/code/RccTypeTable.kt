package com.ukenoveldiiyar.rcc.compiler.plugin.ir.code

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccTypeTable.REFERENCE_EXTERN

enum class RccTypeTable(
    val slots: Int,
    val letter: Char,
) {
    BOOLEAN(1, 'Z'),
    BYTE(1, 'B'),
    CHAR(1, 'C'),
    SHORT(1, 'S'),
    INT(1, 'I'),
    LONG(1, 'J'),
    FLOAT(1, 'F'),
    DOUBLE(1, 'D'),
    REFERENCE(1, 'R'),
    REFERENCE_EXTERN(1, 'E'),
    VOID(0, 'V');

    val isPrimitive: Boolean get() = this != REFERENCE && this != REFERENCE_EXTERN && this != VOID

    val isNumeric: Boolean get() = this != BOOLEAN && this != REFERENCE && this != VOID
}

fun RccTypeTable.toStackType(): RccStackValueTable? = when (this) {
    RccTypeTable.BOOLEAN, RccTypeTable.BYTE, RccTypeTable.CHAR, RccTypeTable.SHORT, RccTypeTable.INT -> RccStackValueTable.INT
    RccTypeTable.LONG -> RccStackValueTable.LONG
    RccTypeTable.FLOAT -> RccStackValueTable.FLOAT
    RccTypeTable.DOUBLE -> RccStackValueTable.DOUBLE
    RccTypeTable.REFERENCE -> RccStackValueTable.REFERENCE
    REFERENCE_EXTERN -> RccStackValueTable.REFERENCE_EXTERN
    RccTypeTable.VOID -> null
}