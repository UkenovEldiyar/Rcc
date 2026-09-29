@file:OptIn(ExperimentalSerializationApi::class)

package com.ukenoveldiyar.rcc.backend.gradle

import com.ukenoveldiyar.rcc.external.config.yaml.ExternalConfigYamlLoader
import com.ukenoveldiyar.rcc.external.schema.ExternalConfigProto
import com.ukenoveldiyar.rcc.external.schema.toProto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class RccComponentEntryTask : DefaultTask() {

    //@SkipWhenEmpty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()

        for (yamlFile in sourceFiles.files) {
            if (!yamlFile.isFile) continue

            val configProto = try {
                yamlFile.reader().use { reader -> ExternalConfigYamlLoader(reader).load() }.toProto()
            } catch (e: Exception) {
                throw GradleException("Failed to parse component descriptor '${yamlFile.path}'", e)
            }

            val outFile = outDir.resolve("${yamlFile.nameWithoutExtension}.pb")

            outFile.writeBytes(ProtoBuf.encodeToByteArray(ExternalConfigProto.serializer(), configProto))
        }
    }
}
