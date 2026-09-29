package com.ukenoveldiyar.rcc.external.binding

import com.ukenoveldiyar.rcc.external.ExternalElement
import com.ukenoveldiyar.rcc.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.external.declarations.ExternalConstructor
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.external.declarations.ExternalProperty
import com.ukenoveldiyar.rcc.external.types.ExternalFunctionType
import com.ukenoveldiyar.rcc.external.types.ExternalType
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

enum class ResolvedBindKind { FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER }

sealed interface ResolvedBind {
    val fqName: String
    val kind: ResolvedBindKind
    val allParameters: List<String>
    val stableKey: String

    data class Fun(val decl: ExternalFunction, val receiver: String?) : ResolvedBind {
        override val fqName get() = decl.fqName.asString()
        override val kind get() = ResolvedBindKind.FUNCTION
        override val allParameters get() = listOfNotNull(receiver) + decl.parameters.map { it.matchFqName() }
        override val stableKey
            get() = buildStableKey("FUNCTION", fqName, receiver, decl.parameters.map { it.matchFqName() })
    }

    data class Ctor(val decl: ExternalConstructor) : ResolvedBind {
        override val fqName get() = decl.fqName.asString()
        override val kind get() = ResolvedBindKind.FUNCTION
        override val allParameters get() = decl.parameters.map { it.matchFqName() }
        override val stableKey
            get() = buildStableKey("FUNCTION", fqName, null, decl.parameters.map { it.matchFqName() })
    }

    data class Getter(val decl: ExternalProperty, val receiver: String?) : ResolvedBind {
        override val fqName get() = decl.fqName.asString()
        override val kind get() = ResolvedBindKind.PROPERTY_GETTER
        override val allParameters get() = listOfNotNull(receiver)
        override val stableKey
            get() = buildStableKey("PROPERTY_GETTER", fqName, receiver, emptyList())
    }

    data class Setter(val decl: ExternalProperty, val receiver: String?) : ResolvedBind {
        override val fqName get() = decl.fqName.asString()
        override val kind get() = ResolvedBindKind.PROPERTY_SETTER
        override val allParameters get() = listOfNotNull(receiver) + listOf(decl.type.matchFqName())
        override val stableKey
            get() = buildStableKey("PROPERTY_SETTER", fqName, receiver, emptyList())
    }
}

/**
 * Разворачивает дерево деклараций в плоский список того, что реально нужно забиндить — через
 * [ExternalVisitor], а не ручной `when`. `data` (`D`) — "receiver", накопленный по пути: вложенность в
 * [ExternalClass] и есть "receiver" (в старой YAML-модели это было отдельное строковое поле
 * `receiver:`), а [ExternalProperty] с `mutable = true` даёт СРАЗУ getter и setter (в старой модели это
 * были две отдельные bind-записи с `:get`/`:set`).
 */
class BindCollector : ExternalVisitor<List<ResolvedBind>, String?>() {
    override fun visitElement(element: ExternalElement, data: String?): List<ResolvedBind> = emptyList()

    override fun visitFile(declaration: ExternalFile, data: String?): List<ResolvedBind> =
        declaration.declarations.flatMap { it.accept(this, null) }

    override fun visitInterface(declaration: ExternalClass, data: String?): List<ResolvedBind> =
        declaration.declarations.flatMap { it.accept(this, declaration.fqName.asString()) }

    override fun visitFunction(declaration: ExternalFunction, data: String?): List<ResolvedBind> =
        listOf(ResolvedBind.Fun(declaration, data ?: declaration.extension?.asFqNameOrNull()))

    override fun visitConstructor(declaration: ExternalConstructor, data: String?): List<ResolvedBind> =
        listOf(ResolvedBind.Ctor(declaration))

    override fun visitProperty(declaration: ExternalProperty, data: String?): List<ResolvedBind> {
        val effectiveReceiver = data ?: declaration.extension?.asFqNameOrNull()
        return buildList {
            add(ResolvedBind.Getter(declaration, effectiveReceiver))
            if (declaration.mutable) add(ResolvedBind.Setter(declaration, effectiveReceiver))
        }
    }
}

fun ExternalTypeRef.matchFqName(): String = when (this) {
    is ExternalType -> fqName.asString()
    is ExternalFunctionType -> "kotlin.Function${(if (receiver != null) 1 else 0) + parameters.size}"
}

fun ExternalTypeRef.asFqNameOrNull(): String? = (this as? ExternalType)?.fqName?.asString()

fun buildStableKey(kind: String, fqName: String, receiver: String?, parameters: List<String>): String =
    buildString {
        append(kind)
        append(':')
        append(fqName)
        val impliedReceiver = fqName.substringBeforeLast('.', "")
        if (receiver != null && receiver != impliedReceiver) {
            append('@')
            append(receiver)
        }
        append('(')
        append(parameters.joinToString(","))
        append(')')
    }
