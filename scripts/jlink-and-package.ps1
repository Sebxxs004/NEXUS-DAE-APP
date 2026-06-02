# Script para crear runtime con jlink y empaquetar con jpackage
param()

if (-not $env:JAVA_HOME) {
    Write-Error "ERROR: JAVA_HOME no está configurado. Configura JAVA_HOME y vuelve a ejecutar."
    exit 1
}

$JFX_VERSION = '17.0.6'
$JFX_ZIP = "javafx-jmods-$JFX_VERSION.zip"
$JFX_DIR = Join-Path $PWD "javafx-jmods-$JFX_VERSION"

if (-not (Test-Path $JFX_DIR)) {
    if (-not (Test-Path $JFX_ZIP)) {
        Write-Host "Descargando $JFX_ZIP desde varias ubicaciones..."
        $urls = @(
            "https://github.com/openjdk/jfx/releases/download/jfx-$JFX_VERSION/$JFX_ZIP",
            "https://download2.gluonhq.com/openjfx/$JFX_VERSION/$JFX_ZIP",
            "https://download2.gluonhq.com/openjfx/$JFX_VERSION/openjfx-$JFX_VERSION_windows-x64_bin-jmods.zip"
        )
        $downloaded = $false
        foreach ($u in $urls) {
            try {
                Write-Host "  intentando: $u"
                Invoke-WebRequest -Uri $u -OutFile $JFX_ZIP -UseBasicParsing -ErrorAction Stop
                $downloaded = $true
                break
            } catch {
                Write-Host "    fallo: $($_.Exception.Message)"
            }
        }
        if (-not $downloaded) {
            Write-Error "ERROR: no se pudo descargar $JFX_ZIP desde las ubicaciones conocidas. Descárgalo manualmente y colócalo en la raíz del proyecto."
            exit 1
        }
    }
    if (-not (Test-Path $JFX_ZIP)) {
        Write-Error "ERROR: $JFX_ZIP no encontrado después del intento de descarga."
        exit 1
    }
    Write-Host "Descomprimiendo $JFX_ZIP..."
    Expand-Archive -Path $JFX_ZIP -DestinationPath $JFX_DIR -Force
} else {
    Write-Host "JavaFX jmods ya descomprimidos en $JFX_DIR"
}

$jdkJmods = Join-Path $env:JAVA_HOME 'jmods'
if (-not (Test-Path $jdkJmods)) {
    Write-Error "ERROR: No se encontró $jdkJmods. Asegúrate de tener un JDK con jmods (no una JRE)."
    exit 1
}

$jlink = Join-Path $env:JAVA_HOME 'bin\jlink.exe'
if (-not (Test-Path $jlink)) {
    Write-Error "ERROR: jlink no encontrado en $jlink"
    exit 1
}

Write-Host "Creando runtime con jlink..."
& $jlink --module-path "$jdkJmods;$JFX_DIR\jmods" --add-modules javafx.controls,javafx.fxml,javafx.graphics --compress=2 --strip-debug --no-man-pages --no-header-files --output jlink-runtime
if ($LASTEXITCODE -ne 0) { Write-Error "jlink falló (exit $LASTEXITCODE)"; exit $LASTEXITCODE }

Write-Host "Probando java de la runtime creada..."
Write-Host "Versión java (runtime):"
& .\jlink-runtime\bin\java -version

Write-Host "Intentando arrancar el JAR con la runtime (esto debe mostrar errores si faltan módulos)."
& .\jlink-runtime\bin\java -jar .\target\package\NEXUS-DAE.jar

Write-Host "Ejecutando jpackage para crear app-image..."
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) { Write-Error "ERROR: jpackage no encontrado en $jpackage"; exit 1 }
& $jpackage --type app-image --input .\target\package --main-jar NEXUS-DAE.jar --main-class com.prisma.Launcher --name NEXUS-DAE --runtime-image jlink-runtime --app-version 1.0.0 --dest .\target\dist
if ($LASTEXITCODE -ne 0) { Write-Error "jpackage falló (exit $LASTEXITCODE)"; exit $LASTEXITCODE }

Write-Host "Empaquetado completado. Salida en target\dist\NEXUS-DAE" 
