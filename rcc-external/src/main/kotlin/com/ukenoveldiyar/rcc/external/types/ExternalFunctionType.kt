package com.ukenoveldiyar.rcc.external.types

import com.ukenoveldiyar.rcc.external.declarations.ExternalAnnotation
import com.ukenoveldiyar.rcc.external.source.SourceElement

class ExternalFunctionType(
    val receiver: ExternalTypeRef?,
    val parameters: List<ExternalTypeRef>,
    val returnType: ExternalTypeRef,
    val annotations: List<ExternalAnnotation> = emptyList(),
    override val sourceElement: SourceElement,
) : ExternalTypeRef()
