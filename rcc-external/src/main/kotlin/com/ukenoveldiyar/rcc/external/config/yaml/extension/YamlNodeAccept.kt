package com.ukenoveldiyar.rcc.external.config.yaml.extension

import org.snakeyaml.engine.v2.nodes.AnchorNode
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal fun <R, D> Node.accept(visitor: YamlNodeVisitor<R, D>, data: D): R = when (this) {
    is ScalarNode -> visitor.visitScalar(this, data)
    is SequenceNode -> visitor.visitSequence(this, data)
    is MappingNode -> visitor.visitMapping(this, data)
    is AnchorNode -> visitor.visitAnchor(this, data)
    else -> error("Not support node type: $this")
}