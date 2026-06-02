#!/usr/bin/env bash
set -euo pipefail

# Empaqueta NEXUS-DAE para macOS (app-image + DMG) con casos/alertas incluidos.
# Requiere: JDK 17+ con jpackage, Maven, macOS arm64/x64.

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

APP_NAME="NEXUS-DAE"
MAIN_JAR="${APP_NAME}.jar"
MAIN_CLASS="com.prisma.Launcher"
APP_VERSION="1.0.0"
DIST_DIR="$PROJECT_ROOT/target/dist"
RES_DIR="$PROJECT_ROOT/package-resources"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven no está disponible en PATH." >&2
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
  echo "jpackage no está disponible. Define JAVA_HOME con un JDK 17+." >&2
  exit 1
}

if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin/java" ]]; then
    export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
  fi
fi

echo "==> Compilando con Maven..."
mvn -q clean package

INPUT_DIR="$PROJECT_ROOT/target/package"
if [[ ! -f "$INPUT_DIR/$MAIN_JAR" ]]; then
  echo "No se encontró $INPUT_DIR/$MAIN_JAR" >&2
  exit 1
fi

mkdir -p "$DIST_DIR"
JPACKAGE_BIN="$(resolve_jpackage)"

echo "==> Generando $APP_NAME.app (runtime JDK embebido + JavaFX nativo)..."
rm -rf "$DIST_DIR/$APP_NAME.app"
"$JPACKAGE_BIN" \
  --type app-image \
  --dest "$DIST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --mac-package-identifier "com.prisma.dae"

echo "==> Copiando casos/ y alertas/ junto al .app y en Contents/app..."
rm -rf "$DIST_DIR/casos" "$DIST_DIR/alertas"
cp -R "$RES_DIR/casos" "$RES_DIR/alertas" "$DIST_DIR/"
mkdir -p "$DIST_DIR/$APP_NAME.app/Contents/app/casos" "$DIST_DIR/$APP_NAME.app/Contents/app/alertas"
cp -R "$RES_DIR/casos/." "$DIST_DIR/$APP_NAME.app/Contents/app/casos/"
cp -R "$RES_DIR/alertas/." "$DIST_DIR/$APP_NAME.app/Contents/app/alertas/"

DMG_PATH="$DIST_DIR/${APP_NAME}-${APP_VERSION}.dmg"
echo "==> Creando DMG: $DMG_PATH"
STAGING="$DIST_DIR/dmg-staging-$$"
rm -rf "$STAGING"
mkdir -p "$STAGING"
cp -R "$DIST_DIR/$APP_NAME.app" "$STAGING/"
cp -R "$DIST_DIR/casos" "$DIST_DIR/alertas" "$STAGING/"
hdiutil create -volname "$APP_NAME" -srcfolder "$STAGING" -ov -format UDZO "$DMG_PATH"
rm -rf "$STAGING"

echo ""
echo "Listo:"
echo "  App:  $DIST_DIR/$APP_NAME.app"
echo "  DMG:  $DMG_PATH"
echo "  Casos: $(find "$DIST_DIR/casos" -type f | wc -l | tr -d ' ') imágenes"
echo "  Alertas: $(find "$DIST_DIR/alertas" -type f | wc -l | tr -d ' ') imágenes"
