"""
Control de acceso y cadena de frío - Gran Hospital del Sur
Basado en GHI_control_Acceso_2.py (material del docente).

Qué agrega respecto al script original:
  1. Los rostros se descargan del sistema de farmacia (usuarios con foto cargada) en lugar de la carpeta Images.
     Cada usuario trae su permiso individual: AUTORIZADO o SIN PERMISO.
  2. Las lecturas del Arduino y los accesos se envían a la API del sistema, que los guarda en PostgreSQL
     y manda las alertas por WhatsApp.
  3. Se conecta igual a un Arduino real (COM7, /dev/ttyACM0) o al Arduino simulado en Wokwi (rfc2217://...).
  4. Los envíos se hacen en un hilo aparte para que el video no se congele si la red tarda.

Se mantienen del original: protocolo serial, comando ALARMA_INTRUSO, registro solo con puerta abierta,
memoria por persona (90 s en el original, configurable en config.py), log diario en .txt, limpieza de logs de más de 15 días y panel lateral.

Teclas:  R = recargar rostros desde el sistema   ESC = salir
"""

from datetime import datetime, timedelta
import os
import queue
import threading
import time

import cv2
import face_recognition
import numpy as np
import requests
import serial

import config

DIR_SCRIPT = os.path.dirname(os.path.abspath(__file__))

AUTORIZADO = "AUTORIZADO"
SIN_PERMISO = "DENEGADO_SIN_PERMISO"
DESCONOCIDO = "DENEGADO_DESCONOCIDO"

COLORES = {  # BGR
    AUTORIZADO: (125, 220, 0),
    SIN_PERMISO: (0, 165, 255),
    DESCONOCIDO: (50, 50, 255),
}


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


def codificar(rostros):
    """Calcula el vector facial de cada foto. Devuelve (encodings, datos)."""
    encodings, datos = [], []
    for r in rostros:
        rgb = cv2.cvtColor(r["imagen"], cv2.COLOR_BGR2RGB)
        enc = face_recognition.face_encodings(rgb)
        if enc:
            encodings.append(enc[0])
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


def abrir_arduino():
    try:
        # serial_for_url acepta tanto "COM7" o "/dev/ttyACM0" como "rfc2217://localhost:4000"
        arduino = serial.serial_for_url(config.PUERTO_ARDUINO, baudrate=config.VELOCIDAD_BAUD, timeout=0.1)
        time.sleep(2)  # el Arduino real se reinicia al abrir el puerto
        print(f"Conectado al Arduino en {config.PUERTO_ARDUINO}")
        return arduino
    except Exception as e:
        print(f"Advertencia: no se pudo conectar al Arduino en {config.PUERTO_ARDUINO}: {e}")
        print("¿Está corriendo la simulación de Wokwi y la pestaña del simulador está visible?")
        return None


def abrir_camara():
    # DirectShow abre la webcam más rápido en Windows; en Linux se usa V4L2 (backend por defecto).
    backend = cv2.CAP_DSHOW if os.name == "nt" else cv2.CAP_ANY
    return cv2.VideoCapture(config.CAMARA_INDICE, backend)


def dibujar_panel(estado):
    """Panel lateral del script original, con una línea extra para el estado del sistema."""
    panel = np.zeros((480, 350, 3), dtype=np.uint8)
    panel[:] = (40, 40, 40)
    cv2.putText(panel, "CONTROL CADENA FRIO", (20, 40), cv2.FONT_HERSHEY_DUPLEX, 0.7, (0, 255, 255), 2)
    cv2.line(panel, (20, 55), (330, 55), (100, 100, 100), 1)

    color_puerta = (0, 255, 0) if estado["puerta"] == "CERRADA" else (0, 0, 255)
    filas = [
        ("ESTADO PUERTA:", estado["puerta"], color_puerta, 100),
        ("TEMPERATURA:", f"{estado['temp']} C", (255, 255, 255), 175),
        ("TEMPORIZADOR:", estado["tiempo"], (255, 255, 0), 250),
        ("ESTADO SISTEMA:", estado["est"], (0, 165, 255), 325),
    ]
    for titulo, valor, color, y in filas:
        cv2.putText(panel, titulo, (20, y), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)
        cv2.putText(panel, str(valor), (20, y + 30), cv2.FONT_HERSHEY_DUPLEX, 0.8, color, 2)

    conectado = estado["sistema"]
    cv2.putText(panel, "SISTEMA FARMACIA:", (20, 400), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1)
    cv2.putText(panel, "CONECTADO" if conectado else "SIN CONEXION", (20, 430),
                cv2.FONT_HERSHEY_DUPLEX, 0.7, (0, 255, 0) if conectado else (0, 0, 255), 2)
    cv2.putText(panel, "R: recargar rostros  ESC: salir", (20, 465),
                cv2.FONT_HERSHEY_SIMPLEX, 0.45, (150, 150, 150), 1)
    return panel


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
    arduino = abrir_arduino()

    def cargar_rostros():
        try:
            descargados = sistema.descargar_rostros()
            print(f"{len(descargados)} rostro(s) descargados del sistema de farmacia")
        except requests.RequestException as e:
            print(f"No se pudo conectar con {config.URL_SISTEMA} ({e.__class__.__name__}). Uso la carpeta de respaldo.")
            descargados = rostros_respaldo()
        enc, datos = codificar(descargados)
        with rostros_lock:
            rostros["encodings"], rostros["datos"] = enc, datos

    cargar_rostros()

    cap = abrir_camara()
    if not cap.isOpened():
        print(f"Error: no se pudo abrir la cámara {config.CAMARA_INDICE}. Probá otro índice en config.py")
        return
    print("Iniciando reconocimiento. R = recargar rostros, ESC = salir.")

    estado = {"temp": "--", "puerta": "DESCONOCIDA", "tiempo": "00:00:00", "est": "Iniciando...", "sistema": False}
    ultimo_registro = {}
    cuadros_seguidos = {}           # clave -> cantidad de cuadros consecutivos en que se vio
    ultima_sincronizacion = time.time()
    ultimo_envio_lectura = 0.0
    ultima_puerta_enviada = None

    while True:
        ret, frame = cap.read()
        if not ret:
            break
        frame = cv2.flip(frame, 1)

        # --- Lectura del Arduino: se leen todas las líneas pendientes y se usa la última ---
        if arduino:
            try:
                linea = None
                while arduino.in_waiting > 0:
                    linea = arduino.readline().decode("utf-8", errors="ignore").strip() or linea
                if linea and "TEMP:" in linea:
                    for parte in linea.split(","):
                        clave, _, valor = parte.partition(":")
                        if clave == "TEMP":
                            estado["temp"] = valor
                        elif clave == "PUERTA":
                            estado["puerta"] = valor
                        elif clave == "TIEMPO":
                            estado["tiempo"] = valor
                        elif clave == "EST":
                            estado["est"] = valor

                    # Envío al sistema cada N segundos, o en el momento en que cambia la puerta
                    ahora = time.time()
                    if (ahora - ultimo_envio_lectura >= config.ENVIAR_LECTURA_CADA_SEG
                            or estado["puerta"] != ultima_puerta_enviada):
                        try:
                            sistema.enviar_lectura(float(estado["temp"]), estado["puerta"],
                                                   estado["tiempo"], estado["est"])
                            print(f"Lectura enviada al sistema: {estado['temp']} C, puerta {estado['puerta']}, estado {estado['est']}")
                            ultimo_envio_lectura = ahora
                            ultima_puerta_enviada = estado["puerta"]
                        except ValueError:
                            pass  # temperatura todavía no válida
            except Exception as e:
                print(f"Error leyendo serial: {e}")

        frame = cv2.resize(frame, (640, 480))
        frame_pequeno = cv2.resize(frame, (0, 0), fx=config.ESCALA, fy=config.ESCALA)
        rgb_pequeno = cv2.cvtColor(frame_pequeno, cv2.COLOR_BGR2RGB)

        ubicaciones = face_recognition.face_locations(rgb_pequeno)
        codificaciones = face_recognition.face_encodings(rgb_pequeno, ubicaciones)

        with rostros_lock:
            encodings_ref = list(rostros["encodings"])
            datos_ref = list(rostros["datos"])

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

            if estado["puerta"] == "ABIERTA" and confirmado:
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

                    sistema.enviar_acceso(usuario_id, nombre, True, distancia)

                    if resultado != AUTORIZADO and arduino:
                        try:
                            arduino.write(b"ALARMA_INTRUSO\n")
                            print("¡Señal de alarma enviada al Arduino por acceso denegado!")
                        except Exception as e:
                            print(f"Error al enviar datos por serial al Arduino: {e}")

            color = COLORES[resultado]
            etiqueta = nombre if resultado != SIN_PERMISO else f"{nombre} (SIN PERMISO)"
            cv2.rectangle(frame, (left, top), (right, bottom), color, 2)
            cv2.rectangle(frame, (left, bottom - 30), (right, bottom), color, cv2.FILLED)
            cv2.putText(frame, etiqueta, (left + 6, bottom - 6), cv2.FONT_HERSHEY_DUPLEX, 0.6, (255, 255, 255), 1)

        # Olvidar las identidades que no aparecieron en este cuadro
        for clave in list(cuadros_seguidos):
            if clave not in claves_en_cuadro:
                del cuadros_seguidos[clave]

        # Cada N segundos se traen los permisos actuales del sistema (sin volver a bajar fotos)
        if time.time() - ultima_sincronizacion >= config.SINCRONIZAR_PERMISOS_CADA_SEG:
            ultima_sincronizacion = time.time()
            threading.Thread(target=sincronizar_permisos, daemon=True).start()

        estado["sistema"] = sistema.conectado
        cv2.imshow("Control Cadena de Frio Farmacos HGS", np.hstack((frame, dibujar_panel(estado))))

        tecla = cv2.waitKey(1) & 0xFF
        if tecla == 27:
            break
        if tecla in (ord("r"), ord("R")):
            print("Recargando rostros desde el sistema...")
            threading.Thread(target=cargar_rostros, daemon=True).start()

    if arduino:
        arduino.close()
    cap.release()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
