package com.ukenoveldiiyar.rcc.compiler.plugin.ir.defaults

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.OpcodeEmitter
import com.ukenoveldiiyar.rcc.compiler.plugin.ir.code.RccInstruction

fun RccInstruction.netStackDelta(index: Int, overrides: Map<Int, Int>): Int {
    val effect = op.stack
    val pushes = effect.pushes
    val pops = effect.pops
    if (pushes != null && pops != null) return pushes - pops
    return overrides[index] ?: error(
        "rcc: netStackDelta($op) по индексу $index — FromCallSite без переданного override " +
            "(RccMethodVisitor обязан посчитать реальный net-эффект при эмите HOST_INVOKE/INVOKE/HOST_LAMBDA)"
    )
}

fun findArgumentStarts(
    emitter: OpcodeEmitter,
    endExclusive: Int,
    argCount: Int,
    overrides: Map<Int, Int>,
): IntArray {
    val starts = IntArray(argCount)
    var idx = endExclusive
    var runningTotal = 0
    var found = 0
    while (found < argCount) {
        idx--
        check(idx >= 0) {
            "rcc: не хватило инструкций при поиске границ аргументов (нужно $argCount, найдено $found)"
        }
        runningTotal += emitter.instructionAt(idx).netStackDelta(idx, overrides)
        if (runningTotal == found + 1) {
            starts[argCount - 1 - found] = idx
            found++
        }
    }
    return starts
}