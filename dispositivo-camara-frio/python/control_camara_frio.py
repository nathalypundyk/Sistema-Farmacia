"""
Control de acceso y cadena de frío - Gran Hospital del Sur
Basado en GHI_control_Acceso_2.py (material del docente).

Qué agrega respecto al script original:
  1. Los rostros se descargan del sistema de farmacia (usuarios con foto cargada) en lugar de la carpeta Images.
     Cada usuario trae su permiso individual: AUTORIZADO o SIN PERMISO.
  2. Las lecturas del Arduino y los accesos se envían a la API del sistema, que los guarda en PostgreSQL
     y manda las alertas por WhatsApp.
  3. Se conecta igual a un Arduino real (COM7, /dev/ttyACM0) o al Arduino simulado en Wokwi (rfc2217://...).
     Con PUERTO_ARDUINO = "AUTO" detecta solo en qué puerto está el Arduino real.
  4. Los envíos se hacen en un hilo aparte para que el video no se congele si la red tarda.
  5. Rendimiento: la cámara, el Arduino y el reconocimiento facial trabajan en paralelo.
     El reconocimiento corre en otro proceso (reconocimiento.py), así el video se ve fluido y sin retraso.
  6. Pantalla con la identidad visual de GHS Pharma (pantalla.py).

Se mantienen del original: protocolo serial, comando ALARMA_INTRUSO, registro solo con puerta abierta,
memoria por persona (90 s en el original, configurable en config.py), log diario en .txt, limpieza de logs de más de 15 días y panel lateral.

Teclas:  R = recargar rostros desde el sistema   ESC = salir
"""

from concurrent.futures import ProcessPoolExecutor
from concurrent.futures.process import BrokenProcessPool
from datetime import datetime, timedelta
import multiprocessing
import os
import queue
import threading
import time

import cv2
import face_recognition
import numpy as np
import requests
import serial
from serial.tools import list_ports

import config
import pantalla
import reconocimiento

DIR_SCRIPT = os.path.dirname(os.path.abspath(__file__))

AUTORIZADO = "AUTORIZADO"
SIN_PERMISO = "DENEGADO_SIN_PERMISO"
DESCONOCIDO = "DENEGADO_DESCONOCIDO"

# ─────────────────────────── Cliente del sistema de farmacia ───────────────────────────

class ClienteSistema:
    """Habla con /api/dispositivo/camara-frio del sistema Spring Boot."""

    def __init__(self, url_base, api_key, al_recibir_resultado=None):
        self.base = url_base.rstrip("/") + "/api/dispositivo/camara-frio"
        self.al_recibir_resultado = al_recibir_resultado
        self.sesion = requests.Session()
        self.sesion.headers.update({"X-API-KEY": api_key})
        self.conectado = False
        self.cola = queue.Queue(maxsize=500)
        threading.Thread(target=self._trabajador, daemon=True).start()

    # Descarga sincrónica (solo al arrancar o al presionar R)
    def descargar_rostros(self):
        resp = self.sesion.get(self.base + "/rostros", timeout=5)
        resp.raise_for_status()
        rostros = []
        for r in resp.json():
            foto = self.sesion.get(f"{self.base}/rostros/{r['usuarioId']}/foto", timeout=10)
            if foto.status_code != 200:
                continue
            imagen = cv2.imdecode(np.frombuffer(foto.content, np.uint8), cv2.IMREAD_COLOR)
            if imagen is None:
                continue
            rostros.append({**r, "imagen": imagen})
        self.conectado = True
        return rostros

    # Consulta liviana: solo los permisos actuales (sin descargar fotos)
    def consultar_permisos(self):
        resp = self.sesion.get(self.base + "/rostros", timeout=5)
        resp.raise_for_status()
        self.conectado = True
        return {r["usuarioId"]: bool(r["autorizado"]) for r in resp.json()}

    # Envíos asincrónicos (lecturas y accesos)
    def enviar_lectura(self, temperatura, puerta, tiempo, estado):
        self._encolar("/lecturas", {"temperatura": temperatura, "puerta": puerta,
                                    "tiempo": tiempo, "estado": estado})

    def enviar_acceso(self, usuario_id, nombre, puerta_abierta, distancia):
        self._encolar("/accesos", {"usuarioId": usuario_id, "nombre": nombre,
                                   "puertaAbierta": puerta_abierta, "distancia": distancia})

    def _encolar(self, ruta, datos):
        try:
            self.cola.put_nowait((ruta, datos))
        except queue.Full:
            print("Advertencia: cola de envíos llena, se descarta un dato")

    def _trabajador(self):
        while True:
            ruta, datos = self.cola.get()
            try:
                resp = self.sesion.post(self.base + ruta, json=datos, timeout=5)
                self.conectado = resp.status_code < 500
                if resp.status_code == 401:
                    print("Error: la API_KEY de config.py no coincide con CAMARAFRIO_API_KEY del sistema")
                elif resp.status_code >= 400:
                    print(f"El sistema rechazó {ruta}: {resp.status_code} {resp.text}")
                elif ruta == "/accesos":
                    resultado = resp.json().get("resultado")
                    print(f"Sistema registró el acceso: {resultado}")
                    if self.al_recibir_resultado and datos.get("usuarioId") is not None:
                        self.al_recibir_resultado(datos["usuarioId"], resultado == AUTORIZADO)
            except requests.RequestException as e:
                self.conectado = False
                print(f"Sin conexión con el sistema ({ruta}): {e.__class__.__name__}")


# ─────────────────────────── Utilidades del script original ───────────────────────────

def limpiar_logs_antiguos(dias_limite):
    """Elimina los logs de accesos con más de N días (igual que el script del docente)."""
    tiempo_limite = datetime.now() - timedelta(days=dias_limite)
    for archivo in os.listdir(DIR_SCRIPT):
        if archivo.startswith("log_accesos_") and archivo.endswith(".txt"):
            ruta = os.path.join(DIR_SCRIPT, archivo)
            if datetime.fromtimestamp(os.path.getmtime(ruta)) < tiempo_limite:
                try:
                    os.remove(ruta)
                    print(f"Log antiguo eliminado: {archivo}")
                except OSError as e:
                    print(f"No se pudo eliminar {archivo}: {e}")


def codificar(rostros, motor):
    """Calcula el vector facial de cada foto (en el proceso de reconocimiento). Devuelve (encodings, datos)."""
    encodings, datos = [], []
    vectores = motor.codificar([cv2.cvtColor(r["imagen"], cv2.COLOR_BGR2RGB) for r in rostros])
    for r, enc in zip(rostros, vectores):
        if enc is not None:
            encodings.append(enc)
            datos.append({"usuarioId": r.get("usuarioId"), "nombre": r["nombre"],
                          "autorizado": r.get("autorizado", True)})
            estado = "AUTORIZADO" if r.get("autorizado", True) else "SIN PERMISO"
            print(f"Rostro cargado: {r['nombre']} [{estado}]")
        else:
            print(f"Advertencia: no se detectó un rostro en la foto de {r['nombre']}")
    return encodings, datos


def rostros_respaldo():
    """Fotos de la carpeta local (modo sin conexión, como el script del docente)."""
    carpeta = os.path.join(DIR_SCRIPT, config.CARPETA_RESPALDO)
    rostros = []
    if not os.path.isdir(carpeta):
        print(f"Tampoco existe la carpeta de respaldo {carpeta}")
        return rostros
    for nombre_archivo in os.listdir(carpeta):
        imagen = cv2.imread(os.path.join(carpeta, nombre_archivo))
        if imagen is not None:
            rostros.append({"usuarioId": None, "nombre": os.path.splitext(nombre_archivo)[0],
                            "autorizado": True, "imagen": imagen})
    return rostros


# Fabricantes (VID USB) de las placas y adaptadores USB-serie más comunes en Arduino Uno
VIDS_ARDUINO = {
    0x2341: "Arduino original",
    0x2A03: "Arduino original (arduino.org)",
    0x1A86: "Clon con chip CH340",
    0x10C4: "Adaptador CP210x",
    0x0403: "Adaptador FTDI",
}
PALABRAS_ARDUINO = ("arduino", "ch340", "ch341", "cp210", "ftdi", "usb-serial", "usb serial")


def detectar_puerto_arduino():
    """Devuelve el puerto (ej. "COM5") donde está conectado el Arduino, o None si no encuentra ninguno."""
    puertos = list(list_ports.comports())
    candidatos = []
    for p in puertos:
        descripcion = f"{p.description} {p.manufacturer or ''}".lower()
        if p.vid in VIDS_ARDUINO or any(palabra in descripcion for palabra in PALABRAS_ARDUINO):
            candidatos.append(p)

    if not candidatos:
        print("No se encontró ningún Arduino conectado por USB.")
        if puertos:
            print("Puertos disponibles:")
            for p in puertos:
                print(f"   {p.device}  ->  {p.description}")
            print('Si el Arduino es uno de esos, ponerlo a mano en config.py (ej. PUERTO_ARDUINO = "COM5").')
        else:
            print("Windows no ve ningún puerto COM: revisar el cable USB y, si es un clon, instalar el driver CH340.")
        return None

    elegido = candidatos[0]
    tipo = VIDS_ARDUINO.get(elegido.vid, elegido.description)
    print(f"Arduino detectado en {elegido.device} ({tipo})")
    if len(candidatos) > 1:
        otros = ", ".join(p.device for p in candidatos[1:])
        print(f"Aviso: también hay otros posibles Arduino en {otros}. Si no es el correcto, fijar el puerto en config.py.")
    return elegido.device


def abrir_arduino():
    puerto = config.PUERTO_ARDUINO
    if str(puerto).strip().upper() == "AUTO":
        puerto = detectar_puerto_arduino()
        if puerto is None:
            return None
    try:
        # serial_for_url acepta tanto "COM7" o "/dev/ttyACM0" como "rfc2217://localhost:4000"
        arduino = serial.serial_for_url(puerto, baudrate=config.VELOCIDAD_BAUD, timeout=0.1)
        time.sleep(2)  # el Arduino real se reinicia al abrir el puerto
        print(f"Conectado al Arduino en {puerto}")
        return arduino
    except Exception as e:
        print(f"Advertencia: no se pudo conectar al Arduino en {puerto}: {e}")
        if str(puerto).startswith("rfc2217"):
            print("¿Está corriendo la simulación de Wokwi y la pestaña del simulador está visible?")
        else:
            print("¿Está abierto el Monitor Serie de Arduino IDE u otro programa usando el puerto? Cerrarlo y reintentar.")
        return None


# ─────────────────────────── Trabajo en paralelo (rendimiento) ───────────────────────────

class CamaraEnVivo:
    """Lee la cámara en un hilo propio y guarda solo el cuadro más nuevo.

    Antes, si el reconocimiento tardaba, la cámara acumulaba cuadros viejos y la imagen
    llegaba con 2 o 3 segundos de retraso. Ahora siempre se muestra el último cuadro.
    """

    def __init__(self, indice):
        # DirectShow abre la webcam más rápido en Windows; en Linux se usa V4L2 (backend por defecto).
        backend = cv2.CAP_DSHOW if os.name == "nt" else cv2.CAP_ANY
        self.cap = cv2.VideoCapture(indice, backend)
        # Se pide 640x480 directo a la cámara (las webcams USB suelen abrir en HD y eso es más lento).
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, pantalla.ANCHO_VIDEO)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, pantalla.ALTO_VIDEO)
        self.cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
        self.activa = self.cap.isOpened()
        self._condicion = threading.Condition()
        self._cuadro = None
        self._numero = 0
        self._hilo = None
        if self.activa:
            self._hilo = threading.Thread(target=self._leer, daemon=True)
            self._hilo.start()

    def _leer(self):
        fallos = 0
        while self.activa:
            ret, cuadro = self.cap.read()
            if not ret:
                fallos += 1
                if fallos >= 50:  # ~1 segundo sin imagen: la cámara se desconectó
                    print("Error: la cámara dejó de enviar imágenes")
                    break
                time.sleep(0.02)
                continue
            fallos = 0
            cuadro = cv2.flip(cuadro, 1)
            if cuadro.shape[1] != pantalla.ANCHO_VIDEO or cuadro.shape[0] != pantalla.ALTO_VIDEO:
                cuadro = cv2.resize(cuadro, (pantalla.ANCHO_VIDEO, pantalla.ALTO_VIDEO))
            with self._condicion:
                self._cuadro = cuadro
                self._numero += 1
                self._condicion.notify_all()
        with self._condicion:
            self.activa = False
            self._condicion.notify_all()

    def siguiente(self, ultimo_numero, espera=0.5):
        """Espera un cuadro más nuevo que ultimo_numero. Devuelve (cuadro, numero)."""
        with self._condicion:
            self._condicion.wait_for(lambda: self._numero != ultimo_numero or not self.activa, timeout=espera)
            return self._cuadro, self._numero

    def cerrar(self):
        self.activa = False
        if self._hilo:
            self._hilo.join(timeout=1)
        self.cap.release()


class LectorArduino:
    """Lee el Arduino en un hilo propio y envía las lecturas al sistema.

    Antes, la lectura serial se hacía en el bucle del video y podía frenarlo hasta 0,1 s
    cuando llegaba una línea a medias. Ahora se arma la línea completa antes de usarla.
    """

    def __init__(self, arduino, sistema):
        self.arduino = arduino
        self.sistema = sistema
        self.puerto = None
        if arduino is not None:
            nombre = str(getattr(arduino, "name", None) or getattr(arduino, "portstr", None) or "Conectado")
            self.puerto = "Wokwi (simulado)" if nombre.startswith("rfc2217") else nombre
        self.estado = {"temp": "--", "puerta": "DESCONOCIDA", "tiempo": "00:00:00", "est": "Iniciando..."}
        self._bloqueo = threading.Lock()
        self._bloqueo_escritura = threading.Lock()
        self._activo = arduino is not None
        self._hilo = None
        if self._activo:
            self._hilo = threading.Thread(target=self._leer, daemon=True)
            self._hilo.start()

    def leer_estado(self):
        with self._bloqueo:
            return dict(self.estado)

    def enviar(self, comando):
        """Manda un comando al Arduino. Devuelve False si no hay Arduino o falló el envío."""
        if not self.arduino:
            return False
        try:
            with self._bloqueo_escritura:
                self.arduino.write(comando)
            return True
        except Exception as e:
            print(f"Error al enviar datos por serial al Arduino: {e}")
            return False

    def _leer(self):
        pendiente = b""
        ultimo_envio_lectura = 0.0
        ultima_puerta_enviada = None
        while self._activo:
            try:
                trozo = self.arduino.readline()
            except Exception as e:
                if self._activo:
                    print(f"Error leyendo serial: {e}")
                    time.sleep(1)
                continue
            if not trozo:
                continue
            pendiente += trozo
            if not pendiente.endswith(b"\n"):
                if len(pendiente) > 1000:  # basura sin fin de línea: se descarta
                    pendiente = b""
                continue  # línea incompleta: se espera el resto
            linea = pendiente.decode("utf-8", errors="ignore").strip()
            pendiente = b""
            if "TEMP:" not in linea:
                continue

            with self._bloqueo:
                for parte in linea.split(","):
                    clave, _, valor = parte.partition(":")
                    if clave == "TEMP":
                        self.estado["temp"] = valor
                    elif clave == "PUERTA":
                        self.estado["puerta"] = valor
                    elif clave == "TIEMPO":
                        self.estado["tiempo"] = valor
                    elif clave == "EST":
                        self.estado["est"] = valor
                estado = dict(self.estado)

            # Envío al sistema cada N segundos, o en el momento en que cambia la puerta (igual que antes)
            ahora = time.time()
            if (ahora - ultimo_envio_lectura >= config.ENVIAR_LECTURA_CADA_SEG
                    or estado["puerta"] != ultima_puerta_enviada):
                try:
                    self.sistema.enviar_lectura(float(estado["temp"]), estado["puerta"],
                                                estado["tiempo"], estado["est"])
                    print(f"Lectura enviada al sistema: {estado['temp']} C, puerta {estado['puerta']}, estado {estado['est']}")
                    ultimo_envio_lectura = ahora
                    ultima_puerta_enviada = estado["puerta"]
                except ValueError:
                    pass  # temperatura todavía no válida

    def cerrar(self):
        self._activo = False
        if self._hilo:
            self._hilo.join(timeout=1)
        if self.arduino:
            self.arduino.close()


class MotorReconocimiento:
    """Ejecuta el reconocimiento facial (reconocimiento.py) en un proceso aparte."""

    def __init__(self):
        # "spawn" en todos los sistemas (es lo que usa Windows): el proceso arranca limpio.
        self._contexto = multiprocessing.get_context("spawn")
        self._ejecutor = self._crear()

    def _crear(self):
        return ProcessPoolExecutor(max_workers=1, mp_context=self._contexto)

    def analizar(self, rgb_pequeno):
        """Devuelve un Future con (ubicaciones, codificaciones), o None si no se pudo enviar."""
        try:
            return self._ejecutor.submit(reconocimiento.analizar, rgb_pequeno)
        except (BrokenProcessPool, RuntimeError):
            self.reiniciar()
            return None

    def codificar(self, fotos_rgb):
        """Vectores de las fotos de referencia (espera el resultado)."""
        if not fotos_rgb:
            return []
        try:
            return self._ejecutor.submit(reconocimiento.codificar_fotos, fotos_rgb).result()
        except BrokenProcessPool:
            self.reiniciar()
            return self._ejecutor.submit(reconocimiento.codificar_fotos, fotos_rgb).result()

    def reiniciar(self):
        print("El proceso de reconocimiento se detuvo; se vuelve a iniciar...")
        self._ejecutor.shutdown(wait=False, cancel_futures=True)
        self._ejecutor = self._crear()

    def cerrar(self):
        self._ejecutor.shutdown(wait=False, cancel_futures=True)


# ─────────────────────────────────── Programa principal ───────────────────────────────────

def main():
    limpiar_logs_antiguos(config.DIAS_LOGS)
    archivo_log = os.path.join(DIR_SCRIPT, f"log_accesos_{datetime.now().strftime('%d-%m-%Y')}.txt")

    # Carga de rostros: primero el sistema; si no responde, la carpeta de respaldo.
    rostros_lock = threading.Lock()
    rostros = {"encodings": [], "datos": []}

    def actualizar_permiso(usuario_id, autorizado):
        """Sincroniza el permiso local con el que tiene el sistema (la fuente de verdad)."""
        with rostros_lock:
            for d in rostros["datos"]:
                if d["usuarioId"] == usuario_id and d["autorizado"] != autorizado:
                    d["autorizado"] = autorizado
                    print(f"Permiso actualizado desde el sistema: {d['nombre']} -> "
                          f"{'AUTORIZADO' if autorizado else 'SIN PERMISO'}")

    def sincronizar_permisos():
        try:
            for usuario_id, autorizado in sistema.consultar_permisos().items():
                actualizar_permiso(usuario_id, autorizado)
        except requests.RequestException:
            pass  # sin conexión: se mantiene la lista actual

    sistema = ClienteSistema(config.URL_SISTEMA, config.API_KEY, actualizar_permiso)
    arduino = LectorArduino(abrir_arduino(), sistema)
    motor = MotorReconocimiento()

    def cargar_rostros():
        try:
            descargados = sistema.descargar_rostros()
            print(f"{len(descargados)} rostro(s) descargados del sistema de farmacia")
        except requests.RequestException as e:
            print(f"No se pudo conectar con {config.URL_SISTEMA} ({e.__class__.__name__}). Uso la carpeta de respaldo.")
            descargados = rostros_respaldo()
        enc, datos = codificar(descargados, motor)
        with rostros_lock:
            rostros["encodings"], rostros["datos"] = enc, datos

    print("Preparando el reconocimiento facial (la primera vez tarda unos segundos)...")
    cargar_rostros()

    camara = CamaraEnVivo(config.CAMARA_INDICE)
    if not camara.activa:
        print(f"Error: no se pudo abrir la cámara {config.CAMARA_INDICE}. Probá otro índice en config.py")
        arduino.cerrar()
        motor.cerrar()
        return
    print("Iniciando reconocimiento. R = recargar rostros, ESC = salir.")
    pantalla.crear_ventana()

    ultimo_registro = {}
    cuadros_seguidos = {}           # clave -> cantidad de cuadros analizados seguidos en que se vio
    ultima_sincronizacion = time.time()
    ultimo_acceso = None            # (nombre, resultado, hora) para el panel
    caras = []                      # recuadros del último análisis, para dibujar
    momento_caras = 0.0
    analisis = None                 # Future del análisis en curso
    numero_cuadro = 0
    numero_analizado = 0
    ventana_vista = False           # para detectar el cierre con la X solo después de que se vio la ventana

    def procesar_analisis(ubicaciones, codificaciones, puerta_abierta):
        """La misma lógica de acceso del script original, aplicada a cada cuadro analizado."""
        nonlocal ultimo_acceso
        with rostros_lock:
            encodings_ref = list(rostros["encodings"])
            datos_ref = list(rostros["datos"])

        nuevas_caras = []
        claves_en_cuadro = set()
        for (top, right, bottom, left), enc_actual in zip(ubicaciones, codificaciones):
            factor = 1 / config.ESCALA
            top, right, bottom, left = (int(v * factor) for v in (top, right, bottom, left))

            nombre, usuario_id, resultado, distancia = "Desconocido", None, DESCONOCIDO, None
            if encodings_ref:
                distancias = face_recognition.face_distance(encodings_ref, enc_actual)
                mejor = int(np.argmin(distancias))
                if distancias[mejor] < config.TOLERANCIA:
                    d = datos_ref[mejor]
                    nombre, usuario_id, distancia = d["nombre"], d["usuarioId"], float(distancias[mejor])
                    resultado = AUTORIZADO if d["autorizado"] else SIN_PERMISO

            # --- Igual que el docente: se registra solo con la puerta abierta y con memoria por persona ---
            # Confirmación: la misma identidad tiene que verse varios cuadros seguidos
            # (evita registrar "Desconocido" por un cuadro movido o mal iluminado).
            clave = usuario_id if usuario_id is not None else nombre
            claves_en_cuadro.add(clave)
            cuadros_seguidos[clave] = cuadros_seguidos.get(clave, 0) + 1
            necesarios = config.CUADROS_CONFIRMAR_DESCONOCIDO if resultado == DESCONOCIDO else config.CUADROS_CONFIRMAR
            confirmado = cuadros_seguidos[clave] >= necesarios

            if puerta_abierta and confirmado:
                ahora = datetime.now()
                previo = ultimo_registro.get(clave)  # (momento, resultado) del último registro
                if (previo is None or previo[1] != resultado
                        or (ahora.timestamp() - previo[0]) > config.COOLDOWN_SEGUNDOS):
                    texto = "ACCESO AUTORIZADO" if resultado == AUTORIZADO else "ACCESO DENEGADO"
                    mensaje = f"{ahora:%Y-%m-%d %H:%M:%S} - [{texto}] - Persona detectada: {nombre} (Puerta Abierta)\n"
                    with open(archivo_log, "a", encoding="utf-8") as f:
                        f.write(mensaje)
                    print(f"Log registrado: {mensaje.strip()}")
                    ultimo_registro[clave] = (ahora.timestamp(), resultado)
                    ultimo_acceso = (nombre, resultado, f"{ahora:%H:%M:%S}")

                    sistema.enviar_acceso(usuario_id, nombre, True, distancia)

                    if resultado != AUTORIZADO:
                        if arduino.enviar(b"ALARMA_INTRUSO\n"):
                            print("¡Señal de alarma enviada al Arduino por acceso denegado!")
                    else:
                        print(f"Acceso autorizado para {nombre}")
                        if arduino.enviar(b"ACCESO_OK\n"):
                            print("Señal de acceso autorizado enviada al Arduino")

            nuevas_caras.append((top, right, bottom, left, nombre, resultado))

        # Olvidar las identidades que no aparecieron en este cuadro
        for clave in list(cuadros_seguidos):
            if clave not in claves_en_cuadro:
                del cuadros_seguidos[clave]
        return nuevas_caras

    try:
        while True:
            cuadro, numero_cuadro = camara.siguiente(numero_cuadro)
            if not camara.activa or cuadro is None:
                break
            cuadro = cuadro.copy()
            estado = arduino.leer_estado()

            # --- Resultado del análisis anterior (si ya terminó) ---
            if analisis is not None and analisis.done():
                try:
                    ubicaciones, codificaciones = analisis.result()
                    caras = procesar_analisis(ubicaciones, codificaciones, estado["puerta"] == "ABIERTA")
                    momento_caras = time.time()
                except BrokenProcessPool:
                    motor.reiniciar()
                except Exception as e:
                    print(f"Error en el reconocimiento facial: {e}")
                analisis = None

            # --- Mandar a analizar el cuadro más nuevo (de a uno por vez) ---
            if analisis is None and numero_cuadro != numero_analizado:
                pequeno = cv2.resize(cuadro, (0, 0), fx=config.ESCALA, fy=config.ESCALA)
                analisis = motor.analizar(cv2.cvtColor(pequeno, cv2.COLOR_BGR2RGB))
                numero_analizado = numero_cuadro

            # --- Dibujo ---
            if time.time() - momento_caras > 1.0:
                caras = []  # análisis viejo (por ejemplo, mientras se recargan los rostros)
            for top, right, bottom, left, nombre, resultado in caras:
                pantalla.dibujar_cara(cuadro, top, right, bottom, left, nombre, resultado)
            pantalla.dibujar_aviso_puerta(cuadro, estado["puerta"])

            # Cada N segundos se traen los permisos actuales del sistema (sin volver a bajar fotos)
            if time.time() - ultima_sincronizacion >= config.SINCRONIZAR_PERMISOS_CADA_SEG:
                ultima_sincronizacion = time.time()
                threading.Thread(target=sincronizar_permisos, daemon=True).start()

            with rostros_lock:
                n_rostros = len(rostros["datos"])
            panel = pantalla.dibujar_panel(estado, arduino.arduino is not None, arduino.puerto,
                                           sistema.conectado, n_rostros, ultimo_acceso)
            cv2.imshow(pantalla.TITULO_VENTANA, pantalla.componer(cuadro, panel))

            tecla = cv2.waitKey(1) & 0xFF
            if tecla == 27:
                break
            if pantalla.ventana_visible():
                ventana_vista = True
            elif ventana_vista:
                break  # se cerró la ventana con la X
            if tecla in (ord("r"), ord("R")):
                print("Recargando rostros desde el sistema...")
                threading.Thread(target=cargar_rostros, daemon=True).start()
    finally:
        arduino.cerrar()
        camara.cerrar()
        motor.cerrar()
        cv2.destroyAllWindows()


if __name__ == "__main__":
    multiprocessing.freeze_support()
    main()
