if (-not $env:JAVA_HOME) { Write-Error 'ERROR: JAVA_HOME no configurado'; exit 1 }
if (Test-Path .\target\dist\PRISMA-DAE) { Remove-Item -Recurse -Force .\target\dist\PRISMA-DAE }
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) { Write-Error "ERROR: jpackage no encontrado en $jpackage"; exit 1 }
# Preparar recursos (copiar carpetas de imágenes que queremos incluir en el paquete)
$resDir = Join-Path $PWD 'package-resources'
if (Test-Path $resDir) { Remove-Item -Recurse -Force $resDir }
New-Item -ItemType Directory -Path $resDir | Out-Null

# Copiar carpetas del proyecto si existen
if (Test-Path (Join-Path $PWD 'casos')) { Copy-Item -Recurse -Force (Join-Path $PWD 'casos') (Join-Path $resDir 'casos') }
if (Test-Path (Join-Path $PWD 'alertas')) { Copy-Item -Recurse -Force (Join-Path $PWD 'alertas') (Join-Path $resDir 'alertas') }

Write-Host "Incluyendo recursos desde: $resDir"
& $jpackage --type app-image --input .\target\package --main-jar PRISMA-DAE.jar --main-class com.prisma.App --name PRISMA-DAE --runtime-image jlink-runtime --resource-dir $resDir --app-version 1.0.0 --dest .\target\dist
if ($LASTEXITCODE -ne 0) { Write-Error "jpackage falló (exit $LASTEXITCODE)"; exit $LASTEXITCODE }
Write-Host 'jpackage completado.'
