package com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallableReferenceAccessChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirResolvedQualifierChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirTypeOperatorCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

internal class RccFirCheckersExtension(
    session: FirSession,
) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {

        override val resolvedQualifierCheckers: Set<FirResolvedQualifierChecker> =
            setOf(FirRccResolvedQualifierChecker)

        override val basicExpressionCheckers =
            setOf(FirRccBasicExpressionChecker)

        override val typeOperatorCallCheckers: Set<FirTypeOperatorCallChecker> =
            setOf(FirRccTypeOperatorCallChecker)

        override val callableReferenceAccessCheckers: Set<FirCallableReferenceAccessChecker> =
            setOf(FirRccCallableReferenceChecker)
    }
}
