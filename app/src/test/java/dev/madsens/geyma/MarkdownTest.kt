package dev.madsens.geyma

import dev.madsens.geyma.files.Markdown
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTest {

    private fun html(src: String) = Markdown.toHtml(src)

    @Test
    fun headingsBecomeHTags() {
        assertTrue(html("# Title").contains("<h1>Title</h1>"))
        assertTrue(html("### Sub section").contains("<h3>Sub section</h3>"))
        // A '#' without a following space is not a heading.
        assertFalse(html("#nothashtag").contains("<h1>"))
    }

    @Test
    fun emphasisRenders() {
        val out = html("This is **bold** and *italic*.")
        assertTrue(out.contains("<strong>bold</strong>"))
        assertTrue(out.contains("<em>italic</em>"))
    }

    @Test
    fun inlineCodeIsPreservedAndNotEmphasized() {
        val out = html("Run `a*b*c` please")
        assertTrue(out.contains("<code>a*b*c</code>"))
        assertFalse(out.contains("<em>"))
    }

    @Test
    fun plainDigitsAreNotMistakenForCodePlaceholders() {
        // Regression: an earlier sentinel scheme turned " 1 " in prose into a code
        // span. Ordinary numbered text must pass through untouched.
        val out = html("There were 1 or 2 things on step 3 here.")
        assertFalse(out.contains("<code>"))
        assertTrue(out.contains("There were 1 or 2 things on step 3 here."))
    }

    @Test
    fun textIsHtmlEscapedSoDocumentsCannotInjectMarkup() {
        val out = html("Compare a < b & c > d")
        assertTrue(out.contains("a &lt; b &amp; c &gt; d"))
        val script = html("<script>alert(1)</script>")
        assertTrue(script.contains("&lt;script&gt;"))
        assertFalse(script.contains("<script>"))
    }

    @Test
    fun linksRenderButExecutableSchemesAreDropped() {
        assertTrue(html("[Geyma](https://madsens.dev)").contains("<a href=\"https://madsens.dev\">Geyma</a>"))
        val js = html("[tap](javascript:evil)")
        assertFalse(js.contains("href"))
        assertTrue(js.contains("tap"))
    }

    @Test
    fun imagesCollapseToAltText() {
        val out = html("![a diagram](secret.png)")
        assertTrue(out.contains("a diagram"))
        assertFalse(out.contains("<img"))
        assertFalse(out.contains("secret.png"))
    }

    @Test
    fun fencedCodeBlockIsEscapedAndWrapped() {
        val out = html("```\nval x = a<b\n```")
        assertTrue(out.contains("<pre><code>"))
        assertTrue(out.contains("val x = a&lt;b"))
    }

    @Test
    fun listsAndBlockquotesAndRules() {
        val ul = html("- one\n- two")
        assertTrue(ul.contains("<ul>"))
        assertTrue(ul.contains("<li>one</li>"))
        assertTrue(ul.contains("<li>two</li>"))

        val ol = html("1. first\n2. second")
        assertTrue(ol.contains("<ol>"))
        assertTrue(ol.contains("<li>first</li>"))

        assertTrue(html("> quoted").contains("<blockquote>quoted</blockquote>"))
        assertTrue(html("---").contains("<hr>"))
    }

    @Test
    fun paragraphsWrap() {
        assertTrue(html("just a line").contains("<p>just a line</p>"))
    }
}
