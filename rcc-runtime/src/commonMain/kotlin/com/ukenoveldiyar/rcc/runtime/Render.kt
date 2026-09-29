package com.ukenoveldiyar.rcc.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import com.ukenoveldiyar.rcc.runtime.stack.pushUnboxed

@Composable
fun Render(
    fqName: String,
    program: ModuleProgram,
    component: HostComponent,
    args: Array<out Any?> = emptyArray()
) {
    val entryPointFunction = program.getEntryPoint(fqName)

    val interpreter = remember(component, program) {
        Interpreter(MemoryLayout(component, program.function, program.constantPool))
    }

    for (i in args.indices) {
        interpreter.stackFrame.pushUnboxed(args[i], entryPointFunction.parameterRaw[i])
    }

    interpreter.stackFrame.pushObject(currentComposer)

    for (i in args.size + 1 until entryPointFunction.parameterCount) {
        interpreter.stackFrame.pushUnboxed(0, entryPointFunction.parameterRaw[i])
    }

    interpreter.call(entryPointFunction)
}

fun callEntryPoint(
    fqName: String,
    program: ModuleProgram,
    component: HostComponent,
    args: Array<out Any?> = emptyArray(),
): Any? {
    val entryPointFunction = program.getEntryPoint(fqName)
    val memoryLayout = MemoryLayout(component, program.function, program.constantPool)
    val interpreter = Interpreter(memoryLayout)

    for (i in args.indices) {
        interpreter.stackFrame.pushUnboxed(args[i], entryPointFunction.parameterRaw[i])
    }

    return interpreter.callResult(entryPointFunction)
}

@Composable
fun callComposableEntryPoint(
    fqName: String,
    program: ModuleProgram,
    component: HostComponent,
    args: Array<out Any?> = emptyArray(),
): Any? {
    val entryPointFunction = program.getEntryPoint(fqName)

    val interpreter = remember(component, program) {
        Interpreter(MemoryLayout(component, program.function, program.constantPool))
    }

    for (i in args.indices) {
        interpreter.stackFrame.pushUnboxed(args[i], entryPointFunction.parameterRaw[i])
    }

    interpreter.stackFrame.pushObject(currentComposer)

    for (i in args.size + 1 until entryPointFunction.parameterCount) {
        interpreter.stackFrame.pushUnboxed(0, entryPointFunction.parameterRaw[i])
    }

    return interpreter.callResult(entryPointFunction)
}
