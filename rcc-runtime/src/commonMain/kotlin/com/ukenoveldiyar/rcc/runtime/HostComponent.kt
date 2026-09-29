package com.ukenoveldiyar.rcc.runtime

import androidx.compose.runtime.Composable
import com.ukenoveldiyar.rcc.runtime.external.ExternalScope

class HostComponent internal constructor(
    private val elementData: ArrayList<HostModule>
) {
    fun module(module: HostModule) {
        elementData.add(module)
    }

    internal fun resolve(name: String): Any? {
        for (module in elementData) {
            module.byName[name]?.let { return it }
        }
        return null
    }
}

internal class HostModuleContents(
    val byName: Map<String, Any>,
)

class HostModule internal constructor(
    @JvmField internal val name: String,
    init: () -> HostModuleContents
) {
    private val contents by lazy(init)

    internal val byName: Map<String, Any> get() = contents.byName
}

class HostModuleBuilder internal constructor(
    private val moduleName: String,
    size: Int,
) {
    private val byName = HashMap<String, Any>(size)

    internal fun bind(name: String, value: Any) {
        check(byName.putIfAbsent(name, value) == null) {
            "rcc: биндинг с именем \"$name\" уже зарегистрирован в модуле \"$moduleName\""
        }
    }

    internal fun build(): HostModuleContents = HostModuleContents(byName)
}

fun HostModuleBuilder.bindNamed(name: String, block: HostInvoke) {
    bind(name, block)
}

internal class ComposableBinding(
    @JvmField val block: HostComposableInvoke,
)

fun HostModuleBuilder.bindComposableNamed(name: String, block: HostComposableInvoke) {
    bind(name, ComposableBinding(block))
}

fun hostComponent(
    size: Int,
    builder: HostComponent.() -> Unit
) = HostComponent(ArrayList(size)).apply(builder)

fun hostModule(
    name: String,
    size: Int,
    builder: HostModuleBuilder.() -> Unit
) = HostModule(
    name = name,
    init = {
        HostModuleBuilder(name, size)
            .apply(builder)
            .build()
    }
)

internal typealias HostInvoke = (ExternalScope) -> Unit
internal typealias HostComposableInvoke = @Composable (ExternalScope) -> Unit
