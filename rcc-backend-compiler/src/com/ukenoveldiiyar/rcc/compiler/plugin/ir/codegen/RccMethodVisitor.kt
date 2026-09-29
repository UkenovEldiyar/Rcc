package com.ukenoveldiiyar.rcc.compiler.plugin.ir.codegen

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.OpcodeEmitter
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.RccBytecodeCollector
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.HostBindingIndex
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.RccReachabilityIndex
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccInstruction
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccOpcodeTable
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccTypeTable
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults.DefaultArgumentResolver
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults.findArgumentStarts
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.JvmType
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.declFqName
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.parseMethodDescriptor
import org.jetbrains.org.objectweb.asm.Handle
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode

class RccMethodVisitor(
    delegate: MethodVisitor,
    private val hostBindingIndex: HostBindingIndex,
    private val reachabilityIndex: RccReachabilityIndex,
    private val onFinished: (List<RccInstruction>, maxStack: Int, maxLocals: Int) -> Unit = { _, _, _ -> },
    private val emitter: OpcodeEmitter = RccBytecodeCollector(),
    private val defaultArgumentResolver: DefaultArgumentResolver? = null,
    private val ownComposerSlot: Int? = null,
) : MethodVisitor(Opcodes.ASM9, delegate) {

    private var maxStack = 0
    private var maxLocals = 0

    private val stackDeltaOverrides = mutableMapOf<Int, Int>()

    private fun recordStackDelta(index: Int, delta: Int) {
        stackDeltaOverrides[index] = delta
    }

    private var pendingSkippedGetstatic = false

    private fun recordedStackDeltas(): Map<Int, Int> = stackDeltaOverrides

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        this.maxStack = maxStack
        this.maxLocals = maxLocals
        super.visitMaxs(maxStack, maxLocals)
    }

    override fun visitInsn(opcode: Int) {
        translateNoOperandInsn(opcode)
        super.visitInsn(opcode)
    }

    private fun translateNoOperandInsn(opcode: Int) {
        when (opcode) {
            Opcodes.NOP -> emitter.emit(RccOpcodeTable.NOP)
            Opcodes.ACONST_NULL -> emitter.emit(RccOpcodeTable.ACONST_NULL)

            Opcodes.ICONST_M1 -> emitter.emit(RccOpcodeTable.I32_PUSH, -1)
            Opcodes.ICONST_0 -> emitter.emit(RccOpcodeTable.I32_PUSH, 0)
            Opcodes.ICONST_1 -> emitter.emit(RccOpcodeTable.I32_PUSH, 1)
            Opcodes.ICONST_2 -> emitter.emit(RccOpcodeTable.I32_PUSH, 2)
            Opcodes.ICONST_3 -> emitter.emit(RccOpcodeTable.I32_PUSH, 3)
            Opcodes.ICONST_4 -> emitter.emit(RccOpcodeTable.I32_PUSH, 4)
            Opcodes.ICONST_5 -> emitter.emit(RccOpcodeTable.I32_PUSH, 5)
            Opcodes.LCONST_0 -> emitter.emit(RccOpcodeTable.I64_PUSH, 0L)
            Opcodes.LCONST_1 -> emitter.emit(RccOpcodeTable.I64_PUSH, 1L)
            Opcodes.FCONST_0 -> emitter.emit(RccOpcodeTable.F32_PUSH, 0.0f)
            Opcodes.FCONST_1 -> emitter.emit(RccOpcodeTable.F32_PUSH, 1.0f)
            Opcodes.FCONST_2 -> emitter.emit(RccOpcodeTable.F32_PUSH, 2.0f)
            Opcodes.DCONST_0 -> emitter.emit(RccOpcodeTable.F64_PUSH, 0.0)
            Opcodes.DCONST_1 -> emitter.emit(RccOpcodeTable.F64_PUSH, 1.0)

            Opcodes.POP -> emitter.emit(RccOpcodeTable.POP)
            Opcodes.DUP -> emitter.emit(RccOpcodeTable.DUP)
            Opcodes.DUP_X1 -> emitter.emit(RccOpcodeTable.DUP_X1)
            Opcodes.SWAP -> emitter.emit(RccOpcodeTable.SWAP)

            Opcodes.IADD -> emitter.emit(RccOpcodeTable.I_ADD)
            Opcodes.LADD -> emitter.emit(RccOpcodeTable.L_ADD)
            Opcodes.FADD -> emitter.emit(RccOpcodeTable.F_ADD)
            Opcodes.DADD -> emitter.emit(RccOpcodeTable.D_ADD)
            Opcodes.ISUB -> emitter.emit(RccOpcodeTable.I_SUB)
            Opcodes.LSUB -> emitter.emit(RccOpcodeTable.L_SUB)
            Opcodes.FSUB -> emitter.emit(RccOpcodeTable.F_SUB)
            Opcodes.DSUB -> emitter.emit(RccOpcodeTable.D_SUB)
            Opcodes.IMUL -> emitter.emit(RccOpcodeTable.I_MUL)
            Opcodes.LMUL -> emitter.emit(RccOpcodeTable.L_MUL)
            Opcodes.FMUL -> emitter.emit(RccOpcodeTable.F_MUL)
            Opcodes.DMUL -> emitter.emit(RccOpcodeTable.D_MUL)
            Opcodes.IDIV -> emitter.emit(RccOpcodeTable.I_DIV)
            Opcodes.LDIV -> emitter.emit(RccOpcodeTable.L_DIV)
            Opcodes.FDIV -> emitter.emit(RccOpcodeTable.F_DIV)
            Opcodes.DDIV -> emitter.emit(RccOpcodeTable.D_DIV)
            Opcodes.IREM -> emitter.emit(RccOpcodeTable.I_REM)
            Opcodes.LREM -> emitter.emit(RccOpcodeTable.L_REM)
            Opcodes.FREM -> emitter.emit(RccOpcodeTable.F_REM)
            Opcodes.DREM -> emitter.emit(RccOpcodeTable.D_REM)
            Opcodes.INEG -> emitter.emit(RccOpcodeTable.I_NEG)
            Opcodes.LNEG -> emitter.emit(RccOpcodeTable.L_NEG)
            Opcodes.FNEG -> emitter.emit(RccOpcodeTable.F_NEG)
            Opcodes.DNEG -> emitter.emit(RccOpcodeTable.D_NEG)

            Opcodes.ISHL -> emitter.emit(RccOpcodeTable.I_SHL)
            Opcodes.LSHL -> emitter.emit(RccOpcodeTable.L_SHL)
            Opcodes.ISHR -> emitter.emit(RccOpcodeTable.I_SHR)
            Opcodes.LSHR -> emitter.emit(RccOpcodeTable.L_SHR)
            Opcodes.IUSHR -> emitter.emit(RccOpcodeTable.I_USHR)
            Opcodes.LUSHR -> emitter.emit(RccOpcodeTable.L_USHR)
            Opcodes.IAND -> emitter.emit(RccOpcodeTable.I_AND)
            Opcodes.LAND -> emitter.emit(RccOpcodeTable.L_AND)
            Opcodes.IOR -> emitter.emit(RccOpcodeTable.I_OR)
            Opcodes.LOR -> emitter.emit(RccOpcodeTable.L_OR)
            Opcodes.IXOR -> emitter.emit(RccOpcodeTable.I_XOR)
            Opcodes.LXOR -> emitter.emit(RccOpcodeTable.L_XOR)

            Opcodes.I2L -> emitter.emit(RccOpcodeTable.I2L)
            Opcodes.I2F -> emitter.emit(RccOpcodeTable.I2F)
            Opcodes.I2D -> emitter.emit(RccOpcodeTable.I2D)
            Opcodes.L2I -> emitter.emit(RccOpcodeTable.L2I)
            Opcodes.L2F -> emitter.emit(RccOpcodeTable.L2F)
            Opcodes.L2D -> emitter.emit(RccOpcodeTable.L2D)
            Opcodes.F2I -> emitter.emit(RccOpcodeTable.F2I)
            Opcodes.F2L -> emitter.emit(RccOpcodeTable.F2L)
            Opcodes.F2D -> emitter.emit(RccOpcodeTable.F2D)
            Opcodes.D2I -> emitter.emit(RccOpcodeTable.D2I)
            Opcodes.D2L -> emitter.emit(RccOpcodeTable.D2L)
            Opcodes.D2F -> emitter.emit(RccOpcodeTable.D2F)
            Opcodes.I2B -> emitter.emit(RccOpcodeTable.I2B)
            Opcodes.I2C -> emitter.emit(RccOpcodeTable.I2C)
            Opcodes.I2S -> emitter.emit(RccOpcodeTable.I2S)

            Opcodes.LCMP -> emitter.emit(RccOpcodeTable.L_CMP)
            Opcodes.FCMPL -> emitter.emit(RccOpcodeTable.F_CMPL)
            Opcodes.FCMPG -> emitter.emit(RccOpcodeTable.F_CMPG)
            Opcodes.DCMPL -> emitter.emit(RccOpcodeTable.D_CMPL)
            Opcodes.DCMPG -> emitter.emit(RccOpcodeTable.D_CMPG)

            Opcodes.IRETURN -> emitter.emit(RccOpcodeTable.RETURN_I)
            Opcodes.LRETURN -> emitter.emit(RccOpcodeTable.RETURN_L)
            Opcodes.FRETURN -> emitter.emit(RccOpcodeTable.RETURN_F)
            Opcodes.DRETURN -> emitter.emit(RccOpcodeTable.RETURN_D)
            Opcodes.ARETURN -> emitter.emit(RccOpcodeTable.RETURN_A)
            Opcodes.RETURN -> emitter.emit(RccOpcodeTable.RETURN_VOID)

            else -> Unit
        }
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        when (opcode) {
            Opcodes.BIPUSH,
            Opcodes.SIPUSH -> emitter.emit(RccOpcodeTable.I32_PUSH, operand)
            else -> Unit
        }
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        when (opcode) {
            Opcodes.ILOAD -> emitter.emit(RccOpcodeTable.I_LOAD, varIndex)
            Opcodes.LLOAD -> emitter.emit(RccOpcodeTable.L_LOAD, varIndex)
            Opcodes.FLOAD -> emitter.emit(RccOpcodeTable.F_LOAD, varIndex)
            Opcodes.DLOAD -> emitter.emit(RccOpcodeTable.D_LOAD, varIndex)
            Opcodes.ALOAD -> emitter.emit(RccOpcodeTable.A_LOAD, varIndex)
            Opcodes.ISTORE -> emitter.emit(RccOpcodeTable.I_STORE, varIndex)
            Opcodes.LSTORE -> emitter.emit(RccOpcodeTable.L_STORE, varIndex)
            Opcodes.FSTORE -> emitter.emit(RccOpcodeTable.F_STORE, varIndex)
            Opcodes.DSTORE -> emitter.emit(RccOpcodeTable.D_STORE, varIndex)
            Opcodes.ASTORE -> emitter.emit(RccOpcodeTable.A_STORE, varIndex)
        }
        super.visitVarInsn(opcode, varIndex)
    }

    override fun visitIincInsn(varIndex: Int, increment: Int) {
        emitter.emit(RccOpcodeTable.I_INC, varIndex, increment)
        super.visitIincInsn(varIndex, increment)
    }

    override fun visitLdcInsn(value: Any?) {
        when (value) {
            is Int -> emitter.emit(RccOpcodeTable.I32_PUSH, value)
            is Long -> emitter.emit(RccOpcodeTable.I64_PUSH, value)
            is Float -> emitter.emit(RccOpcodeTable.F32_PUSH, value)
            is Double -> emitter.emit(RccOpcodeTable.F64_PUSH, value)
            is String -> emitter.emit(RccOpcodeTable.CP_LOAD, value)
            else -> throw UnsupportedOperationException(
                "RccMethodVisitor: LDC константа типа ${value?.let { it::class.qualifiedName }} пока не поддерживается"
            )
        }
        super.visitLdcInsn(value)
    }

    override fun visitJumpInsn(opcode: Int, label: Label) {
        val op = when (opcode) {
            Opcodes.IFEQ -> RccOpcodeTable.IF_EQ
            Opcodes.IFNE -> RccOpcodeTable.IF_NE
            Opcodes.IFLT -> RccOpcodeTable.IF_LT
            Opcodes.IFGE -> RccOpcodeTable.IF_GE
            Opcodes.IFGT -> RccOpcodeTable.IF_GT
            Opcodes.IFLE -> RccOpcodeTable.IF_LE
            Opcodes.IF_ICMPEQ -> RccOpcodeTable.IF_ICMP_EQ
            Opcodes.IF_ICMPNE -> RccOpcodeTable.IF_ICMP_NE
            Opcodes.IF_ICMPLT -> RccOpcodeTable.IF_ICMP_LT
            Opcodes.IF_ICMPGE -> RccOpcodeTable.IF_ICMP_GE
            Opcodes.IF_ICMPGT -> RccOpcodeTable.IF_ICMP_GT
            Opcodes.IF_ICMPLE -> RccOpcodeTable.IF_ICMP_LE
            Opcodes.IF_ACMPEQ -> RccOpcodeTable.IF_RCMP_EQ
            Opcodes.IF_ACMPNE -> RccOpcodeTable.IF_RCMP_NE
            Opcodes.IFNULL -> RccOpcodeTable.IF_NULL
            Opcodes.IFNONNULL -> RccOpcodeTable.IF_NONNULL
            Opcodes.GOTO -> RccOpcodeTable.GOTO

            else -> null
        }
        op?.let { emitter.emitBranch(it, label) }
        super.visitJumpInsn(opcode, label)
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        if (opcode == Opcodes.GETSTATIC) {
            val stableKey = hostBindingIndex.resolveFieldGet(owner, name)
            if (stableKey != null) {
                val index = emitter.size
                emitter.emit(RccOpcodeTable.HOST_INVOKE, stableKey)
                recordStackDelta(index, 1)
                pendingSkippedGetstatic = false
            } else {
                pendingSkippedGetstatic = true
            }
        }
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean,
    ) {
        val hadPendingSkippedGetstatic = pendingSkippedGetstatic
        pendingSkippedGetstatic = false

        if (owner == INLINE_MARKER_OWNER) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }
        if (owner == INTRINSICS_OWNER && name.startsWith("checkNotNull")) {
            val (paramTypes, _) = parseMethodDescriptor(descriptor)
            repeat(paramTypes.size) { emitter.emit(RccOpcodeTable.POP) }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }
        if (owner == PSEUDO_INSN_OWNER) {
            when (name) {
                "FAKE_ALWAYS_TRUE_IFEQ" -> emitter.emit(RccOpcodeTable.I32_PUSH, 0)
                "FAKE_ALWAYS_FALSE_IFEQ" -> emitter.emit(RccOpcodeTable.I32_PUSH, 1)
                "AS_NOT_NULL" -> Unit
                else -> Unit
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }
        if (owner == COMPOSER_KT_OWNER) {
            when (name) {
                "sourceInformationMarkerStart", "traceEventStart", "traceEventEnd", "sourceInformation" -> {
                    val (paramTypes, _) = parseMethodDescriptor(descriptor)
                    repeat(paramTypes.size) { emitter.emit(RccOpcodeTable.POP) }
                }
                "sourceInformationMarkerEnd" -> emitter.emit(RccOpcodeTable.POP)
                "isTraceInProgress" -> emitter.emit(RccOpcodeTable.I32_PUSH, 0)
                else -> throw UnsupportedOperationException(
                    "RccMethodVisitor: $COMPOSER_KT_OWNER.$name$descriptor — незнакомый вызов ComposerKt, " +
                        "не входит в проверенный набор (sourceInformationMarkerStart/End, sourceInformation, " +
                        "isTraceInProgress, traceEventStart/End)"
                )
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }
        if (name == "constructor-impl" && opcode == Opcodes.INVOKESTATIC) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }
        val boxType = BOXED_OWNER_TYPE[owner]
        if (boxType != null && name == "valueOf") {
            val index = emitter.size
            emitter.emit(RccOpcodeTable.BOX, boxType)
            recordStackDelta(index, 0)
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }
        if (boxType != null && name == UNBOX_METHOD_NAME[boxType]) {
            val index = emitter.size
            emitter.emit(RccOpcodeTable.UNBOX, boxType)
            recordStackDelta(index, 0)
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }
        val numberUnboxType = if (owner == "java/lang/Number") UNBOX_TYPE_BY_METHOD_NAME[name] else null
        if (numberUnboxType != null) {
            val index = emitter.size
            emitter.emit(RccOpcodeTable.UNBOX, numberUnboxType)
            recordStackDelta(index, 0)
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            return
        }

        val (paramTypes, returnType) = parseMethodDescriptor(descriptor)
        val defaultBridge = defaultBridgeInfo(name, paramTypes)
        val effDescriptor = defaultBridge?.let { buildDescriptor(it.realParamTypes, returnType) } ?: descriptor
        val effName = defaultBridge?.realName ?: name

        val stableKey = hostBindingIndex.resolve(opcode, owner, effName, effDescriptor)
        val localFqName = declFqName(opcode == Opcodes.INVOKESTATIC, owner, name)
        val localId = reachabilityIndex.resolve(localFqName)

        if (hadPendingSkippedGetstatic) {
            check(stableKey != null && defaultBridge == null) {
                "rcc: $owner.$name$descriptor — вызов сразу за пропущенным (no-op) GETSTATIC " +
                    "обязан быть простым receiver-less host-геттером, не default-мостом и не " +
                    "нерезолвящимся вызовом"
            }
        }

        val receiverCount = if (opcode == Opcodes.INVOKESTATIC || hadPendingSkippedGetstatic) 0 else 1
        val pushCount = if (returnType == JvmType.VoidType) 0 else 1

        when {
            stableKey != null -> {
                val resolver = defaultArgumentResolver
                val maskRemovedCount: Int
                if (defaultBridge != null) {
                    checkNotNull(resolver) {
                        "rcc: $owner.$name$descriptor — host-байндинг на функцию с default-параметрами " +
                            "требует DefaultArgumentResolver, но он не передан"
                    }
                    spliceOrdinaryDefaults(resolver, owner, defaultBridge, descriptor, effDescriptor, opcode)
                    maskRemovedCount = 2
                } else {
                    val info = composableInvokeInfo(owner, name, descriptor, paramTypes)
                    if (info != null) {
                        if (info.hasDefaultMask) {
                            checkNotNull(resolver) {
                                "rcc: $owner.$name$descriptor — composable host-байндинг с default-параметрами " +
                                    "требует DefaultArgumentResolver, но он не передан"
                            }
                            spliceComposableDefaults(resolver, owner, name, descriptor, info, opcode)
                        }

                        collapseChangedInts(info)
                        maskRemovedCount = (if (info.hasDefaultMask) 1 else 0) + (info.changedCount - 1)
                    } else {
                        maskRemovedCount = 0
                    }
                }
                val index = emitter.size
                emitter.emit(RccOpcodeTable.HOST_INVOKE, stableKey)
                recordStackDelta(index, pushCount - (paramTypes.size - maskRemovedCount + receiverCount))
            }
            localId != null -> {
                val index = emitter.size
                emitter.emit(RccOpcodeTable.INVOKE, localId)
                recordStackDelta(index, pushCount - paramTypes.size)
            }
            else -> throw UnsupportedOperationException(
                "RccMethodVisitor: вызов $owner.$name$descriptor не резолвится ни в host-байндинг, " +
                    "ни в достижимую локальную функцию"
            )
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    private data class DefaultBridgeInfo(val realName: String, val realParamTypes: List<JvmType>)

    private fun defaultBridgeInfo(name: String, paramTypes: List<JvmType>): DefaultBridgeInfo? {
        if (!name.endsWith("\$default") || paramTypes.size < 2) return null
        val maskType = paramTypes[paramTypes.size - 2]
        val markerType = paramTypes[paramTypes.size - 1]
        val isMask = maskType is JvmType.Primitive && maskType.descriptor == 'I'
        val isMarker = markerType is JvmType.ObjectType && markerType.internalName == "java/lang/Object"
        if (!isMask || !isMarker) return null
        return DefaultBridgeInfo(name.removeSuffix("\$default"), paramTypes.dropLast(2))
    }

    private data class ComposableInvokeInfo(
        val composerIndex: Int,
        val realParamCount: Int,
        val changedCount: Int,
        val hasDefaultMask: Boolean,
    )

    private fun composableInvokeInfo(
        owner: String,
        name: String,
        descriptor: String,
        paramTypes: List<JvmType>,
    ): ComposableInvokeInfo? {
        val composerIndex = paramTypes.indexOfFirst {
            it is JvmType.ObjectType && it.internalName == COMPOSER_TYPE_INTERNAL_NAME
        }
        if (composerIndex < 0) return null
        val expectedChangedCount = if (composerIndex <= 0) 1 else (composerIndex - 1) / 10 + 1

        val hasDefaultMask = when (val trailingInts = paramTypes.size - composerIndex - 1) {
            expectedChangedCount -> false
            expectedChangedCount + 1 -> true
            else -> error(
                "rcc: $owner.$name$descriptor — composable-вызов с неожиданным числом trailing-Int " +
                    "после Composer ($trailingInts, ожидалось $expectedChangedCount или " +
                    "${expectedChangedCount + 1})"
            )
        }
        return ComposableInvokeInfo(composerIndex,
            composerIndex, expectedChangedCount, hasDefaultMask)
    }

    private fun collapseChangedInts(info: ComposableInvokeInfo) {
        val starts = findArgumentStarts(
            emitter,
            endExclusive = emitter.size,
            argCount = info.changedCount,
            overrides = stackDeltaOverrides,
        )
        emitter.spliceRange(
            starts[0],
            emitter.size,
            listOf(RccInstruction(RccOpcodeTable.I32_PUSH, listOf(DIFFERENT_BITS_SLOT_0))),
        )
    }

    private fun spliceOrdinaryDefaults(
        resolver: DefaultArgumentResolver,
        owner: String,
        bridge: DefaultBridgeInfo,
        bridgeDescriptor: String,
        realDescriptor: String,
        opcode: Int,
    ) {
        val realParamCount = bridge.realParamTypes.size
        val starts = findArgumentStarts(emitter, endExclusive = emitter.size, argCount = realParamCount + 2, overrides = stackDeltaOverrides)
        val maskStart = starts[realParamCount]
        val markerStart = starts[realParamCount + 1]

        check(markerStart == emitter.size - 1 && emitter.instructionAt(markerStart).op == RccOpcodeTable.ACONST_NULL) {
            "rcc: $owner.${bridge.realName}\$default — marker-аргумент неожиданной формы (ожидался " +
                "ACONST_NULL, ровно 1 инструкция)"
        }
        val maskInsn = emitter.instructionAt(maskStart)
        check(maskStart == markerStart - 1 && maskInsn.op == RccOpcodeTable.I32_PUSH) {
            "rcc: $owner.${bridge.realName}\$default — defaultMask-аргумент неожиданной формы " +
                "(ожидался I32_PUSH, ровно 1 инструкция)"
        }
        val maskValue = maskInsn.operands[0] as Int

        emitter.spliceRange(maskStart, emitter.size, emptyList())

        val isStatic = opcode == Opcodes.INVOKESTATIC
        val maskSlot = localSlotOf(bridge.realParamTypes, realParamCount, isStatic)
        val receiverOffset = if (hostBindingIndex.hasReceiver(opcode, owner, bridge.realName, realDescriptor)) 1 else 0
        for (i in realParamCount - 1 downTo receiverOffset) {
            val bitIndex = i - receiverOffset
            if ((maskValue shr bitIndex) and 1 == 0) continue
            val paramSlot = localSlotOf(bridge.realParamTypes, i, isStatic)
            val resolved = resolver.resolveOrdinaryDefault(owner, bridge.realName, bridgeDescriptor, maskSlot, bitIndex, paramSlot)
                ?: throw UnsupportedOperationException(
                    "rcc: не удалось разрешить default-значение параметра #$bitIndex у " +
                        "$owner.${bridge.realName}\$default$bridgeDescriptor"
                )
            replaceDefaultedArgument(resolver, starts[i], starts[i + 1], resolved, "$owner.${bridge.realName}, параметр #$bitIndex")
        }
    }

    private fun spliceComposableDefaults(
        resolver: DefaultArgumentResolver,
        owner: String,
        name: String,
        descriptor: String,
        info: ComposableInvokeInfo,
        opcode: Int,
    ) {
        val totalBeforeMask = info.realParamCount + 1 + info.changedCount
        val starts = findArgumentStarts(emitter, endExclusive = emitter.size, argCount = totalBeforeMask + 1, overrides = stackDeltaOverrides)
        val maskStart = starts[totalBeforeMask]

        check(maskStart == emitter.size - 1) {
            "rcc: $owner.$name$descriptor — defaultMask-аргумент занимает больше 1 инструкции"
        }
        val maskInsn = emitter.instructionAt(maskStart)
        check(maskInsn.op == RccOpcodeTable.I32_PUSH) {
            "rcc: $owner.$name$descriptor — defaultMask-аргумент неожиданной формы (${maskInsn.op})"
        }
        val maskValue = maskInsn.operands[0] as Int

        emitter.spliceRange(maskStart, emitter.size, emptyList())

        val (fullParamTypes, _) = parseMethodDescriptor(descriptor)
        val isStatic = opcode == Opcodes.INVOKESTATIC
        val maskSlot = localSlotOf(fullParamTypes, fullParamTypes.size - 1, isStatic)

        val composerIndexInTarget = fullParamTypes.indexOfFirst {
            it is JvmType.ObjectType && it.internalName == COMPOSER_TYPE_INTERNAL_NAME
        }
        val composerSlotInTarget = if (composerIndexInTarget >= 0) {
            localSlotOf(fullParamTypes, composerIndexInTarget, isStatic)
        } else {
            null
        }
        for (i in info.realParamCount - 1 downTo 0) {
            if ((maskValue shr i) and 1 == 0) continue
            val paramSlot = localSlotOf(fullParamTypes, i, isStatic)
            val resolved = resolver.resolveComposableDefault(owner, name, descriptor, maskSlot, i, paramSlot)
                ?: throw UnsupportedOperationException(
                    "rcc: не удалось разрешить default-значение параметра #$i у $owner.$name$descriptor"
                )
            replaceDefaultedArgument(
                resolver, starts[i], starts[i + 1], resolved, "$owner.$name, параметр #$i",
                composerSlotInTarget = composerSlotInTarget,
            )
        }
    }

    private fun replaceDefaultedArgument(
        resolver: DefaultArgumentResolver,
        start: Int,
        end: Int,
        resolvedInsns: List<AbstractInsnNode>,
        context: String,
        composerSlotInTarget: Int? = null,
    ) {
        val zeroValueInsns = (start until end).filter { emitter.instructionAt(it).op != RccOpcodeTable.NOP }
        check(zeroValueInsns.size == 1 && emitter.instructionAt(zeroValueInsns.single()).op in ZERO_VALUE_PLACEHOLDER_OPS) {
            "rcc: default-параметр ($context) — placeholder неожиданной формы " +
                "(${(start until end).map { emitter.instructionAt(it).op }}), ожидался один из " +
                "$ZERO_VALUE_PLACEHOLDER_OPS плюс NOP"
        }
        val tempEmitter = RccBytecodeCollector()
        val replay = RccMethodVisitor(
            delegate = NoOpMethodVisitor,
            hostBindingIndex = hostBindingIndex,
            reachabilityIndex = reachabilityIndex,
            emitter = tempEmitter,
            defaultArgumentResolver = resolver,
            ownComposerSlot = ownComposerSlot,
        )
        for (insn in resolvedInsns) {
            if (composerSlotInTarget != null && ownComposerSlot != null &&
                insn is VarInsnNode && insn.opcode == Opcodes.ALOAD && insn.`var` == composerSlotInTarget
            ) {
                replay.visitVarInsn(Opcodes.ALOAD, ownComposerSlot)
            } else {
                insn.accept(replay)
            }
        }
        val replayedInsns = tempEmitter.finish()
        for ([localIndex, delta] in replay.recordedStackDeltas()) {
            recordStackDelta(start + localIndex, delta)
        }
        emitter.spliceRange(start, end, replayedInsns)
    }

    private fun jvmSlotWidth(type: JvmType): Int =
        if (type is JvmType.Primitive && (type.descriptor == 'J' || type.descriptor == 'D')) 2 else 1

    private fun localSlotOf(paramTypes: List<JvmType>, index: Int, isStatic: Boolean): Int {
        var slot = if (isStatic) 0 else 1
        for (i in 0 until index) slot += jvmSlotWidth(paramTypes[i])
        return slot
    }

    private fun jvmTypeDescriptor(type: JvmType): String = when (type) {
        is JvmType.Primitive -> type.descriptor.toString()
        is JvmType.ObjectType -> "L${type.internalName};"
        is JvmType.ArrayType -> "[" + jvmTypeDescriptor(type.element)
        JvmType.VoidType -> "V"
    }

    private fun buildDescriptor(paramTypes: List<JvmType>, returnType: JvmType): String =
        "(" + paramTypes.joinToString("") { jvmTypeDescriptor(it) } + ")" + jvmTypeDescriptor(returnType)

    override fun visitInvokeDynamicInsn(
        name: String,
        descriptor: String,
        bootstrapMethodHandle: Handle,
        vararg bootstrapMethodArguments: Any,
    ) {
        if (bootstrapMethodHandle.owner == LAMBDA_METAFACTORY_OWNER && bootstrapMethodHandle.name == "metafactory") {
            val implHandle = bootstrapMethodArguments.getOrNull(1) as? Handle
                ?: throw UnsupportedOperationException(
                    "RccMethodVisitor: LambdaMetafactory.metafactory без impl-аргумента (индекс 1) " +
                        "в bootstrap args для invokedynamic $name$descriptor"
                )
            val id = reachabilityIndex.idForLambdaImpl(implHandle.owner, implHandle.name, implHandle.desc)
            val captureCount = Type.getArgumentTypes(descriptor).size
            reachabilityIndex.noteLambdaCaptureCount(id, captureCount)
            val index = emitter.size
            emitter.emit(RccOpcodeTable.HOST_LAMBDA, id)
            recordStackDelta(index, 1 - captureCount)
        } else {
            throw UnsupportedOperationException(
                "RccMethodVisitor: invokedynamic $name$descriptor через " +
                    "${bootstrapMethodHandle.owner}.${bootstrapMethodHandle.name} — поддержан только " +
                    "$LAMBDA_METAFACTORY_OWNER.metafactory"
            )
        }
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitLabel(label: Label) {
        emitter.markLabel(label)
        super.visitLabel(label)
    }

    override fun visitEnd() {
        onFinished(emitter.finish(), maxStack, maxLocals)
        super.visitEnd()
    }

    private object NoOpMethodVisitor : MethodVisitor(Opcodes.ASM9)

    private companion object {
        const val INLINE_MARKER_OWNER = "kotlin/jvm/internal/InlineMarker"
        const val INTRINSICS_OWNER = "kotlin/jvm/internal/Intrinsics"
        const val PSEUDO_INSN_OWNER = "kotlin/jvm/internal/\$PseudoInsn"
        const val COMPOSER_KT_OWNER = "androidx/compose/runtime/ComposerKt"
        const val COMPOSER_TYPE_INTERNAL_NAME = "androidx/compose/runtime/Composer"
        const val LAMBDA_METAFACTORY_OWNER = "java/lang/invoke/LambdaMetafactory"
        const val DIFFERENT_BITS_SLOT_0 = 4

        val ZERO_VALUE_PLACEHOLDER_OPS: Set<RccOpcodeTable> = setOf(
            RccOpcodeTable.ACONST_NULL,
            RccOpcodeTable.I32_PUSH,
            RccOpcodeTable.I64_PUSH,
            RccOpcodeTable.F32_PUSH,
            RccOpcodeTable.F64_PUSH,
        )

        val BOXED_OWNER_TYPE: Map<String, RccTypeTable> = mapOf(
            "java/lang/Integer" to RccTypeTable.INT,
            "java/lang/Long" to RccTypeTable.LONG,
            "java/lang/Float" to RccTypeTable.FLOAT,
            "java/lang/Double" to RccTypeTable.DOUBLE,
        )

        val UNBOX_METHOD_NAME: Map<RccTypeTable, String> = mapOf(
            RccTypeTable.INT to "intValue",
            RccTypeTable.LONG to "longValue",
            RccTypeTable.FLOAT to "floatValue",
            RccTypeTable.DOUBLE to "doubleValue",
        )

        val UNBOX_TYPE_BY_METHOD_NAME: Map<String, RccTypeTable> =
            UNBOX_METHOD_NAME.entries.associate { (type, name) -> name to type }
    }
}
