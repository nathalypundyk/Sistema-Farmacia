#!/bin/bash
# Script de backup de base de datos PostgreSQL
# Uso: ./backup-database.sh

set -e

BACKUP_DIR="./backups"
CONTAINER_NAME="farmacia-db"
DB_NAME="farmaciadb"
DB_USER="postgres"

# Crear directorio de backups si no existe
mkdir -p "$BACKUP_DIR"

# Crear backup con timestamp
BACKUP_FILE="$BACKUP_DIR/backup_$(date +%Y%m%d_%H%M%S).sql.gz"

echo "📦 Iniciando backup de base de datos..."
echo "   Contenedor: $CONTAINER_NAME"
echo "   Base de datos: $DB_NAME"
echo "   Archivo de salida: $BACKUP_FILE"

# Ejecutar pg_dump dentro del contenedor Docker y comprimir
docker exec "$CONTAINER_NAME" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "✅ Backup completado exitosamente"
    echo "   Tamaño: $FILE_SIZE"
    echo "   Ruta: $BACKUP_FILE"
else
    echo "❌ Error durante el backup"
    exit 1
fi

# Opcional: Eliminar backups más antiguos de 30 días
echo "🧹 Limpiando backups antiguos (>30 días)..."
find "$BACKUP_DIR" -type f -name "backup_*.sql.gz" -mtime +30 -delete
echo "✅ Limpieza completada"
