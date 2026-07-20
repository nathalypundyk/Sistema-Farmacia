# Script de inicio del servicio en Windows
# Uso: .\start.ps1

Write-Host "🚀 Iniciando Sistema de Farmacia..." -ForegroundColor Green
Write-Host ""

# Verificar que Docker está disponible
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker encontrado: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker no está instalado o no está en PATH" -ForegroundColor Red
    Write-Host "   Descárgalo desde: https://www.docker.com/products/docker-desktop"
    exit 1
}

Write-Host ""
Write-Host "📦 Construyendo imagen Docker..." -ForegroundColor Cyan
docker-compose build
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error durante el build" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "🏃 Iniciando contenedores..." -ForegroundColor Cyan
docker-compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error al iniciar contenedores" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "⏳ Esperando a que la aplicación esté lista..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

Write-Host "🔍 Verificando estado del servicio..." -ForegroundColor Cyan

$healthUrl = "http://localhost:8080/actuator/health"
try {
    $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 5
    if ($response.StatusCode -eq 200) {
        Write-Host "✅ Servicio listo y saludable" -ForegroundColor Green
        Write-Host ""
        Write-Host "🌐 Aplicación disponible en: http://localhost:8080" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "📊 Estado de contenedores:" -ForegroundColor Cyan
        docker-compose ps
    } else {
        Write-Host "⚠️  Respuesta inesperada del servicio (código: $($response.StatusCode))" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠️  Aplicación no está lista. Verificando logs..." -ForegroundColor Yellow
    Write-Host ""
    docker-compose logs app
}
