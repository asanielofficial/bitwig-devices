#!/usr/bin/env bash
# Build one device:  scripts/build.sh tremolo-plus
set -euo pipefail
source "$(dirname "$0")/common.sh"
DEV="${1:?usage: scripts/build.sh <device-folder-name under src/>}"
PROP="$ROOT/src/$DEV/device.properties"
[ -f "$PROP" ] || { echo "missing $PROP"; exit 1; }
prop() { grep -E "^$1=" "$PROP" | head -1 | cut -d= -f2-; }
MAIN="$(prop main)"; NIT="$ROOT/$(prop nit)"; OUT="$ROOT/$(prop output)"
REF="${REFERENCE_DIR:-$ROOT/reference}"
for t in $(prop templates); do [ -f "$REF/$t" ] || { echo "missing template $REF/$t (see docs/BUILDING.md)"; exit 1; }; done
[ -d "$BITWIG_ICONS" ] || { echo "icons folder not found: $BITWIG_ICONS (set BITWIG_ICONS)"; exit 1; }
java -cp "$CP" "$MAIN" --reference "$REF" --icons "$BITWIG_ICONS" --nit "$NIT" --out "$OUT"
