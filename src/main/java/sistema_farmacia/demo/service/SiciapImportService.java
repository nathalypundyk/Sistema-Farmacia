package sistema_farmacia.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.MedicamentoRepository;

import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SiciapImportService {

    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private ImportacionService importacionService; // reutiliza normalizar()
    @Autowired private SimilitudService similitudService;

    // ── DTOs ────────────────────────────────────────────────────────────────

    public static class CandidatoSiciap implements Serializable {
        public Long medicamentoId;
        public String nombre;
        public String concentracion;
        public String codigoSiciapActual;
        public int score;
        public CandidatoSiciap() {}
        public CandidatoSiciap(Long id, String n, String c, String cs, int s) {
            medicamentoId = id; nombre = n; concentracion = c; codigoSiciapActual = cs; score = s;
        }
    }

    public static class ItemSiciapMatch implements Serializable {
        public int indice;
        public String codigoSiciap;
        public String nombreExterno;
        public String concExterna;
        public Long medicamentoId;
        public String nombreInterno;
        public String concInterna;
        public String codigoSiciapActual; // null si el medicamento aún no tiene código
        public String tipoMatch;          // EXACTO | NOMBRE
    }

    public static class ItemSiciapPendiente implements Serializable {
        public int indice;
        public String codigoSiciap;
        public String nombreExterno;
        public String concExterna;
        public List<CandidatoSiciap> candidatos;
    }

    public static class ItemSiciapSinMatch implements Serializable {
        public String codigoSiciap;
        public String nombreExterno;
        public String concExterna;
    }

    public static class SiciapPreviewResponse implements Serializable {
        public List<ItemSiciapMatch> matches = new ArrayList<>();
        public List<ItemSiciapPendiente> pendientes = new ArrayList<>();
        public List<ItemSiciapSinMatch> sinMatch = new ArrayList<>();
        public int totalFilas;
    }

    public static class ResolucionSiciap implements Serializable {
        public int indice; // índice en preview.pendientes[]
        public Long medicamentoId;
    }

    public static class SiciapConfirmarRequest implements Serializable {
        public List<Integer> matchesSeleccionados; // índices en preview.matches[]
        public List<ResolucionSiciap> resoluciones;
        public boolean crearNuevos; // crear medicamentos para los items sinMatch
    }

    public static class SiciapResultado {
        public int actualizados;
        public int sobreescritos; // ya tenían código y se reemplazó
        public int creados;       // nuevos medicamentos creados desde sinMatch
    }

    // ── Lectura de celdas ────────────────────────────────────────────────────

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case FORMULA -> {
                try { yield String.valueOf((long) cell.getNumericCellValue()); }
                catch (Exception e) {
                    try { yield cell.getStringCellValue().trim(); } catch (Exception ex) { yield ""; }
                }
            }
            default -> "";
        };
    }

    // ── Proceso principal ────────────────────────────────────────────────────
    // Estructura IMI: datos desde fila 6 (índice 0)
    // Col 2: Código SICIAP | Col 4: Nombre genérico | Col 8: Concentración

    public SiciapPreviewResponse procesarImi(MultipartFile file) throws Exception {
        Map<String, String[]> grupos = new LinkedHashMap<>(); // key → [codigo, nombre, conc]

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            int total = 0;

            for (int i = 6; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String codigo = getCellString(row.getCell(2));
                String nombre = getCellString(row.getCell(4));
                String conc   = getCellString(row.getCell(8));
                if (codigo.isEmpty() || nombre.isEmpty()) continue;

                String key = importacionService.normalizar(nombre) + "|" + importacionService.normalizar(conc);
                grupos.putIfAbsent(key, new String[]{codigo, nombre, conc});
                total++;
            }

            SiciapPreviewResponse resp = new SiciapPreviewResponse();
            resp.totalFilas = total;

            List<Medicamento> todosMeds = medicamentoRepo.findAll();
            int matchIdx = 0, pendIdx = 0;

            for (Map.Entry<String, String[]> entry : grupos.entrySet()) {
                String[] info     = entry.getValue();
                String codigoSic  = info[0];
                String nombreExt  = info[1];
                String concExt    = info[2];
                String nomNorm    = importacionService.normalizar(nombreExt);
                String concNorm   = importacionService.normalizar(concExt);

                // Paso 1 — exacto: nombre + concentración
                Optional<Medicamento> exacto = todosMeds.stream()
                        .filter(m -> importacionService.normalizar(m.getNombre()).equals(nomNorm)
                                  && importacionService.normalizar(m.getConcentracion()).equals(concNorm))
                        .findFirst();
                if (exacto.isPresent()) {
                    resp.matches.add(buildMatch(matchIdx++, codigoSic, nombreExt, concExt, exacto.get(), "EXACTO"));
                    continue;
                }

                // Paso 2 — solo nombre (insumos sin concentración en BD)
                Optional<Medicamento> soloNombre = todosMeds.stream()
                        .filter(m -> importacionService.normalizar(m.getNombre()).equals(nomNorm)
                                  && (m.getConcentracion() == null || m.getConcentracion().isBlank()))
                        .findFirst();
                if (soloNombre.isPresent()) {
                    resp.matches.add(buildMatch(matchIdx++, codigoSic, nombreExt, concExt, soloNombre.get(), "NOMBRE"));
                    continue;
                }

                // Sin match — buscar candidatos con similitud mejorada
                List<CandidatoSiciap> candidatos = todosMeds.stream()
                        .map(m -> new CandidatoSiciap(
                                m.getId(), m.getNombre(), m.getConcentracion(), m.getCodigoSiciap(),
                                similitudService.similitudGlobal(nomNorm, concNorm,
                                        importacionService.normalizar(m.getNombre()),
                                        importacionService.normalizar(m.getConcentracion()))))
                        .filter(c -> c.score >= 50)
                        .sorted((a, b) -> b.score - a.score)
                        .limit(5)
                        .collect(Collectors.toList());

                if (candidatos.isEmpty()) {
                    ItemSiciapSinMatch s = new ItemSiciapSinMatch();
                    s.codigoSiciap = codigoSic; s.nombreExterno = nombreExt; s.concExterna = concExt;
                    resp.sinMatch.add(s);
                } else {
                    ItemSiciapPendiente p = new ItemSiciapPendiente();
                    p.indice = pendIdx++; p.codigoSiciap = codigoSic;
                    p.nombreExterno = nombreExt; p.concExterna = concExt; p.candidatos = candidatos;
                    resp.pendientes.add(p);
                }
            }

            return resp;
        }
    }

    private ItemSiciapMatch buildMatch(int idx, String codigoSic, String nombreExt, String concExt,
                                       Medicamento med, String tipo) {
        ItemSiciapMatch m = new ItemSiciapMatch();
        m.indice              = idx;
        m.codigoSiciap        = codigoSic;
        m.nombreExterno       = nombreExt;
        m.concExterna         = concExt;
        m.medicamentoId       = med.getId();
        m.nombreInterno       = med.getNombre();
        m.concInterna         = med.getConcentracion();
        m.codigoSiciapActual  = med.getCodigoSiciap();
        m.tipoMatch           = tipo;
        return m;
    }

    // ── Confirmación ─────────────────────────────────────────────────────────

    @Transactional
    public SiciapResultado confirmar(SiciapPreviewResponse preview, SiciapConfirmarRequest req) {
        SiciapResultado res = new SiciapResultado();

        Set<Integer> seleccionados = req.matchesSeleccionados != null
                ? new HashSet<>(req.matchesSeleccionados)
                : rangoCompleto(preview.matches.size());

        for (ItemSiciapMatch item : preview.matches) {
            if (!seleccionados.contains(item.indice)) continue;
            aplicarCodigo(item.medicamentoId, item.codigoSiciap, res);
        }

        if (req.resoluciones != null) {
            for (ResolucionSiciap rm : req.resoluciones) {
                if (rm.medicamentoId == null || rm.indice >= preview.pendientes.size()) continue;
                String codigo = preview.pendientes.get(rm.indice).codigoSiciap;
                aplicarCodigo(rm.medicamentoId, codigo, res);
            }
        }

        if (req.crearNuevos && preview.sinMatch != null && !preview.sinMatch.isEmpty()) {
            // Códigos ya usados (en matches confirmados + en BD) para evitar duplicados
            Set<String> codigosUsados = medicamentoRepo.findAll().stream()
                    .map(Medicamento::getCodigoSiciap)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (ItemSiciapSinMatch item : preview.sinMatch) {
                if (codigosUsados.contains(item.codigoSiciap)) continue;
                String conc = (item.concExterna == null || item.concExterna.isBlank()) ? null : item.concExterna;
                medicamentoRepo.save(Medicamento.builder()
                        .nombre(item.nombreExterno)
                        .concentracion(conc)
                        .codigoSiciap(item.codigoSiciap)
                        .minimo(ImportacionService.STOCK_DEFECTO)
                        .deseable(ImportacionService.STOCK_DEFECTO)
                        .build());
                codigosUsados.add(item.codigoSiciap);
                res.creados++;
            }
        }

        return res;
    }

    private void aplicarCodigo(Long medicamentoId, String codigoSiciap, SiciapResultado res) {
        Medicamento med = medicamentoRepo.findById(medicamentoId).orElse(null);
        if (med == null) return;
        if (med.getCodigoSiciap() != null) res.sobreescritos++;
        med.setCodigoSiciap(codigoSiciap);
        medicamentoRepo.save(med);
        res.actualizados++;
    }

    private Set<Integer> rangoCompleto(int size) {
        Set<Integer> s = new HashSet<>();
        for (int i = 0; i < size; i++) s.add(i);
        return s;
    }
}
