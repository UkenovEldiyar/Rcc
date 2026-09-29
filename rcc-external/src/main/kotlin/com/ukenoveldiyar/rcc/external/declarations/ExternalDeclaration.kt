package com.ukenoveldiyar.rcc.external.declarations

import com.ukenoveldiyar.rcc.external.ExternalElement
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

sealed class ExternalDeclaration : ExternalElement {

    override fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R =
        visitor.visitDeclaration(this, data)
}
