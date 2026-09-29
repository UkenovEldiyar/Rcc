package com.ukenoveldiyar.rcc.external.types

import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement

class ExternalType(
    val fqName: FqName,
    override val sourceElement: SourceElement,
) : ExternalTypeRef()
