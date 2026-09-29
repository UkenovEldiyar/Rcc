package com.ukenoveldiyar.rcc.schema

import com.ukenoveldiyar.rcc.external.declarations.ExternalAnnotation
import com.ukenoveldiyar.rcc.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.external.declarations.ExternalConstructor
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.external.declarations.ExternalProperty
import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.schema.ExternalDeclarationProto
import com.ukenoveldiyar.rcc.external.schema.toDomain
import com.ukenoveldiyar.rcc.external.schema.toProto
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.source.SourceRange
import com.ukenoveldiyar.rcc.external.types.ExternalFunctionType
import com.ukenoveldiyar.rcc.external.types.ExternalType
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalSerializationApi::class)
class ExternalSchemaMappingTest {

    private fun source(start: Int, end: Int): SourceElement = SourceElement(SourceRange(start, end))

    @Test
    fun `domain round-trips through proto bytes`() {
        val file = ExternalFile(
            sourceElement = source(0, 100),
            annotations = listOf(ExternalAnnotation(FqName("androidx.compose.runtime.Composable"), source(0, 10))),
            declarations = listOf(
                ExternalFunction(
                    fqName = FqName("androidx.compose.foundation.layout.Column"),
                    extension = null,
                    parameters = listOf(
                        ExternalType(FqName("androidx.compose.ui.Modifier"), source(10, 20)),
                        ExternalFunctionType(
                            receiver = ExternalType(FqName("androidx.compose.foundation.layout.ColumnScope"), source(20, 30)),
                            parameters = emptyList(),
                            returnType = ExternalType(FqName("kotlin.Unit"), source(30, 35)),
                            annotations = listOf(ExternalAnnotation(FqName("androidx.compose.runtime.Composable"), source(35, 45))),
                            sourceElement = source(20, 45),
                        ),
                    ),
                    sourceElement = source(10, 45),
                ),
                ExternalClass(
                    fqName = FqName("androidx.compose.foundation.layout.ColumnScope"),
                    declarations = listOf(
                        ExternalProperty(
                            fqName = FqName("androidx.compose.foundation.layout.ColumnScope.weight"),
                            type = ExternalType(FqName("kotlin.Float"), source(50, 55)),
                            mutable = true,
                            extension = null,
                            sourceElement = source(45, 55),
                        ),
                        ExternalConstructor(
                            fqName = FqName("androidx.compose.ui.unit.Dp"),
                            parameters = listOf(ExternalType(FqName("kotlin.Float"), source(55, 60))),
                            sourceElement = source(55, 60),
                        ),
                    ),
                    sourceElement = source(45, 60),
                ),
            ),
            name = "Column.kt",
        )

        val bytes = ProtoBuf.encodeToByteArray(ExternalDeclarationProto.serializer(), file.toProto())
        val decoded = assertIs<ExternalFile>(
            ProtoBuf.decodeFromByteArray(ExternalDeclarationProto.serializer(), bytes).toDomain(),
        )

        assertEquals("Column.kt", decoded.name)
        assertEquals(SourceRange(0, 100), decoded.sourceElement.sourceRange)
        assertEquals(FqName("androidx.compose.runtime.Composable"), decoded.annotations.single().fqName)
        assertEquals(2, decoded.declarations.size)

        val function = assertIs<ExternalFunction>(decoded.declarations[0])
        assertEquals(FqName("androidx.compose.foundation.layout.Column"), function.fqName)
        assertNull(function.extension)
        assertEquals(FqName("androidx.compose.ui.Modifier"), assertIs<ExternalType>(function.parameters[0]).fqName)

        val lambda = assertIs<ExternalFunctionType>(function.parameters[1])
        assertEquals(
            FqName("androidx.compose.foundation.layout.ColumnScope"),
            assertIs<ExternalType>(lambda.receiver).fqName,
        )
        assertEquals(emptyList<ExternalTypeRef>(), lambda.parameters)
        assertEquals(FqName("kotlin.Unit"), assertIs<ExternalType>(lambda.returnType).fqName)
        assertEquals(FqName("androidx.compose.runtime.Composable"), lambda.annotations.single().fqName)
        assertEquals(SourceRange(20, 45), lambda.sourceElement.sourceRange)

        val cls = assertIs<ExternalClass>(decoded.declarations[1])
        val property = assertIs<ExternalProperty>(cls.declarations[0])
        assertEquals(true, property.mutable)
        assertEquals(FqName("kotlin.Float"), assertIs<ExternalType>(property.type).fqName)

        val constructor = assertIs<ExternalConstructor>(cls.declarations[1])
        assertEquals(FqName("androidx.compose.ui.unit.Dp"), constructor.fqName)
        assertEquals(FqName("kotlin.Float"), assertIs<ExternalType>(constructor.parameters.single()).fqName)
    }
}
