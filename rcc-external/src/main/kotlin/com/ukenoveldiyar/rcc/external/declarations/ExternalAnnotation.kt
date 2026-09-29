package com.ukenoveldiyar.rcc.external.declarations

import com.ukenoveldiyar.rcc.external.ExternalElement
import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

class ExternalAnnotation(
    val fqName: FqName,
    override val sourceElement: SourceElement
) : ExternalElement {

    override fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R =
        visitor.visitAnnotation(this, data)
}
