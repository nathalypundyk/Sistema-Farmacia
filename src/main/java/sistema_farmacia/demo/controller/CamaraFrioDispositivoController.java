package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.AccesoCamaraFrio;
import sistema_farmacia.demo.model.LecturaTemperatura;
import sistema_farmacia.demo.service.CamaraFrioService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/**
 * API que consume el script puente (Python) que corre junto a la cámara de frío.
 * No usa la sesión web: se autentica con una clave de dispositivo enviada en el encabezado X-API-KEY
 * (propiedad camarafrio.dispositivo.api-key / variable de entorno CAMARAFRIO_API_KEY).
 */
@RestController
@RequestMapping("/api/dispositivo/camara-frio")
public class CamaraFrioDispositivoController {

    private final CamaraFrioService service;

    @Value("${camarafrio.dispositivo.api-key}")
    private String apiKey;

    public CamaraFrioDispositivoController(CamaraFrioService service) {
        this.service = service;
    }

    private boolean claveValida(String recibida) {
        if (recibida == null || apiKey == null || apiKey.isBlank()) return false;
        return MessageDigest.isEqual(recibida.getBytes(StandardCharsets.UTF_8),
                                     apiKey.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<Object> noAutorizado() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Clave de dispositivo inválida"));
    }

    /** Rostros de referencia: usuarios activos con foto, indicando si tienen permiso. */
    @GetMapping("/rostros")
    public ResponseEntity<Object> rostros(@RequestHeader(value = "X-API-KEY", required = false) String key) {
        if (!claveValida(key)) return noAutorizado();
        return ResponseEntity.ok(service.rostrosParaDispositivo());
    }

    /** Foto de referencia de un usuario (bytes JPG/PNG). */
    @GetMapping("/rostros/{usuarioId}/foto")
    public ResponseEntity<?> foto(@RequestHeader(value = "X-API-KEY", required = false) String key,
                                  @PathVariable Long usuarioId) {
        if (!claveValida(key)) return noAutorizado();
        return service.autorizacionDe(usuarioId)
                .filter(a -> a.tieneFoto())
                .<ResponseEntity<?>>map(a -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(a.getFotoContentType()))
                        .body(a.getFotoRostro()))
                .orElse(ResponseEntity.notFound().build());
    }

    public record LecturaRequest(Double temperatura, String puerta, String tiempo, String estado) {}

    @PostMapping("/lecturas")
    public ResponseEntity<Object> lectura(@RequestHeader(value = "X-API-KEY", required = false) String key,
                                          @RequestBody LecturaRequest req) {
        if (!claveValida(key)) return noAutorizado();
        try {
            LecturaTemperatura l = service.registrarLectura(req.temperatura(), req.puerta(), req.tiempo(), req.estado());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("id", l.getId(), "fueraDeRango", l.getFueraDeRango()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.unprocessableEntity().body(Map.of("error", e.getMessage()));
        }
    }

    public record AccesoRequest(Long usuarioId, String nombre, Boolean puertaAbierta, Double distancia) {}

    @PostMapping("/accesos")
    public ResponseEntity<Object> acceso(@RequestHeader(value = "X-API-KEY", required = false) String key,
                                         @RequestBody AccesoRequest req) {
        if (!claveValida(key)) return noAutorizado();
        AccesoCamaraFrio a = service.registrarAcceso(req.usuarioId(), req.nombre(), req.puertaAbierta(), req.distancia());
        // Se devuelve el resultado decidido por el servidor; el script lo usa para disparar el buzzer.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", a.getId(), "resultado", a.getResultado()));
    }
}
