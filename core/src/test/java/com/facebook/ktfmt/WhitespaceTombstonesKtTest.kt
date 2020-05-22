/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl ")).isEqualTo(
        "  sdfl$SPACE_TOMBSTONE")
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  ")).isEqualTo(
        "  sdfl $SPACE_TOMBSTONE")

    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  \n skdjfh")).isEqualTo(
        "  sdfl $SPACE_TOMBSTONE\n skdjfh")
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  \n skdjfh ")).isEqualTo(
        "  sdfl $SPACE_TOMBSTONE\n skdjfh$SPACE_TOMBSTONE")
    assertThat(replaceTrailingWhitespaceWithTombstone("  sdfl  \n\n skdjfh ")).isEqualTo(
        "  sdfl $SPACE_TOMBSTONE\n\n skdjfh$SPACE_TOMBSTONE")
  }
}
