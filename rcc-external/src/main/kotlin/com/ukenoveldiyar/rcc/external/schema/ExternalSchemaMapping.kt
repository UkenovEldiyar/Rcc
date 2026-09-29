package com.ukenoveldiyar.rcc.external.schema

import com.ukenoveldiyar.rcc.external.config.ExternalConfig
import com.ukenoveldiyar.rcc.external.declarations.ExternalAnnotation
import com.ukenoveldiyar.rcc.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.external.declarations.ExternalConstructor
import com.ukenoveldiyar.rcc.external.declarations.ExternalDeclaration
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.external.declarations.ExternalProperty
import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.source.SourceRange
import com.ukenoveldiyar.rcc.external.types.ExternalFunctionType
import com.ukenoveldiyar.rcc.external.types.ExternalType
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef

fun ExternalConfig.toProto(): ExternalConfigProto = ExternalConfigProto(
    packageName = packageName,
    name = name,
    element = (element as ExternalDeclaration).toProto(),
)

fun ExternalConfigProto.toDomain(): ExternalConfig = ExternalConfig(
    packageName = packageName,
    name = name,
    element = element.toDomain(),
)

fun SourceElement.toProto(): SourceRangeProto =
    SourceRangeProto(sourceRange.startOffset, sourceRange.endOffset)

fun SourceRangeProto.toDomain(): SourceElement =
    SourceElement(SourceRange(startOffset, endOffset))

fun ExternalAnnotation.toProto(): ExternalAnnotationProto = ExternalAnnotationProto(
    fqName = fqName.asString(),
    sourceRange = sourceElement.toProto(),
)

fun ExternalAnnotationProto.toDomain(): ExternalAnnotation = ExternalAnnotation(
    fqName = FqName(fqName),
    sourceElement = sourceRange.toDomain(),
)

fun ExternalTypeRef.toProto(): ExternalTypeRefProto = when (this) {
    is ExternalType -> ExternalTypeProto(
        fqName = fqName.asString(),
        sourceRange = sourceElement.toProto(),
    )

    is ExternalFunctionType -> ExternalFunctionTypeProto(
        receiver = receiver?.toProto(),
        parameters = parameters.map { it.toProto() },
        returnType = returnType.toProto(),
        annotations = annotations.map { it.toProto() },
        sourceRange = sourceElement.toProto(),
    )
}

fun ExternalTypeRefProto.toDomain(): ExternalTypeRef = when (this) {
    is ExternalTypeProto -> ExternalType(
        fqName = FqName(fqName),
        sourceElement = sourceRange.toDomain(),
    )

    is ExternalFunctionTypeProto -> ExternalFunctionType(
        receiver = receiver?.toDomain(),
        parameters = parameters.map { it.toDomain() },
        returnType = returnType.toDomain(),
        annotations = annotations.map { it.toDomain() },
        sourceElement = sourceRange.toDomain(),
    )
}

fun ExternalDeclaration.toProto(): ExternalDeclarationProto = when (this) {
    is ExternalFile -> ExternalFileProto(
        sourceRange = sourceElement.toProto(),
        annotations = annotations.map { it.toProto() },
        declarations = declarations.map { it.toProto() },
        name = name,
    )

    is ExternalClass -> ExternalClassProto(
        fqName = fqName.asString(),
        declarations = declarations.map { it.toProto() },
        sourceRange = sourceElement.toProto(),
    )

    is ExternalFunction -> ExternalFunctionProto(
        fqName = fqName.asString(),
        extension = extension?.toProto(),
        parameters = parameters.map { it.toProto() },
        sourceRange = sourceElement.toProto(),
    )

    is ExternalProperty -> ExternalPropertyProto(
        fqName = fqName.asString(),
        type = type.toProto(),
        mutable = mutable,
        extension = extension?.toProto(),
        sourceRange = sourceElement.toProto(),
    )

    is ExternalConstructor -> ExternalConstructorProto(
        fqName = fqName.asString(),
        parameters = parameters.map { it.toProto() },
        sourceRange = sourceElement.toProto(),
    )
}

fun ExternalDeclarationProto.toDomain(): ExternalDeclaration = when (this) {
    is ExternalFileProto -> ExternalFile(
        sourceElement = sourceRange.toDomain(),
        annotations = annotations.map { it.toDomain() },
        declarations = declarations.map { it.toDomain() },
        name = name,
    )

    is ExternalClassProto -> ExternalClass(
        fqName = FqName(fqName),
        declarations = declarations.map { it.toDomain() },
        sourceElement = sourceRange.toDomain(),
    )

    is ExternalFunctionProto -> ExternalFunction(
        fqName = FqName(fqName),
        extension = extension?.toDomain(),
        parameters = parameters.map { it.toDomain() },
        sourceElement = sourceRange.toDomain(),
    )

    is ExternalPropertyProto -> ExternalProperty(
        fqName = FqName(fqName),
        type = type.toDomain(),
        mutable = mutable,
        extension = extension?.toDomain(),
        sourceElement = sourceRange.toDomain(),
    )

    is ExternalConstructorProto -> ExternalConstructor(
        fqName = FqName(fqName),
        parameters = parameters.map { it.toDomain() },
        sourceElement = sourceRange.toDomain(),
    )
}
