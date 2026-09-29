package com.ukenoveldiyar.rcc.compiler.plugin.dump

import com.ukenoveldiyar.rcc.compiler.plugin.directives.RccTestDirectives
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.getOrCreateTempDirectory
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.withExtension

internal class RccBytecodeDumpHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(RccTestDirectives)

    private var shouldDump = false

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (RccTestDirectives.DUMP_RCC in module.directives) {
            shouldDump = true
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        if (!shouldDump) return

        val outputDir = testServices.getOrCreateTempDirectory("rcc-out")
        val rccFiles = outputDir.walkTopDown()
            .filter { it.isFile && it.extension == "rcc" }
            .sortedBy { it.name }
            .toList()

        val actualDump = buildString {
            for (file in rccFiles) {
                val module = RccModuleReader.read(file.readBytes())
                append("// ").append(file.name).append('\n')
                append(RccDisassembler.disassemble(module))
            }
        }

        val expectedFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension("rcc.txt")
        testServices.assertions.assertEqualsToFile(expectedFile, actualDump)
    }
}
