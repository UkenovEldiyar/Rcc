package com.ukenoveldiyar.rcc.runtime

class ConstantPool internal constructor(
    @JvmField internal val descriptors: Array<out String>,
) {
    internal val size: Int get() = descriptors.size
    internal operator fun get(index: Int): String = descriptors[index]
}