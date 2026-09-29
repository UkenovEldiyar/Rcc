package com.ukenoveldiiyar.rcc.compiler.plugin.ir.write

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.RccReachabilityIndex
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccInstruction
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccOpcodeTable
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccTypeTable
import java.io.ByteArrayOutputStream
import java.io.File

class RccModuleWriter(
    private val reachabilityIndex: RccReachabilityIndex,
    private val outputFile: File,
) {
    private class CompiledFunction(
        val maxStack: Int,
        val maxLocals: Int,
        val parameterRaw: ByteArray,
        val returnType: Byte,
        val code: IntArray,
        val constants: LongArray,
    )

    private val functionsById = mutableMapOf<Int, CompiledFunction>()
    private val stringPool = mutableListOf<String>()
    private val stringPoolIndex = mutableMapOf<String, Int>()

    @Synchronized
    fun recordFunction(
        id: Int,
        instructions: List<RccInstruction>,
        maxStack: Int,
        maxLocals: Int,
        parameterRaw: ByteArray,
        returnType: Byte,
    ) {
        val (code, constants) = lowerFunction(instructions)
        functionsById[id] = CompiledFunction(
            maxStack = maxStack,
            maxLocals = maxLocals,
            parameterRaw = parameterRaw,
            returnType = returnType,
            code = code,
            constants = constants,
        )
        tryWrite()
    }

    private fun tryWrite() {
        val total = reachabilityIndex.totalIds
        if (total == 0) return
        for (id in 0 until total) if (id !in functionsById) return
        write(total)
    }

    private fun write(total: Int) {
        val out = ByteArrayOutputStream()

        out.writeIntLe(MAGIC)
        out.writeShortLe(VERSION)

        out.writeIntLe(stringPool.size)
        stringPool.forEach { out.writeRccString(it) }

        out.writeIntLe(total)
        for (id in 0 until total) {
            val fn = functionsById.getValue(id)
            val captureParameterCount = reachabilityIndex.captureCountOf(id)
            out.writeIntLe(id)
            out.writeShortLe(fn.maxStack)
            out.writeShortLe(fn.maxLocals)
            out.writeShortLe(fn.parameterRaw.size)
            out.write(fn.parameterRaw)
            out.writeShortLe(captureParameterCount)
            out.write(fn.parameterRaw, 0, captureParameterCount)
            out.writeShortLe(fn.parameterRaw.size - captureParameterCount)
            out.write(fn.returnType.toInt())
            out.writeIntLe(fn.code.size)
            fn.code.forEach { out.writeIntLe(it) }
            out.writeIntLe(fn.constants.size)
            fn.constants.forEach { out.writeLongLe(it) }
        }

        val entryPoints = reachabilityIndex.entryPoints
        out.writeIntLe(entryPoints.size)
        entryPoints.forEach { (fqName, id) ->
            out.writeRccString(fqName)
            out.writeIntLe(id)
        }

        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(out.toByteArray())
    }

    private fun stringId(value: String): Int = stringPoolIndex.getOrPut(value) {
        stringPool += value
        stringPool.size - 1
    }

    private class Lowered(val op: Int, val operands: List<Int>, val branchOperandIndex: Int)

    private fun lowerFunction(instructions: List<RccInstruction>): Pair<IntArray, LongArray> {
        val constants = mutableListOf<Long>()
        val lowered = instructions.map { lowerInstruction(it, constants) }

        val offset = IntArray(lowered.size + 1)
        for (i in lowered.indices) offset[i + 1] = offset[i] + 1 + lowered[i].operands.size

        val code = IntArray(offset[lowered.size])
        for (i in lowered.indices) {
            val l = lowered[i]
            var w = offset[i]
            code[w++] = l.op
            l.operands.forEachIndexed { operandPos, operand ->
                code[w++] = if (operandPos == l.branchOperandIndex) offset[operand] else operand
            }
        }
        return code to constants.toLongArray()
    }

    private fun lowerInstruction(insn: RccInstruction, constants: MutableList<Long>): Lowered {
        fun const(value: Long): Int {
            constants += value
            return constants.size - 1
        }

        fun noOperand(op: Int) = Lowered(op, emptyList(), -1)
        fun oneOperand(op: Int, value: Int) = Lowered(op, listOf(value), -1)
        fun branch(op: Int) = Lowered(op, listOf(insn.operands[0] as Int), 0)

        return when (insn.op) {
            RccOpcodeTable.NOP -> noOperand(Wire.NOP)
            RccOpcodeTable.ACONST_NULL -> noOperand(Wire.A_CONST_NULL)
            RccOpcodeTable.I32_PUSH -> oneOperand(Wire.I32_PUSH, insn.operands[0] as Int)
            RccOpcodeTable.I64_PUSH -> oneOperand(Wire.I64_PUSH, const(insn.operands[0] as Long))
            RccOpcodeTable.F32_PUSH -> oneOperand(Wire.I32_PUSH, (insn.operands[0] as Float).toRawBits())
            RccOpcodeTable.F64_PUSH -> oneOperand(Wire.I64_PUSH, const((insn.operands[0] as Double).toRawBits()))
            RccOpcodeTable.CP_LOAD -> oneOperand(Wire.CP_LOAD, stringId(insn.operands[0] as String))

            RccOpcodeTable.I_LOAD, RccOpcodeTable.L_LOAD, RccOpcodeTable.F_LOAD, RccOpcodeTable.D_LOAD ->
                oneOperand(Wire.RAW_LOAD, insn.operands[0] as Int)
            RccOpcodeTable.A_LOAD -> oneOperand(Wire.A_LOAD, insn.operands[0] as Int)
            RccOpcodeTable.I_STORE, RccOpcodeTable.L_STORE, RccOpcodeTable.F_STORE, RccOpcodeTable.D_STORE ->
                oneOperand(Wire.RAW_STORE, insn.operands[0] as Int)
            RccOpcodeTable.A_STORE -> oneOperand(Wire.A_STORE, insn.operands[0] as Int)
            RccOpcodeTable.I_INC -> Lowered(
                Wire.I_INC,
                listOf(insn.operands[0] as Int, insn.operands[1] as Int),
                -1,
            )

            RccOpcodeTable.POP -> noOperand(Wire.POP)
            RccOpcodeTable.DUP -> noOperand(Wire.DUP)
            RccOpcodeTable.DUP_X1 -> noOperand(Wire.DUP_X1)
            RccOpcodeTable.SWAP -> noOperand(Wire.SWAP)

            RccOpcodeTable.I_ADD -> noOperand(Wire.I_ADD)
            RccOpcodeTable.L_ADD -> noOperand(Wire.L_ADD)
            RccOpcodeTable.F_ADD -> noOperand(Wire.F_ADD)
            RccOpcodeTable.D_ADD -> noOperand(Wire.D_ADD)
            RccOpcodeTable.I_SUB -> noOperand(Wire.I_SUB)
            RccOpcodeTable.L_SUB -> noOperand(Wire.L_SUB)
            RccOpcodeTable.F_SUB -> noOperand(Wire.F_SUB)
            RccOpcodeTable.D_SUB -> noOperand(Wire.D_SUB)
            RccOpcodeTable.I_MUL -> noOperand(Wire.I_MUL)
            RccOpcodeTable.L_MUL -> noOperand(Wire.L_MUL)
            RccOpcodeTable.F_MUL -> noOperand(Wire.F_MUL)
            RccOpcodeTable.D_MUL -> noOperand(Wire.D_MUL)
            RccOpcodeTable.I_DIV -> noOperand(Wire.I_DIV)
            RccOpcodeTable.L_DIV -> noOperand(Wire.L_DIV)
            RccOpcodeTable.F_DIV -> noOperand(Wire.F_DIV)
            RccOpcodeTable.D_DIV -> noOperand(Wire.D_DIV)
            RccOpcodeTable.I_REM -> noOperand(Wire.I_REM)
            RccOpcodeTable.L_REM -> noOperand(Wire.L_REM)
            RccOpcodeTable.F_REM -> noOperand(Wire.F_REM)
            RccOpcodeTable.D_REM -> noOperand(Wire.D_REM)
            RccOpcodeTable.I_NEG -> noOperand(Wire.I_NEG)
            RccOpcodeTable.L_NEG -> noOperand(Wire.L_NEG)
            RccOpcodeTable.F_NEG -> noOperand(Wire.F_NEG)
            RccOpcodeTable.D_NEG -> noOperand(Wire.D_NEG)

            RccOpcodeTable.I_XOR -> noOperand(Wire.I_XOR)
            RccOpcodeTable.L_XOR -> noOperand(Wire.L_XOR)
            RccOpcodeTable.I_AND -> noOperand(Wire.I_AND)
            RccOpcodeTable.L_AND -> noOperand(Wire.L_AND)
            RccOpcodeTable.I_OR -> noOperand(Wire.I_OR)
            RccOpcodeTable.L_OR -> noOperand(Wire.L_OR)
            RccOpcodeTable.I_SHL -> noOperand(Wire.I_SHL)
            RccOpcodeTable.L_SHL -> noOperand(Wire.L_SHL)
            RccOpcodeTable.I_SHR -> noOperand(Wire.I_SHR)
            RccOpcodeTable.L_SHR -> noOperand(Wire.L_SHR)
            RccOpcodeTable.I_USHR -> noOperand(Wire.I_USHR)
            RccOpcodeTable.L_USHR -> noOperand(Wire.L_USHR)

            RccOpcodeTable.I2L -> noOperand(Wire.I2L)
            RccOpcodeTable.I2F -> noOperand(Wire.I2F)
            RccOpcodeTable.I2D -> noOperand(Wire.I2D)
            RccOpcodeTable.L2I -> noOperand(Wire.L2I)
            RccOpcodeTable.L2F -> noOperand(Wire.L2F)
            RccOpcodeTable.L2D -> noOperand(Wire.L2D)
            RccOpcodeTable.F2I -> noOperand(Wire.F2I)
            RccOpcodeTable.F2L -> noOperand(Wire.F2L)
            RccOpcodeTable.F2D -> noOperand(Wire.F2D)
            RccOpcodeTable.D2I -> noOperand(Wire.D2I)
            RccOpcodeTable.D2L -> noOperand(Wire.D2L)
            RccOpcodeTable.D2F -> noOperand(Wire.D2F)
            RccOpcodeTable.I2B -> noOperand(Wire.I2B)
            RccOpcodeTable.I2C -> noOperand(Wire.I2C)
            RccOpcodeTable.I2S -> noOperand(Wire.I2S)

            RccOpcodeTable.L_CMP -> noOperand(Wire.L_CMP)
            RccOpcodeTable.F_CMPL -> noOperand(Wire.F_CMPL)
            RccOpcodeTable.F_CMPG -> noOperand(Wire.F_CMPG)
            RccOpcodeTable.D_CMPL -> noOperand(Wire.D_CMPL)
            RccOpcodeTable.D_CMPG -> noOperand(Wire.D_CMPG)

            RccOpcodeTable.IF_EQ -> branch(Wire.IF_EQ)
            RccOpcodeTable.IF_NE -> branch(Wire.IF_NE)
            RccOpcodeTable.IF_LT -> branch(Wire.IF_LT)
            RccOpcodeTable.IF_GE -> branch(Wire.IF_GE)
            RccOpcodeTable.IF_GT -> branch(Wire.IF_GT)
            RccOpcodeTable.IF_LE -> branch(Wire.IF_LE)
            RccOpcodeTable.IF_ICMP_EQ -> branch(Wire.IF_ICMP_EQ)
            RccOpcodeTable.IF_ICMP_NE -> branch(Wire.IF_ICMP_NE)
            RccOpcodeTable.IF_ICMP_LT -> branch(Wire.IF_ICMP_LT)
            RccOpcodeTable.IF_ICMP_GE -> branch(Wire.IF_ICMP_GE)
            RccOpcodeTable.IF_ICMP_GT -> branch(Wire.IF_ICMP_GT)
            RccOpcodeTable.IF_ICMP_LE -> branch(Wire.IF_ICMP_LE)

            RccOpcodeTable.IF_RCMP_EQ -> branch(Wire.IF_ACMP_EQ)
            RccOpcodeTable.IF_RCMP_NE -> branch(Wire.IF_ACMP_NE)
            RccOpcodeTable.IF_NULL -> branch(Wire.IF_NULL)
            RccOpcodeTable.IF_NONNULL -> branch(Wire.IF_NONNULL)
            RccOpcodeTable.GOTO -> branch(Wire.GOTO)

            RccOpcodeTable.RETURN_VOID -> noOperand(Wire.RETURN)
            RccOpcodeTable.RETURN_I, RccOpcodeTable.RETURN_L, RccOpcodeTable.RETURN_F, RccOpcodeTable.RETURN_D ->

                noOperand(Wire.RAW_RETURN)
            RccOpcodeTable.RETURN_A -> noOperand(Wire.A_RETURN)

            RccOpcodeTable.INVOKE -> oneOperand(Wire.INVOKE, insn.operands[0] as Int)
            RccOpcodeTable.HOST_INVOKE -> Lowered(
                Wire.HOST_INVOKE,

                listOf(0, stringId(insn.operands[0] as String)),
                -1,
            )
            RccOpcodeTable.HOST_LAMBDA -> oneOperand(Wire.HOST_LAMBDA, insn.operands[0] as Int)

            RccOpcodeTable.BOX -> oneOperand(Wire.BOX, rawTypeKindOf(insn.operands[0] as RccTypeTable))
            RccOpcodeTable.UNBOX -> oneOperand(Wire.UNBOX, rawTypeKindOf(insn.operands[0] as RccTypeTable))
        }
    }

    private fun rawTypeKindOf(type: RccTypeTable): Int = when (type) {
        RccTypeTable.INT -> RawTypeKind.INT
        RccTypeTable.LONG -> RawTypeKind.LONG
        RccTypeTable.FLOAT -> RawTypeKind.FLOAT
        RccTypeTable.DOUBLE -> RawTypeKind.DOUBLE
        RccTypeTable.REFERENCE, RccTypeTable.REFERENCE_EXTERN -> RawTypeKind.OBJECT
        else -> throw IllegalArgumentException("rcc: BOX/UNBOX не поддержан для типа $type")
    }

    private object RawTypeKind {
        const val INT = 2
        const val LONG = 3
        const val FLOAT = 4
        const val DOUBLE = 5
        const val OBJECT = 6
    }

    private object Wire {
        const val NOP = 1
        const val A_CONST_NULL = 2
        const val I32_PUSH = 3
        const val I64_PUSH = 4
        const val CP_LOAD = 5
        const val RAW_LOAD = 6
        const val A_LOAD = 7
        const val RAW_STORE = 8
        const val A_STORE = 9
        const val POP = 10
        const val DUP = 11
        const val DUP_X1 = 12
        const val SWAP = 14
        const val I_INC = 39
        const val I_ADD = 15
        const val L_ADD = 16
        const val F_ADD = 17
        const val D_ADD = 18
        const val I_SUB = 19
        const val L_SUB = 20
        const val F_SUB = 21
        const val D_SUB = 22
        const val I_MUL = 23
        const val L_MUL = 24
        const val F_MUL = 25
        const val D_MUL = 26
        const val I_DIV = 27
        const val L_DIV = 28
        const val F_DIV = 29
        const val D_DIV = 30
        const val I_REM = 31
        const val L_REM = 32
        const val F_REM = 33
        const val D_REM = 34
        const val I_NEG = 35
        const val L_NEG = 36
        const val F_NEG = 37
        const val D_NEG = 38
        const val I2L = 40
        const val I2F = 41
        const val I2D = 42
        const val L2I = 43
        const val L2F = 44
        const val L2D = 45
        const val F2I = 46
        const val F2L = 47
        const val F2D = 48
        const val D2I = 49
        const val D2L = 50
        const val D2F = 51
        const val I2B = 52
        const val I2C = 53
        const val I2S = 54
        const val L_CMP = 55
        const val F_CMPL = 56
        const val F_CMPG = 57
        const val D_CMPL = 58
        const val D_CMPG = 59
        const val IF_EQ = 60
        const val IF_NE = 61
        const val IF_LT = 62
        const val IF_GE = 63
        const val IF_GT = 64
        const val IF_LE = 65
        const val IF_ICMP_EQ = 66
        const val IF_ICMP_NE = 67
        const val IF_ICMP_LT = 68
        const val IF_ICMP_GE = 69
        const val IF_ICMP_GT = 70
        const val IF_ICMP_LE = 71
        const val IF_ACMP_EQ = 72
        const val IF_ACMP_NE = 73
        const val IF_NULL = 74
        const val IF_NONNULL = 75
        const val GOTO = 76
        const val RAW_RETURN = 79
        const val A_RETURN = 83
        const val RETURN = 84
        const val INVOKE = 85
        const val HOST_INVOKE = 86
        const val HOST_LAMBDA = 87
        const val BOX = 90
        const val UNBOX = 91
        const val I_XOR = 92

        const val I_AND = 101
        const val L_AND = 102
        const val I_OR = 103
        const val L_OR = 104
        const val L_XOR = 105
        const val I_SHL = 106
        const val L_SHL = 107
        const val I_SHR = 108
        const val L_SHR = 109
        const val I_USHR = 110
        const val L_USHR = 111

    }

    private companion object {
        const val MAGIC = 0x52434331
        const val VERSION = 1
    }
}

private fun ByteArrayOutputStream.writeIntLe(value: Int) {
    write(value ushr 0); write(value ushr 8); write(value ushr 16); write(value ushr 24)
}

private fun ByteArrayOutputStream.writeShortLe(value: Int) {
    write(value ushr 0); write(value ushr 8)
}

private fun ByteArrayOutputStream.writeLongLe(value: Long) {
    for (shift in 0..56 step 8) write((value ushr shift).toInt())
}

private fun ByteArrayOutputStream.writeRccString(value: String) {
    val bytes = value.encodeToByteArray()
    writeIntLe(bytes.size)
    write(bytes)
}
