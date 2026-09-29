package com.ukenoveldiyar.rcc.schema

import com.ukenoveldiyar.rcc.external.schema.ExternalAnnotationProto
import com.ukenoveldiyar.rcc.external.schema.ExternalClassProto
import com.ukenoveldiyar.rcc.external.schema.ExternalConstructorProto
import com.ukenoveldiyar.rcc.external.schema.ExternalDeclarationProto
import com.ukenoveldiyar.rcc.external.schema.ExternalFileProto
import com.ukenoveldiyar.rcc.external.schema.ExternalFunctionProto
import com.ukenoveldiyar.rcc.external.schema.ExternalFunctionTypeProto
import com.ukenoveldiyar.rcc.external.schema.ExternalPropertyProto
import com.ukenoveldiyar.rcc.external.schema.ExternalTypeProto
import com.ukenoveldiyar.rcc.external.schema.SourceRangeProto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalSerializationApi::class)
class ExternalSchemaTest {

    private fun range(start: Int, end: Int) = SourceRangeProto(start, end)

    @Test
    fun `round-trips every declaration and type ref kind through protobuf`() {
        val file = ExternalFileProto(
            sourceRange = range(0, 100),
            annotations = listOf(ExternalAnnotationProto("androidx.compose.runtime.Composable", range(0, 10))),
            declarations = listOf(
                ExternalFunctionProto(
                    fqName = "androidx.compose.foundation.layout.Column",
                    extension = null,
                    parameters = listOf(
                        ExternalTypeProto("androidx.compose.ui.Modifier", range(10, 20)),
                        ExternalFunctionTypeProto(
                            receiver = ExternalTypeProto("androidx.compose.foundation.layout.ColumnScope", range(20, 30)),
                            parameters = emptyList(),
                            returnType = ExternalTypeProto("kotlin.Unit", range(30, 35)),
                            annotations = listOf(ExternalAnnotationProto("androidx.compose.runtime.Composable", range(35, 45))),
                            sourceRange = range(20, 45),
                        ),
                    ),
                    sourceRange = range(10, 45),
                ),
                ExternalClassProto(
                    fqName = "androidx.compose.foundation.layout.ColumnScope",
                    declarations = listOf(
                        ExternalPropertyProto(
                            fqName = "androidx.compose.foundation.layout.ColumnScope.weight",
                            type = ExternalTypeProto("kotlin.Float", range(50, 55)),
                            mutable = true,
                            extension = null,
                            sourceRange = range(45, 55),
                        ),
                        ExternalConstructorProto(
                            fqName = "androidx.compose.ui.unit.Dp",
                            parameters = listOf(ExternalTypeProto("kotlin.Float", range(55, 60))),
                            sourceRange = range(55, 60),
                        ),
                    ),
                    sourceRange = range(45, 60),
                ),
            ),
            name = "Column.kt",
        )

        val bytes = ProtoBuf.encodeToByteArray(ExternalDeclarationProto.serializer(), file)
        val decoded = assertIs<ExternalFileProto>(
            ProtoBuf.decodeFromByteArray(ExternalDeclarationProto.serializer(), bytes),
        )

        assertEquals(file, decoded)
    }
}
