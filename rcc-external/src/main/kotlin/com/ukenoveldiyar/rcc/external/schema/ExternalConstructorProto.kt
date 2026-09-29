package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExternalConstructorProto(
    @ProtoNumber(1) val fqName: String,
    @ProtoNumber(2) val parameters: List<ExternalTypeRefProto> = emptyList(),
    @ProtoNumber(3) val sourceRange: SourceRangeProto,
) : ExternalDeclarationProto()
