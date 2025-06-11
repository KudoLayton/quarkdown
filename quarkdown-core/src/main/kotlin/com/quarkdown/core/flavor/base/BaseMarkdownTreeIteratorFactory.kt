package com.quarkdown.core.flavor.base

import com.quarkdown.core.ast.iterator.ObservableAstIterator
import com.quarkdown.core.context.MutableContext
import com.quarkdown.core.context.hooks.LinkDefinitionRegistrationHook
import com.quarkdown.core.context.hooks.presence.CodePresenceHook
import com.quarkdown.core.context.hooks.presence.MathPresenceHook
import com.quarkdown.core.context.hooks.presence.MermaidDiagramPresenceHook
import com.quarkdown.core.context.hooks.presence.TikzDiagramPresenceHook
import com.quarkdown.core.flavor.TreeIteratorFactory

/**
 * [BaseMarkdownFlavor] tree iterator factory.
 */
class BaseMarkdownTreeIteratorFactory : TreeIteratorFactory {
    override fun default(context: MutableContext): ObservableAstIterator =
        ObservableAstIterator()
            // Registers link definitions.
            .attach(LinkDefinitionRegistrationHook(context))
            // Allows loading code libraries (e.g. highlight.js syntax highlighting)
            // if at least one code block is present.
            .attach(CodePresenceHook(context))
            // Allows loading Mermaid libraries
            // if at least one diagram is present.
            .attach(MermaidDiagramPresenceHook(context))
            // Allows loading Tikz libraries
            // if at least one diagram is present.
            .attach(TikzDiagramPresenceHook(context))
            // Allows loading math libraries (e.g. KaTeX)
            // if at least one math block is present.
            .attach(MathPresenceHook(context))
}
