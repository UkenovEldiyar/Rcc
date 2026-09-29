package com.ukenoveldiyar.rcc.external.config.yaml.extension

import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.NodeTuple
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal operator fun NodeTuple.component1(): Node = keyNode
internal operator fun NodeTuple.component2(): Node = valueNode

internal fun Node.asMapNode(): MappingNode = this as MappingNode
internal fun Node.asSequenceNode(): SequenceNode = this as SequenceNode
internal fun Node.asScalarNode(): ScalarNode = this as ScalarNode

internal fun MappingNode.entryOrNull(key: String): Node? = value.firstOrNull { [keyNode, _] -> keyNode.asScalarNode().value == key }?.valueNode

internal fun MappingNode.entry(key: String): Node = entryOrNull(key) ?: error("Not found key: $key")

internal fun MappingNode.scalarOrNull(key: String): String? = entryOrNull(key)?.asScalarNode()?.value

internal fun MappingNode.scalar(key: String): String = entry(key).asScalarNode().value

internal fun MappingNode.stringList(key: String): List<String> =
    (entryOrNull(key) as? SequenceNode)?.value?.map { it.asScalarNode().value } ?: emptyList()