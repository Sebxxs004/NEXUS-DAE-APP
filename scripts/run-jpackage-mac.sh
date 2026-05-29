#!/usr/bin/env bash
set -euo pipefail

# run-jpackage-mac.sh
# Uso:
#   ./run-jpackage-mac.sh /path/to/jdk <main-jar> <main-class>
# Alternativamente, adapta variables en el script.

if [ "$#" -lt 3 ]; then
  echo "Uso: $0 <JDK_HOME> <MAIN_JAR> <MAIN_CLASS>"
  exit 2
fi

JDK_HOME="$1"
MAIN_JAR="$2"
MAIN_CLASS="$3"
APP_NAME="PRISMA-DAE"
APP_VERSION="1.0.0"
OUT_DIR="target/dist/${APP_NAME}-mac"
RES_DIR="package-resources"

mkdir -p "$OUT_DIR"

"$JDK_HOME/bin/jpackage" \
  --type app-image \
  --input "$(dirname "$MAIN_JAR")" \
  --main-jar "$(basename "$MAIN_JAR")" \
  --main-class "$MAIN_CLASS" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --dest "$OUT_DIR" \
  --resource-dir "$RES_DIR" \
  || { echo "jpackage falló"; exit 1; }

# Si se desea, crear dmg
APP_BUNDLE="$OUT_DIR/$APP_NAME.app"
if [ -d "$APP_BUNDLE" ]; then
  DMG_PATH="$OUT_DIR/${APP_NAME}-${APP_VERSION}.dmg"
  hdiutil create -volname "$APP_NAME" -srcfolder "$APP_BUNDLE" -ov -format UDZO "$DMG_PATH"
  echo "DMG creado: $DMG_PATH"
else
  echo "No se encontró $APP_BUNDLE after jpackage"
fi

echo "run-jpackage-mac.sh finalizado"
