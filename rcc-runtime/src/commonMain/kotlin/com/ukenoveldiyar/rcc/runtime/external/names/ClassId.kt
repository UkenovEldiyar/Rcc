package com.ukenoveldiyar.rcc.runtime.external.names

abstract class ClassId {
    abstract val packageFqName: FqName
    abstract val className: String

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    abstract override fun toString(): String
}

fun classId(packageFqName: FqName, className: String): ClassId =
    ClassIdImpl(packageFqName, className)

internal data class ClassIdImpl(
    override val packageFqName: FqName,
    override val className: String,
) : ClassId()
