# Sistema de Gestión de Farmacia — Gran Hospital del Sur (HGS)

Aplicación web para el control de stock de medicamentos hospitalarios. Permite mantener el
inventario actualizado mediante importaciones de planillas Excel, configurar niveles de stock
mínimo y deseable, detectar medicamentos por debajo del mínimo o próximos a vencer, y enviar
alertas. El acceso está segmentado por roles y las operaciones sensibles se protegen con un PIN
personal.

El stock no se actualiza con entradas/salidas en tiempo real (no hay integración con los sistemas
de movimiento del hospital): se actualiza importando dos tipos de archivo Excel —el listado de
stock con lotes y el listado de códigos SICIAP—.

---

## 1. Tecnologías

| Componente        | Tecnología                                   |
| ----------------- | -------------------------------------------- |
| Lenguaje          | Java 17                                       |
| Framework         | Spring Boot 3.2.4                              |
| Seguridad         | Spring Security + Thymeleaf Extras Security 6 |
| Persistencia      | Spring Data JPA / Hibernate                   |
| Base de datos     | PostgreSQL                                     |
| Vistas            | Thymeleaf, HTML, CSS, JavaScript              |
| Lectura/escritura Excel | Apache POI 5.2.5                        |
| Utilidades        | Lombok, Spring Boot DevTools                   |
| Construcción      | Maven                                          |

---

## 2. Requisitos previos

- JDK 17 o superior.
- PostgreSQL 13 o superior, con una base de datos creada para la aplicación.
- Maven 3.8+ (o el wrapper del IDE).
- Opcional: servicio de WhatsApp accesible por HTTP para el envío de alertas.

---

## 3. Configuración

La configuración se encuentra en `src/main/resources/application.properties`.

### Base de datos

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/farmacia_hgs
spring.datasource.username=postgres
spring.datasource.password=1234
spring.jpa.hibernate.ddl-auto=update
```

- La base de datos `farmacia_hgs` debe existir antes de iniciar la aplicación.
- `ddl-auto=update`: Hibernate crea y actualiza las tablas automáticamente a partir de las
  entidades. No elimina columnas ni tablas existentes.

### Puerto

```properties
server.port=8081
```

### Alertas por WhatsApp

```properties
whatsapp.api.url=http://localhost:3000
whatsapp.directora.numero=595XXXXXXXXX
```

- `whatsapp.api.url`: URL del servicio que entrega los mensajes.
- `whatsapp.directora.numero`: número de destino con código de país, sin el signo `+`.

### Límites de carga de archivos

```properties
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB
```

---

## 4. Docker (Recomendado para Producción)

### Inicio rápido con Docker Compose

La forma más sencilla de desplegar la aplicación es con Docker Compose, que maneja automáticamente
la base de datos PostgreSQL y la aplicación Spring Boot.

**Requisitos:**
- Docker Desktop instalado (https://www.docker.com/products/docker-desktop)

**Pasos:**

1. Clonar o descargar el proyecto
2. Configurar variables de entorno (opcional):
   ```bash
   cp .env.example .env
   # Editar .env con credenciales seguras si es necesario
   ```
3. Iniciar la aplicación:
   - **Linux/Mac:** `./scripts/start.sh`
   - **Windows:** `.\scripts\start.ps1`
   - O manual: `docker-compose up -d`

4. Acceder a la aplicación: `http://localhost:8080`

### Características de esta configuración

✅ **Reinicio automático** — Si se va la luz o falla el proceso, se reinicia solo
✅ **Health checks** — Docker monitorea la salud de los servicios
✅ **Logs rotados** — No se llenan los discos (máx 10MB, 3 archivos)
✅ **Variables de entorno** — Fácil de configurar credenciales seguras
✅ **Base de datos persistente** — Los datos se guardan entre reinicios (volumen `postgres_data`)

### Credenciales iniciales en Docker

| Usuario | Contraseña | PIN |
|---------|-----------|-----|
| admin | HospitalAdmin2024! | 9876 |
| farmacia | Farmacia2024! | (ninguno) |

⚠️ **IMPORTANTE:** Cambiar estas credenciales en la primera ejecución desde el menú de usuario.

### Scripts disponibles

| Script | Descripción |
|--------|-------------|
| `scripts/start.sh` | Inicia Docker Compose (Linux/Mac) |
| `scripts/start.ps1` | Inicia Docker Compose (Windows) |
| `scripts/monitor-service.sh` | Monitorea y reinicia si falla (Linux/Mac) |
| `scripts/monitor-service.ps1` | Monitorea y reinicia si falla (Windows) |
| `scripts/backup-database.sh` | Realiza backup de BD (Linux/Mac) |
| `scripts/restore-database.sh` | Restaura BD desde backup (Linux/Mac) |

### Comandos útiles

```bash
# Ver estado de contenedores
docker-compose ps

# Ver logs de la aplicación
docker-compose logs -f app

# Ver logs de la base de datos
docker-compose logs -f db

# Detener la aplicación
docker-compose down

# Reiniciar solo la aplicación
docker-compose up -d --no-deps app

# Hacer backup de la base de datos
./scripts/backup-database.sh

# Restaurar desde backup
./scripts/restore-database.sh backups/backup_XXXXXXXX_XXXXXX.sql.gz
```

### Configuración en .env

Crear un archivo `.env` en la raíz del proyecto (copiar desde `.env.example`):

```env
# Database
DB_NAME=farmaciadb
DB_USER=postgres
DB_PASSWORD=CONTRASEÑA_SEGURA_AQUI
DB_PORT=5432

# Application
APP_PORT=8080

# Logging
LOG_LEVEL_SECURITY=INFO
LOG_LEVEL_APP=INFO

# WhatsApp
WHATSAPP_DIRECTORA_NUMERO=595XXXXXXXXX
```

⚠️ El archivo `.env` está en `.gitignore` y no se commitea (contiene credenciales).

### Inicio automático en Windows

Para que el servicio inicie automáticamente al bootear:

1. Abrir **Task Scheduler**
2. Crear tarea:
   - Nombre: `Farmacia Sistema Start`
   - Trigger: `Al iniciar sesión` o `Al iniciar el sistema`
   - Action: Ejecutar programa
   - Program: `powershell.exe`
   - Arguments: `-NoProfile -ExecutionPolicy Bypass -File "C:\ruta\al\scripts\start.ps1"`

3. Marcar: "Run with highest privileges" y "Run whether user is logged in or not"

### Monitoreo continuo

Para asegurar que el sistema siempre esté en línea:

**Windows:**
```powershell
# Ejecutar en PowerShell (como administrador)
& "C:\ruta\al\scripts\monitor-service.ps1"
```

**Linux/Mac:**
```bash
# Ejecutar en segundo plano
nohup ./scripts/monitor-service.sh > logs/monitor.log 2>&1 &
```

---

## 5. Ejecución Local (Sin Docker)

### Desde un IDE (NetBeans / IntelliJ / VS Code)

1. Abrir el proyecto como proyecto Maven y esperar la descarga de dependencias.
2. Verificar la conexión a PostgreSQL en `application.properties`.
3. Ejecutar la clase `sistema_farmacia.demo.DemoApplication`.

### Desde la línea de comandos

```bash
mvn clean package
mvn spring-boot:run
```

### Acceso

La aplicación queda disponible en `http://localhost:8081`.

---

## 6. Usuarios iniciales

Al primer arranque, si no existen roles, la clase `DataLoader` crea estos usuarios de prueba:

| Usuario | Contraseña | PIN    | Rol           |
| ------- | ---------- | ------ | ------------- |
| admin   | HospitalAdmin2024! | 9876   | ADMIN         |
| farmacia | Farmacia2024! | (sin PIN) | FARMACEUTICO |

⚠️ **IMPORTANTE:** Estas credenciales son solo para el primer ingreso. Cada usuario debe cambiar su 
contraseña y su PIN desde el menú de su cuenta (esquina superior derecha) antes de que otros 
usuarios accedan al sistema.

---

## 7. Roles y permisos

| Función                                          | ADMIN | FARMACEUTICO |
| ------------------------------------------------ | :---: | :----------: |
| Ver panel, alertas y catálogo de medicamentos    |  Sí   |     Sí       |
| Gestión de lotes                                 |  Sí   |     Sí       |
| Importar stock (Excel)                           |  Sí   |     Sí       |
| Crear, editar y eliminar medicamentos            |  Sí   |     No       |
| Configurar stock mínimo y deseable               |  Sí   |     No       |
| Importar / asociar códigos SICIAP                |  Sí   |     No       |
| Gestión de usuarios y roles                      |  Sí   |     No       |
| Auditoría de importaciones                       |  Sí   |     No       |

Las restricciones se aplican en el backend (reglas de URL en `SecurityConfig`) y en el frontend
(los elementos no permitidos no se muestran). La protección no depende únicamente de la interfaz.

---

## 8. Seguridad: contraseñas y PIN

- Contraseñas y PIN se almacenan siempre con hash BCrypt; nunca en texto plano.
- Cambio de contraseña y de PIN: autoservicio desde el menú de la cuenta (esquina superior
  derecha), opciones "Cambiar mi contraseña" y "Cambiar mi PIN". Ambas piden la contraseña actual
  como confirmación de identidad.
- Un administrador puede restablecer la contraseña de otros usuarios desde la pantalla de Usuarios
  (recuperación de cuentas), pero no puede cambiar la suya propia desde ese formulario.
- El PIN protege el apartado de Configuración de stock: al entrar se solicita una vez y queda
  habilitado mientras dure la sesión.

---

## 9. Módulos principales

| Ruta                  | Descripción                                                        | Acceso |
| --------------------- | ------------------------------------------------------------------ | ------ |
| `/`                   | Panel con indicadores de stock crítico y próximos vencimientos     | Todos  |
| `/lotes`              | Gestión de lotes (cantidad, vencimiento)                           | Todos  |
| `/medicamentos`       | Catálogo de medicamentos                                           | Todos (edición: ADMIN) |
| `/config`             | Configuración de stock mínimo y deseable (protegido con PIN)       | ADMIN  |
| `/importacion`        | Importación de stock desde Excel                                   | ADMIN, FARMACEUTICO |
| `/importacion-siciap` | Importación y asociación de códigos SICIAP                         | ADMIN  |
| `/auditoria`          | Historial de importaciones                                         | ADMIN  |
| `/usuarios`           | Gestión de usuarios y roles                                        | ADMIN  |
| `/export/pdf`, `/export/lotes`, `/export/auditoria` | Exportaciones a Excel (POI)          | Autenticado |

---

## 10. Importación de datos

### 10.1. Importación de stock

1. Se sube el Excel y el sistema agrupa las filas por nombre y concentración (normalizados),
   leyendo lotes, vencimientos y cantidades.
2. Cada grupo se intenta emparejar con el catálogo en este orden: equivalencia guardada,
   coincidencia exacta de nombre + concentración, coincidencia por nombre. Si no hay coincidencia,
   se buscan candidatos por similitud (Jaro-Winkler y Levenshtein, ver `SimilitudService`).
3. La previsualización muestra tres grupos: coincidencias automáticas, pendientes de asignación
   manual, y sin coincidencia.
4. Los ítems sin coincidencia no se descartan: marcando la opción correspondiente se crean como
   medicamentos nuevos (sin código SICIAP, con stock mínimo y deseable por defecto de 100). Se
   evita duplicar si ya existe un medicamento equivalente.

### 10.2. Importación de códigos SICIAP

- Empareja cada código del archivo con un medicamento del catálogo (por nombre y concentración) y
  asigna el código. Si un medicamento creado sin código aparece luego en un archivo SICIAP, el
  emparejamiento por nombre permite completarle el código.

### 10.3. Asociación manual de códigos SICIAP

- Un administrador puede asignar o corregir el código SICIAP de un medicamento desde la pantalla de
  Medicamentos (acción de edición).

---

## 11. Estructura del proyecto

```text
src/main/java/sistema_farmacia/demo/
├── config/        Configuración (seguridad, carga inicial de datos)
├── controller/    Controladores web y endpoints REST
├── model/         Entidades JPA
├── repository/    Repositorios Spring Data
├── service/       Lógica de negocio (inventario, importación, similitud, alertas, WhatsApp)
└── DemoApplication.java

src/main/resources/
├── templates/     Vistas Thymeleaf
├── static/        Recursos estáticos (CSS, JS)
└── application.properties
```

Entidades principales: `Medicamento`, `Lote`, `Usuario`, `Rol`, `Alerta`, `ConsumoHistorico`,
`ImportacionLog`, `MedicamentoEquivalencia`.

---

## 12. Carga inicial de datos (DataLoader)

`config/DataLoader.java` implementa `CommandLineRunner` y se ejecuta una vez al iniciar la
aplicación. Su única función es garantizar que exista un acceso inicial:

- Si la tabla de roles está vacía, crea los roles `ADMIN` y `FARMACEUTICO` y los dos usuarios de
  prueba de la sección 6 (con contraseña y PIN ya hasheados).
- Si ya existen roles, no hace nada: es idempotente y no modifica datos existentes.

Conviene mantenerlo, porque en una base vacía no habría ningún usuario con el cual iniciar sesión.
En un entorno real, las credenciales por defecto deben cambiarse después del primer ingreso.

---

## 13. Notas de mantenimiento

- Cambiar las credenciales por defecto y la contraseña de la base de datos antes de un despliegue
  real.
- Las tablas se generan con `ddl-auto=update`; para un control más estricto del esquema en
  producción se recomienda evaluar migraciones gestionadas (por ejemplo, Flyway o Liquibase).
- Cuando se usa Docker Compose, los datos de PostgreSQL se persisten en el volumen `postgres_data`.
- Realizar backups regulares usando el script `scripts/backup-database.sh`.
