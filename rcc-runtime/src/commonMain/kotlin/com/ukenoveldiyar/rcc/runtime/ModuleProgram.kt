package com.ukenoveldiyar.rcc.runtime

class ModuleProgram internal constructor(
    internal val entryPointFunction: HashMap<String, Function>,
    internal val function: Array<out Function>,
    internal val constantPool: ConstantPool,
) {
    fun getEntryPoint(name: String): Function {
        return entryPointFunction[name]!!
    }
}