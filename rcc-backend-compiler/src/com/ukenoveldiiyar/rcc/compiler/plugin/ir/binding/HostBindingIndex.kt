package com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding

import com.ukenoveldiiyar.rcc.compiler.plugin.RccCompilerComponentContainer
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.JvmType
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.parseMethodDescriptor
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm.toKotlinFqName
import com.ukenoveldiyar.rcc.external.binding.ResolvedBind
import com.ukenoveldiyar.rcc.external.binding.ResolvedBindKind
import org.jetbrains.org.objectweb.asm.Opcodes

class HostBindingIndex(container: RccCompilerComponentContainer) {
    private val byFqName: Map<String, List<ResolvedBind>> = container.components

    fun resolve(opcode: Int, owner: String, name: String, descriptor: String): String? {
        val candidate = resolveCandidate(opcode, owner, name, descriptor) ?: return null
        return candidate.stableKey
    }

    fun hasReceiver(opcode: Int, owner: String, name: String, descriptor: String): Boolean {
        val candidate = resolveCandidate(opcode, owner, name, descriptor) ?: return false
        return candidate is ResolvedBind.Fun && candidate.receiver != null
    }

    fun resolveFieldGet(owner: String, name: String): String? {

        val ownerFqName = owner.toKotlinFqName()
        val candidates = if (name == "INSTANCE") {
            listOf(ownerFqName, "$ownerFqName.$name")
        } else {
            listOf("$ownerFqName.$name")
        }
        for (fqName in candidates) {
            val candidate = pickCandidate(byFqName[fqName].orEmpty(), ResolvedBindKind.FUNCTION, emptyList())
            if (candidate != null) return candidate.stableKey
        }
        return null
    }

    private fun resolveCandidate(opcode: Int, owner: String, name: String, descriptor: String): ResolvedBind? {
        val (paramTypes, _) = parseMethodDescriptor(descriptor)
        val composerIndex = paramTypes.indexOfFirst {
            it is JvmType.ObjectType && it.internalName == "androidx/compose/runtime/Composer"
        }
        val paramFqNames = (if (composerIndex >= 0) paramTypes.subList(0, composerIndex) else paramTypes)
            .map { it.toKotlinFqName() }
        val ownerFqName = owner.toKotlinFqName()
        val isStatic = opcode == Opcodes.INVOKESTATIC

        if (isStatic) {

            val facadeStripped = ownerFqName.substringBeforeLast('.', missingDelimiterValue = ownerFqName)

            val rawOwner = owner.replace('/', '.').replace('$', '.')
            for (candidateName in demangleCandidates(name)) {

                val memberCandidates = buildList {
                    add(candidateName to ResolvedBindKind.FUNCTION)
                    addAll(classifyMember(candidateName, paramFqNames.size))
                    if (paramFqNames.isNotEmpty()) {
                        addAll(classifyMember(candidateName, paramFqNames.size - 1))
                    }
                }.distinct()
                for ((memberName, kind) in memberCandidates) {
                    for (ownerCandidate in listOf(facadeStripped, ownerFqName, rawOwner).distinct()) {
                        val fqName = "$ownerCandidate.$memberName"
                        val candidate = pickCandidate(byFqName[fqName].orEmpty(), kind, paramFqNames)
                        if (candidate != null) return candidate
                    }
                }
            }
            return null
        }

        for (candidateName in demangleCandidates(name)) {

            for ((memberName, kind) in classifyMember(candidateName, paramFqNames.size)) {
                val fqName = "$ownerFqName.$memberName"

                val matchParams = listOf(ownerFqName) + paramFqNames
                val candidate = pickCandidate(byFqName[fqName].orEmpty(), kind, matchParams)
                if (candidate != null) return candidate
            }
        }
        return null
    }

    private fun demangleCandidates(name: String): List<String> {
        val dash = name.indexOf('-')
        if (dash <= 0) return listOf(name)
        return listOf(name.substring(0, dash), name)
    }

    private fun pickCandidate(
        all: List<ResolvedBind>,
        expectedKind: ResolvedBindKind,
        matchParams: List<String>,
    ): ResolvedBind? {
        val candidates = all.filter { it.kind == expectedKind }
        return when (candidates.size) {
            0 -> null
            1 -> candidates.single()
            else -> candidates.firstOrNull { it.allParameters == matchParams }
        }
    }

    private fun classifyMember(jvmName: String, paramCount: Int): List<Pair<String, ResolvedBindKind>> {
        if (paramCount == 0 && jvmName.length > 3 && jvmName.startsWith("get") && jvmName[3].isUpperCase()) {
            val stripped = jvmName.removePrefix("get")
            val lowered = stripped.replaceFirstChar { it.lowercaseChar() }
            return if (lowered == stripped) {
                listOf(stripped to ResolvedBindKind.PROPERTY_GETTER)
            } else {
                listOf(
                    lowered to ResolvedBindKind.PROPERTY_GETTER,
                    stripped to ResolvedBindKind.PROPERTY_GETTER,
                )
            }
        }
        if (paramCount == 0 && jvmName.length > 2 && jvmName.startsWith("is") && jvmName[2].isUpperCase()) {
            return listOf(jvmName to ResolvedBindKind.PROPERTY_GETTER)
        }
        if (paramCount == 1 && jvmName.length > 3 && jvmName.startsWith("set") && jvmName[3].isUpperCase()) {
            val stripped = jvmName.removePrefix("set")
            val lowered = stripped.replaceFirstChar { it.lowercaseChar() }
            return if (lowered == stripped) {
                listOf(stripped to ResolvedBindKind.PROPERTY_SETTER)
            } else {
                listOf(
                    lowered to ResolvedBindKind.PROPERTY_SETTER,
                    stripped to ResolvedBindKind.PROPERTY_SETTER,
                )
            }
        }
        return listOf(jvmName to ResolvedBindKind.FUNCTION)
    }
}
