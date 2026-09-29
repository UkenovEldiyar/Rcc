package com.ukenoveldiyar.rcc.runtime

import com.ukenoveldiyar.rcc.runtime.code.Opcode

internal class RccStackOverflowError : RuntimeException()

internal class RccUnsupportedOpCodeError(
    val opcode: Opcode,
    val functionId: Int,
    val ip: Int,
    val sp: Int,
) : Error("Opcode $opcode is not implemented: function=$functionId ip=$ip sp=$sp")