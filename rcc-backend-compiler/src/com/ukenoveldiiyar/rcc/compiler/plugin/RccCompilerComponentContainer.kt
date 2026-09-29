package com.ukenoveldiiyar.rcc.compiler.plugin

import com.ukenoveldiyar.rcc.external.binding.BindCollector
import com.ukenoveldiyar.rcc.external.binding.ResolvedBind
import com.ukenoveldiyar.rcc.external.config.ExternalConfigFileLoader
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile

class RccCompilerComponentContainer(
    val components: Map<String, List<ResolvedBind>>
) {
    companion object {

        fun load(paths: List<String>): RccCompilerComponentContainer {
            val components = mutableMapOf<String, MutableList<ResolvedBind>>()

            ExternalConfigFileLoader.loadAll(paths)
                .map {
                    it.element as? ExternalFile ?: error("rcc: expected a file declaration at the root of a component descriptor, was ${it.element}")
                }
                .flatMap { it.accept(BindCollector(), null) }
                .forEach { bind ->
                    components.getOrPut(bind.fqName) { mutableListOf() } += bind
                }

            return RccCompilerComponentContainer(components)
        }
    }
}
