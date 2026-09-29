package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExternalFileProto(
    @ProtoNumber(1) val sourceRange: SourceRangeProto,
    @ProtoNumber(2) val annotations: List<ExternalAnnotationProto>,
    @ProtoNumber(3) val declarations: List<ExternalDeclarationProto>,
    @ProtoNumber(4) val name: String,
) : ExternalDeclarationProto()
