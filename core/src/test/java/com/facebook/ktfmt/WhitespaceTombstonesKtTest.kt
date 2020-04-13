// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.ktfmt

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class WhitespaceTombstonesKtTest {

  @Test
  fun testReplaceTrailingWhitespaceWithTombstone() {
    assertThat(replaceTrailingWhitespaceWithTombstone("")).isEqualTo("")

    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl")).isEqualTo("  sdfl")
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl "))
        .isEqualTo("  sdfl$SPACE_TOMBSTONE")
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  "))
        .isEqualTo("  sdfl $SPACE_TOMBSTONE")

    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  \n skdjfh"))
        .isEqualTo("  sdfl $SPACE_TOMBSTONE\n skdjfh")
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  \n skdjfh "))
        .isEqualTo("  sdfl $SPACE_TOMBSTONE\n skdjfh$SPACE_TOMBSTONE")
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  \n\n skdjfh "))
        .isEqualTo("  sdfl $SPACE_TOMBSTONE\n\n skdjfh$SPACE_TOMBSTONE")
  }
}
