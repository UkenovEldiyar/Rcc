package com.ukenoveldiiyar.rcc.compiler.plugin.ir

import com.ukenoveldiiyar.rcc.compiler.plugin.ir.binding.RccReachabilityIndex
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class RccIrGenerationExtension(
    private val reachability: RccReachabilityIndex,
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        moduleFragment.transformChildrenVoid(HoistedComposableLambdaInliner())

        val roots = mutableListOf<IrFunction>()

        moduleFragment.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                if (declaration.hasAnnotationEntryPoint()) roots += declaration
                super.visitFunction(declaration)
            }
        })

        roots.forEach {
            reachability.registerEntryPoint(it)
        }

        val ownFiles = moduleFragment.files.toHashSet()

        val visited = mutableSetOf<IrFunction>()
        val queue = ArrayDeque(roots)

        while (queue.isNotEmpty()) {
            val function = queue.removeFirst()
            if (!visited.add(function)) continue
            reachability.register(function)

            function.body?.acceptChildrenVoid(
                object : IrVisitorVoid() {
                    override fun visitElement(element: IrElement) {
                        element.acceptChildrenVoid(this)
                    }

                    override fun visitCall(expression: IrCall) {
                        val callee = expression.symbol.owner

                        if (callee.fileOrNull in ownFiles && callee.body != null && callee !in visited) {
                            queue += callee
                        }

                        super.visitCall(expression)
                    }
                }
            )
        }
    }
}

private class HoistedComposableLambdaInliner : IrElementTransformerVoidWithContext() {
    override fun visitCall(expression: IrCall): IrExpression {
        val getter = expression.symbol.owner
        val property = getter.correspondingPropertySymbol?.owner
        val field = property?.backingField
        val parentClass = field?.parent as? IrClass
        val initializer = (field?.initializer as? IrExpressionBody)?.expression
        if (parentClass != null &&
            parentClass.name.asString().startsWith("ComposableSingletons$") &&
            initializer != null
        ) {
            return initializer.deepCopyWithSymbols(currentDeclarationParent)
        }
        return super.visitCall(expression)
    }
}
