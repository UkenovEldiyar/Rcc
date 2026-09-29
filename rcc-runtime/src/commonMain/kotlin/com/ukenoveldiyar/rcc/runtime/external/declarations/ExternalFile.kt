package com.ukenoveldiyar.rcc.runtime.external.declarations

import com.ukenoveldiyar.rcc.runtime.external.descriptor.ExternalDescriptor
import com.ukenoveldiyar.rcc.runtime.external.names.FqName
import com.ukenoveldiyar.rcc.runtime.external.names.Name

abstract class ExternalFile : ExternalDeclaration() {
    abstract val packageFqName: FqName
    abstract val name: String

    abstract fun getDeclaration(key: Name, descriptor: ExternalDescriptor): ExternalFunction
}