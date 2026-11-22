#!/usr/bin/env bash
# Syntax-check a Nitro source file with Bitwig own parser:  scripts/nitcheck.sh src/tremolo-plus/tremolo_plus.nit
set -euo pipefail
source "$(dirname "$0")/common.sh"
java -cp "$CP" NitCompile "${1:?input .nit file}"
