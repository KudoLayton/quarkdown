package com.quarkdown.core.ast.attributes.presence

import com.quarkdown.core.ast.attributes.AstAttributes
import com.quarkdown.core.ast.attributes.MutableAstAttributes
import com.quarkdown.core.property.Property

/**
 * If this property is present in [com.quarkdown.core.ast.attributes.AstAttributes.thirdPartyPresenceProperties]
 * and its [value] is true, it means there is at least one Tikz diagram in the AST.
 * This is used to load the Tikz library in HTML rendering only if necessary.
 * @see com.quarkdown.core.context.hooks.presence.TikzDiagramPresenceHook
 */
data class TikzDiagramPresenceProperty(
    override val value: Boolean,
) : Property<Boolean> {
    companion object : Property.Key<Boolean>

    override val key: Property.Key<Boolean> = TikzDiagramPresenceProperty
}

/**
 * Whether there is at least one Tikz diagram in the AST.
 * @see TikzDiagramPresenceProperty
 */
val AstAttributes.hasTikzDiagram: Boolean
    get() = hasThirdParty(TikzDiagramPresenceProperty)

/**
 * Marks the presence of Tikz diagrams in the AST
 * if at least one diagram is present in the document.
 * @see TikzDiagramPresenceProperty
 * @see com.quarkdown.core.context.hooks.presence.TikzDiagramPresenceHook
 */
fun MutableAstAttributes.markTikzDiagramPresence() = markThirdPartyPresence(TikzDiagramPresenceProperty(true))
