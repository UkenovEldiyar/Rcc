package com.ukenoveldiiyar.rcc.compiler.plugin.ir

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.HostBindingIndex
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.RccReachabilityIndex
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.codegen.RccMethodVisitor
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults.DefaultArgumentResolver
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.JvmType
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.declFqName
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.parseMethodDescriptor
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.write.RccModuleWriter
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.write.toRawTypeWire
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class RccClassGeneratorExtension(
    private val hostBindingIndex: HostBindingIndex,
    private val reachabilityIndex: RccReachabilityIndex,
    private val moduleWriter: RccModuleWriter,
    private val defaultArgumentResolver: DefaultArgumentResolver,
) : ClassGeneratorExtension {
    override fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator =
        RccDelegatingClassGenerator(
            generator,
            hostBindingIndex,
            reachabilityIndex,
            moduleWriter,
            defaultArgumentResolver
        )
}

private class RccDelegatingClassGenerator(
    private val delegate: ClassGenerator,
    private val hostBindingIndex: HostBindingIndex,
    private val reachabilityIndex: RccReachabilityIndex,
    private val moduleWriter: RccModuleWriter,
    private val defaultArgumentResolver: DefaultArgumentResolver,
) : ClassGenerator by delegate {

    private var ownerInternalName: String = ""

    override fun defineClass(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String,
        interfaces: Array<out String>,
    ) {
        ownerInternalName = name
        delegate.defineClass(version, access, name, signature, superName, interfaces)
    }

    override fun newMethod(
        declaration: IrFunction?,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val mv = delegate.newMethod(declaration, access, name, desc, signature, exceptions)

        val isEntryPoint = declaration?.hasAnnotationEntryPoint() == true
        val isStatic = access and Opcodes.ACC_STATIC != 0
        val reachableId = reachabilityIndex.resolve(declFqName(isStatic, ownerInternalName, name))
        val isReachable = reachableId != null

        if (isEntryPoint || isReachable) reachabilityIndex.markOwnerRelevant(ownerInternalName)

        val isLambdaImpl =
            name.contains("\$lambda\$") && reachabilityIndex.isRelevantOwner(ownerInternalName)
        val lambdaId = if (isLambdaImpl) reachabilityIndex.idForLambdaImpl(
            ownerInternalName,
            name,
            desc
        ) else null

        if (declaration == null || (!isEntryPoint && !isReachable && !isLambdaImpl)) return mv

        val functionId = checkNotNull(reachableId ?: lambdaId) {
            "rcc: не удалось определить id функции для $ownerInternalName.$name$desc " +
                    "(isEntryPoint=$isEntryPoint, isReachable=$isReachable, isLambdaImpl=$isLambdaImpl) — " +
                    "fqName, посчитанный здесь (ASM, после lowering), разошёлся с тем, что посчитал " +
                    "reachability-проход (IR, до lowering)"
        }

        val (paramTypes, returnJvmType) = parseMethodDescriptor(desc)
        val parameterRaw = ByteArray(paramTypes.size) { i -> paramTypes[i].toRawTypeWire() }
        val returnTypeWire = returnJvmType.toRawTypeWire()

        val composerIndex = paramTypes.indexOfFirst {
            it is JvmType.ObjectType && it.internalName == "androidx/compose/runtime/Composer"
        }

        val ownComposerSlot = if (composerIndex >= 0) {
            var slot = 0
            for (i in 0 until composerIndex) {
                slot += if (paramTypes[i] is JvmType.Primitive &&
                    (paramTypes[i] as JvmType.Primitive).descriptor.let { it == 'J' || it == 'D' }
                ) 2 else 1
            }
            slot
        } else {
            null
        }

        return RccMethodVisitor(
            delegate = mv,
            hostBindingIndex = hostBindingIndex,
            reachabilityIndex = reachabilityIndex,
            defaultArgumentResolver = defaultArgumentResolver,
            ownComposerSlot = ownComposerSlot,
            onFinished = { instructions, maxStack, maxLocals ->
                moduleWriter.recordFunction(
                    id = functionId,
                    instructions = instructions,
                    maxStack = maxStack,
                    maxLocals = maxLocals,
                    parameterRaw = parameterRaw,
                    returnType = returnTypeWire,
                )
            },
        )
    }
}
