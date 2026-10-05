"""
Configuración del script puente de la cámara de frío.
Es el único archivo que hay que tocar para cambiar de PC, de sistema operativo
o de Arduino simulado a Arduino real.
"""

# --- Sistema de farmacia (Spring Boot) ---------------------------------------
# Con docker-compose la app queda en el puerto 8080 (APP_PORT en .env).
URL_SISTEMA = "http://localhost:8080"
# Debe ser igual a CAMARAFRIO_API_KEY del .env / docker-compose del sistema.
API_KEY = "hgs-camara-frio-demo"

# --- Arduino -------------------------------------------------------------------
# "AUTO" = busca solo el Arduino real conectado por USB (Uno original o clon con CH340/CP210x/FTDI).
#          Si el COM cambia porque se enchufa en otro USB o en otra PC, no hay que tocar nada.
PUERTO_ARDUINO = "AUTO"
# Para fijar un puerto a mano, reemplazar "AUTO" por uno de estos:
#   Arduino real en Windows:  PUERTO_ARDUINO = "COM7"
#   Arduino real en Linux:    PUERTO_ARDUINO = "/dev/ttyACM0"   (o /dev/ttyUSB0 en clones con CH340)
#   Arduino simulado en Wokwi (wokwi.toml -> rfc2217ServerPort = 4000):
#                             PUERTO_ARDUINO = "rfc2217://localhost:4000?ign_set_control"
VELOCIDAD_BAUD = 9600

# --- Cámara ----------------------------------------------------------------------
# 0 = webcam integrada de la notebook. Si hay una cámara USB extra, probar 1 o 2.
CAMARA_INDICE = 0

# --- Reconocimiento facial -------------------------------------------------------
TOLERANCIA = 0.6            # distancia máxima para considerar que dos rostros coinciden (la del docente)
ESCALA = 0.5                # tamaño de la imagen que se analiza (el docente usa 0.25; 0.5 detecta mejor a distancia)
CUADROS_CONFIRMAR = 3       # cuadros seguidos para confirmar a una persona registrada
CUADROS_CONFIRMAR_DESCONOCIDO = 6   # cuadros seguidos para confirmar a un desconocido (evita falsas alarmas)
# No repetir el registro de la misma persona antes de este tiempo.
# El script del docente usa 90 s; para la demostración se baja a 15 s.
# Si cambia el resultado (por ejemplo, se le quitó el permiso), se registra en el momento.
COOLDOWN_SEGUNDOS = 15

# --- Envío de datos --------------------------------------------------------------
ENVIAR_LECTURA_CADA_SEG = 5     # cada cuánto se manda la temperatura al sistema (o antes si cambia la puerta)
SINCRONIZAR_PERMISOS_CADA_SEG = 10  # cada cuánto se consultan los permisos actuales (las fotos nuevas siguen con R)

# --- Respaldo sin conexión -------------------------------------------------------
# Si el sistema no responde al arrancar, se usan las fotos de esta carpeta (como en el script del docente).
CARPETA_RESPALDO = "Images"
DIAS_LOGS = 15
