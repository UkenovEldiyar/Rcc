package com.ukenoveldiyar.rcc.external

import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

interface ExternalElement {
    val sourceElement: SourceElement

    fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R
}