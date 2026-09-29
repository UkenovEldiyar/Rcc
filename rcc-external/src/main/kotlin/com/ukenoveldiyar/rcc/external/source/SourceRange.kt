package com.ukenoveldiyar.rcc.external.source

interface SourceRange {

    val startOffset: Int

    val endOffset: Int
}

fun SourceRange(startOffset: Int, endOffset: Int): SourceRange =
    SourceRangeDefault(startOffset, endOffset)

private data class SourceRangeDefault(
    override val startOffset: Int,
    override val endOffset: Int,
) : SourceRange {
    override fun toString(): String = "$startOffset..$endOffset"
}
