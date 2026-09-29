package com.ukenoveldiyar.rcc.runtime.external

import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalDeclaration
import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.runtime.external.names.FqName
import com.ukenoveldiyar.rcc.runtime.external.names.Name
import com.ukenoveldiyar.rcc.runtime.external.names.name

abstract class ExternalContainer {
    abstract operator fun get(key: ContainerId): ExternalDeclaration
}

abstract class ContainerId {
    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    abstract override fun toString(): String
}

fun externalContainer(
    block: ExternalContainerBuilder.() -> Unit
): ExternalContainer = ExternalContainerImpl(
    initContainer = {
        ExternalContainerBuilder()
            .apply(block)
            .build()
    }
)

internal class ExternalContainerImpl(
    initContainer: () -> Map<ContainerId, ExternalDeclaration>
) : ExternalContainer() {

    val container by lazy(initContainer)

    override fun get(key: ContainerId): ExternalDeclaration =
        container[key] ?: error("Not found external component by key: $key")
}

internal data class ContainerIdImpl(
    val fqName: FqName,
    val name: Name,
) : ContainerId()

class ExternalContainerBuilder internal constructor() {

    private val container = mutableMapOf<ContainerId, ExternalDeclaration>()

    fun bind(declaration: ExternalFile) {
        container[declaration.containerId()] = declaration
    }

    fun bind(declaration: ExternalClass) {
        container[declaration.containerId()] = declaration
    }

    internal fun build(): Map<ContainerId, ExternalDeclaration> = container.toMap()
}

private fun ExternalFile.containerId(): ContainerId = ContainerIdImpl(
    fqName = packageFqName,
    name = name(name),
)

private fun ExternalClass.containerId(): ContainerId = ContainerIdImpl(
    fqName = classId.packageFqName,
    name = name(classId.className),
)
