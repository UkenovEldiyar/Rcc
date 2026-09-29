package com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers

import com.ukenoveldiiyar.rcc.compiler.plugin.fir.findOrNullParentEntryPoint
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirTypeOperatorCallChecker
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall

object FirRccTypeOperatorCallChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirTypeOperatorCall) {
        findOrNullParentEntryPoint() ?: return

        if (
            expression.operation == FirOperation.AS ||
            expression.operation == FirOperation.IS ||
            expression.operation == FirOperation.NOT_IS ||
            expression.operation == FirOperation.SAFE_AS
        ) {
            reporter.reportOn(
                expression.source,
                FirRccErrors.RCC_UNSUPPORTED_TYPE_OPERATOR,
                expression.operation,
            )
        }

    }
}