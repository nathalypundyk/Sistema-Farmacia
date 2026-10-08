"""
Diseño de la ventana del control de cadena de frío, con la identidad visual de GHS Pharma
(los mismos colores del modo oscuro del sistema web: static/css/style.css).

OpenCV solo trae letras sin acentos, así que los textos se dibujan con Pillow usando
Segoe UI (la letra que usa el sistema en Windows). Para no gastar tiempo en cada cuadro:
  - el panel lateral se redibuja solo cuando cambia algún dato;
  - las etiquetas que van sobre el video se guardan en memoria y se reutilizan.
"""

from functools import lru_cache

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ANCHO_VIDEO, ALTO_VIDEO = 640, 480
ANCHO_PANEL = 360
TITULO_VENTANA = "GHS Pharma - Cadena de frio"   # sin acentos: Windows los muestra mal en el título


# ─────────────────────────── Paleta del sistema (RGB, como en el CSS) ───────────────────────────

def _hex(color):
    color = color.lstrip("#")
    return tuple(int(color[i:i + 2], 16) for i in (0, 2, 4))


FONDO = _hex("#0f172a")         # --bg-main (modo oscuro)
TARJETA = _hex("#1e293b")       # --card-bg
BORDE = _hex("#334155")         # --border-color
TEXTO = _hex("#f8fafc")         # --text-primary
TEXTO_2 = _hex("#94a3b8")       # --text-secondary
ACENTO = _hex("#3b82f6")        # --accent
EXITO = _hex("#10b981")         # --success
AVISO = _hex("#f59e0b")         # --warning
PELIGRO = _hex("#ef4444")       # --danger


def bgr(rgb):
    return rgb[2], rgb[1], rgb[0]


# ─────────────────────────── Tipografía ───────────────────────────

_FUENTES = {
    "regular": ["segoeui.ttf", "C:/Windows/Fonts/segoeui.ttf",
                "/usr/share/fonts/opentype/inter/Inter-Regular.otf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "DejaVuSans.ttf", "arial.ttf"],
    "semi": ["seguisb.ttf", "C:/Windows/Fonts/seguisb.ttf",
             "/usr/share/fonts/opentype/inter/Inter-SemiBold.otf",
             "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "DejaVuSans-Bold.ttf", "arialbd.ttf"],
    "negrita": ["segoeuib.ttf", "C:/Windows/Fonts/segoeuib.ttf",
                "/usr/share/fonts/opentype/inter/Inter-Bold.otf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", "DejaVuSans-Bold.ttf", "arialbd.ttf"],
}


@lru_cache(maxsize=None)
def fuente(tamano, peso="regular"):
    for nombre in _FUENTES[peso]:
        try:
            return ImageFont.truetype(nombre, tamano)
        except OSError:
            continue
    try:
        return ImageFont.load_default(size=tamano)   # Pillow 10.1 o superior
    except TypeError:
        return ImageFont.load_default()


def _ancho(draw, texto, f):
    return draw.textlength(texto, font=f)


def _recortar(draw, texto, f, ancho_max):
    """Acorta el texto con '…' si no entra en el ancho disponible."""
    if _ancho(draw, texto, f) <= ancho_max:
        return texto
    while texto and _ancho(draw, texto + "…", f) > ancho_max:
        texto = texto[:-1]
    return texto + "…"


# ─────────────────────────── Traducción de los datos del Arduino ───────────────────────────

# EST que manda el sketch -> (texto para mostrar, color)
ESTADOS_ARDUINO = {
    "ok": ("Normal", EXITO),
    "alert temp": ("Temperatura alta", PELIGRO),
    "tiempo max-exed": ("Abierta más de 5 min", AVISO),
    "alerta intruso!": ("Alerta de intruso", PELIGRO),
    "acceso autorizado": ("Acceso autorizado", EXITO),
}

RESULTADOS = {  # resultado del acceso -> (texto, color)
    "AUTORIZADO": ("Autorizado", EXITO),
    "DENEGADO_SIN_PERMISO": ("Sin permiso", AVISO),
    "DENEGADO_DESCONOCIDO": ("No registrado", PELIGRO),
}


def texto_temperatura(valor):
    try:
        t = float(valor)
    except (TypeError, ValueError):
        return "-- °C", False
    if t <= -100:                      # el DS18B20 devuelve -127 cuando no responde
        return "Sin sensor", True
    return f"{t:.1f} °C".replace(".", ","), False


def texto_estado(est, hay_arduino):
    if not hay_arduino:
        return "Sin Arduino", TEXTO_2
    clave = (est or "").strip().lower()
    if clave in ESTADOS_ARDUINO:
        return ESTADOS_ARDUINO[clave]
    return (est.strip() or "Esperando datos"), TEXTO_2


def texto_puerta(puerta):
    return {"ABIERTA": ("Abierta", AVISO), "CERRADA": ("Cerrada", EXITO)}.get(puerta, ("Sin datos", TEXTO_2))


# ─────────────────────────── Panel lateral ───────────────────────────

def _tarjeta(draw, x0, y0, x1, y1):
    draw.rounded_rectangle((x0, y0, x1, y1), radius=12, fill=TARJETA, outline=BORDE, width=1)


def _etiqueta(draw, x, y, texto):
    draw.text((x, y), texto, font=fuente(12, "semi"), fill=TEXTO_2)


def _pastilla(draw, x_derecha, y, texto, color, alinear="derecha"):
    """Etiqueta redondeada con un punto de color, como los 'badge' del sistema web."""
    f = fuente(12, "semi")
    ancho = int(_ancho(draw, texto, f)) + 30
    x0 = x_derecha - ancho if alinear == "derecha" else x_derecha
    fondo = tuple(int(c * 0.22 + t * 0.78) for c, t in zip(color, TARJETA))
    draw.rounded_rectangle((x0, y, x0 + ancho, y + 24), radius=12, fill=fondo)
    draw.ellipse((x0 + 10, y + 8, x0 + 18, y + 16), fill=color)
    draw.text((x0 + 23, y + 4), texto, font=f, fill=color)
    return ancho


def _logo(draw, x, y):
    draw.rounded_rectangle((x, y, x + 40, y + 40), radius=10, fill=ACENTO)
    draw.rounded_rectangle((x + 16, y + 8, x + 24, y + 32), radius=2, fill=TEXTO)
    draw.rounded_rectangle((x + 8, y + 16, x + 32, y + 24), radius=2, fill=TEXTO)


def _fila_estado(draw, y, titulo, valor, color):
    draw.ellipse((22, y + 6, 30, y + 14), fill=color)
    draw.text((40, y), titulo, font=fuente(13), fill=TEXTO_2)
    f = fuente(13, "semi")
    draw.text((ANCHO_PANEL - 20 - _ancho(draw, valor, f), y), valor, font=f, fill=TEXTO)


@lru_cache(maxsize=8)
def _panel_cacheado(datos):
    (temp, puerta, tiempo, est, hay_arduino, puerto, sistema_ok, n_rostros,
     acceso_nombre, acceso_resultado, acceso_hora) = datos

    img = Image.new("RGB", (ANCHO_PANEL, ALTO_VIDEO), FONDO)
    d = ImageDraw.Draw(img)
    d.line((0, 0, 0, ALTO_VIDEO), fill=BORDE, width=1)

    # Encabezado
    _logo(d, 20, 18)
    d.text((72, 15), "GHS Pharma", font=fuente(20, "negrita"), fill=TEXTO)
    d.text((72, 42), "Cadena de frío · Depósito central", font=fuente(13), fill=TEXTO_2)
    d.line((20, 76, ANCHO_PANEL - 20, 76), fill=BORDE, width=1)

    # Temperatura
    texto_est, color_est = texto_estado(est, hay_arduino)
    valor_temp, error_sensor = texto_temperatura(temp)
    alerta_temp = color_est == PELIGRO and "temp" in str(est).lower()
    color_temp = PELIGRO if (error_sensor or alerta_temp) else TEXTO
    _tarjeta(d, 20, 90, ANCHO_PANEL - 20, 200)
    _etiqueta(d, 36, 104, "TEMPERATURA")
    _pastilla(d, ANCHO_PANEL - 34, 100, _recortar(d, texto_est, fuente(12, "semi"), 170), color_est)
    d.text((34, 124), valor_temp, font=fuente(46 if not error_sensor else 34, "negrita"), fill=color_temp)

    # Puerta y tiempo abierta
    medio = ANCHO_PANEL // 2
    texto_p, color_p = texto_puerta(puerta)
    _tarjeta(d, 20, 212, medio - 6, 292)
    _etiqueta(d, 36, 226, "PUERTA")
    d.ellipse((36, 260, 46, 270), fill=color_p)
    d.text((54, 250), texto_p, font=fuente(22, "semi"), fill=TEXTO)

    _tarjeta(d, medio + 6, 212, ANCHO_PANEL - 20, 292)
    _etiqueta(d, medio + 22, 226, "TIEMPO ABIERTA")
    d.text((medio + 22, 250), tiempo, font=fuente(22, "semi"),
           fill=AVISO if puerta == "ABIERTA" else TEXTO)

    # Último acceso
    _tarjeta(d, 20, 304, ANCHO_PANEL - 20, 392)
    _etiqueta(d, 36, 318, "ÚLTIMO ACCESO")
    f_r = fuente(12)
    texto_r = f"{n_rostros} rostro{'s' if n_rostros != 1 else ''} cargado{'s' if n_rostros != 1 else ''}"
    d.text((ANCHO_PANEL - 36 - _ancho(d, texto_r, f_r), 318), texto_r, font=f_r, fill=TEXTO_2)
    if acceso_nombre:
        d.text((36, 336), _recortar(d, acceso_nombre, fuente(18, "semi"), 200),
               font=fuente(18, "semi"), fill=TEXTO)
        txt_res, col_res = RESULTADOS.get(acceso_resultado, (acceso_resultado, TEXTO_2))
        _pastilla(d, 36, 361, txt_res, col_res, alinear="izquierda")
        f_h = fuente(13)
        d.text((ANCHO_PANEL - 36 - _ancho(d, acceso_hora, f_h), 364), acceso_hora, font=f_h, fill=TEXTO_2)
    else:
        d.text((36, 342), "Todavía no hay registros", font=fuente(15), fill=TEXTO_2)
        d.text((36, 364), "Se registra con la puerta abierta", font=fuente(12), fill=TEXTO_2)

    # Conexiones
    _fila_estado(d, 406, "Sistema de farmacia", "Conectado" if sistema_ok else "Sin conexión",
                 EXITO if sistema_ok else PELIGRO)
    _fila_estado(d, 428, "Arduino", puerto if hay_arduino else "No conectado",
                 EXITO if hay_arduino else PELIGRO)

    # Atajos
    d.line((20, 452, ANCHO_PANEL - 20, 452), fill=BORDE, width=1)
    f_a = fuente(12)
    atajos = "R  recargar rostros     ESC  salir"
    d.text(((ANCHO_PANEL - _ancho(d, atajos, f_a)) / 2, 458), atajos, font=f_a, fill=TEXTO_2)

    return cv2.cvtColor(np.array(img), cv2.COLOR_RGB2BGR)


def dibujar_panel(estado, hay_arduino, puerto, sistema_ok, n_rostros, ultimo_acceso):
    """Devuelve el panel lateral (BGR). Solo se redibuja cuando cambia algún dato."""
    acceso = ultimo_acceso or (None, None, None)
    datos = (str(estado["temp"]), estado["puerta"], estado["tiempo"], str(estado["est"]),
             bool(hay_arduino), str(puerto or ""), bool(sistema_ok), int(n_rostros), *acceso)
    return _panel_cacheado(datos)


# ─────────────────────────── Elementos sobre el video ───────────────────────────

def _pegar(frame, rgba, x, y):
    """Pega una imagen RGBA sobre el cuadro (BGR) respetando la transparencia y los bordes."""
    h, w = rgba.shape[:2]
    x0, y0 = max(x, 0), max(y, 0)
    x1, y1 = min(x + w, frame.shape[1]), min(y + h, frame.shape[0])
    if x1 <= x0 or y1 <= y0:
        return
    parte = rgba[y0 - y:y1 - y, x0 - x:x1 - x]
    alfa = parte[:, :, 3:4].astype(np.float32) / 255.0
    destino = frame[y0:y1, x0:x1]
    destino[:] = (parte[:, :, 2::-1] * alfa + destino * (1 - alfa)).astype(np.uint8)


@lru_cache(maxsize=128)
def _etiqueta_cara(nombre, resultado):
    texto_res, color = RESULTADOS[resultado]
    f_n, f_r = fuente(15, "semi"), fuente(12)
    medir = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    nombre = _recortar(medir, nombre, f_n, 260)
    ancho = int(max(_ancho(medir, nombre, f_n), _ancho(medir, texto_res, f_r) + 16)) + 24
    img = Image.new("RGBA", (ancho, 48), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, ancho - 1, 47), radius=10, fill=(*FONDO, 225), outline=(*color, 255), width=2)
    d.text((12, 5), nombre, font=f_n, fill=TEXTO)
    d.ellipse((12, 31, 19, 38), fill=color)
    d.text((25, 26), texto_res, font=f_r, fill=color)
    return np.array(img)


def dibujar_cara(frame, top, right, bottom, left, nombre, resultado):
    """Recuadro con esquinas marcadas y una etiqueta con el nombre y el resultado."""
    color = bgr(RESULTADOS[resultado][1])
    cv2.rectangle(frame, (left, top), (right, bottom), color, 1, cv2.LINE_AA)
    largo = max(12, min(right - left, bottom - top) // 4)
    for (x, y, dx, dy) in ((left, top, 1, 1), (right, top, -1, 1), (left, bottom, 1, -1), (right, bottom, -1, -1)):
        cv2.line(frame, (x, y), (x + dx * largo, y), color, 4, cv2.LINE_AA)
        cv2.line(frame, (x, y), (x, y + dy * largo), color, 4, cv2.LINE_AA)
    etiqueta = _etiqueta_cara(nombre, resultado)
    y = bottom + 8 if bottom + 8 + etiqueta.shape[0] <= frame.shape[0] else top - 8 - etiqueta.shape[0]
    x = max(0, min(left, frame.shape[1] - etiqueta.shape[1]))   # que no se corte en los bordes
    _pegar(frame, etiqueta, x, y)


@lru_cache(maxsize=4)
def _aviso_puerta(puerta):
    if puerta == "ABIERTA":
        texto, color = "Puerta abierta · registrando accesos", AVISO
    elif puerta == "CERRADA":
        texto, color = "Puerta cerrada · sin registro", EXITO
    else:
        texto, color = "Esperando datos del Arduino", TEXTO_2
    f = fuente(13, "semi")
    medir = ImageDraw.Draw(Image.new("RGBA", (1, 1)))
    ancho = int(_ancho(medir, texto, f)) + 36
    img = Image.new("RGBA", (ancho, 30), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((0, 0, ancho - 1, 29), radius=15, fill=(*FONDO, 210))
    d.ellipse((13, 11, 21, 19), fill=color)
    d.text((28, 6), texto, font=f, fill=TEXTO)
    return np.array(img)


def dibujar_aviso_puerta(frame, puerta):
    _pegar(frame, _aviso_puerta(puerta), 14, 14)


def componer(frame, panel):
    return np.hstack((frame, panel))


def crear_ventana():
    cv2.namedWindow(TITULO_VENTANA, cv2.WINDOW_NORMAL | cv2.WINDOW_KEEPRATIO)
    cv2.resizeWindow(TITULO_VENTANA, ANCHO_VIDEO + ANCHO_PANEL, ALTO_VIDEO)


def ventana_visible():
    """True mientras la ventana esté abierta (sirve para detectar el cierre con la X)."""
    try:
        return cv2.getWindowProperty(TITULO_VENTANA, cv2.WND_PROP_VISIBLE) >= 1
    except cv2.error:
        return False
