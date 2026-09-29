package com.ukenoveldiyar.rcc.external.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.ukenoveldiyar.rcc.external.ksp.sorce.SourceComponentMetadata

class RccComponentProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val metadataDir = requireNotNull(environment.options[ARG_METADATA_DIR]) {
            "KSP option '$ARG_METADATA_DIR' is required. " +
                "Apply the rcc-external-gradle plugin to set it automatically."
        }
        return RccComponentProcessor(
            source = SourceComponentMetadata(metadataDir),
            environment = environment,
        )
    }

    companion object {
        const val ARG_METADATA_DIR = "rccComponentMetadataDir"
    }
}
