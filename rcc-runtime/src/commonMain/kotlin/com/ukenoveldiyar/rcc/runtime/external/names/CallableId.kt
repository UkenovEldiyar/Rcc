package com.ukenoveldiyar.rcc.runtime.external.names

import com.ukenoveldiyar.rcc.runtime.external.names.impl.CallableIdImpl

abstract class CallableId {
    abstract fun asString(): String

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    abstract override fun toString(): String
}

fun callableId(
    packageFqName: FqName,
    ownerName: String,
    callableName: Name,
): CallableId = CallableIdImpl(
    packageFqName = packageFqName,
    className = ownerName,
    callableName = callableName
)