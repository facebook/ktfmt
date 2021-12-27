/**
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

/* Forked from https://microsoft.github.io/monaco-editor/ */

"use strict";

var editor = null;
var style = "--kotlinlang-style";
var columnLimit = 100;
var options = null;

require.config({ paths: { vs: "node_modules/monaco-editor/min/vs" } });

$(document).ready(function () {
  require(["vs/editor/editor.main"], function () {
    editor = monaco.editor.create(document.getElementById("editor"), {
      value: [
        "/**",
        " * Formats the given Javadoc comment, which must start with /** and end with */. The output will",
        " * start and end with the same characters.",
        " */",
        "class Handler : RequestHandler<APIGatewayProxyRequestEvent, String> {",
        "    override fun handleRequest(event: APIGatewayProxyRequestEvent, context: Context?): String {",
        "        for ((i, item) in items.withIndex()) {",
        "            if (needDot) {",
        "                // fillMode is bla bla",
        "                val fillMode =",
        "                    if (unconsumedPrefixes.isNotEmpty() && i <= unconsumedPrefixes.peekFirst()) {",
        "                        prefixFillMode /* bla bla */",
        "                    } else {",
        "                        Doc.FillMode.UNIFIED",
        "                    }",
        '                builder.breakOp(fillMode, "", ZERO, Optional.of(nameTag))',
        "                builder.token((item as KtQualifiedExpression).operationSign.value)",
        "            }",
        "            emitSelectorUpToParenthesis(item)",
        "            if (unconsumedPrefixes.isNotEmpty() && i == unconsumedPrefixes.peekFirst()) {",
        "                builder.close()",
        "                unconsumedPrefixes.removeFirst()",
        "            }",
        "            if (i == items.size - 1 && hasTrailingLambda) {",
        "                builder.close()",
        "            }",
        "            val argsIndent =",
        "                Indent.If.make(",
        "                    nameTag,",
        "                    expressionBreakIndent,",
        "                    if (trailingDereferences) expressionBreakIndent else ZERO",
        "                )",
        "        }",
        "    }",
        "}",
      ].join("\n"),
      language: "kotlin",
      rulers: [columnLimit],
      scrollBeyondLastLine: false,
    });
  });
  options = editor.getOptions();
  window.onresize = function () {
    editor.layout();
  };

  $(".style-picker").change(function () {
    changeStyle(this.selectedIndex);
  });

  $(".column-limit-picker").change(function () {
    columnLimit = parseInt(this.value);
    editor.updateOptions({'rulers': [columnLimit]});
    reformatEditor();
  });

  $("#editorForm").submit(function (event) {
    event.preventDefault();
    reformatEditor();
  });
});

function changeStyle(newStyle) {
  style = newStyle === 0 ? "--kotlinlang-style" : undefined;
  reformatEditor();
}

function reformatEditor() {
    disableDemoUi();
  $.ajax({
    type: "POST",
    url: "https://8uj6xa47qa.execute-api.us-east-2.amazonaws.com/ktfmt",
    contentType: "application/json",
    data: JSON.stringify({
      source: editor.getValue(),
      maxWidth: columnLimit,
      style: style,
    }),
    dataType: "json",
    error: function (xhr, errorType, errorThrown) {
      showError(errorThrown);
    },
  })
    .done(function (data) {
      if (data.err) {
        showError(data.err);
      } else {
        $("#error-message").hide();
        editor.setValue(data.source);
      }
    })
    .always(function () {
        enableDemoUi();
    });
}

function disableDemoUi() {
    $('.loading.editor').show();
    $("#editorForm :input").prop("disabled", true);
    editor.updateOptions({ readOnly: true, language: 'text' });
}

function enableDemoUi() {
    $("#editorForm :input").prop("disabled", false);
    editor.updateOptions({ readOnly: false });
    $('.loading.editor').fadeOut({ duration: 300 });
}

function showError(error) {
  var errorMessage = $("#error-message");
  errorMessage.text("Error: " + error);
  errorMessage.show();
}
