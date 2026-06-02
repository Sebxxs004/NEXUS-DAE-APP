$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw 'Maven no esta disponible en PATH.'
}

function Resolve-JPackage {
    $command = Get-Command jpackage -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw 'jpackage no esta disponible. Verifica JAVA_HOME o usa un JDK 17+ que incluya jpackage.'
}

mvn clean package

$inputDir = Join-Path $projectRoot 'target\package'
$outputDir = Join-Path $projectRoot 'target\dist'
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$jpackage = Resolve-JPackage

& $jpackage `
    --type app-image `
    --dest $outputDir `
    --input $inputDir `
    --name 'PRISMA-DAE' `
    --main-jar 'PRISMA-DAE.jar' `
    --main-class 'com.prisma.Launcher' `
    --app-version '1.0.0'

$appDir = Join-Path $outputDir 'PRISMA-DAE'
if (-not (Test-Path $appDir)) {
    throw "No se encontro la carpeta empaquetada: $appDir"
}

# Garantiza que los recursos externos queden junto al .exe
$casosSource = Join-Path $projectRoot 'casos'
$alertasSource = Join-Path $projectRoot 'alertas'
if (Test-Path $casosSource) {
    Copy-Item -Recurse -Force $casosSource (Join-Path $appDir 'casos')
}
if (Test-Path $alertasSource) {
    Copy-Item -Recurse -Force $alertasSource (Join-Path $appDir 'alertas')
}

$zipPath = Join-Path $outputDir 'PRISMA-DAE-1.0.0-windows.zip'
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}
Compress-Archive -Path (Join-Path $outputDir 'PRISMA-DAE') -DestinationPath $zipPath -Force

Write-Host "Listo. Paquete portable creado en $zipPath"
