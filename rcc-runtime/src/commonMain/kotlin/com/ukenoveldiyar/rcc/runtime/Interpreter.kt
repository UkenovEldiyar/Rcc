package com.ukenoveldiyar.rcc.runtime

import androidx.compose.runtime.Composer
import androidx.compose.runtime.internal.ComposableLambda
import androidx.compose.runtime.internal.composableLambda
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.NOP
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.A_CONST_NULL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.A_LOAD
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.A_RETURN
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.A_STORE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.BOX
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.CP_LOAD
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D2F
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D2I
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D2L
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.DUP
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.DUP_X1
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.DUP_X2
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_ADD
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_CMPG
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_CMPL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_DIV
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_MUL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_NEG
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_REM
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.D_SUB
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F2D
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F2I
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F2L
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_ADD
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_CMPG
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_CMPL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_DIV
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_MUL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_NEG
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_REM
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.F_SUB
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.GOTO
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.HOST_COMPOSABLE_LAMBDA
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.HOST_INVOKE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.HOST_LAMBDA
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I2B
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I2C
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I2D
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I2F
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I2L
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I2S
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I32_PUSH
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I64_PUSH
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ACMP_EQ
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ACMP_NE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_EQ
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_GE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_GT
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ICMP_EQ
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ICMP_GE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ICMP_GT
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ICMP_LE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ICMP_LT
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_ICMP_NE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_LE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_LT
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_NE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_NONNULL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.IF_NULL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.INVOKE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_ADD
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_AND
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_DIV
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_INC
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_MUL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_NEG
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_OR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_REM
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_SHL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_SHR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_USHR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.RAW_RETURN
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_SUB
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.I_XOR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L2D
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L2F
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L2I
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.LOOKUP_SWITCH
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_ADD
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_AND
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_CMP
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_DIV
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_MUL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_NEG
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_OR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_REM
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_SHL
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_SHR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_USHR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_SUB
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.L_XOR
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.POP
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.RAW_LOAD
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.RAW_STORE
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.RETURN
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.SWAP
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.TABLE_SWITCH
import com.ukenoveldiyar.rcc.runtime.code.OpCodeKind.UNBOX
import com.ukenoveldiyar.rcc.runtime.external.ExternalScopeImpl
import com.ukenoveldiyar.rcc.runtime.lambda.Capture
import com.ukenoveldiyar.rcc.runtime.lambda.createComposableLambda
import com.ukenoveldiyar.rcc.runtime.lambda.createLambda
import com.ukenoveldiyar.rcc.runtime.lambda.popCapture
import com.ukenoveldiyar.rcc.runtime.stack.RETURN_SITE_EXIT
import com.ukenoveldiyar.rcc.runtime.stack.StackFrame
import com.ukenoveldiyar.rcc.runtime.stack.packCodeSite
import com.ukenoveldiyar.rcc.runtime.stack.popBoxed
import com.ukenoveldiyar.rcc.runtime.stack.pushUnboxed
import com.ukenoveldiyar.rcc.runtime.stack.unpackId
import com.ukenoveldiyar.rcc.runtime.stack.unpackIp

internal class Interpreter(
    val memoryLayout: MemoryLayout,
    val stackFrame: StackFrame = StackFrame(),
) {
    lateinit var instruction: Instruction

    val interpreterFactory = { Interpreter(memoryLayout) }

    fun call(function: Function) {
        run(function, boxResult = false)
    }

    fun callResult(function: Function): Any? = run(function, boxResult = true)

    private fun run(
        function: Function,
        boxResult: Boolean
    ): Any? {
        val savedInstruction = if (::instruction.isInitialized) instruction else null
        val savedIp = stackFrame.ip
        val savedFunctionId = stackFrame.functionId
        val savedFp = stackFrame.fp
        val savedBp = stackFrame.bp
        val savedSp = stackFrame.sp - function.parameterCount

        instruction = memoryLayout.functions[function.id].instruction

        stackFrame.pushFrame(
            parameterCount = function.parameterCount,
            maxStackSize = function.maxStackSize,
            maxLocalSize = function.maxLocalSize,
            returnSite = RETURN_SITE_EXIT,
        )

        stackFrame.functionId = function.id
        stackFrame.ip = 0

        var result: Any? = null

        try {
            interpreter()
            if (boxResult) result = stackFrame.popBoxed(function.returnType)
        } finally {
            savedInstruction?.let { instruction = it }
            stackFrame.ip = savedIp
            stackFrame.functionId = savedFunctionId
            stackFrame.fp = savedFp
            stackFrame.bp = savedBp

            if (stackFrame.sp > savedSp) {
                stackFrame.slotsObject.fill(null, savedSp, stackFrame.sp)
            }

            stackFrame.sp = savedSp
        }

        return result
    }

    private fun interpreter() {
        loop@ while (true) {
            when (val opcode = instruction.opcode(stackFrame.ip++)) {
                NOP -> Unit
                A_CONST_NULL -> stackFrame.pushNull()
                I32_PUSH -> stackFrame.pushInt(instruction.operandI32(stackFrame.ip++))
                I64_PUSH -> stackFrame.pushLong(instruction.operandI64(stackFrame.ip++))

                CP_LOAD -> {
                    val cpIndex = instruction.operandI32(stackFrame.ip++)
                    stackFrame.pushObject(memoryLayout.constantPool[cpIndex])
                }

                RAW_LOAD -> stackFrame.loadRaw(instruction.operandI32(stackFrame.ip++))
                A_LOAD -> stackFrame.loadObject(instruction.operandI32(stackFrame.ip++))

                RAW_STORE -> stackFrame.storeRaw(instruction.operandI32(stackFrame.ip++))
                A_STORE -> stackFrame.storeObject(instruction.operandI32(stackFrame.ip++))

                POP -> stackFrame.drop()
                DUP -> stackFrame.dup()
                DUP_X1 -> stackFrame.dupX1()
                DUP_X2 -> stackFrame.dupX2()

                SWAP -> stackFrame.swap()

                I_ADD -> stackFrame.pushInt(stackFrame.popInt() + stackFrame.popInt())
                L_ADD -> stackFrame.pushLong(stackFrame.popLong() + stackFrame.popLong())
                F_ADD -> stackFrame.pushFloat(stackFrame.popFloat() + stackFrame.popFloat())
                D_ADD -> stackFrame.pushDouble(stackFrame.popDouble() + stackFrame.popDouble())

                I_SUB -> {
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    stackFrame.pushInt(left - right)
                }

                L_SUB -> {
                    val right = stackFrame.popLong()
                    val left = stackFrame.popLong()
                    stackFrame.pushLong(left - right)
                }

                F_SUB -> {
                    val right = stackFrame.popFloat()
                    val left = stackFrame.popFloat()
                    stackFrame.pushFloat(left - right)
                }

                D_SUB -> {
                    val right = stackFrame.popDouble()
                    val left = stackFrame.popDouble()
                    stackFrame.pushDouble(left - right)
                }

                I_MUL -> stackFrame.pushInt(stackFrame.popInt() * stackFrame.popInt())
                L_MUL -> stackFrame.pushLong(stackFrame.popLong() * stackFrame.popLong())
                F_MUL -> stackFrame.pushFloat(stackFrame.popFloat() * stackFrame.popFloat())
                D_MUL -> stackFrame.pushDouble(stackFrame.popDouble() * stackFrame.popDouble())

                I_DIV -> {
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    stackFrame.pushInt(left / right)
                }

                L_DIV -> {
                    val right = stackFrame.popLong()
                    val left = stackFrame.popLong()
                    stackFrame.pushLong(left / right)
                }

                F_DIV -> {
                    val right = stackFrame.popFloat()
                    val left = stackFrame.popFloat()
                    stackFrame.pushFloat(left / right)
                }

                D_DIV -> {
                    val right = stackFrame.popDouble()
                    val left = stackFrame.popDouble()
                    stackFrame.pushDouble(left / right)
                }

                I_REM -> {
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    stackFrame.pushInt(left % right)
                }

                L_REM -> {
                    val right = stackFrame.popLong()
                    val left = stackFrame.popLong()
                    stackFrame.pushLong(left % right)
                }

                F_REM -> {
                    val right = stackFrame.popFloat()
                    val left = stackFrame.popFloat()
                    stackFrame.pushFloat(left % right)
                }

                D_REM -> {
                    val right = stackFrame.popDouble()
                    val left = stackFrame.popDouble()
                    stackFrame.pushDouble(left % right)
                }

                I_NEG -> stackFrame.pushInt(-stackFrame.popInt())
                L_NEG -> stackFrame.pushLong(-stackFrame.popLong())
                F_NEG -> stackFrame.pushFloat(-stackFrame.popFloat())
                D_NEG -> stackFrame.pushDouble(-stackFrame.popDouble())

                I_INC -> {
                    val index = instruction.operandI32(stackFrame.ip++)
                    val delta = instruction.operandI32(stackFrame.ip++)

                    stackFrame.incInt(index, delta)
                }

                I_XOR -> stackFrame.pushInt(stackFrame.popInt() xor stackFrame.popInt())
                L_XOR -> stackFrame.pushLong(stackFrame.popLong() xor stackFrame.popLong())
                I_AND -> stackFrame.pushInt(stackFrame.popInt() and stackFrame.popInt())
                L_AND -> stackFrame.pushLong(stackFrame.popLong() and stackFrame.popLong())
                I_OR -> stackFrame.pushInt(stackFrame.popInt() or stackFrame.popInt())
                L_OR -> stackFrame.pushLong(stackFrame.popLong() or stackFrame.popLong())

                I_SHL -> {
                    val shift = stackFrame.popInt()
                    val value = stackFrame.popInt()
                    stackFrame.pushInt(value shl shift)
                }

                L_SHL -> {
                    val shift = stackFrame.popInt()
                    val value = stackFrame.popLong()
                    stackFrame.pushLong(value shl shift)
                }

                I_SHR -> {
                    val shift = stackFrame.popInt()
                    val value = stackFrame.popInt()
                    stackFrame.pushInt(value shr shift)
                }

                L_SHR -> {
                    val shift = stackFrame.popInt()
                    val value = stackFrame.popLong()
                    stackFrame.pushLong(value shr shift)
                }

                I_USHR -> {
                    val shift = stackFrame.popInt()
                    val value = stackFrame.popInt()
                    stackFrame.pushInt(value ushr shift)
                }

                L_USHR -> {
                    val shift = stackFrame.popInt()
                    val value = stackFrame.popLong()
                    stackFrame.pushLong(value ushr shift)
                }

                I2L -> stackFrame.pushLong(stackFrame.popInt().toLong())
                I2F -> stackFrame.pushFloat(stackFrame.popInt().toFloat())
                I2D -> stackFrame.pushDouble(stackFrame.popInt().toDouble())
                L2I -> stackFrame.pushInt(stackFrame.popLong().toInt())
                L2F -> stackFrame.pushFloat(stackFrame.popLong().toFloat())
                L2D -> stackFrame.pushDouble(stackFrame.popLong().toDouble())
                F2I -> stackFrame.pushInt(stackFrame.popFloat().toInt())
                F2L -> stackFrame.pushLong(stackFrame.popFloat().toLong())
                F2D -> stackFrame.pushDouble(stackFrame.popFloat().toDouble())
                D2I -> stackFrame.pushInt(stackFrame.popDouble().toInt())
                D2L -> stackFrame.pushLong(stackFrame.popDouble().toLong())
                D2F -> stackFrame.pushFloat(stackFrame.popDouble().toFloat())
                I2B -> stackFrame.pushInt(stackFrame.popInt().toByte().toInt())
                I2C -> stackFrame.pushInt(stackFrame.popInt().toChar().code)
                I2S -> stackFrame.pushInt(stackFrame.popInt().toShort().toInt())

                L_CMP -> {
                    val right = stackFrame.popLong()
                    val left = stackFrame.popLong()
                    stackFrame.pushInt(left.compareTo(right))
                }

                F_CMPL -> {
                    val right = stackFrame.popFloat()
                    val left = stackFrame.popFloat()
                    stackFrame.pushInt(compareFloat(left, right, -1))
                }

                F_CMPG -> {
                    val right = stackFrame.popFloat()
                    val left = stackFrame.popFloat()
                    stackFrame.pushInt(compareFloat(left, right, 1))
                }

                D_CMPL -> {
                    val right = stackFrame.popDouble()
                    val left = stackFrame.popDouble()
                    stackFrame.pushInt(compareDouble(left, right, -1))
                }

                D_CMPG -> {
                    val right = stackFrame.popDouble()
                    val left = stackFrame.popDouble()
                    stackFrame.pushInt(compareDouble(left, right, 1))
                }

                IF_EQ -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popInt() == 0) stackFrame.ip = target
                }

                IF_NE -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popInt() != 0) stackFrame.ip = target
                }

                IF_LT -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popInt() < 0) stackFrame.ip = target
                }

                IF_GE -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popInt() >= 0) stackFrame.ip = target
                }

                IF_GT -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popInt() > 0) stackFrame.ip = target
                }

                IF_LE -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popInt() <= 0) stackFrame.ip = target
                }

                IF_ICMP_EQ -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    if (left == right) stackFrame.ip = target
                }

                IF_ICMP_NE -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    if (left != right) stackFrame.ip = target
                }

                IF_ICMP_LT -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    if (left < right) stackFrame.ip = target
                }

                IF_ICMP_GE -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    if (left >= right) stackFrame.ip = target
                }

                IF_ICMP_GT -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    if (left > right) stackFrame.ip = target
                }

                IF_ICMP_LE -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popInt()
                    val left = stackFrame.popInt()
                    if (left <= right) stackFrame.ip = target
                }

                IF_ACMP_EQ -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popObject()
                    val left = stackFrame.popObject()
                    if (left === right) stackFrame.ip = target
                }

                IF_ACMP_NE -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    val right = stackFrame.popObject()
                    val left = stackFrame.popObject()
                    if (left !== right) stackFrame.ip = target
                }

                IF_NULL -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popObject() == null) stackFrame.ip = target
                }

                IF_NONNULL -> {
                    val target = instruction.operandI32(stackFrame.ip++)
                    if (stackFrame.popObject() != null) stackFrame.ip = target
                }

                GOTO -> {
                    stackFrame.ip = instruction.operandI32(stackFrame.ip)
                }

                TABLE_SWITCH -> {
                    val tableStart = stackFrame.ip
                    val defaultTarget = instruction.operandI32(tableStart)

                    val low = instruction.operandI32(tableStart + 1)
                    val high = instruction.operandI32(tableStart + 2)
                    val key = stackFrame.popInt()

                    stackFrame.ip = if (key !in low..high) {
                        defaultTarget
                    } else {
                        instruction.operandI32(tableStart + 3 + (key - low))
                    }
                }

                LOOKUP_SWITCH -> {
                    val tableStart = stackFrame.ip

                    val defaultTarget = instruction.operandI32(tableStart)
                    val pairCount = instruction.operandI32(tableStart + 1)
                    val key = stackFrame.popInt()
                    val pairsStart = tableStart + 2

                    var low = 0
                    var high = pairCount - 1
                    var target = defaultTarget

                    while (low <= high) {
                        val middle = (low + high) ushr 1
                        val middleKey = instruction.operandI32(pairsStart + middle * 2)
                        when {
                            key < middleKey -> high = middle - 1
                            key > middleKey -> low = middle + 1
                            else -> {
                                target = instruction.operandI32(pairsStart + middle * 2 + 1)
                                break
                            }
                        }
                    }

                    stackFrame.ip = target
                }

                RAW_RETURN -> {
                    val result = stackFrame.popRaw()
                    val returnSite = stackFrame.popFrame()
                    stackFrame.pushRaw(result)

                    if (returnSite == RETURN_SITE_EXIT) break@loop

                    stackFrame.functionId = unpackId(returnSite)
                    stackFrame.ip = unpackIp(returnSite)

                    instruction = memoryLayout.functions[stackFrame.functionId].instruction
                }

                A_RETURN -> {
                    val resultObject = stackFrame.popObject()
                    val returnSite = stackFrame.popFrame()

                    stackFrame.pushObject(resultObject)
                    if (returnSite == RETURN_SITE_EXIT) break@loop

                    stackFrame.functionId = unpackId(returnSite)
                    stackFrame.ip = unpackIp(returnSite)

                    instruction = memoryLayout.functions[stackFrame.functionId].instruction
                }

                RETURN -> {
                    val returnSite = stackFrame.popFrame()
                    if (returnSite == RETURN_SITE_EXIT) break@loop

                    stackFrame.functionId = unpackId(returnSite)
                    stackFrame.ip = unpackIp(returnSite)

                    instruction = memoryLayout.functions[stackFrame.functionId].instruction
                }

                INVOKE -> invoke()
                HOST_INVOKE -> hostInvoke()
                HOST_LAMBDA -> hostLambda()
                HOST_COMPOSABLE_LAMBDA -> hostComposableLambda()

                BOX -> {
                    val rawType = instruction.operandI32(stackFrame.ip++).toByte()
                    stackFrame.pushObject(stackFrame.popBoxed(rawType))
                }

                UNBOX -> {
                    val rawType = instruction.operandI32(stackFrame.ip++).toByte()
                    stackFrame.pushUnboxed(stackFrame.popObject(), rawType)
                }

                else -> throw RccUnsupportedOpCodeError(
                    opcode = opcode,
                    functionId = stackFrame.functionId,
                    ip = stackFrame.ip,
                    sp = stackFrame.sp,
                )
            }
        }
    }

    private fun invoke() {
        val constFunctionId = instruction.operandI32(stackFrame.ip++)

        val constFunction = memoryLayout.functions[constFunctionId]

        stackFrame.pushFrame(
            parameterCount = constFunction.parameterCount,
            maxStackSize = constFunction.maxStackSize,
            maxLocalSize = constFunction.maxLocalSize,
            returnSite = packCodeSite(stackFrame.functionId, stackFrame.ip),
        )

        stackFrame.functionId = constFunctionId
        stackFrame.ip = 0
        instruction = constFunction.instruction
    }

    private fun hostInvoke() {
        val functionId = instruction.operandI32(stackFrame.ip++)
        val componentId = instruction.operandI32(stackFrame.ip++)
        val name = memoryLayout.constantPool[componentId]
        val binding = checkNotNull(memoryLayout.hostComponent.resolve(name)) {
            "rcc: host-биндинг \"$name\" не зарегистрирован ни в одном подключённом module()"
        }

        if (binding is ComposableBinding) {
            val change = stackFrame.popInt()
            val composer = stackFrame.popObject() as Composer
            val hostCall = ExternalScopeImpl(stackFrame)
            (binding.block as ComposableLambda).invoke(hostCall, composer, change)
        } else {
            val hostCall = ExternalScopeImpl(stackFrame)
            @Suppress("UNCHECKED_CAST")
            (binding as HostInvoke).invoke(hostCall)
        }
    }

    private fun hostLambda() {
        val functionId = instruction.operandI32(stackFrame.ip++)
        val function = memoryLayout.functions[functionId]

        val capture = if (function.hasCapture) {
            stackFrame.popCapture(function.captureParameterCount)
        } else {
            Capture.EMPTY
        }

        val lambda = createLambda(
            function = function,
            capture = capture,
            interpreter = interpreterFactory,
        )

        stackFrame.pushObject(lambda)
    }

    private fun hostComposableLambda() {
        val functionId = instruction.operandI32(stackFrame.ip++)
        val function = memoryLayout.functions[functionId]

        val capture = if (function.hasCapture) {
            stackFrame.popCapture(function.captureParameterCount)
        } else {
            Capture.EMPTY
        }

        val key = stackFrame.popInt()
        val composer = stackFrame.popObject() as Composer

        val composableLambda = composableLambda(
            composer = composer,
            key = key,
            tracked = function.hasCapture,
            block = createComposableLambda(
                function = function,
                capture = capture,
                interpreter = interpreterFactory
            )
        )

        stackFrame.pushObject(composableLambda)
    }

    private fun compareFloat(left: Float, right: Float, nanResult: Int): Int = when {
        left.isNaN() || right.isNaN() -> nanResult
        left > right -> 1
        left < right -> -1
        else -> 0
    }

    private fun compareDouble(left: Double, right: Double, nanResult: Int): Int = when {
        left.isNaN() || right.isNaN() -> nanResult
        left > right -> 1
        left < right -> -1
        else -> 0
    }
}