# 💊 Sistema de Gestión de Farmacia — Hospital HGS

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Sistema integral para el control de stock de medicamentos hospitalarios. Diseñado para optimizar la gestión de inventario, prevenir quiebres de stock y alertar sobre lotes próximos a vencer mediante un sistema inteligente basado en reglas de negocio.

---

## ✨ Características Principales

- **Control de Stock en Tiempo Real:** Visualización instantánea del inventario disponible.
- **Alertas Inteligentes:** Notificaciones automáticas de stock bajo y vencimientos inminentes.
- **Gestión por Lotes:** Trazabilidad completa de cada medicamento por fecha de vencimiento y depósito.
- **Dashboard Dinámico:** Interfaz moderna y responsive para una gestión eficiente.
- **Multi-Depósito:** Soporte para múltiples ubicaciones físicas (Farmacia Central, Depósito Externo, etc.).

---

## 🛠️ Tecnologías Utilizadas

| Componente | Tecnología | Propósito |
| :--- | :--- | :--- |
| **Backend** | Java 17 / Spring Boot 3 | Lógica central y API |
| **Persistencia** | Spring Data JPA / Hibernate | Gestión de base de datos |
| **Base de Datos** | H2 (Dev) / PostgreSQL (Prod) | Almacenamiento de datos |
| **Frontend** | Thymeleaf / HTML5 / CSS3 / JS | Vistas dinámicas y UI |
| **Herramientas** | Maven / Lombok / Docker | Gestión de dependencias y despliegue |

---

## 🚀 Guía de Instalación y Ejecución

Sigue estos pasos para poner en funcionamiento el sistema en tu entorno local.

### 1. Requisitos Previos

- **JDK 17 o superior**
- **Maven 3.8+**
- **Git** (opcional, para clonar el repo)
- **Docker & Docker Compose** (opcional, para despliegue con base de datos real)

### 2. Clonar el Proyecto

```bash
git clone https://github.com/tu-usuario/sistema-farmacia.git
cd sistema-farmacia
```

### 3. Configuración y Ejecución (Local)

El sistema está configurado por defecto para usar una base de datos **H2 en memoria**, por lo que no necesitas configurar ninguna base de datos externa para probarlo.

#### Usando Maven:
```bash
mvn clean install
mvn spring-boot:run
```

#### Usando un IDE (NetBeans / IntelliJ / VS Code):
1. Abre el proyecto como un proyecto Maven.
2. Espera a que se descarguen las dependencias.
3. Ejecuta la clase `DemoApplication.java`.

### 4. Ejecución con Docker (PostgreSQL)

Si prefieres usar una base de datos PostgreSQL real, puedes usar el entorno Dockerizado:

```bash
mvn clean package -DskipTests
docker-compose up --build
```

---

## 📊 Acceso al Sistema

Una vez iniciado, el sistema estará disponible en:

- **Dashboard:** [http://localhost:8080](http://localhost:8080)
- **H2 Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
  - *JDBC URL:* `jdbc:h2:mem:farmaciadb`
  - *User:* `sa` / *Password:* (vacío)

---

## 📂 Estructura del Proyecto

```text
src/main/java/sistema_farmacia/demo/
├── model/        # Entidades JPA (Tablas)
├── repository/   # Interfaces de acceso a datos
├── service/      # Lógica de negocio y alertas
├── controller/   # Controladores Web (MVC)
└── DataLoader/   # Carga inicial de datos de prueba
```

---

## ⚙️ Configuración de Alertas

Puedes ajustar las reglas de negocio en `AlertaService.java`:

```java
private static final int UMBRAL_STOCK_BAJO = 10;        // Unidades mínimas
private static final int DIAS_PROXIMO_VENCIMIENTO = 30; // Días de margen
```

---

## 🤝 Contribuciones

1. Haz un Fork del proyecto.
2. Crea una rama para tu mejora (`git checkout -b feature/MejoraIncreible`).
3. Haz commit de tus cambios (`git commit -m 'Add: Nueva funcionalidad'`).
4. Haz Push a la rama (`git push origin feature/MejoraIncreible`).
5. Abre un Pull Request.

---

Desarrollado para el **Hospital HGS** - Gestión de Farmacia Eficiente.
sa`
- **Password:** *(dejar vacío)*

---

## Patrones de diseño usados (MVC)

| Capa | Clase | Responsabilidad |
|---|---|---|
| **Model** | `Medicamento`, `Lote`, etc. | Representan los datos y las tablas |
| **Repository** | `MedicamentoRepository`, etc. | Acceso a la base de datos (SQL automático) |
| **Service** | `AlertaService` | Lógica de negocio (cálculos, reglas) |
| **Controller** | `MedicamentoController` | Recibe la request, llama al service, devuelve la vista |
| **View** | `index.html` | Muestra los datos con Thymeleaf |

---
