#!/usr/bin/env bash
# Decode any .bwdevice / .bwmodulator to a readable tree:  scripts/dump.sh devices/Tremolo+.bwdevice out.txt
set -euo pipefail
source "$(dirname "$0")/common.sh"
java -cp "$CP" Dump "${1:?input file}" "${2:?output text file}"
