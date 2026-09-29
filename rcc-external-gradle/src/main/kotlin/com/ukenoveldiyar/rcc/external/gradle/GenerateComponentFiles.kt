package com.ukenoveldiyar.rcc.external.gradle

import com.ukenoveldiyar.rcc.external.config.yaml.ExternalConfigYamlLoader
import com.ukenoveldiyar.rcc.external.schema.ExternalConfigProto
import com.ukenoveldiyar.rcc.external.schema.toProto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.problems.ProblemGroup
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

@CacheableTask
@OptIn(ExperimentalSerializationApi::class)
abstract class GenerateComponentFiles @Inject constructor(
    private val problems: Problems,
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rootDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputComponent: DirectoryProperty

    @TaskAction
    fun run() {
        val root = rootDir.get().asFile
        val outputDir = outputComponent.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        for (file in root.walkTopDown().filter { it.isFile }) {
            if (file.extension != "yaml" && file.extension != "yml") {
                reportNonYamlFile(file)
                continue
            }

            val configProto = try {
                file.reader().use { reader -> ExternalConfigYamlLoader(reader).load() }.toProto()
            } catch (e: Exception) {
                throw GradleException("Failed to parse component descriptor '${file.path}'", e)
            }

            val relativeDir = file.relativeTo(root).parentFile
            val targetDir = if (relativeDir != null) outputDir.resolve(relativeDir.path) else outputDir
            targetDir.mkdirs()

            val outFile = targetDir.resolve("${file.nameWithoutExtension}.pb")
            outFile.writeBytes(ProtoBuf.encodeToByteArray(ExternalConfigProto.serializer(), configProto))
        }
    }

    private fun reportNonYamlFile(file: File) {
        problems.reporter.report(
            ProblemId.create(
                "non-yaml-file-in-components-root",
                "Non-YAML file in rccComponent root directory",
                PROBLEM_GROUP,
            )
        ) { spec ->
            spec.severity(Severity.ADVICE)
                .contextualLabel("'${file.name}' is not a component descriptor")
                .fileLocation(file.absolutePath)
                .details(
                    "The rccComponent root directory is expected to contain only *.yaml/*.yml " +
                        "component descriptors. '${file.name}' was ignored."
                )
                .solution(
                    "Move '${file.name}' out of the rccComponent root directory, or rename it " +
                        "with a .yaml/.yml extension if it is meant to be a component descriptor."
                )
        }
    }

    companion object {
        private val PROBLEM_GROUP: ProblemGroup = ProblemGroup.create("rcc-component", "RCC Component")
    }
}
