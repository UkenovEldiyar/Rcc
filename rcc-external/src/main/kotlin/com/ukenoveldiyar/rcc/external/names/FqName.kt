package com.ukenoveldiyar.rcc.external.names

data class FqName(
    private val fqName: String,
) {
    fun asString(): String = fqName
}
