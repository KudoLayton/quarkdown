package com.quarkdown.core.ast.quarkdown.block

import com.quarkdown.core.ast.Node
import com.quarkdown.core.visitor.node.NodeVisitor

/**
 * A Tikz diagram.
 * @param code Tikz code of the diagram
 */
class TikzDiagram(
    val code: String,
) : Node {
    override fun <T> accept(visitor: NodeVisitor<T>) = visitor.visit(this)
}
