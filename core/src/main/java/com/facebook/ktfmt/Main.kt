// Copyright (c) Facebook, Inc. and its affiliates.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.facebook.ktfmt

import com.google.googlejavaformat.FormattingError
import java.io.File
import java.io.IOException

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    println("Usage: ktfmt File1.kt File2.kt ...")
    return
  }

  for (fileName in args) try {
    val code = File(fileName).readText()
    File(fileName).writeText(format(code))
    println("Done formatting $fileName")
  } catch (e: IOException) {
    println("Error formatting $fileName: ${e.message}; skipping.")
  } catch (e: FormattingError) {
    println("Formatting Error when processing $fileName: ${e.message}; skipping.")
    if (args.size == 1) {
      e.printStackTrace()
    }
  }
}
