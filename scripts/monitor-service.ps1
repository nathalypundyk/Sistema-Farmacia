# Script de monitoreo del servicio en Windows
# Verifica cada 5 minutos si la app está corriendo
# Si falla, intenta reiniciarla

$HealthUrl = "http://localhost:8080/actuator/health"
$MaxRetries = 3
$RetryDelay = 10
$CheckInterval = 300  # 5 minutos

function Check-Health {
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Log-Message {
    param([string]$Message)
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] $Message"
}

function Restart-Service {
    Log-Message "🔧 Intentando reiniciar servicio..."
    Push-Location
    docker-compose up -d --no-deps app
    Pop-Location
    Start-Sleep -Seconds 10
}

Log-Message "🔍 Iniciando monitoreo de servicio"
Log-Message "   URL: $HealthUrl"
Log-Message "   Intervalo: $CheckInterval segundos"

while ($true) {
    if (Check-Health) {
        Log-Message "✅ Servicio saludable"
        Start-Sleep -Seconds $CheckInterval
    } else {
        Log-Message "⚠️  Servicio no responde"

        $retryCount = 0
        while ($retryCount -lt $MaxRetries) {
            Log-Message "   Reintento $($retryCount + 1)/$MaxRetries..."
            Start-Sleep -Seconds $RetryDelay

            if (Check-Health) {
                Log-Message "✅ Servicio recuperado"
                break
            }

            $retryCount++
        }

        if ($retryCount -eq $MaxRetries) {
            Log-Message "❌ Servicio no responde después de $MaxRetries intentos"
            Restart-Service

            Start-Sleep -Seconds 10
            if (Check-Health) {
                Log-Message "✅ Servicio reiniciado exitosamente"
            } else {
                Log-Message "❌ ALERTA: Servicio no se recuperó después de reinicio"
                # Aquí se podría enviar notificación
            }
        }
    }
}
