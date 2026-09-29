package com.ukenoveldiyar.rcc.external.declarations

import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

class ExternalConstructor(
    val fqName: FqName,
    val parameters: List<ExternalTypeRef> = emptyList(),
    override val sourceElement: SourceElement,
) : ExternalDeclaration() {

    override fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R =
        visitor.visitConstructor(this, data)
}
