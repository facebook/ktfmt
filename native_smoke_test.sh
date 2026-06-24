#!/usr/bin/env bash
# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Smoke-tests a ktfmt native binary: confirms it runs and actually formats Kotlin without crashing.
# This exercises the reflection-heavy parser/formatter path that a bare `--version` check would miss
# (e.g. stale GraalVM reachability metadata).
#
# Run from the repo root.
# Usage: ./native_smoke_test.sh [path-to-ktfmt-binary]
set -euo pipefail

bin="${1:-./core/build/native/nativeCompile/ktfmt}"
if [[ ! -e "$bin" && -e "${bin}.exe" ]]; then
  bin="${bin}.exe"
fi
if [[ ! -x "$bin" ]]; then
  echo "ktfmt native binary not found (or not executable) at: $bin" >&2
  echo "Pass the binary path as the first argument, or build it first with:" >&2
  echo "  ./gradlew :ktfmt:nativeCompile" >&2
  exit 1
fi

# A format crash is almost always stale GraalVM reachability metadata.
stale_hint() {
  echo "native ktfmt crashed while formatting; the GraalVM reachability metadata may be stale." >&2
  exit 1
}

"$bin" --version

# Representative constructs (enum/class/lambda) that drive Kotlin-compiler PSI reflection.
sample=$'enum class E { A, B }\nclass Foo(val a: Int) { fun bar() = listOf(1, 2).map { it * 2 } }'
formatted="$(printf '%s\n' "$sample" | "$bin" -)" || stale_hint
printf '%s\n' "$formatted"
[[ -n "$formatted" ]] || stale_hint

# Idempotency: re-formatting the output must be a no-op.
reformatted="$(printf '%s\n' "$formatted" | "$bin" -)" || stale_hint
[[ "$formatted" == "$reformatted" ]] || {
  echo 'native ktfmt output is not idempotent' >&2
  exit 1
}

# Broader coverage: formatting ktfmt's own sources drives many more Kotlin constructs, and thus
# reflective paths, than the snippet above. Skipped when the sources aren't present.
if [[ -d core/src/main ]]; then
  "$bin" -n core/src/main core/src/test >/dev/null || stale_hint
fi

echo "Native smoke test passed."
