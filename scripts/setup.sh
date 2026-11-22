#!/usr/bin/env bash
# One-time setup: extracts the classes from YOUR Bitwig install into .work/ (git-ignored) and compiles the tools.
set -euo pipefail
source "$(dirname "$0")/common.sh"
[ -f "$BITWIG_JAR" ] || { echo "not found: $BITWIG_JAR"; exit 1; }
[ -f "$LIBS_JAR" ]   || { echo "not found: $LIBS_JAR"; exit 1; }
rm -rf "$WORK/classes" "$WORK/tools"; mkdir -p "$WORK/classes" "$WORK/tools"
echo "extracting $BITWIG_JAR ..."
( cd "$WORK/classes" && jar xf "$BITWIG_JAR" && rm -f com.class )      # root class named "com" shadows the com. package
echo "extracting ANTLR runtime from $LIBS_JAR ..."
( cd "$WORK/classes" && jar xf "$LIBS_JAR" org/antlr/v4 )
java "$ROOT/tools/src/PatchClasses.java" "$WORK/classes"
echo "compiling tools ..."
javac -cp "$WORK/classes" -d "$WORK/tools" "$ROOT"/tools/src/*.java "$ROOT"/src/*/*.java
echo "setup done"
