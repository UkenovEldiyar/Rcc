package com.ukenoveldiyar.rcc.external.config.yaml.extension

import org.snakeyaml.engine.v2.nodes.AnchorNode
import org.snakeyaml.engine.v2.nodes.CollectionNode
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode

internal abstract class YamlNodeVisitor<out R, in D> {
    abstract fun visitNode(element: Node, data: D): R

    open fun visitCollection(node: CollectionNode<*>, data: D): R = visitNode(node, data)

    open fun visitMapping(node: MappingNode, data: D): R = visitCollection(node, data)

    open fun visitSequence(node: SequenceNode, data: D): R = visitCollection(node, data)

    open fun visitScalar(node: ScalarNode, data: D): R = visitNode(node, data)

    open fun visitAnchor(node: AnchorNode, data: D): R = visitNode(node, data)
}