package com.ukenoveldiyar.rcc.runtime.external.declarations.impl

import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.runtime.external.descriptor.ExternalDescriptor
import com.ukenoveldiyar.rcc.runtime.external.names.CallableId

internal class ExternalFunctionImpl(
    override val callableId: CallableId,
    override val descriptor: ExternalDescriptor,
    override val invoke: Any,
) : ExternalFunction()
