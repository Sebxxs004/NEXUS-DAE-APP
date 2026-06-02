# Empaquetado multiplataforma

Este proyecto se puede distribuir como un paquete auto-contenido para Windows y macOS usando `jpackage`.
El paquete incluye la aplicacion y su runtime, asi que la maquina destino no necesita instalar Java ni otras dependencias.

## Requisitos para construir

- JDK 17 o superior con `jpackage` disponible.
- Maven en `PATH`.
- En Windows, JavaFX y las dependencias nativas se resuelven al construir en Windows.
- En macOS, el paquete debe construirse en macOS.
- Si `jpackage` no esta en `PATH`, los scripts usan `JAVA_HOME` para localizarlo.

## Windows

Ejecuta:

```powershell
.\scripts\package-windows.ps1
```

Salida esperada:

- `target\dist\NEXUS-DAE-1.0.0-windows.zip`

## macOS

Ejecuta:

```bash
bash scripts/package-macos.sh
```

Salida esperada:

- `target/dist/NEXUS-DAE.app`
- `target/dist/NEXUS-DAE-1.0.0.dmg` (incluye `casos/` y `alertas/` junto a la app)

## Nota importante

No existe un unico ejecutable nativo que funcione igual como archivo final en macOS y Windows.
Lo correcto es usar el mismo repositorio y generar un paquete nativo por sistema operativo con runtime incluido.
