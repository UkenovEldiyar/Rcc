package com.ukenoveldiyar.rcc.runtime

import com.ukenoveldiyar.rcc.runtime.code.type.Type
import com.ukenoveldiyar.rcc.runtime.code.type.Types

class Function internal constructor(
    @JvmField internal val id: Int,
    @JvmField internal val maxStackSize: Int,
    @JvmField internal val maxLocalSize: Int,
    @JvmField internal val parameterCount: Int,
    @JvmField internal val parameterRaw: Types,
    @JvmField internal val captureParameterCount: Int,
    @JvmField internal val userParameterCount: Int,
    @JvmField internal val returnType: Type,
    @JvmField internal val instruction: Instruction,
) {
    @JvmField
    internal val hasCapture: Boolean = captureParameterCount != 0
}