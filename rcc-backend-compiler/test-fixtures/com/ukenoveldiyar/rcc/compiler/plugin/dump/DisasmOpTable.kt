package com.ukenoveldiyar.rcc.compiler.plugin.dump

internal enum class OperandShape { NONE }

internal data class DisasmOpInfo(val name: String, val shape: OperandShape)

internal object DisasmOpTable {
    val opcodes: Set<Int> get() = emptySet()

    fun infoOf(opcode: Int): DisasmOpInfo = DisasmOpInfo("<unknown opcode $opcode>", OperandShape.NONE)
}
