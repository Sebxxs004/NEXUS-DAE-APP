# Ejecuta jlink y jpackage usando los jmods descomprimidos
if (-not $env:JAVA_HOME) { Write-Error 'ERROR: JAVA_HOME no configurado'; exit 1 }

$jdkJmods = Join-Path $env:JAVA_HOME 'jmods'
$jfxmods = Join-Path $PSScriptRoot '..\javafx-jmods-17.0.6\javafx-jmods-17.0.6'
$jlink = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
if (-not (Test-Path $jlink)) { Write-Error "ERROR: jlink no encontrado en $jlink"; exit 1 }

Write-Host "jdk jmods: $jdkJmods"
Write-Host "jfx jmods: $jfxmods"

Write-Host 'Ejecutando jlink...'
& $jlink --module-path "$jdkJmods;$jfxmods" --add-modules javafx.controls,javafx.fxml,javafx.graphics --compress=2 --strip-debug --no-man-pages --no-header-files --output jlink-runtime
if ($LASTEXITCODE -ne 0) { Write-Error "jlink falló (exit $LASTEXITCODE)"; exit $LASTEXITCODE }

Write-Host 'Versión java (runtime):'
& .\jlink-runtime\bin\java -version

Write-Host 'Probando el JAR con la runtime nueva...'
& .\jlink-runtime\bin\java -jar .\target\package\PRISMA-DAE.jar

$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) { Write-Error "ERROR: jpackage no encontrado en $jpackage"; exit 1 }

Write-Host 'Ejecutando jpackage...'
& $jpackage --type app-image --input .\target\package --main-jar PRISMA-DAE.jar --main-class com.prisma.Launcher --name PRISMA-DAE --runtime-image jlink-runtime --app-version 1.0.0 --dest .\target\dist
if ($LASTEXITCODE -ne 0) { Write-Error "jpackage falló (exit $LASTEXITCODE)"; exit $LASTEXITCODE }

Write-Host 'Empaquetado completado.'
