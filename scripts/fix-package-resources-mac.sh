#!/usr/bin/env bash
set -euo pipefail

# fix-package-resources-mac.sh
# Copia package-resources dentro de Contents/Resources de la app y recrea dmg/zip
# Uso: ./fix-package-resources-mac.sh <path-to-app-bundle> <out-dir>

if [ "$#" -lt 2 ]; then
  echo "Uso: $0 <path-to-app-bundle> <out-dir>"
  exit 2
fi

APP_BUNDLE="$1"
OUT_DIR="$2"
RES_DIR="package-resources"

if [ ! -d "$APP_BUNDLE" ]; then
  echo "No se encontró app bundle: $APP_BUNDLE"; exit 3
fi
if [ ! -d "$RES_DIR" ]; then
  echo "No se encontró $RES_DIR"; exit 4
fi

TARGET_RES_DIR="$APP_BUNDLE/Contents/Resources"
mkdir -p "$TARGET_RES_DIR"

cp -R "$RES_DIR/casos" "$TARGET_RES_DIR/" || true
cp -R "$RES_DIR/alertas" "$TARGET_RES_DIR/" || true

# Recrear dmg y zip
DMG_PATH="$OUT_DIR/$(basename "$APP_BUNDLE").dmg"
ZIP_PATH="$OUT_DIR/$(basename "$APP_BUNDLE").zip"

hdiutil create -volname "${APP_BUNDLE##*/}" -srcfolder "$APP_BUNDLE" -ov -format UDZO "$DMG_PATH"
(cd "$OUT_DIR" && zip -r -9 "$ZIP_PATH" "$(basename "$APP_BUNDLE")")

echo "Recursos copiados y DMG/ZIP creados: $DMG_PATH, $ZIP_PATH"
