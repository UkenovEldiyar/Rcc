@file:Suppress("NOTHING_TO_INLINE")

package com.ukenoveldiyar.rcc.runtime.stack

import com.ukenoveldiyar.rcc.runtime.RccStackOverflowError
import com.ukenoveldiyar.rcc.runtime.code.type.Type
import com.ukenoveldiyar.rcc.runtime.code.RawTypeKind.DOUBLE
import com.ukenoveldiyar.rcc.runtime.code.RawTypeKind.FLOAT
import com.ukenoveldiyar.rcc.runtime.code.RawTypeKind.INT
import com.ukenoveldiyar.rcc.runtime.code.RawTypeKind.LONG
import com.ukenoveldiyar.rcc.runtime.code.RawTypeKind.OBJECT
import com.ukenoveldiyar.rcc.runtime.code.RawTypeKind.UNIT

internal class StackFrame(
    initialSize: Int = 64
) {
    @JvmField var slots = LongArray(initialSize)

    @JvmField var slotsObject = Array<Any?>(initialSize) { null }

    val size get() = slots.size

    @JvmField var ip: Int = 0

    @JvmField var fp: Int = 0

    @JvmField var bp: Int = 0

    @JvmField var sp: Int = 0

    @JvmField var functionId: Int = -1

    inline fun pushNull() { slotsObject[sp++] = null }
    inline fun pushRaw(raw: Long) = pushLong(raw)
    inline fun pushInt(value: Int) { slots[sp++] = value.toLong() }
    inline fun pushLong(value: Long) { slots[sp++] = value }
    inline fun pushFloat(value: Float) { slots[sp++] = value.toRawBits().toLong() }
    inline fun pushDouble(value: Double) { slots[sp++] = value.toRawBits() }
    inline fun pushObject(value: Any?) { slotsObject[sp++] = value }

    inline fun popRaw(): Long = popLong()
    inline fun popInt(): Int = slots[--sp].toInt()
    inline fun popLong(): Long = slots[--sp]
    inline fun popFloat(): Float = Float.fromBits(slots[--sp].toInt())
    inline fun popDouble(): Double = Double.fromBits(slots[--sp])
    inline fun popObject(): Any? {
        val value = slotsObject[--sp]
        slotsObject[sp] = null
        return value
    }

    inline fun drop() { slotsObject[--sp] = null }

    inline fun loadRaw(localIndex: Int): Unit = pushRaw(getLocalRaw(localIndex))
    inline fun loadObject(localIndex: Int): Unit = pushObject(getLocalObject(localIndex))

    inline fun storeRaw(localIndex: Int): Unit = setLocalRaw(localIndex, popRaw())
    inline fun storeObject(localIndex: Int): Unit = setLocalObject(localIndex, popObject())

    inline fun pushFrame(
        parameterCount: Int,
        maxStackSize: Int,
        maxLocalSize: Int,
        returnSite: Long,
    ) {
        val calleeFp = sp - parameterCount
        val calleeLocalStart = sp
        val calleeLocalEnd = calleeFp + maxLocalSize
        val calleeBp = calleeLocalEnd + HEADER_SIZE
        val requiredSize = calleeBp + maxStackSize

        ensureCapacity(requiredSize)

        slots.fill(EMPTY_SLOT, calleeLocalStart, calleeLocalEnd)

        slots[calleeBp + HEADER_CALLER_FRAME_POINTER] = fp.toLong()
        slots[calleeBp + HEADER_CALLER_BASE_POINTER] = bp.toLong()
        slots[calleeBp + HEADER_RETURN_SITE] = returnSite

        fp = calleeFp
        bp = calleeBp
        sp = calleeBp
    }

    inline fun popFrame(): Long {
        val calleeFp = fp
        val callerFp = slots[bp + HEADER_CALLER_FRAME_POINTER].toInt()
        val callerBp = slots[bp + HEADER_CALLER_BASE_POINTER].toInt()
        val returnSite = slots[bp + HEADER_RETURN_SITE]

        slotsObject.fill(null, calleeFp, sp)

        fp = callerFp
        bp = callerBp
        sp = calleeFp

        return returnSite
    }

    inline fun incInt(localIndex: Int, delta: Int) {
        slots[fp + localIndex] = (slots[fp + localIndex].toInt() + delta).toLong()
    }

    inline fun dup() {
        slots[sp] = slots[sp - 1]
        slotsObject[sp] = slotsObject[sp - 1]
        sp++
    }

    inline fun dupX1() {
        val slot1 = sp - 1
        val slot2 = sp - 2

        val value1 = slots[slot1]
        val object1 = slotsObject[slot1]

        slots[sp] = value1
        slotsObject[sp] = object1

        slots[slot1] = slots[slot2]
        slotsObject[slot1] = slotsObject[slot2]

        slots[slot2] = value1
        slotsObject[slot2] = object1

        sp++
    }

    inline fun dupX2() {
        val slot1 = sp - 1
        val slot2 = sp - 2
        val slot3 = sp - 3

        val value1 = slots[slot1]
        val object1 = slotsObject[slot1]

        slots[sp] = value1
        slotsObject[sp] = object1

        slots[slot1] = slots[slot2]
        slotsObject[slot1] = slotsObject[slot2]

        slots[slot2] = slots[slot3]
        slotsObject[slot2] = slotsObject[slot3]

        slots[slot3] = value1
        slotsObject[slot3] = object1

        sp++
    }

    inline fun swap() {
        val slot1 = sp - 1
        val slot2 = sp - 2

        val value1 = slots[slot1]
        val object1 = slotsObject[slot1]

        slots[slot1] = slots[slot2]
        slotsObject[slot1] = slotsObject[slot2]

        slots[slot2] = value1
        slotsObject[slot2] = object1
    }

    private fun ensureCapacity(required: Int) {
        if (required > MAX_STACK_SIZE) throw RccStackOverflowError()
        if (required <= slots.size) return

        var size = slots.size
        while (size < required) size = size shl 1

        slots = slots.copyOf(size)
        slotsObject = slotsObject.copyOf(size)
    }

    private inline fun getLocalRaw(localIndex: Int): Long = slots[fp + localIndex]
    private inline fun getLocalObject(localIndex: Int): Any? = slotsObject[fp + localIndex]

    private inline fun setLocalRaw(localIndex: Int, value: Long) { slots[fp + localIndex] = value }
    private inline fun setLocalObject(localIndex: Int, value: Any?) { slotsObject[fp + localIndex] = value }

    override fun toString(): String = """
        StackFrame(
            ip=$ip
            fp=$fp
            bp=$bp
            sp=$sp
            size=$size
            functionId=$functionId
            slotsLive=${slots.copyOfRange(0, sp).contentToString()}
            objectsLive=${slotsObject.copyOfRange(0, sp).contentToString()}
        )
    """.trimIndent()
}

internal inline fun StackFrame.pushBoolean(value: Boolean): Unit = pushInt(if (value) 1 else 0)
internal inline fun StackFrame.pushByte(value: Byte): Unit = pushInt(value.toInt())
internal inline fun StackFrame.pushShort(value: Short): Unit = pushInt(value.toInt())
internal inline fun StackFrame.pushChar(value: Char): Unit = pushInt(value.code)

internal inline fun StackFrame.popBoolean(): Boolean = popInt() != 0
internal inline fun StackFrame.popByte(): Byte = popInt().toByte()
internal inline fun StackFrame.popShort(): Short = popInt().toShort()
internal inline fun StackFrame.popChar(): Char = popInt().toChar()

internal fun StackFrame.popBoxed(type: Type): Any? = when (type) {
    UNIT -> Unit
    INT -> popInt()
    LONG -> popLong()
    FLOAT -> popFloat()
    DOUBLE -> popDouble()
    OBJECT -> popObject()
    else -> error(TODO())
}

internal fun StackFrame.pushUnboxed(value: Any?, type: Type): Unit = when (type) {
    INT -> pushInt(value as Int)
    LONG -> pushLong(value as Long)
    FLOAT -> pushFloat(value as Float)
    DOUBLE -> pushDouble(value as Double)
    OBJECT -> pushObject(value)
    else -> error(TODO())
}

//internal inline fun StackFrame.popBoxed(type: StackValueType): Any? = when (type) {
//    StackValueTypeTable.INT -> popInt()
//    StackValueTypeTable.LONG -> popLong()
//    StackValueTypeTable.FLOAT -> popFloat()
//    StackValueTypeTable.DOUBLE -> popDouble()
//    StackValueTypeTable.REFERENCE -> error("Not yet supported")
//    StackValueTypeTable.REFERENCE_EXTERNAL -> popObject()
//    else -> error("Unknown stack value type: $type")
//}
//
//internal inline fun StackFrame.pushUnboxed(value: Any?, type: StackValueType): Unit = when (type) {
//    StackValueTypeTable.INT -> pushInt(value as Int)
//    StackValueTypeTable.LONG -> pushLong(value as Long)
//    StackValueTypeTable.FLOAT -> pushFloat(value as Float)
//    StackValueTypeTable.DOUBLE -> pushDouble(value as Double)
//    StackValueTypeTable.REFERENCE -> error("Not yet supported")
//    StackValueTypeTable.REFERENCE_EXTERNAL -> pushObject(value)
//    else -> error("Unknown stack value type: $type")
//}

internal const val RETURN_SITE_EXIT: Long = -1L

private const val EMPTY_SLOT: Long = 0L
private const val MAX_STACK_SIZE = 2048

private const val HEADER_SIZE = 3
private const val HEADER_CALLER_FRAME_POINTER = -3
private const val HEADER_CALLER_BASE_POINTER = -2
private const val HEADER_RETURN_SITE = -1

internal inline fun packCodeSite(
    id: Int,
    ip: Int
): Long = (id.toLong() shl 32) or (ip.toLong() and 0xFFFFFFFFL)

internal inline fun unpackId(returnSite: Long): Int = (returnSite ushr 32).toInt()
internal inline fun unpackIp(returnSite: Long): Int = returnSite.toInt()





