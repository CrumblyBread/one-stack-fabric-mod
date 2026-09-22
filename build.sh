#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
JDK="$(echo "$ROOT"/.jdk/jdk-*)"
if [[ ! -x "$JDK/bin/javac" ]]; then
  echo "JDK 25 not found under .jdk/. Download it first, or install a system JDK 25."
  exit 1
fi
export JAVA_HOME="$JDK"
export PATH="$JAVA_HOME/bin:$PATH"
exec "$ROOT/gradlew" "$@"
