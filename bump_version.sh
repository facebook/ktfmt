#!/bin/sh
# Copyright (c) Facebook, Inc. and its affiliates.
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

display_usage() {
    echo "Usage:"
    echo "/bump_version.sh CURRENT_VERSION NEXT_VERSION"
    echo
    echo "Example:"
    echo "./bump_version.sh 0.19 0.20"
}

if [  $# -le 1 ]
then
    display_usage
    exit 1
fi

CURRENT_VERSION="$1"
NEXT_VERSION="$2"

POM1="$HOME/fbsource/xplat/ktfmt/pom.xml"
POM2="$HOME/fbsource/xplat/ktfmt/core/pom.xml"
BUILD_GRADLE="$HOME/fbsource/xplat/ktfmt/ktfmt_idea_plugin/build.gradle.kts"

sed -i.bak "s/$CURRENT_VERSION-SNAPSHOT/$CURRENT_VERSION/g" "$POM1" "$POM2"
sed -i.bak "s/val ktfmtVersion = \"$CURRENT_VERSION-SNAPSHOT\"/val ktfmtVersion = \"$CURRENT_VERSION\"/g" "$BUILD_GRADLE"
hg commit -m "[ktfmt] Bump version to $CURRENT_VERSION" "$POM1" "$POM2" "$BUILD_GRADLE"

sed -i.bak "s/$CURRENT_VERSION/$NEXT_VERSION-SNAPSHOT/g" "$POM1" "$POM2"
sed -i.bak "s/val ktfmtVersion = \"$CURRENT_VERSION\"/val ktfmtVersion = \"$NEXT_VERSION-SNAPSHOT\"/g" "$BUILD_GRADLE"
hg commit -m "[ktfmt] Bump version to $NEXT_VERSION-SNAPSHOT" "$POM1" "$POM2" "$BUILD_GRADLE"

rm "$POM1.bak" "$POM2.bak" "$BUILD_GRADLE.bak"
