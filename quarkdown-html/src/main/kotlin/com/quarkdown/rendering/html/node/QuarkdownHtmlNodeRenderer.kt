package com.quarkdown.rendering.html.node

import com.quarkdown.core.ast.AstRoot
import com.quarkdown.core.ast.Node
import com.quarkdown.core.ast.attributes.id.getId
import com.quarkdown.core.ast.attributes.location.LocationTrackableNode
import com.quarkdown.core.ast.attributes.location.formatLocation
import com.quarkdown.core.ast.attributes.location.getLocationLabel
import com.quarkdown.core.ast.base.block.BlockQuote
import com.quarkdown.core.ast.base.block.Heading
import com.quarkdown.core.ast.base.block.Table
import com.quarkdown.core.ast.base.inline.CodeSpan
import com.quarkdown.core.ast.dsl.buildInline
import com.quarkdown.core.ast.quarkdown.CaptionableNode
import com.quarkdown.core.ast.quarkdown.FunctionCallNode
import com.quarkdown.core.ast.quarkdown.block.Box
import com.quarkdown.core.ast.quarkdown.block.Clipped
import com.quarkdown.core.ast.quarkdown.block.Collapse
import com.quarkdown.core.ast.quarkdown.block.Container
import com.quarkdown.core.ast.quarkdown.block.Figure
import com.quarkdown.core.ast.quarkdown.block.FullColumnSpan
import com.quarkdown.core.ast.quarkdown.block.Math
import com.quarkdown.core.ast.quarkdown.block.MermaidDiagram
import com.quarkdown.core.ast.quarkdown.block.Numbered
import com.quarkdown.core.ast.quarkdown.block.PageBreak
import com.quarkdown.core.ast.quarkdown.block.SlidesFragment
import com.quarkdown.core.ast.quarkdown.block.Stacked
import com.quarkdown.core.ast.quarkdown.block.TikzDiagram
import com.quarkdown.core.ast.quarkdown.block.list.FocusListItemVariant
import com.quarkdown.core.ast.quarkdown.block.list.LocationTargetListItemVariant
import com.quarkdown.core.ast.quarkdown.block.toc.TableOfContentsView
import com.quarkdown.core.ast.quarkdown.block.toc.convertToListNode
import com.quarkdown.core.ast.quarkdown.inline.InlineCollapse
import com.quarkdown.core.ast.quarkdown.inline.MathSpan
import com.quarkdown.core.ast.quarkdown.inline.PageCounter
import com.quarkdown.core.ast.quarkdown.inline.TextTransform
import com.quarkdown.core.ast.quarkdown.inline.Whitespace
import com.quarkdown.core.ast.quarkdown.invisible.PageMarginContentInitializer
import com.quarkdown.core.ast.quarkdown.invisible.SlidesConfigurationInitializer
import com.quarkdown.core.context.Context
import com.quarkdown.core.context.localization.localizeOrNull
import com.quarkdown.core.context.shouldAutoPageBreak
import com.quarkdown.core.document.layout.caption.CaptionPosition
import com.quarkdown.core.document.layout.caption.CaptionPositionInfo
import com.quarkdown.core.document.numbering.DocumentNumbering
import com.quarkdown.core.document.numbering.NumberingFormat
import com.quarkdown.core.rendering.tag.buildMultiTag
import com.quarkdown.core.rendering.tag.buildTag
import com.quarkdown.core.rendering.tag.tagBuilder
import com.quarkdown.rendering.html.HtmlIdentifierProvider
import com.quarkdown.rendering.html.HtmlTagBuilder
import com.quarkdown.rendering.html.css.asCSS

/**
 * A renderer for Quarkdown ([com.quarkdown.core.flavor.quarkdown.QuarkdownFlavor]) nodes that exports their content into valid HTML code.
 * @param context additional information produced by the earlier stages of the pipeline
 */
class QuarkdownHtmlNodeRenderer(
    context: Context,
) : BaseHtmlNodeRenderer(context) {
    /**
     * A `<div class="styleClass">...</div>` tag.
     */
    private fun div(
        styleClass: String? = null,
        init: HtmlTagBuilder.() -> Unit,
    ) = tagBuilder("div", init = init)
        .className(styleClass)
        .build()

    /**
     * A `<div class="styleClass">children</div>` tag.
     */
    private fun div(
        styleClass: String,
        children: List<Node>,
    ) = div(styleClass) { +children }

    /**
     * Adds a `data-location` attribute to the location-trackable node, if its location is available.
     * The location is formatted according to [format].
     */
    private fun HtmlTagBuilder.location(
        node: LocationTrackableNode,
        format: (DocumentNumbering) -> NumberingFormat?,
    ) = optionalAttribute(
        "data-location",
        node
            .takeIf { context.options.enableLocationAwareness } // Location lookup could be disabled by settings.
            ?.formatLocation(context, format)
            ?.takeUnless { it.isEmpty() },
    )

    /**
     * Retrieves the location-based label of the [node], displays an optional caption preceded by the label, and also applies the label as its ID.
     * The label is pre-formatted according to the current [NumberingFormat].
     *
     * At the end, thanks to injected CSS variables, the visible outcome is `<localized_kind> <label>: <caption>`.
     *
     * @param node node to display the caption, and apply the ID, for
     * @param captionTagName tag name of the caption element. E.g. "figcaption" for figures, "caption" for tables
     * @param kindLocalizationKey localization key for the kind of the element. E.g. "figure" for figures, "table" for tables.
     * It allows localizing the kind name depending on the current locale.
     * @param idPrefix prefix for the ID. For instance, the prefix `figure` lets the ID be `figure-X.Y`, where `X.Y` is the label.
     * @see CaptionableNode
     * @see getLocationLabel to retrieve the numbered label
     */
    private fun HtmlTagBuilder.numberedCaption(
        node: CaptionableNode,
        captionTagName: String,
        kindLocalizationKey: String,
        idPrefix: String = kindLocalizationKey,
        position: CaptionPosition,
    ): HtmlTagBuilder {
        // The location-based, numbering format dependent identifier of the node, e.g. 1.1.
        val label = (node as? LocationTrackableNode)?.getLocationLabel(context)

        return this.apply {
            // The label is set as the ID of the element.
            label?.let { optionalAttribute("id", "$idPrefix-$it") }

            node.caption?.let { caption ->
                +buildTag(captionTagName) {
                    className("caption-${position.asCSS}")

                    +escapeCriticalContent(caption)
                    // The label is set as an attribute for styling.
                    optionalAttribute("data-element-label", label)
                    // Localized name of the element (e.g. `figure` -> `Figure` for English locale).
                    optionalAttribute("data-localized-kind", context.localizeOrNull(key = kindLocalizationKey))
                }
            }
        }
    }

    // Quarkdown node rendering

    // The function was already expanded by previous stages: its output nodes are stored in its children.
    override fun visit(node: FunctionCallNode): CharSequence = visit(AstRoot(node.children))

    // Block

    override fun visit(node: Figure<*>) =
        buildTag("figure") {
            +node.child

            // Figure ID, e.g. 1.1, based on the current numbering format.
            this.numberedCaption(
                node,
                "figcaption",
                kindLocalizationKey = "figure",
                position =
                    context.documentInfo.layout.captionPosition
                        .getOrDefault(CaptionPositionInfo::figures),
            )
        }

    // An empty div that acts as a page break.
    override fun visit(node: PageBreak) =
        tagBuilder("div")
            .className("page-break")
            .hidden()
            .build()

    override fun visit(node: Math) =
        buildTag("formula") {
            +node.expression
            attribute("data-block", "")
        }

    override fun visit(node: Container) =
        buildTag("div") {
            classNames(
                "container",
                "fullwidth".takeIf { node.fullWidth },
                "float".takeIf { node.float != null },
            )

            +node.children

            style {
                "width" value node.width
                "height" value node.height
                "color" value node.foregroundColor
                "background-color" value node.backgroundColor
                "margin" value node.margin
                "padding" value node.padding
                "border-color" value node.borderColor
                "border-width" value node.borderWidth
                "border-radius" value node.cornerRadius

                "border-style" value
                    when {
                        // If the border style is set, it is used.
                        node.borderStyle != null -> node.borderStyle
                        // If border properties are set, a normal (solid) border is used.
                        node.borderColor != null || node.borderWidth != null -> Container.BorderStyle.NORMAL
                        // No border style.
                        else -> null
                    }

                "justify-items" value node.alignment
                "text-align" value node.textAlignment
                "float" value node.float
            }
        }

    override fun visit(node: Stacked) =
        div("stack stack-${node.layout.asCSS}") {
            +node.children

            style {
                (node.layout as? Stacked.Grid)?.let {
                    // The amount of 'auto' matches the amount of columns/rows.
                    "grid-template-columns" value "auto ".repeat(it.columnCount).trimEnd()
                }

                "justify-content" value node.mainAxisAlignment
                "align-items" value node.crossAxisAlignment
                "gap" value node.gap
            }
        }

    override fun visit(node: Numbered) =
        buildMultiTag {
            +node.children
        }

    override fun visit(node: FullColumnSpan) = div("full-column-span", node.children)

    override fun visit(node: Clipped) =
        div("clip clip-${node.clip.asCSS}") {
            +Container(children = node.children)
        }

    override fun visit(node: Box) =
        div {
            classNames("box", node.type.asCSS)

            if (node.title != null) {
                tag("header") {
                    tag("h4", node.title!!)

                    style {
                        "color" value node.foregroundColor // Must be repeated to force override.
                        "padding" value node.padding
                    }
                }
            }

            // Box actual content.
            +div("box-content") {
                +node.children

                style { "padding" value node.padding }
            }

            // Box style. Padding is applied separately to the header and the content.
            style {
                "background-color" value node.backgroundColor
                "color" value node.foregroundColor
            }
        }

    override fun visit(node: Collapse) =
        buildTag("details") {
            if (node.isOpen) {
                attribute("open", "")
            }

            tag("summary") { +node.title }
            +node.children
        }

    override fun visit(node: Whitespace) =
        // If at least one of the dimensions is set, the square will have a fixed size.
        // Otherwise, a blank character is rendered.
        when {
            node.width == null && node.height == null -> {
                buildTag("span", "&nbsp;")
            }

            else -> {
                buildTag("div") {
                    style {
                        "width" value node.width
                        "height" value node.height
                    }
                }
            }
        }

    override fun visit(node: TableOfContentsView): CharSequence {
        val tableOfContents = context.attributes.tableOfContents ?: return ""

        return buildMultiTag {
            // Localized title.
            val titleText = context.localizeOrNull(key = "tableofcontents")

            // Title heading. Its content is either the node's user-set title or a default localized one.
            +Heading(
                depth = 1,
                text = node.title ?: buildInline { titleText?.let { text(it) } },
                customId = "table-of-contents",
            )

            // Content
            +buildTag("nav") {
                +node.convertToListNode(
                    this@QuarkdownHtmlNodeRenderer,
                    tableOfContents.items,
                    linkUrlMapper = { item ->
                        "#" + HtmlIdentifierProvider.Companion.of(this@QuarkdownHtmlNodeRenderer).getId(item.target)
                    },
                )
            }
        }
    }

    override fun visit(node: MermaidDiagram) =
        buildTag("pre") {
            classNames("mermaid", "fill-height")
            +escapeCriticalContent(node.code)
        }

    override fun visit(node: TikzDiagram) =
        buildTag("script") {
            attribute("type", "text/tikz")
            +escapeCriticalContent(node.code)
        }

    // Inline

    override fun visit(node: MathSpan) = buildTag("formula", node.expression)

    override fun visit(node: SlidesFragment): CharSequence =
        tagBuilder("div", node.children)
            .classNames("fragment", node.behavior.asCSS)
            .build()

    override fun visit(node: TextTransform) =
        buildTag("span") {
            +node.children

            className(node.data.size?.asCSS) // e.g. 'size-small' class

            style {
                "font-weight" value node.data.weight
                "font-style" value node.data.style
                "font-variant" value node.data.variant
                "text-decoration" value node.data.decoration
                "text-transform" value node.data.case
                "color" value node.data.color
            }
        }

    override fun visit(node: InlineCollapse) =
        buildTag("span") {
            // Dynamic behavior is handled by JS.
            className("inline-collapse")
            attribute("data-full-text", buildMultiTag { +node.text })
            attribute("data-collapsed-text", buildMultiTag { +node.placeholder })
            attribute("data-collapsed", !node.isOpen)
            +(if (node.isOpen) node.text else node.placeholder)
        }

    // Invisible nodes

    override fun visit(node: PageMarginContentInitializer) =
        // HTML content.
        // In slides and paged documents, these elements are copied to each page through the slides.js or paged.js script.
        div("page-margin-content page-margin-${node.position.asCSS}", node.children)

    override fun visit(node: PageCounter) =
        // The current or total page number.
        // The actual number is filled by a script at runtime
        // (either slides.js or paged.js, depending on the document type).
        buildTag("span") {
            +"-" // The default placeholder in case it is not filled by a script (e.g. plain documents).
            className(
                when (node.target) {
                    PageCounter.Target.CURRENT -> "current-page-number"
                    PageCounter.Target.TOTAL -> "total-page-number"
                },
            )
        }

    override fun visit(node: SlidesConfigurationInitializer): CharSequence =
        buildTag("script") {
            // Inject properties that are read by the slides.js script after the document is loaded.
            +buildString {
                node.centerVertically?.let {
                    append("const slides_center = $it;")
                }
                node.showControls?.let {
                    append("const slides_showControls = $it;")
                }
                node.transition?.let {
                    append("const slides_transitionStyle = '${it.style.asCSS}';")
                    append("const slides_transitionSpeed = '${it.speed.asCSS}';")
                }
            }
        }

    // Additional behavior of base nodes

    // On top of the default behavior, an anchor ID is set,
    // and it could force an automatic page break if suitable.
    override fun visit(node: Heading): String {
        val tagBuilder =
            when {
                // When a heading has a depth of 0 (achievable only via functions), it is an invisible marker with an ID.
                node.isMarker ->
                    tagBuilder("div") {
                        className("marker")
                        hidden()
                    }
                // Regular headings.
                else -> tagBuilder("h${node.depth}", node.text)
            }

        // The heading tag itself.
        val tag =
            tagBuilder
                .optionalAttribute(
                    "id",
                    // Generate an automatic identifier if allowed by settings.
                    HtmlIdentifierProvider.Companion
                        .of(renderer = this)
                        .takeIf { context.options.enableAutomaticIdentifiers || node.customId != null }
                        ?.getId(node),
                ).location(node, DocumentNumbering::headings)
                .build()

        return buildMultiTag {
            if (context.shouldAutoPageBreak(node)) {
                +PageBreak()
            }
            +tag
        }
    }

    // On top of the base behavior, a blockquote can have a type and an attribution.
    override fun visit(node: BlockQuote) =
        buildTag("blockquote") {
            // If the quote has a type (e.g. TIP),
            // the whole quote is marked as a 'tip' blockquote
            // and a localized label is shown (e.g. 'Tip:' for English).
            node.type?.asCSS?.let { type ->
                className(type)
                // The type is associated to a localized label
                // only if the documant language is set and the set language is supported.
                context.localizeOrNull(key = type)?.let { localizedLabel ->
                    // The localized label is set as a CSS variable.
                    // Themes can customize label appearance and formatting.
                    style { "--quote-type-label" value "'$localizedLabel'" }
                    // The quote is marked as labeled to allow further customization.
                    attribute("data-labeled", "")
                }
            }

            +node.children
            node.attribution?.let {
                +tagBuilder("p", it)
                    .className("attribution")
                    .build()
            }
        }

    // Quarkdown introduces table captions, also numerated.
    override fun visit(node: Table) =
        super
            .tableBuilder(node)
            .apply {
                numberedCaption(
                    node,
                    "caption",
                    kindLocalizationKey = "table",
                    position =
                        context.documentInfo.layout.captionPosition
                            .getOrDefault(CaptionPositionInfo::tables),
                )
            }.build()

    // A code span can contain additional content, such as a color preview.
    override fun visit(node: CodeSpan): String {
        val codeTag = super.visit(node)

        // The code is wrapped to allow additional content.
        return buildTag("span") {
            className("codespan-content")

            +codeTag

            when (val content = node.content) {
                null -> {} // No additional content.
                is CodeSpan.ColorContent -> {
                    // If the code contains a color code, show the color preview.
                    +buildTag("span") {
                        style { "background-color" value content.color }
                        className("color-preview")
                    }
                }
            }
        }
    }

    // List item variants.

    override fun visit(variant: FocusListItemVariant): HtmlTagBuilder.() -> Unit =
        {
            if (variant.isFocused) {
                className("focused")
            }
        }

    override fun visit(variant: LocationTargetListItemVariant): HtmlTagBuilder.() -> Unit = { location(variant.target, variant.format) }
}
