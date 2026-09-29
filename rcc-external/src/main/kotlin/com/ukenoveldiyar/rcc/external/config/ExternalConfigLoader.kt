package com.ukenoveldiyar.rcc.external.config

interface ExternalConfigLoader {
    fun load(): ExternalConfig
}