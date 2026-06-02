#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven no esta disponible en PATH." >&2
  exit 1
fi

resolve_jpackage() {
  if command -v jpackage >/dev/null 2>&1; then
    command -v jpackage
    return 0
  fi

  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" ]]; then
    echo "$JAVA_HOME/bin/jpackage"
    return 0
  fi

  echo "jpackage no esta disponible. Verifica JAVA_HOME o usa un JDK 17+ que lo incluya." >&2
  exit 1
fi

mvn clean package

INPUT_DIR="$PROJECT_ROOT/target/package"
OUTPUT_DIR="$PROJECT_ROOT/target/dist"
mkdir -p "$OUTPUT_DIR"
JPACKAGE_BIN="$(resolve_jpackage)"

"$JPACKAGE_BIN" \
  --type dmg \
  --dest "$OUTPUT_DIR" \
  --input "$INPUT_DIR" \
  --name "PRISMA-DAE" \
  --main-jar "PRISMA-DAE.jar" \
  --main-class "com.prisma.Launcher" \
  --app-version "1.0.0" \
  --mac-package-identifier "com.prisma.dae"

echo "Listo. Instalador creado en $OUTPUT_DIR/PRISMA-DAE-1.0.0.dmg"
