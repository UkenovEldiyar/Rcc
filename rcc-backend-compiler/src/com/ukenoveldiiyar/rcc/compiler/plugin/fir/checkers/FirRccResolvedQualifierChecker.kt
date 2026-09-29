package com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers

import com.ukenoveldiiyar.rcc.compiler.plugin.fir.findOrNullParentEntryPoint
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirResolvedQualifierChecker
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.ClassId

object FirRccResolvedQualifierChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirResolvedQualifier) {
        findOrNullParentEntryPoint() ?: return

        val reportList = mutableListOf<Pair<KtSourceElement?, FirBasedSymbol<*>>>()

        context(reportList) {
            checkResolvedQualifier(expression)
        }

        reportList.forEach { (source, symbol) ->
            reporter.reportOn(
                source,
                FirRccErrors.RCC_NOT_COMPONENT,
                symbol
            )
        }
    }

    context(
        context: CheckerContext,
        reportList: MutableList<Pair<KtSourceElement?, FirBasedSymbol<*>>>
    )
    fun checkResolvedQualifier(expression: FirResolvedQualifier) {
        val enclosingAccess = context.callsOrAssignments.lastOrNull() as? FirQualifiedAccessExpression

        if (enclosingAccess?.explicitReceiver === expression) return

        val classId = if (expression.resolvedToCompanionObject) {
            expression.accessedObjectSymbol?.classId ?: return
        } else {
            val relativeClassFqName = expression.relativeClassFqName ?: return
            ClassId(expression.packageFqName, relativeClassFqName, false)
        }

        val objectSymbol = context.session.getRegularClassSymbolByClassId(classId) ?: return

        val resolveComponent = context.rccComponents[classId.toString().toRccComponentFqName()]

        if (resolveComponent == null) {
            reportList += expression.source to objectSymbol
        }
    }
}