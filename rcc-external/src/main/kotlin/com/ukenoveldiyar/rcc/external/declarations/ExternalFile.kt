package com.ukenoveldiyar.rcc.external.declarations

import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

class ExternalFile(
    override val sourceElement: SourceElement,
    val annotations: List<ExternalAnnotation>,
    val declarations: List<ExternalDeclaration>,
    val name: String
) : ExternalDeclaration() {

    override fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R =
        visitor.visitFile(this, data)
}
