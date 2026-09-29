package com.ukenoveldiyar.rcc.external.names

data class Name(
    private val name: String
) {
    fun asString(): String = name
}
