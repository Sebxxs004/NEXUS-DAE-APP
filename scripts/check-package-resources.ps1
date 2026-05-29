$root = Join-Path $PSScriptRoot '..\target\dist\PRISMA-DAE'
Write-Host "Buscando carpetas en: $root"
if (-not (Test-Path $root)) { Write-Host "No existe: $root"; exit 1 }
$found = Get-ChildItem -Recurse $root | Where-Object { $_.PSIsContainer -and ($_.Name -eq 'casos' -or $_.Name -eq 'alertas') }
if ($found) { $found | ForEach-Object { Write-Host $_.FullName } } else { Write-Host "No se encontraron carpetas 'casos' ni 'alertas' dentro del paquete." }
