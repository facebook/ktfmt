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

package com.facebook.ktfmt.onlineformatter

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.facebook.ktfmt.cli.ParseResult
import com.facebook.ktfmt.cli.ParsedArgs
import com.facebook.ktfmt.format.Formatter
import com.google.gson.Gson

class Handler : RequestHandler<APIGatewayProxyRequestEvent, String> {
  init {
    // Warm up.
    gson.toJson(Formatter.format(Formatter.KOTLINLANG_FORMAT, "/* foo */ fun foo() {}"))
  }

  override fun handleRequest(event: APIGatewayProxyRequestEvent, context: Context?): String {
    return gson.toJson(
        try {
          val request = gson.fromJson(event.body, Request::class.java)
          val style = request?.style ?: return "{}"
          when (val parseResult = ParsedArgs.parseOptions(listOfNotNull(style).toTypedArray())) {
            is ParseResult.Ok -> {
              val parsedArgs = parseResult.parsedValue
              Response(
                  source =
                      Formatter.format(
                          parsedArgs.formattingOptions.copy(maxWidth = request.maxWidth ?: 100),
                          request.source.orEmpty(),
                      ),
                  err = null,
              )
            }
            is ParseResult.Error -> {
              Response(null, parseResult.errorMessage)
            }
            is ParseResult.ShowMessage -> {
              Response(null, parseResult.message)
            }
          }
        } catch (e: Exception) {
          Response(null, e.message)
        })
  }

  companion object {
    val gson = Gson()
  }
}

class Request {
  var source: String? = ""
  var style: String? = ""
  var maxWidth: Int? = 100
}

data class Response(val source: String?, val err: String?)
