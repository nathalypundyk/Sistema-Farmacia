# Cámara de frío: Arduino simulado + reconocimiento facial + Sistema Farmacia

Módulo de la materia **Electrónica Digital y Microprocesadores**, integrado al Sistema de Farmacia del Gran Hospital del Sur.
Como el hardware no está disponible, el Arduino se simula en **Wokwi for VS Code**. El sketch del docente (`cadenafrioControl_e.ino`) se usa **sin modificaciones**.

```
 Wokwi (Arduino Uno simulado)            PC de la demo (Windows)                   Sistema Farmacia (Docker)
 DS18B20 · puerta · buzzer · LCD  ─RFC2217─▶ control_camara_frio.py  ──HTTP + X-API-KEY──▶  Spring Boot + PostgreSQL
            ▲  ALARMA_INTRUSO       :4000    (webcam + face_recognition)                       │
            └──────────────────────────────────────┘                                          └──▶ WhatsApp (encargada)
```

## Carpetas

| Carpeta | Contenido |
|---|---|
| `arduino/cadenafrioControl_e/` | Sketch del docente sin cambios, `diagram.json` (circuito), `wokwi.toml` y `libraries.txt` |
| `python/` | `control_camara_frio.py` (script puente), `config.py` y `requirements.txt` |

## Paso 1: levantar el sistema de farmacia (rama `feature/camara-frio`)

```powershell
git checkout feature/camara-frio
copy .env.example .env      # revisar CAMARAFRIO_API_KEY
docker compose up -d --build
```

Entrar a `http://localhost:8080` con un usuario ADMIN. En el menú aparece **Cadena de frío**.
Al final de esa pantalla, en "Personal con acceso a la cámara", cargar la foto de cada integrante y marcar **Autorizar**.
Para la demo conviene dejar a una persona con foto pero **sin** autorizar, así se ve el caso "Denegado · sin permiso".

## Paso 2: simular el Arduino en Wokwi

1. Instalar **VS Code**, la extensión **Wokwi Simulator** y **Arduino IDE 2**.
2. En VS Code: `F1 > Wokwi: Request a New License` (es gratuita, se pide con la cuenta de wokwi.com).
3. Abrir `arduino/cadenafrioControl_e/cadenafrioControl_e.ino` en Arduino IDE, instalar las bibliotecas
   **OneWire**, **DallasTemperature** y **LiquidCrystal I2C** (Frank de Brabander), elegir placa **Arduino Uno**
   y hacer `Sketch > Export Compiled Binary`. Se crea la carpeta `build/arduino.avr.uno/`.
4. En VS Code abrir la carpeta `arduino/cadenafrioControl_e/` y ejecutar `F1 > Wokwi: Start Simulator`.
   **La pestaña del simulador tiene que quedar visible**; si se oculta, la simulación se pausa.

Durante la simulación:
- **Temperatura:** clic sobre el sensor DS18B20 y mover el control deslizante. Por encima de 22 °C suena el buzzer.
- **Puerta:** el interruptor deslizante es el fin de carrera. En una posición la puerta queda ABIERTA (LED encendido y empieza el temporizador).
- A los 5 minutos de puerta abierta el Arduino muestra `tiempo max-exed`.

## Paso 3: script de reconocimiento facial

### Instalar face_recognition en Windows (evita compilar dlib)

Usar **Python 3.11 o 3.12 de 64 bits**:

```powershell
cd dispositivo-camara-frio\python
py -3.11 -m venv venv
venv\Scripts\activate
pip install dlib-bin
pip install --no-deps face_recognition face_recognition_models
pip install opencv-python numpy pyserial requests pillow click "setuptools<81"
```

Si `pip install dlib-bin` falla, la alternativa es instalar **Visual Studio Build Tools** (carga "Desarrollo para el escritorio con C++") y **CMake**, y después `pip install face_recognition`.

### Configurar y ejecutar

Revisar `config.py`: `URL_SISTEMA`, `API_KEY` (igual a la del `.env`), `PUERTO_ARDUINO` y `CAMARA_INDICE`.

```powershell
python control_camara_frio.py
```

Teclas en la ventana de la cámara: **R** recarga los rostros desde el sistema (después de cargar fotos nuevas), **ESC** sale.
Los **permisos** se sincronizan solos cada 10 segundos; la tecla R solo hace falta cuando se cargan **fotos nuevas**.
Para evitar falsas alarmas, una persona se registra recién cuando se la reconoce en 3 cuadros seguidos (6 si es desconocida).

## Guion sugerido para la demo (5 minutos)

1. Mostrar el circuito en Wokwi y el LCD con temperatura y puerta.
2. Mostrar la pantalla **Cadena de frío** del sistema: temperatura en vivo, gráfico y dispositivo "En línea".
3. Subir la temperatura a más de 22 °C en el sensor: suena el buzzer, la tarjeta se pone roja y llega el WhatsApp.
4. Abrir la puerta (interruptor) y ponerse frente a la cámara una persona **autorizada**: recuadro verde y registro "Autorizado".
5. Persona con foto pero **sin permiso**: recuadro naranja, buzzer, registro "Denegado · sin permiso" y WhatsApp.
6. Persona **no registrada**: recuadro rojo, buzzer, registro "Denegado · desconocido".
7. Quitar el permiso a alguien desde el sistema, esperar unos 10 segundos y mostrar que ahora se le deniega el acceso.

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| `No se pudo conectar al Arduino en rfc2217://...` | Wokwi no está corriendo o la pestaña del simulador no está visible |
| El panel muestra `SIN CONEXION` | El sistema no está levantado o la `API_KEY` no coincide (mirar la consola: dice 401) |
| `No module named 'pkg_resources'` | Falta `pip install "setuptools<81"` |
| No abre la cámara | Probar `CAMARA_INDICE = 1` en `config.py` |
| Temperatura `-127` | El sensor está desconectado en el diagrama (el sistema descarta esas lecturas) |
| Se registra a la misma persona una sola vez | Es la memoria por persona (`COOLDOWN_SEGUNDOS` en `config.py`, 15 s en la demo; el docente usa 90 s) |
| La temperatura no cambia en la pantalla | Mirar la consola del script: tiene que mostrar `Lectura enviada al sistema...` cada 5 s |
