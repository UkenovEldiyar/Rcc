package com.ukenoveldiyar.rcc.compiler.plugin.dump

import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readIntLe
import kotlinx.io.readLongLe
import kotlinx.io.readShortLe

internal class DumpFunction(
    val id: Int,
    val maxStackSize: Int,
    val maxLocalSize: Int,
    val parameterCount: Int,
    val parameterRaw: ByteArray,
    val captureParameterCount: Int,
    val captureParameterRaw: ByteArray,
    val userParameterCount: Int,
    val returnType: Byte,
    val code: IntArray,
    val constants: LongArray,
)

internal class DumpModule(
    val stringPool: List<String>,
    val functions: List<DumpFunction>,
    val entryPoints: Map<String, Int>,
)

internal object RccModuleReader {
    private const val MAGIC = 0x52434331
    private const val VERSION = 1

    fun read(bytes: ByteArray): DumpModule {
        val source: Source = Buffer().also { it.write(bytes) }

        val magic = source.readIntLe()
        require(magic == MAGIC) {
            "rcc-dump: неверная сигнатура: ожидалось 0x${MAGIC.toString(16)}, получено 0x${magic.toString(16)}"
        }

        val version = source.readShortLe().toInt() and 0xFFFF
        require(version == VERSION) {
            "rcc-dump: неподдерживаемая версия: $version (ожидалась $VERSION)"
        }

        val stringPool = readStringPool(source)
        val functions = readFunctions(source)
        val entryPoints = readEntryPoints(source)

        return DumpModule(stringPool, functions, entryPoints)
    }

    private fun readStringPool(source: Source): List<String> {
        val count = source.readIntLe()
        return List(count) { source.readRccString() }
    }

    private fun readFunctions(source: Source): List<DumpFunction> {
        val count = source.readIntLe()
        return List(count) { readFunction(source) }
    }

    private fun readFunction(source: Source): DumpFunction {
        val id = source.readIntLe()
        val maxStackSize = source.readShortLe().toInt() and 0xFFFF
        val maxLocalSize = source.readShortLe().toInt() and 0xFFFF
        val parameterCount = source.readShortLe().toInt() and 0xFFFF
        val parameterRaw = source.readByteArray(parameterCount)
        val captureParameterCount = source.readShortLe().toInt() and 0xFFFF
        val captureParameterRaw = source.readByteArray(captureParameterCount)
        val userParameterCount = source.readShortLe().toInt() and 0xFFFF
        val returnType = source.readByte()
        val codeLength = source.readIntLe()
        val code = IntArray(codeLength) { source.readIntLe() }
        val constantsLength = source.readIntLe()
        val constants = LongArray(constantsLength) { source.readLongLe() }

        return DumpFunction(
            id = id,
            maxStackSize = maxStackSize,
            maxLocalSize = maxLocalSize,
            parameterCount = parameterCount,
            parameterRaw = parameterRaw,
            captureParameterCount = captureParameterCount,
            captureParameterRaw = captureParameterRaw,
            userParameterCount = userParameterCount,
            returnType = returnType,
            code = code,
            constants = constants,
        )
    }

    private fun readEntryPoints(source: Source): Map<String, Int> {
        val count = source.readIntLe()
        val entryPoints = LinkedHashMap<String, Int>(count)
        repeat(count) {
            val fqName = source.readRccString()
            val functionId = source.readIntLe()
            entryPoints[fqName] = functionId
        }
        return entryPoints
    }

    private fun Source.readRccString(): String {
        val length = readIntLe()
        return readByteArray(length).decodeToString()
    }
}
