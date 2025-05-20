#!/bin/sh
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

set -e

# Absolute path to this script, e.g. /home/user/ktfmt/smoke_tests.sh
SCRIPT=$(readlink -f "$0")
# Absolute path this script is in, thus /home/user/ktfmt/
ROOT_DIR=$(dirname "$SCRIPT")
cd "$ROOT_DIR"

echo "Testing ktfmt core library"
echo
./gradlew :ktfmt:build
echo

echo "Testing ktfmt IDEA plugin"
echo
./gradlew :idea_plugin:build
echo

echo "Testing online formatter"
echo
./gradlew :lambda:build
./gradlew :build
echo
