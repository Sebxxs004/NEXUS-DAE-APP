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
    --main-class 'com.prisma.App' `
    --app-version '1.0.0'

$zipPath = Join-Path $outputDir 'PRISMA-DAE-1.0.0-windows.zip'
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}
Compress-Archive -Path (Join-Path $outputDir 'PRISMA-DAE') -DestinationPath $zipPath -Force

Write-Host "Listo. Paquete portable creado en $zipPath"