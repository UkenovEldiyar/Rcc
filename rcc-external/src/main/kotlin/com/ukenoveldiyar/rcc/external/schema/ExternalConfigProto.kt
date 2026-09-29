package com.ukenoveldiyar.rcc.external.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ExternalConfigProto(
    @ProtoNumber(1) val packageName: String,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val element: ExternalDeclarationProto,
)
