package com.ukenoveldiyar.rcc.runtime.external.descriptor

import com.ukenoveldiyar.rcc.runtime.external.ExternalElement
import com.ukenoveldiyar.rcc.runtime.external.names.ClassId

sealed class ExternalDescriptor : ExternalElement() {
    data class Function(val parameterTypes: List<ClassId>) : ExternalDescriptor()
    data class PropertyGetter(val receiver: ClassId?) : ExternalDescriptor()
    data class PropertySetter(val receiver: ClassId?) : ExternalDescriptor()
}