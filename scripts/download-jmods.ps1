# descarga y descomprime OpenJFX jmods (Gluon)
$JFX_VERSION = '17.0.6'
$zip = 'javafx-jmods-17.0.6.zip'
$dir = Join-Path $PSScriptRoot '..\javafx-jmods-17.0.6'
if (Test-Path $dir) { Remove-Item -Recurse -Force $dir }

$url = 'https://download2.gluonhq.com/openjfx/17.0.6/openjfx-17.0.6_windows-x64_bin-jmods.zip'
Write-Host "Descargando $url -> $zip"
Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
Write-Host "Descomprimiendo $zip -> $dir"
Expand-Archive -Path $zip -DestinationPath $dir -Force
Write-Host "Listado (primeros archivos):"
Get-ChildItem -Recurse $dir | Select-Object -First 30 | ForEach-Object { Write-Host $_.FullName }
