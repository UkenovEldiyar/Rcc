package com.ukenoveldiyar.rcc.runtime.external.declarations

import com.ukenoveldiyar.rcc.runtime.external.descriptor.ExternalDescriptor
import com.ukenoveldiyar.rcc.runtime.external.names.CallableId

abstract class ExternalFunction : ExternalDeclaration() {
    abstract val callableId: CallableId
    abstract val descriptor: ExternalDescriptor
    abstract val invoke: Any
}