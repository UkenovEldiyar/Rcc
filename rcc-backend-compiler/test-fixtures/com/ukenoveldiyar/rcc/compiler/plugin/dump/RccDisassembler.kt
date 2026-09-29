package com.ukenoveldiyar.rcc.compiler.plugin.dump

internal object RccDisassembler {
    fun disassemble(module: DumpModule): String = buildString {
        append("entryPoints:\n")
        for ((fqName, functionId) in module.entryPoints.toSortedMap()) {
            append("  ").append(fqName).append(" -> #").append(functionId).append('\n')
        }
    }
}
