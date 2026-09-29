package com.ukenoveldiiyar.rcc.compiler.plugin.ir

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

interface SimpleFunctionLoweringPass : FileLoweringPass {
    fun lower(irSimpleFunction: IrSimpleFunction)

    override fun lower(irFile: IrFile) = runOnFilePostfix(irFile)
}

private fun SimpleFunctionLoweringPass.runOnFilePostfix(
    irFile: IrFile
) {
    irFile.acceptVoid(
        object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction) {
                declaration.acceptChildrenVoid(this)
                lower(declaration)
            }
        }
    )
}
