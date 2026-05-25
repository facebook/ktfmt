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

# Regenerates the GraalVM native-image reachability metadata for the ktfmt binary
# (core/src/main/native-image/resources/...). Run this from the repo root when CI reports the
# metadata is stale, which usually happens after a kotlin-compiler-embeddable upgrade. See
# core/README.md.
#
# You do not need to know GraalVM to run this. It only needs a GraalVM JDK (which bundles the
# tracing agent); if one is missing it tells you how to install it.
set -euo pipefail

meta_dir="core/src/main/native-image/resources/META-INF/native-image/com/facebook/ktfmt"

# 1. Find a GraalVM `java` (it bundles the native-image tracing agent).
if [[ -n "${GRAALVM_HOME:-}" && -x "${GRAALVM_HOME}/bin/java" ]]; then
  java_bin="${GRAALVM_HOME}/bin/java"
elif command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -qi graalvm; then
  java_bin="java"
else
  cat >&2 <<'EOF'
This script needs a GraalVM JDK (for the native-image tracing agent), which was not found.

Install one, then re-run this script:
  - SDKMAN:   sdk install java 25-graalce && sdk use java 25-graalce
  - Homebrew: brew install --cask graalvm-jdk
  - Manual:   https://www.graalvm.org/downloads/  (then `export GRAALVM_HOME=<that dir>`)
EOF
  exit 1
fi
echo "Using GraalVM java: $java_bin"

# 2. Build the JVM fat jar to trace (uses your normal JDK for Gradle, not necessarily GraalVM).
echo "Building the fat jar..."
./gradlew :ktfmt:shadowJar --no-daemon
jars=(core/build/libs/ktfmt-*-with-dependencies.jar)
jar="${jars[0]}"
[[ -e "$jar" ]] || {
  echo "Could not find the fat jar in core/build/libs/" >&2
  exit 1
}

# 3. Trace reflection by formatting ktfmt's own sources, merging into the existing metadata. The
#    merge happens in a temp dir holding only reachability-metadata.json, so the agent updates just
#    that file and can't reintroduce legacy split-config files. `-n` (dry-run) avoids editing sources.
tmp="$(mktemp -d)"
cp "${meta_dir}/reachability-metadata.json" "${tmp}/"
echo "Tracing reflection over core/src..."
"$java_bin" \
  -agentlib:native-image-agent=config-merge-dir="$tmp" \
  -jar "$jar" \
  -n core/src/main core/src/test
cp "${tmp}/reachability-metadata.json" "${meta_dir}/reachability-metadata.json"
rm -rf "$tmp"

echo
echo "Updated ${meta_dir}/reachability-metadata.json."
echo "Review the diff, then rebuild and smoke-test the native binary:"
echo "  ./gradlew :ktfmt:nativeCompile && ./native_smoke_test.sh"
