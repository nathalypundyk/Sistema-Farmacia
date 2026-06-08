package sistema_farmacia.demo.service;

import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sistema_farmacia.demo.model.*;
import sistema_farmacia.demo.repository.*;

import java.io.InputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportacionService {

    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private MedicamentoEquivalenciaRepository equivalenciaRepo;
    @Autowired private LoteRepository loteRepo;
    @Autowired private AlertaRepository alertaRepo;
    @Autowired private InventarioService inventarioService;
    @Autowired private sistema_farmacia.demo.repository.ConsumoHistoricoRepository consumoHistoricoRepo;

    // ── DTOs ────────────────────────────────────────────────────────────────

    public static class LotePreview implements Serializable {
        public String nroLote;
        public LocalDate fechaVencimiento;
        public int cantidad;
        public LotePreview() {}
        public LotePreview(String n, LocalDate f, int c) { nroLote=n; fechaVencimiento=f; cantidad=c; }
    }

    public static class CandidatoMatch implements Serializable {
        public Long medicamentoId;
        public String nombre;
        public String concentracion;
        public int score;
        public CandidatoMatch() {}
        public CandidatoMatch(Long id, String n, String c, int s) { medicamentoId=id; nombre=n; concentracion=c; score=s; }
    }

    public static class ItemMatch implements Serializable {
        public int indiceEnMatches;
        public String nombreExterno;
        public String concExterna;
        public String formaFarmaceutica;
        public String presentacion;
        public Long medicamentoId;
        public String nombreInterno;
        public String concInterna;
        public String codigoSiciap;
        public int stockActual;
        public int stockNuevo;
        public List<LotePreview> lotes;
        public String tipoMatch; // EQUIVALENCIA | EXACTO | NOMBRE
    }

    public static class ItemPendiente implements Serializable {
        public int indiceEnPendientes;
        public String nombreExterno;
        public String concExterna;
        public String formaFarmaceutica;
        public String presentacion;
        public List<LotePreview> lotes;
        public List<CandidatoMatch> candidatos;
    }

    public static class ItemSinMatch implements Serializable {
        public String nombreExterno;
        public String concExterna;
        public List<LotePreview> lotes;
    }

    public static class PreviewResponse implements Serializable {
        public List<ItemMatch> matches = new ArrayList<>();
        public List<ItemPendiente> pendientes = new ArrayList<>();
        public List<ItemSinMatch> sinMatch = new ArrayList<>();
        public int totalGrupos;
    }

    public static class ResolucionManual implements Serializable {
        public int indice; // índice en preview.pendientes[]
        public Long medicamentoId;
        public boolean guardarEquivalencia;
    }

    public static class ConfirmarRequest implements Serializable {
        public List<Integer> matchesSeleccionados; // índices en preview.matches[]
        public List<ResolucionManual> resoluciones;
    }

    public static class ResultadoConfirmacion {
        public int medicamentosActualizados;
        public int lotesCreados;
        public int lotesActualizados;
        public int equivalenciasGuardadas;
        public int alertasActivas;
    }

    // ── Normalización ────────────────────────────────────────────────────────

    public String normalizar(String s) {
        if (s == null) return "";
        return s.trim()
                .toUpperCase()
                .replaceAll("[áÁ]", "A").replaceAll("[éÉ]", "E")
                .replaceAll("[íÍ]", "I").replaceAll("[óÓ]", "O")
                .replaceAll("[úÚüÜ]", "U")
                .replaceAll("\\s+", " ")
                .replaceAll("\\.$", "")
                .trim();
    }

    public int similitud(String a, String b) {
        if (a.equals(b)) return 100;
        if (a.startsWith(b) || b.startsWith(a)) return 85;
        if (a.contains(b) || b.contains(a)) return 70;
        String[] ta = a.split(" ");
        String[] tb = b.split(" ");
        Set<String> inter = new HashSet<>(Arrays.asList(ta));
        inter.retainAll(Arrays.asList(tb));
        Set<String> union = new HashSet<>(Arrays.asList(ta));
        union.addAll(Arrays.asList(tb));
        return union.isEmpty() ? 0 : (int)(inter.size() * 100.0 / union.size());
    }

    // ── Lectura de celdas ────────────────────────────────────────────────────

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case FORMULA -> {
                try { yield String.valueOf((long) cell.getNumericCellValue()); }
                catch (Exception e) {
                    try { yield cell.getStringCellValue().trim(); } catch (Exception ex) { yield ""; }
                }
            }
            default -> "";
        };
    }

    private LocalDate parseFecha(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
            return cell.getLocalDateTimeCellValue().toLocalDate();
        String s = getCellString(cell);
        if (s.isEmpty()) return null;
        for (String fmt : List.of("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "d/M/yyyy", "dd-MM-yyyy")) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern(fmt)); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private int parseInteger(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(getCellString(cell).replace(",", "").replace(".", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── Parseo del Excel SICIAP ──────────────────────────────────────────────
    // Columnas fijas: 2=nombre, 8=conc, 10=forma, 11=presentación,
    //                 12=cant total, 15=fecha vencimiento, 16=cant lote, 21=nro lote
    // Filas 0-5: encabezados institucionales → ignorar
    // Forward-fill en columnas 2 y 8 por celdas mergeadas

    public PreviewResponse procesarExcel(MultipartFile file) throws Exception {
        Map<String, String[]> grupoInfo = new LinkedHashMap<>();        // key → [nombre, conc, formaFarm, presentacion]
        Map<String, List<LotePreview>> lotesPerGrupo = new LinkedHashMap<>();

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            String lastNombre = "", lastConc = "", lastForma = "", lastPres = "";

            for (int i = 6; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nombre = getCellString(row.getCell(2));
                String conc   = getCellString(row.getCell(8));
                String forma  = getCellString(row.getCell(10));
                String pres   = getCellString(row.getCell(11));

                // Forward-fill de celdas mergeadas
                if (!nombre.isEmpty()) lastNombre = nombre; else nombre = lastNombre;
                if (!conc.isEmpty())   lastConc   = conc;   else conc   = lastConc;
                if (!forma.isEmpty())  lastForma  = forma;  else forma  = lastForma;
                if (!pres.isEmpty())   lastPres   = pres;   else pres   = lastPres;
                if (nombre.isEmpty()) continue;

                String key = normalizar(nombre) + "|" + normalizar(conc);
                grupoInfo.putIfAbsent(key, new String[]{nombre, conc, forma, pres});

                String nroLote = getCellString(row.getCell(21));
                if (!nroLote.isEmpty()) {
                    LotePreview lp = new LotePreview(
                            nroLote,
                            parseFecha(row.getCell(15)),
                            parseInteger(row.getCell(16))
                    );
                    lotesPerGrupo.computeIfAbsent(key, k -> new ArrayList<>()).add(lp);
                }
            }
        }

        List<Medicamento> todosMeds = medicamentoRepo.findAll();
        PreviewResponse resp = new PreviewResponse();
        resp.totalGrupos = grupoInfo.size();
        int matchIdx = 0, pendIdx = 0;

        for (Map.Entry<String, String[]> entry : grupoInfo.entrySet()) {
            String key        = entry.getKey();
            String nombreExt  = entry.getValue()[0];
            String concExt    = entry.getValue()[1];
            String formaExt   = entry.getValue()[2];
            String presExt    = entry.getValue()[3];
            String nomNorm    = normalizar(nombreExt);
            String concNorm   = normalizar(concExt);
            List<LotePreview> lotes = lotesPerGrupo.getOrDefault(key, List.of());

            // Paso 1 — equivalencia guardada
            Optional<MedicamentoEquivalencia> eq =
                    equivalenciaRepo.findByNombreExternoAndConcentracionExterna(nomNorm, concNorm);
            if (eq.isPresent()) {
                resp.matches.add(buildMatch(matchIdx++, nombreExt, concExt, formaExt, presExt, eq.get().getMedicamento(), lotes, "EQUIVALENCIA"));
                continue;
            }

            // Paso 2 — nombre normalizado + concentración normalizada
            Optional<Medicamento> exacto = todosMeds.stream()
                    .filter(m -> normalizar(m.getNombre()).equals(nomNorm)
                              && normalizar(m.getConcentracion()).equals(concNorm))
                    .findFirst();
            if (exacto.isPresent()) {
                resp.matches.add(buildMatch(matchIdx++, nombreExt, concExt, formaExt, presExt, exacto.get(), lotes, "EXACTO"));
                continue;
            }

            // Paso 3 — solo nombre (insumos/material sin concentración en BD)
            Optional<Medicamento> solonombre = todosMeds.stream()
                    .filter(m -> normalizar(m.getNombre()).equals(nomNorm)
                              && (m.getConcentracion() == null || m.getConcentracion().isBlank()))
                    .findFirst();
            if (solonombre.isPresent()) {
                resp.matches.add(buildMatch(matchIdx++, nombreExt, concExt, formaExt, presExt, solonombre.get(), lotes, "NOMBRE"));
                continue;
            }

            // Sin match — buscar candidatos por similitud
            List<CandidatoMatch> candidatos = todosMeds.stream()
                    .map(m -> new CandidatoMatch(m.getId(), m.getNombre(), m.getConcentracion(),
                                                 similitud(nomNorm, normalizar(m.getNombre()))))
                    .filter(c -> c.score >= 40)
                    .sorted((a, b) -> b.score - a.score)
                    .limit(5)
                    .collect(Collectors.toList());

            if (candidatos.isEmpty()) {
                ItemSinMatch s = new ItemSinMatch();
                s.nombreExterno = nombreExt; s.concExterna = concExt; s.lotes = lotes;
                resp.sinMatch.add(s);
            } else {
                ItemPendiente p = new ItemPendiente();
                p.indiceEnPendientes = pendIdx++;
                p.nombreExterno = nombreExt; p.concExterna = concExt;
                p.formaFarmaceutica = formaExt; p.presentacion = presExt;
                p.lotes = lotes; p.candidatos = candidatos;
                resp.pendientes.add(p);
            }
        }

        return resp;
    }

    private ItemMatch buildMatch(int idx, String nombreExt, String concExt,
                                 String formaFarm, String presentacion,
                                 Medicamento med, List<LotePreview> lotes, String tipo) {
        ItemMatch m = new ItemMatch();
        m.indiceEnMatches    = idx;
        m.nombreExterno      = nombreExt;
        m.concExterna        = concExt;
        m.formaFarmaceutica  = formaFarm;
        m.presentacion       = presentacion;
        m.medicamentoId      = med.getId();
        m.nombreInterno      = med.getNombre();
        m.concInterna        = med.getConcentracion();
        m.codigoSiciap       = med.getCodigoSiciap();
        m.stockActual        = med.getStockTotal();
        m.stockNuevo         = lotes.stream().mapToInt(l -> l.cantidad).sum();
        m.lotes              = lotes;
        m.tipoMatch          = tipo;
        return m;
    }

    // ── Confirmación ─────────────────────────────────────────────────────────

    @Transactional
    public ResultadoConfirmacion confirmar(PreviewResponse preview, ConfirmarRequest req, String usuario) {
        ResultadoConfirmacion res = new ResultadoConfirmacion();

        Set<Integer> seleccionados = req.matchesSeleccionados != null
                ? new HashSet<>(req.matchesSeleccionados)
                : rangoCompleto(preview.matches.size());

        LocalDate hoyImport = LocalDate.now();

        // Aplicar matches automáticos seleccionados
        for (ItemMatch item : preview.matches) {
            if (!seleccionados.contains(item.indiceEnMatches)) continue;
            Medicamento med = medicamentoRepo.findById(item.medicamentoId).orElse(null);
            if (med == null) continue;
            if (item.formaFarmaceutica != null && !item.formaFarmaceutica.isBlank())
                med.setFormaFarmaceutica(item.formaFarmaceutica);
            if (item.presentacion != null && !item.presentacion.isBlank())
                med.setPresentacion(item.presentacion);
            medicamentoRepo.save(med);
            aplicarLotes(med, item.lotes, res);
            registrarConsumo(med, item.stockActual, item.stockNuevo, hoyImport);
            inventarioService.verificarAlertas(med);
            res.alertasActivas += alertaRepo.findAllByMedicamento(med).size();
            res.medicamentosActualizados++;
        }

        // Aplicar resoluciones manuales
        if (req.resoluciones != null) {
            for (ResolucionManual rm : req.resoluciones) {
                if (rm.medicamentoId == null || rm.indice >= preview.pendientes.size()) continue;
                ItemPendiente pendiente = preview.pendientes.get(rm.indice);
                Medicamento med = medicamentoRepo.findById(rm.medicamentoId).orElse(null);
                if (med == null) continue;

                int stockAntes = med.getStockTotal();
                int stockDespues = pendiente.lotes.stream().mapToInt(l -> l.cantidad).sum();
                if (pendiente.formaFarmaceutica != null && !pendiente.formaFarmaceutica.isBlank())
                    med.setFormaFarmaceutica(pendiente.formaFarmaceutica);
                if (pendiente.presentacion != null && !pendiente.presentacion.isBlank())
                    med.setPresentacion(pendiente.presentacion);

                if (rm.guardarEquivalencia) {
                    String nomNorm  = normalizar(pendiente.nombreExterno);
                    String concNorm = normalizar(pendiente.concExterna);
                    if (equivalenciaRepo.findByNombreExternoAndConcentracionExterna(nomNorm, concNorm).isEmpty()) {
                        equivalenciaRepo.save(MedicamentoEquivalencia.builder()
                                .nombreExterno(nomNorm)
                                .concentracionExterna(concNorm)
                                .medicamento(med)
                                .fechaConfirmacion(LocalDateTime.now())
                                .usuarioConfirmacion(usuario)
                                .build());
                        res.equivalenciasGuardadas++;
                    }
                }

                aplicarLotes(med, pendiente.lotes, res);
                registrarConsumo(med, stockAntes, stockDespues, hoyImport);
                inventarioService.verificarAlertas(med);
                res.alertasActivas += alertaRepo.findAllByMedicamento(med).size();
                res.medicamentosActualizados++;
            }
        }

        return res;
    }

    private void aplicarLotes(Medicamento med, List<LotePreview> lotesPrev, ResultadoConfirmacion res) {
        List<Lote> existentes = loteRepo.findByMedicamento(med);
        for (LotePreview lp : lotesPrev) {
            if (lp.nroLote == null || lp.nroLote.isEmpty()) continue;
            Optional<Lote> existente = existentes.stream()
                    .filter(l -> lp.nroLote.equalsIgnoreCase(l.getNroLote()))
                    .findFirst();
            if (existente.isPresent()) {
                existente.get().setCantidadDisponible(lp.cantidad);
                loteRepo.save(existente.get());
                res.lotesActualizados++;
            } else {
                loteRepo.save(Lote.builder()
                        .nroLote(lp.nroLote)
                        .fechaVencimiento(lp.fechaVencimiento)
                        .cantidadDisponible(lp.cantidad)
                        .medicamento(med)
                        .build());
                res.lotesCreados++;
            }
        }
    }

    private void registrarConsumo(Medicamento med, int stockAnterior, int stockNuevo, LocalDate fecha) {
        int consumo = stockAnterior - stockNuevo;
        if (consumo <= 0) return; // reposición de stock, no es consumo
        consumoHistoricoRepo.save(sistema_farmacia.demo.model.ConsumoHistorico.builder()
                .medicamento(med)
                .fecha(fecha)
                .stockAnterior(stockAnterior)
                .stockNuevo(stockNuevo)
                .consumo(consumo)
                .build());
    }

    private Set<Integer> rangoCompleto(int size) {
        Set<Integer> s = new HashSet<>();
        for (int i = 0; i < size; i++) s.add(i);
        return s;
    }
}
