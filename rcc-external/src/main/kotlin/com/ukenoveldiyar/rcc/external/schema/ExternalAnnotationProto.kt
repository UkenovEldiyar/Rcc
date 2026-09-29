package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExternalAnnotationProto(
    @ProtoNumber(1) val fqName: String,
    @ProtoNumber(2) val sourceRange: SourceRangeProto,
)
