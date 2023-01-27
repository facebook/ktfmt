package com.facebook.ktfmt.format

import com.google.common.collect.ImmutableList
import com.google.googlejavaformat.DocBuilder
import com.google.googlejavaformat.Op

/**
 * A dummy [Op] that prevents comments from being moved ahead of it, into parent [Level]s.
 *
 * If a comment is the first thing in a [Level], [OpBuilder] moves it outside of that level during
 * [Doc] building. It does so recursively, until the comment is not the first element of the level.
 * This behaviour can be very confusing, where comments seem to absorb or ignore expected
 * indentation.
 */
object FenceCommentsOp : Op {
  val AS_LIST = ImmutableList.of<Op>(FenceCommentsOp)

  override fun add(builder: DocBuilder) {
    // Do nothing. This Op simply needs to be in the OpsBuilder.
  }
}
