package com.ukenoveldiiyar.rcc.compiler.plugin.ir

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccInstruction
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccOpcodeTable
import org.jetbrains.org.objectweb.asm.Label

interface OpcodeEmitter {
    fun emit(op: RccOpcodeTable, vararg operands: Any?)

    fun emitBranch(op: RccOpcodeTable, target: Label)

    fun markLabel(label: Label)

    fun finish(): List<RccInstruction>

    val size: Int

    fun instructionAt(index: Int): RccInstruction

    fun spliceRange(start: Int, end: Int, replacement: List<RccInstruction>)
}

class RccBytecodeCollector : OpcodeEmitter {
    private val instructions = mutableListOf<RccInstruction>()
    private val labelPositions = mutableMapOf<Label, Int>()

    private val pendingBranches = mutableMapOf<Int, Label>()

    override fun emit(op: RccOpcodeTable, vararg operands: Any?) {
        instructions += RccInstruction(op, operands.toList())
    }

    override fun emitBranch(op: RccOpcodeTable, target: Label) {
        pendingBranches[instructions.size] = target
        instructions += RccInstruction(op, operands = listOf(null))
    }

    override fun markLabel(label: Label) {
        labelPositions[label] = instructions.size
    }

    override fun finish(): List<RccInstruction> {
        pendingBranches.forEach { [index, label] ->
            val target = labelPositions[label]

            instructions[index] = instructions[index].copy(operands = listOf(target))
        }

        return instructions.toList()
    }

    override val size: Int get() = instructions.size

    override fun instructionAt(index: Int): RccInstruction = instructions[index]

    override fun spliceRange(start: Int, end: Int, replacement: List<RccInstruction>) {
        require(start in 0..end && end <= instructions.size) {
            "spliceRange: некорректный диапазон [$start, $end) при size=${instructions.size}"
        }

        val delta = replacement.size - (end - start)

        if (delta != 0) {
            if (pendingBranches.isNotEmpty()) {
                val shifted = pendingBranches.entries.associate { (idx, label) ->
                    (if (idx >= end) idx + delta else idx) to label
                }
                pendingBranches.clear()
                pendingBranches.putAll(shifted)
            }
            for (label in labelPositions.keys.toList()) {
                val pos = labelPositions.getValue(label)
                if (pos >= end) labelPositions[label] = pos + delta
            }
        }

        repeat(end - start) { instructions.removeAt(start) }

        instructions.addAll(start, replacement)
    }
}
