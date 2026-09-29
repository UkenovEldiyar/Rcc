package com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.org.objectweb.asm.tree.IntInsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode

class DefaultArgumentResolver(private val classpathRoots: List<java.io.File>) {
    private val finder = ClasspathClassFinder(classpathRoots)
    private val classNodeCache = mutableMapOf<String, ClassNode?>()

    private fun classNode(ownerInternalName: String): ClassNode? =
        classNodeCache.getOrPut(ownerInternalName) {
            val bytes = finder.findClassBytes(ownerInternalName) ?: return@getOrPut null
            val node = ClassNode(Opcodes.ASM9)
            ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG)
            node
        }

    fun resolveComposableDefault(
        ownerInternalName: String,
        methodName: String,
        methodDescriptor: String,
        maskSlot: Int,
        paramIndex: Int,
        paramSlot: Int,
    ): List<AbstractInsnNode>? {
        val method = findMethod(ownerInternalName, methodName, methodDescriptor) ?: return null
        return extractDefaultExpression(method, maskSlot, paramIndex, paramSlot)
    }

    fun resolveOrdinaryDefault(
        ownerInternalName: String,
        methodName: String,
        defaultBridgeDescriptor: String,
        maskSlot: Int,
        paramIndex: Int,
        paramSlot: Int,
    ): List<AbstractInsnNode>? {
        val method = findMethod(ownerInternalName, "$methodName\$default", defaultBridgeDescriptor) ?: return null
        return extractDefaultExpression(method, maskSlot, paramIndex, paramSlot)
    }

    private fun findMethod(ownerInternalName: String, name: String, descriptor: String): MethodNode? {
        var owner = ownerInternalName
        repeat(FACADE_FORWARD_LIMIT) {
            val node = classNode(owner)?.methods?.firstOrNull { it.name == name && it.desc == descriptor }
                ?: return null
            val forwardTarget = facadeForwardTarget(node, name, descriptor) ?: return node
            owner = forwardTarget
        }
        return null
    }

    private fun facadeForwardTarget(method: MethodNode, name: String, descriptor: String): String? {
        val real = mutableListOf<AbstractInsnNode>()
        var node: AbstractInsnNode? = method.instructions.first
        while (node != null) {
            if (node.opcode >= 0) real += node
            node = node.next
        }
        val invokeIndex = real.indexOfFirst { it.opcode == Opcodes.INVOKESTATIC }
        if (invokeIndex < 0 || invokeIndex != real.size - 2) return null
        if (!(0 until invokeIndex).all { isLoadOpcode(real[it].opcode) }) return null
        if (!isReturnOpcode(real.last().opcode)) return null
        val invoke = real[invokeIndex] as? MethodInsnNode ?: return null
        if (invoke.name != name || invoke.desc != descriptor) return null
        return invoke.owner
    }

    private fun isLoadOpcode(opcode: Int): Boolean = opcode in Opcodes.ILOAD..Opcodes.ALOAD
    private fun isReturnOpcode(opcode: Int): Boolean = opcode in Opcodes.IRETURN..Opcodes.RETURN

    private fun extractDefaultExpression(
        method: MethodNode,
        maskSlot: Int,
        paramIndex: Int,
        paramSlot: Int,
    ): List<AbstractInsnNode>? {
        val bit = 1 shl paramIndex
        var node: AbstractInsnNode? = method.instructions.first
        while (node != null) {
            if (node.opcode == Opcodes.ILOAD && (node as VarInsnNode).`var` == maskSlot) {
                val bitNode = node.next
                if (bitNode != null && intConstantValue(bitNode) == bit) {
                    val andNode = bitNode.next
                    if (andNode != null && andNode.opcode == Opcodes.IAND) {
                        val ifeqNode = andNode.next
                        if (ifeqNode is JumpInsnNode && ifeqNode.opcode == Opcodes.IFEQ) {
                            val collected = mutableListOf<AbstractInsnNode>()
                            var cur: AbstractInsnNode? = ifeqNode.next
                            while (cur != null && !isStoreOpcode(cur.opcode)) {
                                if (cur.opcode >= 0) collected += cur
                                cur = cur.next
                            }
                            if (cur != null && (cur as VarInsnNode).`var` == paramSlot) {
                                return collected
                            }

                        }
                    }
                }
            }
            node = node.next
        }
        return null
    }

    private fun isStoreOpcode(opcode: Int): Boolean = opcode in ISTORE..ASTORE

    private fun intConstantValue(insn: AbstractInsnNode): Int? = when (insn.opcode) {
        Opcodes.ICONST_M1 -> -1
        Opcodes.ICONST_0 -> 0
        Opcodes.ICONST_1 -> 1
        Opcodes.ICONST_2 -> 2
        Opcodes.ICONST_3 -> 3
        Opcodes.ICONST_4 -> 4
        Opcodes.ICONST_5 -> 5
        Opcodes.BIPUSH, Opcodes.SIPUSH -> (insn as IntInsnNode).operand
        Opcodes.LDC -> (insn as LdcInsnNode).cst as? Int
        else -> null
    }

    private companion object {

        val ISTORE = Opcodes.ISTORE
        val ASTORE = Opcodes.ASTORE

        const val FACADE_FORWARD_LIMIT = 5
    }
}