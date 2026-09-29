package com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers

import com.ukenoveldiiyar.rcc.compiler.plugin.fir.findOrNullParentEntryPoint
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess

object FirRccCallableReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCallableReferenceAccess) {
        findOrNullParentEntryPoint() ?: return

        reporter.reportOn(
            expression.source,
            FirRccErrors.RCC_UNSUPPORTED_CALLABLE_REFERENCE,
            expression.calleeReference.name
        )
    }
}