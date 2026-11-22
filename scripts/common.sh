# shared by the other scripts (source it, do not run it)
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$ROOT/.work"
: "${BITWIG_DIR:?Set BITWIG_DIR to your Bitwig Studio folder (the one containing bin/bitwig.jar), e.g. export BITWIG_DIR=\"/c/Program Files/Bitwig Studio\"}"
BITWIG_JAR="${BITWIG_JAR:-$BITWIG_DIR/bin/bitwig.jar}"
LIBS_JAR="${LIBS_JAR:-$BITWIG_DIR/bin/libs.jar}"
BITWIG_ICONS="${BITWIG_ICONS:-$BITWIG_DIR/resources/icons}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; *) SEP=":";; esac
CP="$WORK/classes$SEP$WORK/tools"
