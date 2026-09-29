package com.ukenoveldiyar.rcc.backend.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.FilesSubpluginOption
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RccBackendCompilerGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        val extension = target.extensions.create(
            EXTENSION_NAME,
            RccBackendCompilerExtension::class.java
        )

        extension.enabled.convention(true)
        extension.buildDir.convention(target.layout.projectDirectory.dir(DEFAULT_BUILD_DIR))
    }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(RccBackendCompilerExtension::class.java)

        val enabled = extension.enabled
        val compilationName = kotlinCompilation.name

        val componentsDir = project.layout.buildDirectory.dir(COMPONENTS_DIR)
        val compiledOutputDir = extension.buildDir.dir("$COMPILED_DIR/$compilationName")

        val componentEntryTask = project.componentEntryTask(componentsDir, extension)

        val components = componentEntryTask.flatMap { it.outputDir }

        kotlinCompilation.compileTaskProvider.configure { task ->
            task.inputs.dir(components)
                .withPropertyName(COMPONENTS)
                .withPathSensitivity(PathSensitivity.RELATIVE)

            task.outputs.dir(compiledOutputDir)
                .withPropertyName(OUTPUT_DIR)
        }


        return project.provider {
            buildList {
                add(SubpluginOption(key = ENABLED, value = enabled.get().toString()))

                add(FilesSubpluginOption(key = COMPONENTS, files = listOf(components.get().asFile)))

                add(
                    FilesSubpluginOption(
                        key = OUTPUT_DIR,
                        files = listOf(compiledOutputDir.get().asFile)
                    )
                )
            }
        }
    }

    private fun Project.componentEntryTask(
        componentsDir: Provider<out org.gradle.api.file.Directory>,
        extension: RccBackendCompilerExtension,
    ): TaskProvider<RccComponentEntryTask> {
        val taskName = "generateRccBackendComponents"
        return if (taskName in tasks.names) {
            tasks.named(taskName, RccComponentEntryTask::class.java)
        } else {
            tasks.register(taskName, RccComponentEntryTask::class.java) { task ->
                task.sourceFiles.from(extension.components)
                task.outputDir.set(componentsDir)
            }
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = PLUGIN_GROUP,
        artifactId = PLUGIN_ARTIFACT,
        version = PLUGIN_VERSION
    )

    companion object {
        const val EXTENSION_NAME = "rccBackendCompiler"

        const val ENABLED = "enabled"
        const val COMPONENTS = "components"
        const val OUTPUT_DIR = "outputDir"

        const val DEFAULT_BUILD_DIR = "buildRcc"
        const val COMPONENTS_DIR = "rcc-backend-components"
        const val COMPILED_DIR = "compiled"
    }
}
