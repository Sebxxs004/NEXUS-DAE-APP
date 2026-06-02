# Copia package-resources (casos, alertas) al app-image y recrea el ZIP
$cwd = Get-Location
$src = Join-Path $cwd 'package-resources'
$dst = Join-Path $cwd 'target\dist\NEXUS-DAE'
if (-not (Test-Path $src)) { Write-Error "No existe $src"; exit 1 }
if (-not (Test-Path $dst)) { Write-Error "No existe el app-image en $dst"; exit 1 }
Write-Host "Copiando desde $src -> $dst"
Copy-Item -Path (Join-Path $src 'casos') -Destination (Join-Path $dst 'casos') -Recurse -Force -ErrorAction Stop
Copy-Item -Path (Join-Path $src 'alertas') -Destination (Join-Path $dst 'alertas') -Recurse -Force -ErrorAction Stop
Write-Host "Copiado." 
$zip = Join-Path $cwd 'target\dist\NEXUS-DAE-1.0.0-windows.zip'
if (Test-Path $zip) { Remove-Item $zip -Force }
Write-Host "Creando ZIP: $zip"
Compress-Archive -Path (Join-Path $dst '*') -DestinationPath $zip -Force
Write-Host "ZIP creado: $zip"
# Verificación rápida
$tmp = Join-Path $cwd 'tmp\NEXUS-check'
if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
Expand-Archive -Path $zip -DestinationPath $tmp
Get-ChildItem -Recurse $tmp | Where-Object { $_.PSIsContainer -and ($_.Name -eq 'casos' -or $_.Name -eq 'alertas') } | ForEach-Object { Write-Host "Found: " $_.FullName }
Write-Host 'Hecho.'
