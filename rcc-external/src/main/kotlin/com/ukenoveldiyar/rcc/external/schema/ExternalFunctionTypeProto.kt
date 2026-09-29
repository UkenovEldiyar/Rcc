package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExternalFunctionTypeProto(
    @ProtoNumber(1) val receiver: ExternalTypeRefProto?,
    @ProtoNumber(2) val parameters: List<ExternalTypeRefProto>,
    @ProtoNumber(3) val returnType: ExternalTypeRefProto,
    @ProtoNumber(4) val annotations: List<ExternalAnnotationProto> = emptyList(),
    @ProtoNumber(5) val sourceRange: SourceRangeProto,
) : ExternalTypeRefProto()
