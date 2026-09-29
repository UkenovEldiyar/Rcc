package com.ukenoveldiyar.rcc.runtime

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readIntLe
import kotlinx.io.readLongLe
import kotlinx.io.readShortLe

abstract class ModuleProgramLoader {

    abstract fun findModule(name: String): ModuleProgram

    protected fun defineModule(
        name: String,
        bytes: ByteArray,
    ): ModuleProgram {
        val source: Source = Buffer().also { it.write(bytes) }
        val magic = source.readIntLe()

        require(magic == MAGIC) {
            "rcc: неверная сигнатура модуля \"$name\": ожидалось 0x${MAGIC.toString(16)}, получено 0x${magic.toString(16)}"
        }

        val version = source.readShortLe().toInt() and 0xFFFF

        require(version == VERSION) {
            "rcc: неподдерживаемая версия байткода модуля \"$name\": $version (ожидалась $VERSION)"
        }

        val constantPool = readConstantPool(source)
        val functions = readFunctions(source)
        val entryPointFunction = readEntryPoints(source, functions)

        return ModuleProgram(
            entryPointFunction = entryPointFunction,
            function = functions,
            constantPool = constantPool,
        )
    }

    private fun readConstantPool(source: Source): ConstantPool {
        val count = source.readIntLe()
        return ConstantPool(Array(count) { source.readRccString() })
    }

    private fun readFunctions(source: Source): Array<Function> {
        val count = source.readIntLe()
        return Array(count) { expectedIndex -> readFunction(source, expectedIndex) }
    }

    private fun readFunction(source: Source, expectedIndex: Int): Function {
        val id = source.readIntLe()
        val maxStackSize = source.readShortLe().toInt() and 0xFFFF
        val maxLocalSize = source.readShortLe().toInt() and 0xFFFF
        val parameterCount = source.readShortLe().toInt() and 0xFFFF
        val parameterRaw = source.readByteArray(parameterCount)
        val captureParameterCount = source.readShortLe().toInt() and 0xFFFF
        source.readByteArray(captureParameterCount)
        val userParameterCount = source.readShortLe().toInt() and 0xFFFF
        val returnType = source.readByte()
        val codeLength = source.readIntLe()
        val code = IntArray(codeLength) { source.readIntLe() }
        val constantsLength = source.readIntLe()
        val constants = LongArray(constantsLength) { source.readLongLe() }

        return Function(
            id = id,
            maxStackSize = maxStackSize,
            maxLocalSize = maxLocalSize,
            parameterCount = parameterCount,
            parameterRaw = parameterRaw,
            captureParameterCount = captureParameterCount,
            userParameterCount = userParameterCount,
            returnType = returnType,
            instruction = Instruction(code = code, constants = constants),
        )
    }

    private fun readEntryPoints(
        source: Source,
        functions: Array<Function>,
    ): HashMap<String, Function> {
        val count = source.readIntLe()
        val entryPoints = HashMap<String, Function>(count)

        repeat(count) {
            val fqName = source.readRccString()
            val functionId = source.readIntLe()

            require(functionId in functions.indices) {
                "rcc: точка входа \"$fqName\" ссылается на несуществующий functionId $functionId"
            }

            entryPoints[fqName] = functions[functionId]
        }

        return entryPoints
    }

    private fun Source.readRccString(): String {
        val length = readIntLe()
        return readByteArray(length).decodeToString()
    }

    private companion object {
        const val MAGIC = 0x52434331
        const val VERSION = 1
    }
}
