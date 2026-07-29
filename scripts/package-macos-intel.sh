#!/usr/bin/env bash
set -euo pipefail

# Empaqueta NEXUS-DAE específicamente para macOS INTEL (x86_64).
# IMPORTANTE: Este script DEBE ejecutarse usando un JDK versión Intel (x64).

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

APP_NAME="NEXUS-DAE-Intel"
MAIN_JAR="NEXUS-DAE.jar"
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

# Verificación de seguridad: Asegurar que el JDK activo sea Intel (x86_64)
# Si se intenta empaquetar librerías Intel con una máquina virtual ARM (M1/M2), el programa crasheará.
JAVA_CMD="java"
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

OS_ARCH=$("$JAVA_CMD" -XshowSettings:properties -version 2>&1 | grep "os.arch" | awk '{print $3}')
if [[ "$OS_ARCH" != "x86_64" && "$OS_ARCH" != "amd64" ]]; then
  echo ""
  echo "=========================================================================="
  echo " ERROR CRÍTICO DE ARQUITECTURA"
  echo "=========================================================================="
  echo " Estás intentando generar un instalador para Mac Intel (x86_64),"
  echo " pero tu Java activo es de arquitectura: $OS_ARCH (Apple Silicon)."
  echo ""
  echo " Si generas el instalador así, el programa estará corrupto y no"
  echo " abrirá ni en Macs antiguas ni en nuevas."
  echo ""
  echo " SOLUCIÓN: Para compilar para Intel desde esta Mac, necesitas descargar"
  echo " e instalar un JDK versión 'macOS x64' (Intel) y configurar tu JAVA_HOME"
  echo " para que apunte a ese JDK antes de correr este script."
  echo "=========================================================================="
  echo ""
  exit 1
fi

# Forzamos a Maven a descargar las librerías nativas de JavaFX versión Mac Intel
echo "==> Compilando con Maven (Forzando librerías Mac Intel)..."
mvn -q clean package -Djavafx.platform=mac

INPUT_DIR="$PROJECT_ROOT/target/package"
if [[ ! -f "$INPUT_DIR/$MAIN_JAR" ]]; then
  echo "No se encontró $INPUT_DIR/$MAIN_JAR" >&2
  exit 1
fi

mkdir -p "$DIST_DIR"
JPACKAGE_BIN="$(resolve_jpackage)"

echo "==> Generando $APP_NAME.app (runtime JDK Intel embebido + JavaFX nativo x64)..."
rm -rf "$DIST_DIR/$APP_NAME.app"
"$JPACKAGE_BIN" \
  --type app-image \
  --dest "$DIST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --mac-package-identifier "com.prisma.dae.intel"

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
