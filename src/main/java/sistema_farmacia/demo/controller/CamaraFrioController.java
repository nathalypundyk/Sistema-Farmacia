package sistema_farmacia.demo.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sistema_farmacia.demo.model.AccesoCamaraFrio;
import sistema_farmacia.demo.model.LecturaTemperatura;
import sistema_farmacia.demo.repository.UsuarioRepository;
import sistema_farmacia.demo.service.CamaraFrioService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Pantalla web "Cadena de frío": monitoreo, bitácora de accesos y permisos por usuario. */
@Controller
public class CamaraFrioController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final CamaraFrioService service;
    private final UsuarioRepository usuarioRepo;

    public CamaraFrioController(CamaraFrioService service, UsuarioRepository usuarioRepo) {
        this.service = service;
        this.usuarioRepo = usuarioRepo;
    }

    @GetMapping("/camara-frio")
    public String pantalla(Model model) {
        model.addAttribute("activePage", "camara-frio");
        model.addAttribute("tempMin", service.getTempMin());
        model.addAttribute("tempMax", service.getTempMax());
        model.addAttribute("usuarios", usuarioRepo.findAll());
        model.addAttribute("autorizaciones", service.autorizacionesPorUsuario());
        return "camara-frio";
    }

    /** Datos para refrescar la pantalla cada pocos segundos (sin recargar la página). */
    @GetMapping("/api/camara-frio/estado")
    @ResponseBody
    public Map<String, Object> estado(@RequestParam(defaultValue = "120") int minutos) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("conectado", service.dispositivoConectado());
        r.put("tempMin", service.getTempMin());
        r.put("tempMax", service.getTempMax());

        service.ultimaLectura().ifPresentOrElse(l -> r.put("ultima", lecturaDto(l)), () -> r.put("ultima", null));

        List<LecturaTemperatura> serie = service.lecturasDesde(LocalDateTime.now().minusMinutes(Math.min(minutos, 1440)));
        // Se limita a 300 puntos para que el gráfico no se vuelva pesado.
        int paso = Math.max(1, serie.size() / 300);
        List<Map<String, Object>> puntos = new ArrayList<>();
        for (int i = 0; i < serie.size(); i += paso) {
            LecturaTemperatura l = serie.get(i);
            puntos.add(Map.of("hora", l.getFechaHora().format(HORA), "temperatura", l.getTemperatura()));
        }
        r.put("serie", puntos);

        List<Map<String, Object>> accesos = new ArrayList<>();
        for (AccesoCamaraFrio a : service.ultimosAccesos()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fechaHora", a.getFechaHora().format(FECHA_HORA));
            m.put("nombre", a.getNombreDetectado());
            m.put("resultado", a.getResultado());
            m.put("puertaAbierta", a.getPuertaAbierta());
            accesos.add(m);
        }
        r.put("accesos", accesos);
        return r;
    }

    private Map<String, Object> lecturaDto(LecturaTemperatura l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("fechaHora", l.getFechaHora().format(FECHA_HORA));
        m.put("temperatura", l.getTemperatura());
        m.put("puertaAbierta", l.getPuertaAbierta());
        m.put("tiempoPuerta", l.getTiempoPuerta());
        m.put("estado", l.getEstadoDispositivo());
        m.put("fueraDeRango", l.getFueraDeRango());
        return m;
    }

    // ───────────── Solo ADMIN (restringido en SecurityConfig) ─────────────

    @PostMapping("/camara-frio/autorizaciones/{usuarioId}")
    public String guardarAutorizacion(@PathVariable Long usuarioId,
                                      @RequestParam(defaultValue = "false") boolean autorizado,
                                      @RequestParam(value = "foto", required = false) MultipartFile foto,
                                      Principal principal) {
        try {
            service.guardarAutorizacion(usuarioId, autorizado, foto, principal.getName());
            return "redirect:/camara-frio?success#personal";
        } catch (Exception e) {
            return "redirect:/camara-frio?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8) + "#personal";
        }
    }

    @GetMapping("/camara-frio/autorizaciones/{usuarioId}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long usuarioId) {
        return service.autorizacionDe(usuarioId)
                .filter(a -> a.tieneFoto())
                .map(a -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(a.getFotoContentType()))
                        .body(a.getFotoRostro()))
                .orElse(ResponseEntity.notFound().build());
    }
}
