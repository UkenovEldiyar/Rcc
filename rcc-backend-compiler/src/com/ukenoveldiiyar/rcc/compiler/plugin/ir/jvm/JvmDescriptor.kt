package com.ukenoveldiiyar.rcc.compiler.plugin.ir.jvm

sealed interface JvmType {
    data class Primitive(val descriptor: Char) : JvmType

    data class ObjectType(val internalName: String) : JvmType
    data class ArrayType(val element: JvmType) : JvmType
    data object VoidType : JvmType
}

fun parseMethodDescriptor(descriptor: String): Pair<List<JvmType>, JvmType> {
    require(descriptor.startsWith('(')) { "Некорректный дескриптор метода: $descriptor" }
    val params = mutableListOf<JvmType>()
    var i = 1
    while (descriptor[i] != ')') {
        val (type, next) = parseType(descriptor, i)
        params += type
        i = next
    }
    val (returnType, end) = parseType(descriptor, i + 1, allowVoid = true)
    require(end == descriptor.length) { "Мусор после дескриптора метода: $descriptor" }
    return params to returnType
}

private fun parseType(
    descriptor: String,
    start: Int,
    allowVoid: Boolean = false
): Pair<JvmType, Int> {
    require(start < descriptor.length) { "Обрыв дескриптора: $descriptor" }
    return when (val c = descriptor[start]) {
        'Z', 'B', 'C', 'S', 'I', 'J', 'F', 'D' -> JvmType.Primitive(c) to start + 1
        'V' -> {
            require(allowVoid) { "V недопустим здесь: $descriptor" }
            JvmType.VoidType to start + 1
        }

        'L' -> {
            val semi = descriptor.indexOf(';', start)
            require(semi > start + 1) { "Незакрытый L-тип в дескрипторе: $descriptor" }
            JvmType.ObjectType(descriptor.substring(start + 1, semi)) to semi + 1
        }

        '[' -> {
            val (element, next) = parseType(descriptor, start + 1)
            JvmType.ArrayType(element) to next
        }

        else -> throw IllegalArgumentException("Неизвестный символ дескриптора '$c' в $descriptor")
    }
}
