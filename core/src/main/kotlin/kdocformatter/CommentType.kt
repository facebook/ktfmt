package kdocformatter

enum class CommentType(
    /** The opening string of the comment. */
    val prefix: String,
    /** The closing string of the comment. */
    val suffix: String,
    /**
     * For multi line comments, the prefix at each comment line after
     * the first one.
     */
    val linePrefix: String
) {
    KDOC("/**", "*/", " * "),
    BLOCK("/*", "*/", ""),
    LINE("//", "", "// ");

    /**
     * The number of characters needed to fit a comment on a line: the
     * prefix, suffix and a single space padding inside these.
     */
    fun singleLineOverhead(): Int {
        return prefix.length + suffix.length + 1 + if (suffix.isEmpty()) 0 else 1
    }

    /**
     * The number of characters required in addition to the line comment
     * for each line in a multi line comment.
     */
    fun lineOverhead(): Int {
        return linePrefix.length
    }
}

fun String.isKDocComment(): Boolean = startsWith("/**")

fun String.isBlockComment(): Boolean = startsWith("/*") && !startsWith("/**")

fun String.isLineComment(): Boolean = startsWith("//")

fun String.commentType(): CommentType {
    return if (isKDocComment()) {
        CommentType.KDOC
    } else if (isBlockComment()) {
        CommentType.BLOCK
    } else if (isLineComment()) {
        CommentType.LINE
    } else {
        error("Not a comment: $this")
    }
}
