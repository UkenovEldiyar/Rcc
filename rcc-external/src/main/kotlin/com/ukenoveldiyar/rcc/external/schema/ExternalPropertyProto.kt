package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExternalPropertyProto(
    @ProtoNumber(1) val fqName: String,
    @ProtoNumber(2) val type: ExternalTypeRefProto,
    @ProtoNumber(3) val mutable: Boolean = false,
    @ProtoNumber(4) val extension: ExternalTypeRefProto? = null,
    @ProtoNumber(5) val sourceRange: SourceRangeProto,
) : ExternalDeclarationProto()
