package com.ukenoveldiiyar.rcc.compiler.plugin.ir

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

interface EntryPointLoweringPass : FileLoweringPass {
    fun lower(declaration: IrSimpleFunction)

    override fun lower(irFile: IrFile) = runOnFile(irFile)
}

fun EntryPointLoweringPass.runOnFile(irFile: IrFile) {
    irFile.declarations
        .filterIsInstance<IrSimpleFunction>()
        .filter { it.hasAnnotationEntryPoint() }
        .forEach { lower(it) }
}