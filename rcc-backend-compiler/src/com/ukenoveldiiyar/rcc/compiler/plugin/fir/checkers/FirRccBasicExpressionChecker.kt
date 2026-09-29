package com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers

import com.ukenoveldiiyar.rcc.compiler.plugin.fir.findOrNullParentEntryPoint
import com.ukenoveldiiyar.rcc.compiler.plugin.fir.services.ARITHMETIC_OPERATOR_NAMES
import com.ukenoveldiyar.rcc.external.binding.ResolvedBindKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpressionChecker
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.StandardClassIds

object FirRccBasicExpressionChecker : FirBasicExpressionChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirStatement) {
        findOrNullParentEntryPoint() ?: return

        val symbol: FirBasedSymbol<*>? = when (expression) {
            is FirPropertyAccessExpression -> checkPropertySymbol(expression)
            is FirFunctionCall -> checkFunctionCallSymbol(expression)
            else -> null
        }

        if (symbol != null) {
            reporter.reportOn(
                source = expression.source,
                factory = FirRccErrors.RCC_NOT_COMPONENT,
                a = symbol
            )
        }
    }

    context(context: CheckerContext)
    private fun checkPropertySymbol(
        expression: FirPropertyAccessExpression,
    ): FirPropertySymbol? {
        val symbol = expression.calleeReference.toResolvedPropertySymbol() ?: return null

        if (symbol.isLocal) return null

        val hasMatchingComponent = context.rccComponents[symbol.callableId?.toString()?.toRccComponentFqName()]
            .orEmpty()
            .any {
                matchProperty(
                    symbol = symbol,
                    componentKind = it.kind
                )
            }

        return symbol.takeUnless { hasMatchingComponent }
    }

    context(context: CheckerContext)
    private fun checkFunctionCallSymbol(
        expression: FirFunctionCall
    ): FirFunctionSymbol<*>? {
        val symbol = expression.calleeReference.toResolvedCallableSymbol() ?: return null

        return when (symbol) {
            is FirConstructorSymbol -> checkConstructorSymbol(expression)

            is FirNamedFunctionSymbol -> {
                if (symbol.isPrimitiveArithmeticOperator()) return null

                checkNamedFunctionSymbol(expression)
            }

            else -> null
        }
    }

    context(context: CheckerContext)
    private fun checkConstructorSymbol(
        extension: FirFunctionCall,
    ): FirConstructorSymbol? {
        val symbol = extension.calleeReference.toResolvedConstructorSymbol() ?: return null

        val hasMatchingComponent = context.rccComponents[symbol.callableId.toString().toRccComponentFqName()]
            .orEmpty()
            .any {
                matchOverload(
                    symbols = symbol.valueParameterSymbols,
                    componentParameterTypes = it.allParameters
                )
            }

        return symbol.takeUnless { hasMatchingComponent }
    }

    context(context: CheckerContext)
    private fun checkNamedFunctionSymbol(
        expression: FirFunctionCall,
    ): FirNamedFunctionSymbol? {
        val symbol = expression.calleeReference.toResolvedNamedFunctionSymbol() ?: return null

        val hasMatchingComponent = context.rccComponents[symbol.callableId.toString().toRccComponentFqName()]
            .orEmpty()
            .any {
                matchOverload(
                    symbols = symbol.valueParameterSymbols,
                    componentParameterTypes = it.allParameters,
                )
            }

        return symbol.takeUnless { hasMatchingComponent }
    }

    context(sessionHolder: SessionHolder)
    private fun matchOverload(
        symbols: List<FirValueParameterSymbol>,
        componentParameterTypes: List<String>,
    ): Boolean {
        if (symbols.size != componentParameterTypes.size) return false

        return symbols
            .zip(componentParameterTypes)
            .all { (symbol, parameter) ->
                symbol.resolvedReturnType.fullyExpandedType().classId?.toString()?.toRccComponentFqName() == parameter
            }
    }

    private fun matchProperty(
        symbol: FirPropertySymbol,
        componentKind: ResolvedBindKind,
    ): Boolean {
        return when {
            symbol.getterSymbol != null -> componentKind == ResolvedBindKind.PROPERTY_GETTER
            symbol.setterSymbol != null -> componentKind == ResolvedBindKind.PROPERTY_SETTER
            else -> false
        }
    }

    private fun FirNamedFunctionSymbol.isPrimitiveArithmeticOperator(): Boolean {
        val classId = callableId.classId ?: return false

        return classId in StandardClassIds.primitiveTypes && callableId.callableName in ARITHMETIC_OPERATOR_NAMES
    }
}