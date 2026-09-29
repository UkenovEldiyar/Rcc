package com.ukenoveldiyar.rcc.runtime.external.declarations.builder

import androidx.compose.runtime.Composable
import com.ukenoveldiyar.rcc.runtime.external.ExternalScope
import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.runtime.external.declarations.impl.ExternalFunctionImpl
import com.ukenoveldiyar.rcc.runtime.external.descriptor.ExternalDescriptor
import com.ukenoveldiyar.rcc.runtime.external.names.CallableId

internal fun function(
    callableId: CallableId,
    descriptor: ExternalDescriptor,
    invoke: ExternalScope.() -> Unit,
): ExternalFunction = ExternalFunctionImpl(
    callableId = callableId,
    descriptor = descriptor,
    invoke = invoke
)

internal fun functionComposable(
    callableId: CallableId,
    descriptor: ExternalDescriptor,
    invoke: @Composable ExternalScope.() -> Unit,
): ExternalFunction = ExternalFunctionImpl(
    callableId = callableId,
    descriptor = descriptor,
    invoke = invoke
)