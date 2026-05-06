# Guía para usar en Visual Studio Code

Este proyecto está configurado para ejecutarse fácilmente en VS Code.

## Pasos para empezar:

1. **Instalar Extensiones**: Al abrir la carpeta, VS Code te sugerirá instalar el "Extension Pack for Java". Haz clic en **Install**.
2. **Sincronización**: Una vez instaladas las extensiones, verás que Maven comienza a descargar las dependencias automáticamente (aparecerá una barra de progreso en la esquina inferior derecha).
3. **Ejecutar**:
   - Ve a la pestaña **Run and Debug** (Ctrl+Shift+D).
   - Selecciona **SistemaFarmaciaApplication** y presiona el botón verde de **Play** (F5).
   - O simplemente abre `src/main/java/sistema_farmacia/demo/DemoApplication.java` y haz clic en **Run** que aparece sobre el método `main`.

## Acceso al Sistema:
- **Aplicación**: [http://localhost:8080](http://localhost:8080)
- **Consola de Base de Datos (H2)**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - **JDBC URL**: `jdbc:h2:mem:farmaciadb`
  - **Usuario**: `sa`
  - **Contraseña**: (vacío)

## Requisitos:
- Tener instalado **Java 17** o superior (Detectado: Java 23 ✅).
