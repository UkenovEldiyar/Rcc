package com.ukenoveldiyar.rcc.external.gradle

import org.gradle.api.file.DirectoryProperty

abstract class ComponentExtension {
    abstract val componentsDir: DirectoryProperty
}