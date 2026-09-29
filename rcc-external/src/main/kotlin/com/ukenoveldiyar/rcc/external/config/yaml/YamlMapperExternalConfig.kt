package com.ukenoveldiyar.rcc.external.config.yaml

import com.ukenoveldiyar.rcc.external.config.ExternalConfig
import com.ukenoveldiyar.rcc.external.config.yaml.YamlMapperExternalConfig.Visitor.Companion.EXTERNAL_ENTRY
import com.ukenoveldiyar.rcc.external.config.yaml.YamlMapperExternalConfig.Visitor.Companion.NAME_SCALAR
import com.ukenoveldiyar.rcc.external.config.yaml.YamlMapperExternalConfig.Visitor.Companion.PACKAGE_SCALAR
import com.ukenoveldiyar.rcc.external.config.yaml.YamlMapperExternalConfig.Visitor.Companion.RESERVED_KEYS
import com.ukenoveldiyar.rcc.external.config.yaml.extension.YamlNodeVisitor
import com.ukenoveldiyar.rcc.external.config.yaml.extension.accept
import com.ukenoveldiyar.rcc.external.config.yaml.extension.asMapNode
import com.ukenoveldiyar.rcc.external.config.yaml.extension.asScalarNode
import com.ukenoveldiyar.rcc.external.config.yaml.extension.asSequenceNode
import com.ukenoveldiyar.rcc.external.config.yaml.extension.component1
import com.ukenoveldiyar.rcc.external.config.yaml.extension.component2
import com.ukenoveldiyar.rcc.external.config.yaml.extension.entry
import com.ukenoveldiyar.rcc.external.config.yaml.extension.entryOrNull
import com.ukenoveldiyar.rcc.external.config.yaml.extension.scalar
import com.ukenoveldiyar.rcc.external.config.yaml.extension.scalarOrNull
import com.ukenoveldiyar.rcc.external.declarations.ExternalAnnotation
import com.ukenoveldiyar.rcc.external.declarations.ExternalClass
import com.ukenoveldiyar.rcc.external.declarations.ExternalConstructor
import com.ukenoveldiyar.rcc.external.declarations.ExternalDeclaration
import com.ukenoveldiyar.rcc.external.declarations.ExternalFile
import com.ukenoveldiyar.rcc.external.declarations.ExternalFunction
import com.ukenoveldiyar.rcc.external.declarations.ExternalProperty
import com.ukenoveldiyar.rcc.external.names.FqName
import com.ukenoveldiyar.rcc.external.source.SourceElement
import com.ukenoveldiyar.rcc.external.source.SourceRange
import com.ukenoveldiyar.rcc.external.types.ExternalTypeRef
import com.ukenoveldiyar.rcc.external.types.parser.ExternalTypeRefParser
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node

class YamlMapperExternalConfig {

    fun map(root: Node): ExternalConfig {
        val external = root
            .asMapNode()
            .entry(EXTERNAL_ENTRY)
            .asMapNode()

        val visitor = Visitor()

        val [typeKey, typeValue] = external.value.first { [keyNode, _] ->
            keyNode.asScalarNode().value !in RESERVED_KEYS
        }

        return ExternalConfig(
            packageName = external.scalar(PACKAGE_SCALAR),
            name = external.scalar(NAME_SCALAR),
            element = typeValue.accept(visitor, typeKey.asScalarNode().value),
        )
    }

    private class Visitor : YamlNodeVisitor<ExternalDeclaration, String>() {

        override fun visitNode(element: Node, data: String): ExternalDeclaration =
            error("Unsupported node for declaration type $data: $element")

        override fun visitMapping(node: MappingNode, data: String): ExternalDeclaration =
            when (data) {
                FILE_NAME -> node.toFile()
                INTERFACE_NAME, CLASS_NAME -> node.toClass()
                FUNCTION_NAME -> node.toFunction()
                PROPERTY_NAME -> node.toProperty()
                CONSTRUCTOR_NAME -> node.toConstructor()
                else -> error("Unknown declaration type: \"$data\"")
            }

        private fun generate(node: Node): ExternalDeclaration {
            val [keyNode, valueNode] = node
                .asMapNode()
                .value
                .single()

            return valueNode.accept(this@Visitor, keyNode.asScalarNode().value)
        }

        private fun MappingNode.toDeclarations(): List<ExternalDeclaration> =
            entryOrNull(DECLARATION_NAME)
                ?.asSequenceNode()
                ?.value
                ?.map { generate(it) }
                ?: emptyList()

        private fun MappingNode.toFile(): ExternalFile = ExternalFile(
            sourceElement = sourceElement(),
            annotations = toAnnotations(),
            declarations = toDeclarations(),
            name = scalar(NAME_SCALAR)
        )

        private fun MappingNode.toClass(): ExternalClass = ExternalClass(
            fqName = FqName(scalar(FQNAME_SCALAR)),
            declarations = toDeclarations(),
            sourceElement = sourceElement(),
        )

        private fun MappingNode.toFunction(): ExternalFunction = ExternalFunction(
            fqName = FqName(scalar(FQNAME_SCALAR)),
            extension = toExtension(),
            parameters = toParameters(),
            sourceElement = sourceElement(),
        )

        private fun MappingNode.toProperty(): ExternalProperty = ExternalProperty(
            fqName = FqName(scalar(FQNAME_SCALAR)),
            type = entry(TYPE_SCALAR).toTypeRef(),
            mutable = scalarOrNull(MUTABLE_SCALAR)?.toBoolean() ?: false,
            extension = toExtension(),
            sourceElement = sourceElement(),
        )

        private fun MappingNode.toConstructor(): ExternalConstructor = ExternalConstructor(
            fqName = FqName(scalar(FQNAME_SCALAR)),
            parameters = toParameters(),
            sourceElement = sourceElement(),
        )

        private fun MappingNode.toExtension(): ExternalTypeRef? =
            entryOrNull(EXTENSION_SCALAR)?.toTypeRef()

        private fun MappingNode.toParameters(): List<ExternalTypeRef> =
            entryOrNull(PARAMETERS_NAME)
                ?.asSequenceNode()
                ?.value
                ?.map { it.toTypeRef() }
                ?: emptyList()

        private fun Node.toTypeRef(): ExternalTypeRef =
            ExternalTypeRefParser(asScalarNode().value, sourceElement()).parse()

        private fun MappingNode.toAnnotations(): List<ExternalAnnotation> =
            entryOrNull(ANNOTATIONS_NAME)
                ?.asSequenceNode()
                ?.value
                ?.map { item ->
                    ExternalAnnotation(
                        fqName = FqName(item.asScalarNode().value),
                        sourceElement = item.sourceElement(),
                    )
                }
                ?: emptyList()

        private fun Node.sourceElement(): SourceElement {
            val startOffset = startMark.orElse(null)?.index ?: 0
            val endOffset = endMark.orElse(null)?.index ?: startOffset
            return SourceElement(SourceRange(startOffset, endOffset))
        }

        companion object {
            const val EXTERNAL_ENTRY = "external"
            const val NAME_SCALAR = "name"
            const val FQNAME_SCALAR = "fqName"
            const val PACKAGE_SCALAR = "package"
            const val DECLARATION_NAME = "declaration"
            const val ANNOTATIONS_NAME = "annotations"
            const val EXTENSION_SCALAR = "extension"
            const val PARAMETERS_NAME = "parameters"
            const val TYPE_SCALAR = "type"
            const val MUTABLE_SCALAR = "mutable"

            const val FILE_NAME = "file"
            const val FUNCTION_NAME = "function"
            const val INTERFACE_NAME = "interface"
            const val CLASS_NAME = "class"
            const val PROPERTY_NAME = "property"
            const val CONSTRUCTOR_NAME = "constructor"

            val RESERVED_KEYS = setOf(PACKAGE_SCALAR, NAME_SCALAR)
        }
    }
}
