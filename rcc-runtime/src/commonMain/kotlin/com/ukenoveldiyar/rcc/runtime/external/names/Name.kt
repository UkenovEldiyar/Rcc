package com.ukenoveldiyar.rcc.runtime.external.names

abstract class Name {
    abstract val value: String
}

fun name(value: String): Name = NameImpl(value)

data class NameImpl(override val value: String) : Name()