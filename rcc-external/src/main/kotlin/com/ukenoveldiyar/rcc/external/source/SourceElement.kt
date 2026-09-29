package com.ukenoveldiyar.rcc.external.source

interface SourceElement {
    val sourceRange: SourceRange
}

fun SourceElement(sourceRange: SourceRange): SourceElement =
    SourceElementDefault(sourceRange)

private data class SourceElementDefault(
    override val sourceRange: SourceRange
) : SourceElement
