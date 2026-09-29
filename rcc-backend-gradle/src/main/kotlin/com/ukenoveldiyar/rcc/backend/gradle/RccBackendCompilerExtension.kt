package com.ukenoveldiyar.rcc.backend.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class RccBackendCompilerExtension {
    abstract val enabled: Property<Boolean>

    abstract val components: ConfigurableFileCollection

    abstract val buildDir: DirectoryProperty
}