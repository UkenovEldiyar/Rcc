package com.ukenoveldiyar.rcc.runtime.external.names.impl

import com.ukenoveldiyar.rcc.runtime.external.names.CallableId
import com.ukenoveldiyar.rcc.runtime.external.names.FqName
import com.ukenoveldiyar.rcc.runtime.external.names.Name

internal data class CallableIdImpl(
    val packageFqName: FqName,
    val className: String,
    val callableName: Name,
) : CallableId() {

    override fun asString(): String =
        "${packageFqName.value}.$className.${callableName.value}"

}