package com.quarkdown.test

import com.quarkdown.core.ast.attributes.presence.hasTikzDiagram
import com.quarkdown.test.util.execute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TIKZ_OPEN = "<figure><script type=\"text/tikz\">"
private const val TIKZ_CLOSE = "</script></figure>"

class TikzTest {
    private fun String.escape() = replace("<", "&lt;").replace(">", "&gt;")

    @Test
    fun tikz() {
        execute(
            """
            .tikz
                \begin{tikzpicture}
                \draw (0, 0) circle (1in);
                \end{tikzpicture}
            """.trimIndent(),
        ) {
            assertEquals(
                TIKZ_OPEN +
                    "\\begin{tikzpicture}\n\\draw (0, 0) circle (1in);\n\\end{tikzpicture}".escape() +
                    TIKZ_CLOSE,
                it,
            )
            assertTrue(attributes.hasTikzDiagram)
        }
    }

    @Test
    fun `tikz with caption`() {
        execute(
            """
            .tikz caption:{My graph}
                \begin{tikzpicture}
                \draw (0, 0) circle (1in);
                \end{tikzpicture}
            """.trimIndent(),
        ) {
            assertEquals(
                TIKZ_OPEN +
                    "\\begin{tikzpicture}\n\\draw (0, 0) circle (1in);\n\\end{tikzpicture}".escape() +
                    "</script><figcaption class=\"caption-bottom\">My graph</figcaption></figure>",
                it,
            )
            assertTrue(attributes.hasTikzDiagram)
        }
    }
}
