package com.ukenoveldiyar.rcc.runtime.code

import com.ukenoveldiyar.rcc.runtime.code.type.StackValueTypes
import com.ukenoveldiyar.rcc.runtime.code.type.Types

class FunctionAttribute(
    @JvmField val maxStack: Short,
    @JvmField val maxLocals: Short,
    @JvmField val parameterType: StackValueTypes,
    @JvmField val returnType: Types,
    @JvmField val code: Opcodes,
){
    val parameterCount = parameterType.size
}