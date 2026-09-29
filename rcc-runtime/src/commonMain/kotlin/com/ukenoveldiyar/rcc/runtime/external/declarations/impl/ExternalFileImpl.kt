package com.ukenoveldiyar.rcc.runtime.external.declarations.impl

import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.runtime.external.descriptor.ExternalDescriptor
import com.ukenoveldiyar.rcc.runtime.external.names.FqName
import com.ukenoveldiyar.rcc.runtime.external.names.Name

internal class ExternalFileImpl(
    initDeclaration: () -> Map<Name, Map<ExternalDescriptor, ExternalFunction>>,
    override val packageFqName: FqName,
    override val name: String,
) : ExternalFile() {

    private val declarations: Map<Name, Map<ExternalDescriptor, ExternalFunction>> by lazy(initDeclaration)

    override fun getDeclaration(
        key: Name,
        descriptor: ExternalDescriptor
    ): ExternalFunction {
        val overloads = declarations[key]
            ?: error("rcc: no declaration named \"${key.value}\" in file \"$name\"")
        val declaration = overloads[descriptor]
            ?: error("rcc: no overload matching $descriptor for \"${key.value}\" in file \"$name\"")

        return declaration
    }
}