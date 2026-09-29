package com.ukenoveldiyar.rcc.external.types

import com.ukenoveldiyar.rcc.external.ExternalElement
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

sealed class ExternalTypeRef : ExternalElement {
    override fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R =
        visitor.visitTypeRef(this, data)
}
