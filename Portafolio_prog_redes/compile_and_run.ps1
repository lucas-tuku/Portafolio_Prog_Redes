# Script de compilación y ejecución para el Krause DataPipe Engine
# EEST N°1 Ing. Otto Krause

$ErrorActionPreference = "Stop"

Write-Host "=========================================" -ForegroundColor Green
Write-Host " Compilando Krause DataPipe Engine...   " -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

# Limpiar carpeta de binarios anterior si existe
if (Test-Path bin) {
    Remove-Item -Recurse -Force bin
}
New-Item -ItemType Directory -Force -Path bin | Out-Null

# Obtener todos los archivos Java del árbol de fuentes
$javaFiles = Get-ChildItem -Recurse src\*.java | ForEach-Object { $_.FullName }

if ($javaFiles.Count -eq 0) {
    Write-Error "No se encontraron archivos fuentes Java en la carpeta src."
}

# Ejecutar compilación
Write-Host "Ejecutando: javac -d bin -sourcepath src [fuentes]" -ForegroundColor Gray
javac -d bin -sourcepath src $javaFiles

Write-Host "`n=========================================" -ForegroundColor Green
Write-Host " Compilación Exitosa. Ejecutando Engine... " -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green

# Ejecutar la aplicación
java -cp bin ar.edu.ottokrause.datapipe.Main
