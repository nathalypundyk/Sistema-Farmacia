#!/bin/bash
# Script de inicio del servicio
# Uso: ./start.sh

set -e

echo "🚀 Iniciando Sistema de Farmacia..."
echo ""

# Verificar que Docker está disponible
if ! command -v docker &> /dev/null; then
    echo "❌ Docker no está instalado o no está en PATH"
    echo "   Descárgalo desde: https://www.docker.com/products/docker-desktop"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ docker-compose no está instalado"
    exit 1
fi

echo "📦 Construyendo imagen Docker..."
docker-compose build

echo ""
echo "🏃 Iniciando contenedores..."
docker-compose up -d

echo ""
echo "⏳ Esperando a que la aplicación esté lista..."
sleep 10

# Verificar que está saludable
echo "🔍 Verificando estado del servicio..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Servicio listo y saludable"
    echo ""
    echo "🌐 Aplicación disponible en: http://localhost:8080"
    echo ""
    docker-compose ps
else
    echo "⚠️  Aplicación no está lista. Verificando logs..."
    docker-compose logs app
fi
