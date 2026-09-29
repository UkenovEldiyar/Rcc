package com.ukenoveldiyar.rcc.runtime.external.names

abstract class FqName {
    abstract val value: String
}

fun fqName(value: String): FqName = FqNameImpl(value)

data class FqNameImpl(override val value: String) : FqName()