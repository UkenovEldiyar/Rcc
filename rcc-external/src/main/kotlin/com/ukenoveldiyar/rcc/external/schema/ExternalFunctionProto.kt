package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExternalFunctionProto(
    @ProtoNumber(1) val fqName: String,
    @ProtoNumber(2) val extension: ExternalTypeRefProto? = null,
    @ProtoNumber(3) val parameters: List<ExternalTypeRefProto> = emptyList(),
    @ProtoNumber(4) val sourceRange: SourceRangeProto,
) : ExternalDeclarationProto()
