package com.ukenoveldiyar.rcc.external.ksp.sorce

import com.ukenoveldiyar.rcc.external.config.ExternalConfig
import com.ukenoveldiyar.rcc.external.config.ExternalConfigFileLoader
import java.io.File

class SourceComponentMetadata(private val path: String) {

    /** Все `.pb`-дескрипторы (рекурсивно — реальные YAML лежат вложенно, по пакетам). */
    fun listFiles(): List<File> = ExternalConfigFileLoader.listFiles(path)

    /**
     * Декодирование вынесено отдельно от [listFiles], чтобы вызывающая сторона могла изолировать
     * ошибку ОДНОГО `.pb` от остальных (см. `RccComponentProcessor.process` — падение здесь не
     * должно обрывать обработку остальных дескрипторов).
     */
    fun decode(file: File): ExternalConfig = ExternalConfigFileLoader.decode(file)
}
