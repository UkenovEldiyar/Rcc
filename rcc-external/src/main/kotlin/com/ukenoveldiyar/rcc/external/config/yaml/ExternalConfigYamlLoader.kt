package com.ukenoveldiyar.rcc.external.config.yaml

import com.ukenoveldiyar.rcc.external.config.ExternalConfig
import com.ukenoveldiyar.rcc.external.config.ExternalConfigLoader
import org.snakeyaml.engine.v2.api.LoadSettings
import org.snakeyaml.engine.v2.api.lowlevel.Compose
import java.io.Reader

class ExternalConfigYamlLoader(
    private val yaml: Reader
) : ExternalConfigLoader {


    override fun load(): ExternalConfig {
        val root = Compose(YAML)
            .composeReader(yaml)
            .orElseThrow { IllegalArgumentException("Yaml file is empty!") }

        return YamlMapperExternalConfig().map(root)
    }

    companion object {
        private val YAML = LoadSettings.builder()
            .setLabel("Rcc-External-Yaml")
            .build()
    }
}
