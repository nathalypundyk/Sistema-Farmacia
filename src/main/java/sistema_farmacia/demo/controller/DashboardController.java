package sistema_farmacia.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.model.Usuario;
import sistema_farmacia.demo.model.ConsumoHistorico;
import sistema_farmacia.demo.repository.ConsumoHistoricoRepository;
import sistema_farmacia.demo.repository.ImportacionLogRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.repository.UsuarioRepository;
import sistema_farmacia.demo.service.InventarioService;
import sistema_farmacia.demo.service.WhatsappService;

import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private ImportacionLogRepository importacionLogRepo;
    @Autowired private InventarioService inventarioService;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ConsumoHistoricoRepository consumoHistoricoRepo;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Medicamento> medicamentos = medicamentoRepo.findAll();
        
        long cantidadCritico = medicamentos.stream().filter(Medicamento::isStockBajo).count();
        long cantidadVencer = medicamentos.stream().filter(Medicamento::isProximoAVencer).count();
        
        List<Medicamento> medicamentosStockBajo = medicamentos.stream()
                .filter(Medicamento::isStockBajo)
                .collect(Collectors.toList());

        List<Medicamento> medicamentosProximosVencer = medicamentos.stream()
                .filter(Medicamento::isProximoAVencer)
                .collect(Collectors.toList());

        model.addAttribute("medicamentos", medicamentos);
        model.addAttribute("cantidadCritico", cantidadCritico);
        model.addAttribute("cantidadVencer", cantidadVencer);
        var ultimaImport = importacionLogRepo.findFirstByTipoOrderByFechaDesc("STOCK");
        model.addAttribute("ultimaImportacion", ultimaImport.orElse(null));
        model.addAttribute("importacionesRecientes",
                importacionLogRepo.findAllByOrderByFechaDesc().stream().limit(5).toList());
        model.addAttribute("medicamentosStockBajo", medicamentosStockBajo);
        model.addAttribute("medicamentosProximosVencer", medicamentosProximosVencer);

        return "index";
    }

    @GetMapping("/config")
    public String config(Model model) throws Exception {
        List<Medicamento> meds = medicamentoRepo.findAll();
        List<Map<String, Object>> medsJson = meds.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",           m.getId());
            map.put("nombre",       m.getNombre()        != null ? m.getNombre()        : "");
            map.put("concentracion",m.getConcentracion() != null ? m.getConcentracion() : "");
            map.put("codigoSiciap", m.getCodigoSiciap()  != null ? m.getCodigoSiciap()  : "");
            map.put("stockTotal",   m.getStockTotal());
            map.put("minimo",       m.getMinimo());
            map.put("deseable",     m.getDeseable());
            return map;
        }).collect(Collectors.toList());
        model.addAttribute("medicamentosJson", objectMapper.writeValueAsString(medsJson));
        return "config";
    }

    // ── DTOs bulk config ─────────────────────────────────────────────────────

    public static class BulkConfigRequest {
        public String pin;
        public List<ItemConfig> items;
        public static class ItemConfig {
            public Long id;
            public Integer minimo;
            public Integer deseable;
        }
    }

    // ── Recomendación inteligente de stock ───────────────────────────────────

    public static class RecomendacionItem {
        public Long    id;
        public double  avgDiario;
        public int     minimoRecomendado;
        public int     deseableRecomendado;
        public long    diasConDatos;
        public String  confianza; // ALTA | MEDIA | BAJA | MUY_BAJA
    }

    // Parámetros fijos conservadores para hospital público
    private static final int DIAS_BUFFER_MINIMO  = 30;  // stock mínimo para 1 mes sin reposición
    private static final int DIAS_OBJETIVO_DESEABLE = 90; // stock deseable para 3 meses

    @GetMapping("/api/config/recomendar")
    @ResponseBody
    public ResponseEntity<?> recomendar() {
        List<RecomendacionItem> resultado = new ArrayList<>();

        for (Medicamento med : medicamentoRepo.findAll()) {
            List<ConsumoHistorico> historial = consumoHistoricoRepo
                    .findByMedicamentoOrderByFechaAsc(med).stream()
                    .filter(h -> h.getConsumo() != null && h.getConsumo() > 0)
                    .toList();

            if (historial.isEmpty()) continue;

            int       totalConsumo  = historial.stream().mapToInt(ConsumoHistorico::getConsumo).sum();
            LocalDate oldest        = historial.get(0).getFecha();
            long      diasTotales   = ChronoUnit.DAYS.between(oldest, LocalDate.now()) + 1;
            // Días distintos con consumo (no número de imports — puede haber 3 imports/día)
            long      diasDistintos = historial.stream()
                                        .map(ConsumoHistorico::getFecha).distinct().count();
            double    avg           = Math.round((double) totalConsumo / diasTotales * 10.0) / 10.0;

            RecomendacionItem item = new RecomendacionItem();
            item.id                  = med.getId();
            item.avgDiario           = avg;
            item.minimoRecomendado   = (int) Math.max(1, Math.ceil(avg * DIAS_BUFFER_MINIMO));
            item.deseableRecomendado = (int) Math.max(item.minimoRecomendado + 1,
                                                      Math.ceil(avg * DIAS_OBJETIVO_DESEABLE));
            item.diasConDatos        = diasDistintos;
            item.confianza           = diasDistintos >= 5 ? "ALTA"
                                     : diasDistintos >= 3 ? "MEDIA"
                                     : diasDistintos >= 2 ? "BAJA"
                                     : "MUY_BAJA";
            resultado.add(item);
        }

        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/api/config/bulk")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> saveBulk(@RequestBody BulkConfigRequest req, Principal principal) {
        try {
            Usuario user = usuarioRepo.findByUsername(principal.getName()).orElseThrow();
            if (user.getPin() == null || !user.getPin().equals(req.pin)) {
                return ResponseEntity.status(403).body(Map.of("error", "PIN incorrecto"));
            }
            int actualizados = 0;
            for (BulkConfigRequest.ItemConfig item : req.items) {
                Medicamento med = medicamentoRepo.findById(item.id).orElse(null);
                if (med == null) continue;
                if (item.minimo  != null) med.setMinimo(item.minimo);
                if (item.deseable != null) med.setDeseable(item.deseable);
                medicamentoRepo.save(med);
                inventarioService.verificarAlertas(med);
                actualizados++;
            }
            return ResponseEntity.ok(Map.of("actualizados", actualizados));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/auth/validarPin")
    @ResponseBody
    public ResponseEntity<?> validarPin(@RequestBody Map<String, String> body, Principal principal) {
        String pin = body.get("pin");
        if (pin == null || pin.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "PIN requerido"));
        }
        Usuario user = usuarioRepo.findByUsername(principal.getName()).orElseThrow();
        if (user.getPin() == null || !user.getPin().equals(pin)) {
            return ResponseEntity.status(403).body(Map.of("error", "PIN incorrecto"));
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
