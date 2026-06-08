package sistema_farmacia.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sistema_farmacia.demo.model.ImportacionLog;
import sistema_farmacia.demo.repository.ImportacionLogRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.service.SiciapImportService;
import sistema_farmacia.demo.service.SiciapImportService.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SiciapImportController {

    @Autowired private SiciapImportService siciapService;
    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ImportacionLogRepository importacionLogRepo;

    private static final String SESSION_KEY = "siciapPreview";

    // ── Página ───────────────────────────────────────────────────────────────

    @GetMapping("/importacion-siciap")
    public String pagina(Model model) throws Exception {
        List<Map<String, Object>> meds = medicamentoRepo.findAll().stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "nombre", m.getNombre() != null ? m.getNombre() : "",
                        "concentracion", m.getConcentracion() != null ? m.getConcentracion() : "",
                        "codigoSiciap", m.getCodigoSiciap() != null ? m.getCodigoSiciap() : ""))
                .collect(Collectors.toList());
        model.addAttribute("medicamentosJson", objectMapper.writeValueAsString(meds));
        return "importacion-siciap";
    }

    // ── REST: preview ────────────────────────────────────────────────────────

    @PostMapping("/api/importacion/siciap/preview")
    @ResponseBody
    public ResponseEntity<?> preview(@RequestParam("archivo") MultipartFile archivo,
                                     HttpSession session) {
        if (archivo.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "No se recibió ningún archivo."));
        try {
            SiciapPreviewResponse resultado = siciapService.procesarImi(archivo);
            session.setAttribute(SESSION_KEY, resultado);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── REST: confirmar ──────────────────────────────────────────────────────

    @PostMapping("/api/importacion/siciap/confirmar")
    @ResponseBody
    public ResponseEntity<?> confirmar(@RequestBody SiciapConfirmarRequest req,
                                       HttpSession session,
                                       Principal principal) {
        SiciapPreviewResponse preview = (SiciapPreviewResponse) session.getAttribute(SESSION_KEY);
        if (preview == null)
            return ResponseEntity.status(410).body(Map.of("error", "Sesión expirada. Volvé a subir el archivo."));
        try {
            String usuario = principal != null ? principal.getName() : "sistema";
            SiciapResultado resultado = siciapService.confirmar(preview, req);
            importacionLogRepo.save(ImportacionLog.builder()
                    .usuario(usuario)
                    .tipo("IMI")
                    .totalItems(preview.totalFilas)
                    .aplicados(resultado.actualizados)
                    .equivalenciasGuardadas(0)
                    .medicamentosCreados(resultado.creados)
                    .build());
            session.removeAttribute(SESSION_KEY);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
