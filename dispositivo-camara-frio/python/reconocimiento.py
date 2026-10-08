"""
Trabajo pesado del reconocimiento facial.

Estas funciones se ejecutan en un PROCESO APARTE (ver MotorReconocimiento en control_camara_frio.py).
Motivo: face_recognition (dlib) bloquea todo el programa de Python mientras calcula el vector de
cada rostro (~150 ms por cara). Si se hace en el mismo proceso que muestra el video, la imagen se
congela y llega con segundos de retraso. En otro proceso usa otro núcleo del procesador y el video
sigue fluido.

Las funciones son las mismas llamadas que hacía el script original, solo que movidas acá.
"""

import face_recognition


def analizar(rgb_pequeno):
    """Busca las caras del cuadro y calcula su vector. Devuelve (ubicaciones, codificaciones)."""
    ubicaciones = face_recognition.face_locations(rgb_pequeno)
    codificaciones = face_recognition.face_encodings(rgb_pequeno, ubicaciones)
    return ubicaciones, codificaciones


def codificar_fotos(fotos_rgb):
    """Vector facial de cada foto de referencia (None si en la foto no se detectó una cara)."""
    resultado = []
    for rgb in fotos_rgb:
        enc = face_recognition.face_encodings(rgb)
        resultado.append(enc[0] if enc else None)
    return resultado
