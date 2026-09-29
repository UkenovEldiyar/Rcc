package com.ukenoveldiiyar.rcc.compiler.plugin.ir.code

data class RccInstruction(
    val op: RccOpcodeTable,
    val operands: List<Any?> = emptyList(),
)
