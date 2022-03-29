/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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

package com.facebook.ktfmt.cli

import com.facebook.ktfmt.format.Formatter
import com.facebook.ktfmt.format.FormattingOptions
import com.facebook.ktfmt.format.FormattingOptions.Style
import org.ec4j.core.Cache
import org.ec4j.core.Cache.Caches
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.Resource.Resources
import org.ec4j.core.ResourcePath.ResourcePaths
import org.ec4j.core.ResourceProperties
import org.ec4j.core.ResourcePropertiesService
import org.ec4j.core.model.Property
import org.ec4j.core.model.PropertyType
import org.ec4j.core.model.PropertyType.IndentStyleValue
import org.ec4j.core.model.PropertyType.IndentStyleValue.space
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.math.absoluteValue

/** ParsedArgs holds the arguments passed to ktfmt on the command-line, after parsing. */
data class ParsedArgs(
    val fileNames: List<String>,
    val formattingOptions: FormattingOptions,
    /**
     * Run the formatter without writing changes to any files. This will print the path of any files
     * that would be changed if the formatter is run normally.
     */
    val dryRun: Boolean,

    /** Return exit code 1 if any formatting changes are detected. */
    val setExitIfChanged: Boolean,
) {
  companion object {

    fun processArgs(err: PrintStream, args: Array<String>): ParsedArgs {
      if (args.size == 1 && args[0].startsWith("@")) {
        return parseOptions(err, File(args[0].substring(1)).readLines().toTypedArray())
      } else {
        return parseOptions(err, args)
      }
    }

    /** parseOptions parses command-line arguments passed to ktfmt. */
    fun parseOptions(err: PrintStream, args: Array<String>): ParsedArgs {
      val fileNames = mutableListOf<String>()
      var formattingOptions = FormattingOptions()
      var dryRun = false
      var setExitIfChanged = false
      var setEditorConfigFile = false
      var setRootDir = false
      var rootDir: String? = null
      var editorConfigFileString: String? = null
      for (arg in args) {
        if (setRootDir) {
          rootDir = arg
          setRootDir = false
        }
        if (setEditorConfigFile) {
          editorConfigFileString = arg
          setEditorConfigFile = false
        }
        when {
          arg == "--dropbox-style" -> formattingOptions = Formatter.DROPBOX_FORMAT
          arg == "--google-style" -> formattingOptions = Formatter.GOOGLE_FORMAT
          arg == "--kotlinlang-style" -> formattingOptions = Formatter.KOTLINLANG_FORMAT
          arg == "--editorconfig-style" -> setEditorConfigFile = true
          arg == "--dry-run" || arg == "-n" -> dryRun = true
          arg == "--set-exit-if-changed" -> setExitIfChanged = true
          arg == "--rootDir" -> setRootDir = true
          arg.startsWith("--") -> err.println("Unexpected option: $arg")
          arg.startsWith("@") -> err.println("Unexpected option: $arg")
          else -> fileNames.add(arg)
        }
      }

      if (editorConfigFileString != null && rootDir != null) {

        val myCache: Cache = Caches.permanent()
        val myLoader: EditorConfigLoader = EditorConfigLoader.default_()

        val propService: ResourcePropertiesService = ResourcePropertiesService.builder()
          .cache(myCache)
          .loader(myLoader)
          .rootDirectory(ResourcePaths.ofPath(Paths.get(rootDir), StandardCharsets.UTF_8))
          .build();

        val props: ResourceProperties = propService.queryProperties(Resources.ofPath(Paths.get(fileNames.get(0)), StandardCharsets.UTF_8));

        println("$props")
        props.properties.entries.map {
          println("${it.key}")
          println("    ${it.value}")
          println("    ${it.value.type}")
        }

        println("charset ${props.properties["charset"]}")
        println("max_line_length ${props.properties["max_line_length"]}")
        val maxLineLength: Property? = props.properties["max_line_length"]
        val maxWith = maxLineLength?.sourceValue?.toInt() ?: 160
        val tabWith = props.properties["tab_width"]?.sourceValue?.toInt() ?: 4

        val indentStyleValue: IndentStyleValue = props.getValue(PropertyType.indent_style, space, true)


        formattingOptions = FormattingOptions(
          style = Style.EDITORCONFIG,
          maxWidth = maxWith,
          blockIndent = tabWith,
          continuationIndent = 2
        )

      }

      return ParsedArgs(fileNames, formattingOptions, dryRun, setExitIfChanged)
    }
  }
}