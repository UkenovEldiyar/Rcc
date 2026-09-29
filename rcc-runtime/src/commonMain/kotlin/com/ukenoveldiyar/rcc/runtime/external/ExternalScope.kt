package com.ukenoveldiyar.rcc.runtime.external

import com.ukenoveldiyar.rcc.runtime.stack.StackFrame
import com.ukenoveldiyar.rcc.runtime.stack.popBoolean
import com.ukenoveldiyar.rcc.runtime.stack.popByte
import com.ukenoveldiyar.rcc.runtime.stack.popChar
import com.ukenoveldiyar.rcc.runtime.stack.popShort
import com.ukenoveldiyar.rcc.runtime.stack.pushBoolean
import com.ukenoveldiyar.rcc.runtime.stack.pushByte
import com.ukenoveldiyar.rcc.runtime.stack.pushChar
import com.ukenoveldiyar.rcc.runtime.stack.pushShort

interface ExternalScope {
    fun popBoolean(): Boolean

    fun popByte(): Byte

    fun popChar(): Char

    fun popShort(): Short

    fun popInt(): Int

    fun popLong(): Long

    fun popFloat(): Float

    fun popDouble(): Double

    fun popObject(): Any?

    fun push(value: Boolean)

    fun push(value: Byte)

    fun push(value: Char)

    fun push(value: Short)

    fun push(value: Int)

    fun push(value: Long)

    fun push(value: Float)

    fun push(value: Double)

    fun push(value: Any?)
}

@Suppress("UNCHECKED_CAST")
fun <T> ExternalScope.popObject(): T = popObject() as T

internal class ExternalScopeImpl(
    private val stackFrame: StackFrame,
) : ExternalScope {
    override fun popBoolean(): Boolean = stackFrame.popBoolean()
    override fun popByte(): Byte = stackFrame.popByte()
    override fun popChar(): Char = stackFrame.popChar()
    override fun popShort(): Short = stackFrame.popShort()
    override fun popInt(): Int = stackFrame.popInt()
    override fun popLong(): Long = stackFrame.popLong()
    override fun popFloat(): Float = stackFrame.popFloat()
    override fun popDouble(): Double = stackFrame.popDouble()
    override fun popObject(): Any? = stackFrame.popObject()

    override fun push(value: Boolean) = stackFrame.pushBoolean(value)
    override fun push(value: Byte) = stackFrame.pushByte(value)
    override fun push(value: Char) = stackFrame.pushChar(value)
    override fun push(value: Short) = stackFrame.pushShort(value)
    override fun push(value: Int) = stackFrame.pushInt(value)
    override fun push(value: Long) = stackFrame.pushLong(value)
    override fun push(value: Float) = stackFrame.pushFloat(value)
    override fun push(value: Double) = stackFrame.pushDouble(value)
    override fun push(value: Any?) = stackFrame.pushObject(value)
}