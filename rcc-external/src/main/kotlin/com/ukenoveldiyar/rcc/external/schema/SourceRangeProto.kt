package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SourceRangeProto(
    @ProtoNumber(1) val startOffset: Int,
    @ProtoNumber(2) val endOffset: Int,
)
