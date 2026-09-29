package com.ukenoveldiyar.rcc.yaml

import com.ukenoveldiyar.rcc.external.config.yaml.ExternalConfigYamlLoader
import com.ukenoveldiyar.rcc.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.types.ExternalFunctionType
import com.ukenoveldiyar.rcc.external.types.ExternalType
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef
import org.junit.jupiter.api.assertThrows
import java.io.Reader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class YamlEngineTest {

    private fun resource(name: String): Reader =
        javaClass.classLoader
            .getResourceAsStream(name)
            ?.reader()
            ?: error("Not found file")

    @Test
    fun `parse empty file`() {
        val input = resource("external-empty.yaml")
        val loader = ExternalConfigYamlLoader(input)

        assertThrows<IllegalArgumentException> {
            loader.load()
        }
    }

    @Test
    fun `parse external file`() {
        val input = resource("external-compose.yaml")
        val loader = ExternalConfigYamlLoader(input)

        val config = loader.load()

        assertEquals("", config.packageName)
        assertEquals("ColumnExtern", config.name)

        val file = assertIs<ExternalFile>(config.element)
        assertEquals("Column.kt", file.name)
        assertEquals(2, file.declarations.size)

        val function = assertIs<ExternalFunction>(file.declarations[0])
        assertEquals(FqName("androidx.compose.foundation.layout.Column"), function.fqName)
        assertEquals(null, function.extension)
        assertEquals(4, function.parameters.size)
        assertUserType("androidx.compose.ui.Modifier", function.parameters[0])
        assertUserType("androidx.compose.foundation.layout.Arrangement.Vertival", function.parameters[1])
        assertUserType("androidx.compose.ui.Alignment.Horizontal", function.parameters[2])

        val composableLambda = assertIs<ExternalFunctionType>(function.parameters[3])
        assertUserType("androidx.compose.foundation.layout.ColumnScope", composableLambda.receiver!!)
        assertEquals(emptyList(), composableLambda.parameters)
        assertUserType("kotlin.Unit", composableLambda.returnType)
        assertEquals(listOf(FqName("androidx.compose.runtime.Composable")), composableLambda.annotations.map { it.fqName })

        val cls = assertIs<ExternalClass>(file.declarations[1])
        assertEquals(FqName("androidx.compose.foundation.layout.ColumnScope"), cls.fqName)
        assertEquals(1, cls.declarations.size)

        val nestedFunction = assertIs<ExternalFunction>(cls.declarations[0])
        assertEquals(FqName("androidx.compose.foundation.layout.weight"), nestedFunction.fqName)
        assertUserType("androidx.compose.ui.Modifier", nestedFunction.extension!!)
        assertEquals(
            listOf(FqName("kotlin.Float"), FqName("kotlin.Boolean")),
            nestedFunction.parameters.map { assertIs<ExternalType>(it).fqName },
        )
    }

    private fun assertUserType(expectedFqName: String, actual: ExternalTypeRef) {
        assertEquals(FqName(expectedFqName), assertIs<ExternalType>(actual).fqName)
    }
}
