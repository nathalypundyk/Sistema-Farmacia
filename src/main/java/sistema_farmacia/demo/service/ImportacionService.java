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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class ImportacionService {

    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private MedicamentoEquivalenciaRepository equivalenciaRepo;
    @Autowired private LoteRepository loteRepo;
    @Autowired private InventarioService inventarioService;

    // ── DTOs serializables para sesión HTTP ──────────────────────────────────

    public static class FilaExcel implements Serializable {
        public String nroLote;
        public LocalDate fechaVencimiento;
        public Integer cantidad;
    }

    public static class GrupoMedicamento implements Serializable {
        public String nombreExterno;
        public String concentracionExterna;
        public List<FilaExcel> lotes = new ArrayList<>();
        public Long medicamentoId;
        public String nombreInterno;
        /** "AUTO" = match directo, "EQUIVALENCIA" = mapeado previo, null = manual pendiente */
        public String tipoMatch;
    }

    public static class ResultadoPreview implements Serializable {
        public List<GrupoMedicamento> matched = new ArrayList<>();
        public List<GrupoMedicamento> unmatched = new ArrayList<>();
        public int totalFilasExcel;
    }

    // ── Normalización ────────────────────────────────────────────────────────

    public String normalizar(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase()
                .replaceAll("[áÁ]", "A").replaceAll("[éÉ]", "E")
                .replaceAll("[íÍ]", "I").replaceAll("[óÓ]", "O")
                .replaceAll("[úÚüÜ]", "U")
                .replaceAll("\\s+", " ")
                .replaceAll("[^A-Z0-9 .,%/()-]", "")
                .trim();
    }

    // ── Helpers de lectura de celdas ─────────────────────────────────────────

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf((long) cell.getNumericCellValue()); }
                catch (Exception e) {
                    try { yield cell.getStringCellValue().trim(); }
                    catch (Exception ex) { yield ""; }
                }
            }
            default -> "";
        };
    }

    private LocalDate parseFecha(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String s = getCellString(cell);
        if (s.isEmpty()) return null;
        for (String fmt : List.of("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy", "d/M/yyyy")) {
            try { return LocalDate.parse(s, DateTimeFormatter.ofPattern(fmt)); }
            catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    // ── Detección automática de columnas por cabecera ────────────────────────

    private int[] detectarColumnas(Row header) throws Exception {
        // índices: [nombre, concentracion, lote, vencimiento, cantidad]
        int[] cols = {-1, -1, -1, -1, -1};
        for (Cell c : header) {
            String h = getCellString(c).toLowerCase();
            if (h.contains("nombre") || h.contains("medicamento")
                    || h.contains("denominacion") || h.contains("descripcion"))      cols[0] = c.getColumnIndex();
            else if (h.contains("concentr") || h.contains("dosis") || h.contains("potencia")) cols[1] = c.getColumnIndex();
            else if ((h.contains("lote") || h.contains("batch"))
                    && !h.contains("fecha") && !h.contains("vencim"))                cols[2] = c.getColumnIndex();
            else if (h.contains("vencim") || (h.contains("fecha") && h.contains("exp"))
                    || h.equals("fecha"))                                             cols[3] = c.getColumnIndex();
            else if (h.contains("saldo") || h.contains("stock") || h.contains("existencia")
                    || h.contains("cantidad") || h.startsWith("cant"))               cols[4] = c.getColumnIndex();
        }
        if (cols[0] == -1)
            throw new Exception("No se encontró columna de nombre/medicamento. Cabeceras detectadas: "
                    + getHeaderNames(header));
        return cols;
    }

    private String getHeaderNames(Row header) {
        StringBuilder sb = new StringBuilder();
        for (Cell c : header) { sb.append("[").append(getCellString(c)).append("] "); }
        return sb.toString().trim();
    }

    // ── Proceso principal ────────────────────────────────────────────────────

    public ResultadoPreview procesarExcel(MultipartFile file) throws Exception {
        // 1. Parsear Excel
        List<GrupoMedicamento> grupos = new LinkedList<>();
        Map<String, GrupoMedicamento> indice = new LinkedHashMap<>();
        int totalFilas = 0;

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getLastRowNum() < 1) throw new Exception("El archivo Excel está vacío");

            int[] cols = detectarColumnas(sheet.getRow(0));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String nombre = getCellString(row.getCell(cols[0]));
                if (nombre.isEmpty()) continue;

                String conc = cols[1] >= 0 ? getCellString(row.getCell(cols[1])) : "";
                String key = normalizar(nombre) + "|" + normalizar(conc);

                GrupoMedicamento grupo = indice.computeIfAbsent(key, k -> {
                    GrupoMedicamento g = new GrupoMedicamento();
                    g.nombreExterno = nombre;
                    g.concentracionExterna = conc;
                    grupos.add(g);
                    return g;
                });

                // Solo agrega fila como lote si tiene número de lote
                String nroLote = cols[2] >= 0 ? getCellString(row.getCell(cols[2])) : "";
                if (!nroLote.isEmpty()) {
                    FilaExcel fila = new FilaExcel();
                    fila.nroLote = nroLote;
                    fila.fechaVencimiento = cols[3] >= 0 ? parseFecha(row.getCell(cols[3])) : null;
                    if (cols[4] >= 0) {
                        try { fila.cantidad = Integer.parseInt(getCellString(row.getCell(cols[4])).replace(",", "").replace(".", "")); }
                        catch (NumberFormatException e) { fila.cantidad = 0; }
                    } else {
                        fila.cantidad = 0;
                    }
                    grupo.lotes.add(fila);
                }
                totalFilas++;
            }
        }

        // 2. Intentar match para cada grupo
        List<Medicamento> todosMeds = medicamentoRepo.findAll();
        ResultadoPreview resultado = new ResultadoPreview();
        resultado.totalFilasExcel = totalFilas;

        for (GrupoMedicamento grupo : grupos) {
            String nomNorm = normalizar(grupo.nombreExterno);
            String concNorm = normalizar(grupo.concentracionExterna);

            // a) Equivalencia guardada previamente
            Optional<MedicamentoEquivalencia> eq =
                    equivalenciaRepo.findByNombreExternoAndConcentracionExterna(nomNorm, concNorm);
            if (eq.isPresent()) {
                grupo.medicamentoId = eq.get().getMedicamento().getId();
                grupo.nombreInterno = eq.get().getMedicamento().getNombre();
                grupo.tipoMatch = "EQUIVALENCIA";
                resultado.matched.add(grupo);
                continue;
            }

            // b) Match automático nombre + concentración normalizado
            Optional<Medicamento> match = todosMeds.stream()
                    .filter(m -> normalizar(m.getNombre()).equals(nomNorm)
                            && normalizar(m.getConcentracion()).equals(concNorm))
                    .findFirst();
            if (match.isPresent()) {
                grupo.medicamentoId = match.get().getId();
                grupo.nombreInterno = match.get().getNombre() + " " + match.get().getConcentracion();
                grupo.tipoMatch = "AUTO";
                resultado.matched.add(grupo);
                continue;
            }

            // c) Sin match — requiere revisión manual
            resultado.unmatched.add(grupo);
        }

        return resultado;
    }

    // ── Confirmar importación ────────────────────────────────────────────────

    @Transactional
    public int confirmarImportacion(List<GrupoMedicamento> grupos) {
        int lotesActualizados = 0;

        for (GrupoMedicamento grupo : grupos) {
            if (grupo.medicamentoId == null) continue;

            Medicamento med = medicamentoRepo.findById(grupo.medicamentoId).orElse(null);
            if (med == null) continue;

            // Guardar equivalencia para mapeos manuales (no AUTO, no ya guardadas)
            if (!"AUTO".equals(grupo.tipoMatch) && !"EQUIVALENCIA".equals(grupo.tipoMatch)) {
                String nomNorm = normalizar(grupo.nombreExterno);
                String concNorm = normalizar(grupo.concentracionExterna);
                if (equivalenciaRepo.findByNombreExternoAndConcentracionExterna(nomNorm, concNorm).isEmpty()) {
                    equivalenciaRepo.save(MedicamentoEquivalencia.builder()
                            .nombreExterno(nomNorm)
                            .concentracionExterna(concNorm)
                            .medicamento(med)
                            .build());
                }
            }

            // Actualizar o crear lotes
            List<Lote> lotesExistentes = loteRepo.findByMedicamento(med);
            for (FilaExcel fila : grupo.lotes) {
                if (fila.nroLote == null || fila.nroLote.isEmpty()) continue;

                Optional<Lote> existente = lotesExistentes.stream()
                        .filter(l -> fila.nroLote.equalsIgnoreCase(l.getNroLote()))
                        .findFirst();

                if (existente.isPresent()) {
                    existente.get().setCantidadDisponible(fila.cantidad);
                    loteRepo.save(existente.get());
                } else {
                    loteRepo.save(Lote.builder()
                            .nroLote(fila.nroLote)
                            .fechaVencimiento(fila.fechaVencimiento)
                            .cantidadDisponible(fila.cantidad)
                            .estado("DISPONIBLE")
                            .medicamento(med)
                            .build());
                }
                lotesActualizados++;
            }

            // Disparar verificación de alertas
            inventarioService.verificarAlertas(med);
        }

        return lotesActualizados;
    }
}
