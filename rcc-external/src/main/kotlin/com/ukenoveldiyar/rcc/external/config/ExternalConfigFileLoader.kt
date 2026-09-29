package com.ukenoveldiyar.rcc.external.config

import com.ukenoveldiyar.rcc.external.schema.ExternalConfigProto
import com.ukenoveldiyar.rcc.external.schema.toDomain
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File

/**
 * Единая точка декодирования `.pb`-дескрипторов host-компонентов (KSP-сгенерированных
 * [ExternalConfigProto]) — используется и KSP-процессором (rcc-external-processor's
 * SourceComponentMetadata), и компилятором (rcc-backend-compiler's RccCompilerComponentContainer),
 * чтобы обе стороны смотрели на один и тот же формат.
 */
@OptIn(ExperimentalSerializationApi::class)
object ExternalConfigFileLoader {
    /** Все `.pb`-дескрипторы под [path], рекурсивно — реальные данные лежат вложенно, по пакетам. */
    fun listFiles(path: String): List<File> =
        File(path).walkTopDown()
            .filter { it.isFile && it.extension == "pb" }
            .toList()

    /** Декодирование вынесено отдельно от [listFiles], чтобы вызывающая сторона могла изолировать
     *  ошибку ОДНОГО `.pb` от остальных. */
    fun decode(file: File): ExternalConfig =
        ProtoBuf.decodeFromByteArray(ExternalConfigProto.serializer(), file.readBytes()).toDomain()

    /** Загружает и декодирует все `.pb` под каждой из [paths] (директории). */
    fun loadAll(paths: List<String>): List<ExternalConfig> =
        paths.flatMap(::listFiles).map(::decode)
}
