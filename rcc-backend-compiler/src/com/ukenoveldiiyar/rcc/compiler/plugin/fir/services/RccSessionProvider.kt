package com.ukenoveldiiyar.rcc.compiler.plugin.fir.services

import com.ukenoveldiiyar.rcc.compiler.plugin.RccCompilerComponentContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent

class RccSessionProvider(
    session: FirSession,
    val componentContainer: RccCompilerComponentContainer
) : FirExtensionSessionComponent(session) {

    companion object {
        operator fun invoke(
            componentContainer: RccCompilerComponentContainer
        ): Factory {
            return Factory {
                RccSessionProvider(it, componentContainer)
            }
        }
    }
}

val FirSession.rccSessionResolveStorage: RccSessionProvider by FirSession.sessionComponentAccessor()