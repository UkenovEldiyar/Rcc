package com.ukenoveldiyar.rcc.external.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class RccComponentGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("com.google.devtools.ksp")

        val extension = project.extensions.create(
            "rccComponent",
            ComponentExtension::class.java,
        )

        val outputDir = project.layout.buildDirectory.dir("generated/rcc-components")

        val generateTask = project.tasks.register(
            "generateRccComponents",
            GenerateComponentFiles::class.java,
        ) {
            it.rootDir.set(extension.componentsDir)
            it.outputComponent.set(outputDir)
        }

        project.extensions.configure(KspExtension::class.java) {
            it.arg("rccComponentMetadataDir", outputDir.get().asFile.absolutePath)
        }

        project.tasks.matching { it.name.startsWith("ksp") }.configureEach {
            it.dependsOn(generateTask)
        }
    }
}
