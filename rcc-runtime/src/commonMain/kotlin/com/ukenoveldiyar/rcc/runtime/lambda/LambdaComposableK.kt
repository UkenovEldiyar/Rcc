package com.ukenoveldiyar.rcc.runtime.lambda

import androidx.compose.runtime.Composer
import com.ukenoveldiyar.rcc.runtime.Function
import com.ukenoveldiyar.rcc.runtime.Interpreter
import com.ukenoveldiyar.rcc.runtime.stack.pushUnboxed

internal fun createComposableLambda(
    function: Function,
    capture: Capture,
    interpreter: () -> Interpreter,
): kotlin.Function<Any?> {
    return when (function.userParameterCount) {
        0 -> ComposableLambda0(function, capture, interpreter)
        1 -> ComposableLambda1(function, capture, interpreter)
        else -> error("rcc: неподдерживаемая арность composable-лямбды: ${function.userParameterCount}")
    }
}

private class ComposableLambda0(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : (Composer, Int) -> Any? {

    override fun invoke(composer: Composer, changed: Int): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame

        stackFrame.pushCapture(capture)
        stackFrame.pushObject(composer)
        stackFrame.pushInt(changed)

        return interpreter.callResult(function)
    }
}

private class ComposableLambda1(
    private val function: Function,
    private val capture: Capture,
    private val newInterpreter: () -> Interpreter,
) : (Any?, Composer, Int) -> Any? {

    override fun invoke(p1: Any?, composer: Composer, changed: Int): Any? {
        val interpreter = newInterpreter()
        val stackFrame = interpreter.stackFrame

        stackFrame.pushCapture(capture)
        stackFrame.pushUnboxed(p1, function.parameterRaw[0])
        stackFrame.pushObject(composer)

        return interpreter.callResult(function)
    }
}