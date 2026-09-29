package com.ukenoveldiyar.rcc.external.config

import com.ukenoveldiyar.rcc.external.ExternalElement

data class ExternalConfig(
    val packageName: String,
    val name: String,
    val element: ExternalElement
)