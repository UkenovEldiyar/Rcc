package com.ukenoveldiyar.rcc.runtime.lambda

import com.ukenoveldiyar.rcc.runtime.Function
import com.ukenoveldiyar.rcc.runtime.Interpreter
import com.ukenoveldiyar.rcc.runtime.stack.pushUnboxed

internal fun createLambda(
    function: Function,
    capture: Capture,
    interpreter: () -> Interpreter,
): kotlin.Function<Any?> = when (function.userParameterCount) {
    0 -> Lambda0(function, capture, interpreter)
    1 -> Lambda1(function, capture, interpreter)
    2 -> Lambda2(function, capture, interpreter)
    3 -> Lambda3(function, capture, interpreter)
    4 -> Lambda4(function, capture, interpreter)
    5 -> Lambda5(function, capture, interpreter)
    else -> error("rcc: неподдерживаемая арность лямбды: ${function.userParameterCount}")
}

private class Lambda0(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : () -> Any? {
    override fun invoke(): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame

        stackFrame.pushCapture(capture)

        return interpreter.callResult(function)
    }
}

private class Lambda1(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : (Any?) -> Any? {
    override fun invoke(p1: Any?): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame
        val o = function.captureParameterCount

        stackFrame.pushCapture(capture)
        stackFrame.pushUnboxed(p1, function.parameterRaw[o])

        return interpreter.callResult(function)
    }
}

private class Lambda2(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : (Any?, Any?) -> Any? {
    override fun invoke(p1: Any?, p2: Any?): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame
        val o = function.captureParameterCount

        stackFrame.pushCapture(capture)
        stackFrame.pushUnboxed(p1, function.parameterRaw[o])
        stackFrame.pushUnboxed(p2, function.parameterRaw[o + 1])

        return interpreter.callResult(function)
    }
}

private class Lambda3(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : (Any?, Any?, Any?) -> Any? {
    override fun invoke(p1: Any?, p2: Any?, p3: Any?): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame
        val o = function.captureParameterCount

        stackFrame.pushCapture(capture)
        stackFrame.pushUnboxed(p1, function.parameterRaw[o])
        stackFrame.pushUnboxed(p2, function.parameterRaw[o + 1])
        stackFrame.pushUnboxed(p3, function.parameterRaw[o + 2])

        return interpreter.callResult(function)
    }
}

private class Lambda4(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : (Any?, Any?, Any?, Any?) -> Any? {
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame
        val o = function.captureParameterCount

        stackFrame.pushCapture(capture)
        stackFrame.pushUnboxed(p1, function.parameterRaw[o])
        stackFrame.pushUnboxed(p2, function.parameterRaw[o + 1])
        stackFrame.pushUnboxed(p3, function.parameterRaw[o + 2])
        stackFrame.pushUnboxed(p4, function.parameterRaw[o + 3])

        return interpreter.callResult(function)
    }
}

private class Lambda5(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : (Any?, Any?, Any?, Any?, Any?) -> Any? {
    override fun invoke(p1: Any?, p2: Any?, p3: Any?, p4: Any?, p5: Any?): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame
        val o = function.captureParameterCount

        stackFrame.pushCapture(capture)
        stackFrame.pushUnboxed(p1, function.parameterRaw[o])
        stackFrame.pushUnboxed(p2, function.parameterRaw[o + 1])
        stackFrame.pushUnboxed(p3, function.parameterRaw[o + 2])
        stackFrame.pushUnboxed(p4, function.parameterRaw[o + 3])
        stackFrame.pushUnboxed(p5, function.parameterRaw[o + 4])

        return interpreter.callResult(function)
    }
}