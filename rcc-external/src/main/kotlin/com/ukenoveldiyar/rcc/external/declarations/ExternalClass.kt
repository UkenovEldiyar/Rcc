package com.ukenoveldiyar.rcc.external.declarations

import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

class ExternalClass(
    val fqName: FqName,
    val declarations: List<ExternalDeclaration> = emptyList(),
    override val sourceElement: SourceElement,
) : ExternalDeclaration() {

    override fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R =
        visitor.visitInterface(this, data)
}
