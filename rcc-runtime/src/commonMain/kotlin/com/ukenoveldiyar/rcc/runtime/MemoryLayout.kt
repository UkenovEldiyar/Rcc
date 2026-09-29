package com.ukenoveldiyar.rcc.runtime

class MemoryLayout internal constructor(
    @JvmField internal val hostComponent: HostComponent,
    @JvmField internal val functions: Array<out Function>,
    @JvmField internal val constantPool: ConstantPool,
)