package com.ukenoveldiyar.rcc.runtime.external.declarations.builder

import androidx.compose.runtime.Composable
import com.ukenoveldiyar.rcc.runtime.external.ExternalScope
import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.runtime.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.runtime.external.declarations.impl.ExternalFileImpl
import com.ukenoveldiyar.rcc.runtime.external.descriptor.ExternalDescriptor
import com.ukenoveldiyar.rcc.runtime.external.names.FqName
import com.ukenoveldiyar.rcc.runtime.external.names.Name
import com.ukenoveldiyar.rcc.runtime.external.names.callableId
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExternalDsl
class ExternalFileBuilder internal constructor(
    private val packageFqName: FqName,
    private val fileName: String,
) {
    private val declarations =
        mutableMapOf<Name, MutableMap<ExternalDescriptor, ExternalFunction>>()

    fun function(
        name: Name,
        descriptor: ExternalDescriptor,
        invoke: ExternalScope.() -> Unit
    ) {
        val overloads = declarations.getOrPut(name) { mutableMapOf() }

        overloads[descriptor] = function(
            callableId = callableId(
                packageFqName = packageFqName,
                ownerName = fileName,
                callableName = name
            ),
            descriptor = descriptor,
            invoke = invoke,
        )
    }

    fun composableFunction(
        name: Name,
        descriptor: ExternalDescriptor,
        invoke: @Composable ExternalScope.() -> Unit
    ) {
        val overloads = declarations.getOrPut(name) { mutableMapOf() }

        overloads[descriptor] = functionComposable(
            callableId = callableId(
                packageFqName = packageFqName,
                ownerName = fileName,
                callableName = name
            ),
            descriptor = descriptor,
            invoke = invoke,
        )
    }

    internal fun build(): ExternalFile = ExternalFileImpl(
        packageFqName = packageFqName,
        name = fileName,
        initDeclaration = { declarations },
    )
}

@OptIn(ExperimentalContracts::class)
fun externalFile(
    packageFqName: FqName,
    name: String,
    init: ExternalFileBuilder.() -> Unit,
): ExternalFile {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }

    return ExternalFileBuilder(packageFqName, fileName = name).apply(init).build()
}