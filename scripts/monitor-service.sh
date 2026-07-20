#!/bin/bash
# Script de monitoreo del servicio
# Verifica cada 5 minutos si la app está corriendo
# Si falla, intenta reiniciarla

HEALTH_URL="http://localhost:8080/actuator/health"
MAX_RETRIES=3
RETRY_DELAY=10

check_health() {
    response=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null)
    if [ "$response" = "200" ]; then
        return 0  # Saludable
    else
        return 1  # No saludable
    fi
}

restart_service() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🔧 Intentando reiniciar servicio..."
    docker-compose up -d --no-deps app
    sleep 10
}

log_message() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1"
}

log_message "🔍 Iniciando monitoreo de servicio"
log_message "   URL: $HEALTH_URL"
log_message "   Intervalo de verificación: 5 minutos"

while true; do
    if check_health; then
        log_message "✅ Servicio saludable"
        sleep 300  # Esperar 5 minutos
    else
        log_message "⚠️  Servicio no responde (HTTP: no 200)"

        # Reintentar varias veces
        retry_count=0
        while [ $retry_count -lt $MAX_RETRIES ]; do
            log_message "   Reintento $((retry_count + 1))/$MAX_RETRIES..."
            sleep $RETRY_DELAY

            if check_health; then
                log_message "✅ Servicio recuperado"
                break
            fi

            retry_count=$((retry_count + 1))
        done

        # Si sigue sin responder, reiniciar
        if [ $retry_count -eq $MAX_RETRIES ]; then
            log_message "❌ Servicio no responde después de $MAX_RETRIES intentos"
            restart_service

            # Verificar si se recuperó
            sleep 10
            if check_health; then
                log_message "✅ Servicio reiniciado exitosamente"
            else
                log_message "❌ ALERTA: Servicio no se recuperó después de reinicio"
                # Aquí se podría enviar una notificación (email, WhatsApp, etc)
            fi
        fi
    fi
done
