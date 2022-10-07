package kdocformatter

/**
 * A list of paragraphs. Each paragraph should start on a new line
 * and end with a newline. In addition, if a paragraph is marked with
 * "separate=true", we'll insert an extra blank line in front of it.
 */
class ParagraphList(private val paragraphs: List<Paragraph>) : Iterable<Paragraph> {
    fun isSingleParagraph() = paragraphs.size <= 1
    override fun iterator(): Iterator<Paragraph> = paragraphs.iterator()
    override fun toString(): String = paragraphs.joinToString { it.content }
}
