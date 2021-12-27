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

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HandlerTest {

  @Test
  fun dontCrash_success() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }","style": "--kotlinlang-style"}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"source":"fun foo() {}\n"}""", response)
  }

  @Test
  fun dontCrash_malformedRequestJson() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }"style": "--kotlinlang-style"}"""
    val response = Handler().handleRequest(event, null)
    assertEquals(
        """{"err":"com.google.gson.stream.MalformedJsonException: """ +
            """Unterminated object at line 1 column 30 path ${'$'}.source"}""",
        response)
  }

  @Test
  fun dontCrash_emptyRequestJson() {
    val event = APIGatewayProxyRequestEvent()
    event.body = ""
    val response = Handler().handleRequest(event, null)
    assertEquals("{}", response)
  }

  @Test
  fun dontCrash_malformedSourceCode() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( { }","style": "--kotlinlang-style"}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"err":"1:10: error: Expecting \u0027)\u0027"}""", response)
  }

  @Test
  fun dontCrash_allNullRequest() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"source":"\n"}""", response)
  }

  @Test
  fun dontCrash_malformedStyle() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }","style": "blabla"}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"source":"fun foo() {}\n"}""", response)
  }

  @Test
  fun dontCrash_noStyle() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }"}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"source":"fun foo() {}\n"}""", response)
  }

  @Test
  fun dontCrash_negativeMaxWidth() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }", "maxWidth": -100}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"source":"fun foo() {}\n"}""", response)
  }

  /** maxWidth == null is treated as if it were 100 */
  @Test
  fun dontCrash_nullMaxWidth() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }", "maxWidth": null}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"source":"fun foo() {}\n"}""", response)
  }

  @Test
  fun dontCrash_nonIntegerMaxWidth() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }", "maxWidth": "bla"}"""
    val response = Handler().handleRequest(event, null)
    assertEquals(
        """{"err":"java.lang.NumberFormatException: For input string: \"bla\""}""", response)
  }

  @Test
  fun dontCrash_coerceMaxWidthToInt() {
    val event = APIGatewayProxyRequestEvent()
    event.body = """{"source": "fun foo ( ) { }", "maxWidth": "100"}"""
    val response = Handler().handleRequest(event, null)
    assertEquals("""{"source":"fun foo() {}\n"}""", response)
  }
}
