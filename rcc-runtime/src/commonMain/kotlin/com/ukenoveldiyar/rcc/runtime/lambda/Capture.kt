package com.ukenoveldiyar.rcc.runtime.lambda

import com.ukenoveldiyar.rcc.runtime.stack.StackFrame

internal class Capture(
    @JvmField val slots: LongArray,
    @JvmField val objects: Array<Any?>,
) {
    val size get() = objects.size

    companion object {
        @JvmField
        val EMPTY = Capture(LongArray(0), emptyArray())
    }
}

internal fun StackFrame.pushCapture(capture: Capture) {
    val toSlot = capture.size

    capture.slots.copyInto(slots, sp, 0, toSlot)
    capture.objects.copyInto(slotsObject, sp, 0, toSlot)

    sp += toSlot
}

internal fun StackFrame.popCapture(count: Int): Capture {
    val fromSlot = sp - count

    val result = Capture(
        slots = slots.copyOfRange(fromSlot, sp),
        objects = slotsObject.copyOfRange(fromSlot, sp),
    )

    slotsObject.fill(null, fromSlot, sp)
    sp = fromSlot

    return result
}