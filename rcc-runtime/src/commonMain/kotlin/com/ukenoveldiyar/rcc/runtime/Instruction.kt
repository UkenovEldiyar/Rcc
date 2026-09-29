@file:Suppress("NOTHING_TO_INLINE")

package com.ukenoveldiyar.rcc.runtime

import com.ukenoveldiyar.rcc.runtime.code.Opcode

internal class Instruction(
    @JvmField val code: IntArray,
    @JvmField val constants: LongArray,
) {

    inline fun opcode(index: Int): Opcode = code[index]
    inline fun operandI32(index: Int): Int = code[index]
    inline fun operandI64(index: Int): Long = constants[code[index]]
}