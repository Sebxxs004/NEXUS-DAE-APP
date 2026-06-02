#!/usr/bin/env bash
set -euo pipefail

# run-jlink-and-package-mac.sh
# Uso:
#   ./run-jlink-and-package-mac.sh /path/to/jdk /path/to/openjfx-jmods.zip [arch]
# Ejemplo:
#   ./run-jlink-and-package-mac.sh /Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home openjfx-17.0.6_macos-x64_bin-jmods.zip x64
# Este script crea un runtime con jlink que incluye módulos JavaFX y ejecuta jpackage para crear el .app y un dmg/zip.

if [ "$#" -lt 2 ]; then
  echo "Uso: $0 <JDK_HOME> <OPENJFX_JMODS_ZIP> [arch]"
  exit 2
fi

JDK_HOME="$1"
OPENJFX_JMODS_ZIP="$2"
ARCH="${3:-x64}"
APP_NAME="NEXUS-DAE"
APP_VERSION="1.0.0"
MAIN_JAR="target/package/NEXUS-DAE.jar"
MAIN_CLASS="com.prisma.Launcher" # Ajusta si tu main es distinto
OUT_DIR="target/dist/${APP_NAME}-mac"
RUNTIME_DIR="jlink-runtime-mac"
RES_DIR="package-resources"

echo "JDK_HOME=$JDK_HOME"
echo "OPENJFX_JMODS_ZIP=$OPENJFX_JMODS_ZIP"

echo "1) Preparando directorios"
rm -rf "$RUNTIME_DIR" "$OUT_DIR"
mkdir -p "$RUNTIME_DIR"

if [ ! -f "$OPENJFX_JMODS_ZIP" ]; then
  echo "Archivo de jmods no encontrado: $OPENJFX_JMODS_ZIP"
  echo "Descárgalo desde Gluon y pásalo como segundo parámetro.";
  exit 3
fi

TMP_JMODS="/tmp/openjfx-jmods-$$"
rm -rf "$TMP_JMODS"
mkdir -p "$TMP_JMODS"

echo "2) Extrayendo jmods"
unzip -q "$OPENJFX_JMODS_ZIP" -d "$TMP_JMODS"

# Detectar módulos de JavaFX que necesitamos
JAVA_MODULES="java.base,java.desktop,java.logging"
# Añadir módulos JavaFX comunes
JAVA_MODULES+",javafx.base,javafx.controls,javafx.fxml,javafx.graphics"

echo "3) Ejecutando jlink (esto puede tardar)"
"$JDK_HOME/bin/jlink" \
  --module-path "$TMP_JMODS":"$JDK_HOME/jmods" \
  --add-modules "$JAVA_MODULES" \
  --compress=2 --no-header-files --no-man-pages \
  --output "$RUNTIME_DIR"

echo "4) Ejecutando jpackage"
if [ ! -f "$MAIN_JAR" ]; then
  echo "No se encontró $MAIN_JAR — asegúrate de ejecutar 'mvn package' antes."; exit 4
fi

# jpackage produce .app en --dest
mkdir -p "$OUT_DIR"
"$JDK_HOME/bin/jpackage" \
  --type app-image \
  --input "target/package" \
  --main-jar "NEXUS-DAE.jar" \
  --main-class "$MAIN_CLASS" \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --dest "$OUT_DIR" \
  --resource-dir "$RES_DIR" \
  --runtime-image "$RUNTIME_DIR" \
  --icon "icons/app.icns" || true

# Crear dmg/zip
echo "5) Empaquetando .app a dmg y zip"
APP_BUNDLE="$OUT_DIR/$APP_NAME.app"
if [ -d "$APP_BUNDLE" ]; then
  DMG_PATH="$OUT_DIR/${APP_NAME}-${APP_VERSION}.dmg"
  echo "Creando DMG: $DMG_PATH"
  hdiutil create -volname "$APP_NAME" -srcfolder "$APP_BUNDLE" -ov -format UDZO "$DMG_PATH"
  ZIP_PATH="$OUT_DIR/${APP_NAME}-${APP_VERSION}-mac.zip"
  echo "Creando ZIP: $ZIP_PATH"
  (cd "$OUT_DIR" && zip -r -9 "$ZIP_PATH" "${APP_NAME}.app")
  echo "listo: $DMG_PATH, $ZIP_PATH"
else
  echo "No se generó $APP_BUNDLE — jpackage falló o produjo otra salida.";
  exit 5
fi

# limpiar
rm -rf "$TMP_JMODS"

echo "Empaquetado mac completado en: $OUT_DIR"
