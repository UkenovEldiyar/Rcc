package com.ukenoveldiyar.rcc.external.declarations

import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef
import com.ukenoveldiyar.rcc.external.visitor.ExternalVisitor

class ExternalProperty(
    val fqName: FqName,
    val type: ExternalTypeRef,
    val mutable: Boolean = false,
    val extension: ExternalTypeRef? = null,
    override val sourceElement: SourceElement,
) : ExternalDeclaration() {

    override fun <R, D> accept(visitor: ExternalVisitor<R, D>, data: D): R =
        visitor.visitProperty(this, data)
}
