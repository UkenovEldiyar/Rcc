package com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment

class RccReachabilityIndex {
    private val idByKey = mutableMapOf<String, Int>()
    private var nextId = 0

    private val relevantOwners = mutableSetOf<String>()

    val totalIds: Int get() = nextId

    val entryPoints: Map<String, Int>
        field = mutableMapOf<String, Int>()

    fun register(function: IrFunction) {
        val fqName = function.rccFqName() ?: return
        idByKey.getOrPut(fqName) { nextId++ }
    }

    fun registerEntryPoint(function: IrFunction) {
        val fqName = function.rccFqName() ?: return
        val id = idByKey.getOrPut(fqName) { nextId++ }
        entryPoints[fqName] = id
    }

    fun resolve(fqName: String): Int? = idByKey[fqName]

    fun markOwnerRelevant(ownerInternalName: String) {
        relevantOwners += ownerInternalName
    }

    fun isRelevantOwner(ownerInternalName: String): Boolean = ownerInternalName in relevantOwners

    fun idForLambdaImpl(owner: String, name: String, descriptor: String): Int =
        idByKey.getOrPut("$owner.$name$descriptor") { nextId++ }

    private val captureCountById = mutableMapOf<Int, Int>()

    fun noteLambdaCaptureCount(id: Int, count: Int) {
        captureCountById[id] = count
    }

    fun captureCountOf(id: Int): Int = captureCountById[id] ?: 0

    private fun IrFunction.rccFqName(): String? = when (val p = parent) {
        is IrPackageFragment -> "${p.packageFqName.asString()}.${name.asString()}"
        else -> null
    }
}
