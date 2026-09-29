package com.ukenoveldiiyar.rcc.compiler.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import kotlin.getValue

object FirRccErrors : KtDiagnosticsContainer() {

    val RCC_NOT_COMPONENT by error1<KtExpression, FirBasedSymbol<*>>(
        SourceElementPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED
    )

    val RCC_UNSUPPORTED_TYPE_OPERATOR by error1<KtExpression, FirOperation>(
        SourceElementPositioningStrategies.OPERATOR
    )

    val RCC_UNSUPPORTED_CALLABLE_REFERENCE by error1<KtExpression, Name>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = RccDefaultErrorMessage
}

private val RCC_SYMBOL_RENDERER = Renderer<FirBasedSymbol<*>> { symbol ->
    when (symbol) {
        is FirClassLikeSymbol<*> -> symbol.classId.asString()
        is FirCallableSymbol<*> -> symbol.callableId.toString()
        else -> symbol.toString()
    }
}

object RccDefaultErrorMessage : BaseDiagnosticRendererFactory() {

    override val MAP by KtDiagnosticFactoryToRendererMap(
        name = "RccBackendCompiler"
    ) { map ->
        map.put(
            FirRccErrors.RCC_NOT_COMPONENT,
            "Rcc: ''{0}'' is missing from the components.",
            RCC_SYMBOL_RENDERER,
        )

        map.put(
            FirRccErrors.RCC_UNSUPPORTED_TYPE_OPERATOR,
            "Rcc: Operator ''{0}'' is not supported in ''@RccEntryPoint'' functions at the moment.",
            Renderer { it.operator },
        )

        map.put(
            FirRccErrors.RCC_UNSUPPORTED_CALLABLE_REFERENCE,
            "Rcc: Callable reference to ''{0}'' is not allowed in ''@RccEntryPoint'' functions.",
            CommonRenderers.NAME,
        )
    }
}
