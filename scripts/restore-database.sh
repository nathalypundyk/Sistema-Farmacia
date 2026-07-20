#!/bin/bash
# Script de restauración de base de datos PostgreSQL desde backup
# Uso: ./restore-database.sh <archivo_backup.sql.gz>

set -e

if [ -z "$1" ]; then
    echo "❌ Uso: $0 <archivo_backup.sql.gz>"
    echo ""
    echo "Ejemplo:"
    echo "   $0 backups/backup_20240720_150000.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"
CONTAINER_NAME="farmacia-db"
DB_NAME="farmaciadb"
DB_USER="postgres"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Archivo de backup no encontrado: $BACKUP_FILE"
    exit 1
fi

echo "⚠️  ADVERTENCIA: Esto restaurará la base de datos desde backup"
echo "   Archivo: $BACKUP_FILE"
echo "   Contenedor: $CONTAINER_NAME"
echo "   Base de datos: $DB_NAME"
echo ""
echo "¿Está seguro? (escriba 'sí' para continuar):"
read -r confirmation

if [ "$confirmation" != "sí" ]; then
    echo "❌ Restauración cancelada"
    exit 1
fi

echo "⏳ Restaurando base de datos..."

# Descomprimir y restaurar
gunzip < "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" "$DB_NAME"

if [ $? -eq 0 ]; then
    echo "✅ Restauración completada exitosamente"
else
    echo "❌ Error durante la restauración"
    exit 1
fi
