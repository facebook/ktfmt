// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.ktfmt

/** See [replaceTrailingWhitespaceWithTombstone]. */
private const val SPACE_TOMBSTONE = '\u0003'

fun String.indexOfWhitespaceTombstone() = this.indexOf(SPACE_TOMBSTONE)

/**
 * Google-java-format removes trailing spaces when it emits formatted code, which is a problem for
 * multiline string literals. We trick it by replacing the last trailing space in such cases with a
 * tombstone, a character that's unlikely to be used in a regular program. After formatting, we
 * replace it back to a space.
 */
fun replaceTrailingWhitespaceWithTombstone(s: String): String {
  if (s.isEmpty()) {
    return s
  }
  return if (s.last() == ' ') s.substring(0, s.length - 1) + SPACE_TOMBSTONE else s
}

/** See [replaceTrailingWhitespaceWithTombstone]. */
fun replaceTombstoneWithTrailingWhitespace(s: String): String {
  return s.replace(SPACE_TOMBSTONE, ' ')
}
