package com.quarkdown.stdlib

import com.quarkdown.core.ast.quarkdown.block.TikzDiagram
import com.quarkdown.core.ast.quarkdown.block.TikzDiagramFigure
import com.quarkdown.core.function.library.loader.Module
import com.quarkdown.core.function.library.loader.moduleOf
import com.quarkdown.core.function.value.data.EvaluableString
import com.quarkdown.core.function.value.wrappedAsValue

/**
 * `Tikz` stdlib module exporter.
 * This module handles generation of Tikz diagrams.
 */
val Tikz: Module =
    moduleOf(
        ::tikz,
    )

private fun tikzFigure(
    caption: String?,
    code: String,
) = TikzDiagramFigure(
    TikzDiagram(code),
    caption,
).wrappedAsValue()

/**
 * Creates a Tikz diagram.
 * @param caption optional caption. If a caption is present, the diagram will be numbered as a figure.
 * @param code the Tikz code of the diagram
 * @return a new [TikzDiagramFigure] node
 */
fun tikz(
    caption: String? = null,
    code: EvaluableString,
) = tikzFigure(caption, code.content)
