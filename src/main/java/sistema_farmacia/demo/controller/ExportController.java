package sistema_farmacia.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sistema_farmacia.demo.model.AuditoriaAjuste;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.AuditoriaAjusteRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.repository.LoteRepository;
import sistema_farmacia.demo.model.Lote;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ExportController {

    @Autowired
    private AuditoriaAjusteRepository auditoriaRepo;

    @Autowired
    private MedicamentoRepository medicamentoRepo;

    @Autowired
    private LoteRepository loteRepo;

    @GetMapping("/export/auditoria")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=auditoria_farmacia.xlsx");

        List<AuditoriaAjuste> auditorias = auditoriaRepo.findAllByOrderByFechaDesc();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Auditoría");

        // ===== Title Row =====
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(40);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📋 Reporte de Auditoría - HGS Pharma");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // ===== Subtitle Row =====
        CellStyle subtitleStyle = workbook.createCellStyle();
        Font subtitleFont = workbook.createFont();
        subtitleFont.setFontHeightInPoints((short) 10);
        subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subtitleStyle.setFont(subtitleFont);
        subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

        Row subtitleRow = sheet.createRow(1);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Generado el: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        subtitleCell.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        // ===== Header Row =====
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(30);
        String[] columns = {"Fecha", "Hora", "Usuario", "Medicamento", "Acción", "Cant. Anterior", "Cant. Nueva"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // ===== Data Rows =====
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        CellStyle altRowStyle = workbook.createCellStyle();
        altRowStyle.cloneStyleFrom(dataStyle);
        altRowStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        altRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        int rowIdx = 4;
        for (AuditoriaAjuste audit : auditorias) {
            Row row = sheet.createRow(rowIdx);
            CellStyle currentStyle = (rowIdx % 2 == 0) ? altRowStyle : dataStyle;

            Cell c0 = row.createCell(0);
            c0.setCellValue(audit.getFecha() != null ? audit.getFecha().format(dateFmt) : "");
            c0.setCellStyle(currentStyle);

            Cell c1 = row.createCell(1);
            c1.setCellValue(audit.getFecha() != null ? audit.getFecha().format(timeFmt) : "");
            c1.setCellStyle(currentStyle);

            Cell c2 = row.createCell(2);
            c2.setCellValue(audit.getUsuario());
            c2.setCellStyle(currentStyle);

            Cell c3 = row.createCell(3);
            c3.setCellValue(audit.getMedicamentoNombre());
            c3.setCellStyle(currentStyle);

            Cell c4 = row.createCell(4);
            c4.setCellValue(audit.getAccion());
            c4.setCellStyle(currentStyle);

            Cell c5 = row.createCell(5);
            c5.setCellValue(audit.getCantidadAnterior() != null ? audit.getCantidadAnterior() : 0);
            c5.setCellStyle(currentStyle);

            Cell c6 = row.createCell(6);
            c6.setCellValue(audit.getCantidadNueva() != null ? audit.getCantidadNueva() : 0);
            c6.setCellStyle(currentStyle);

            rowIdx++;
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3500));
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/export/pdf")
    public void exportToPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=inventario_medicamentos.xlsx");

        List<Medicamento> medicamentos = medicamentoRepo.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventario");

        // ===== Title =====
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(40);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("💊 Inventario de Medicamentos - HGS Pharma");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // ===== Subtitle =====
        CellStyle subStyle = workbook.createCellStyle();
        Font subFont = workbook.createFont();
        subFont.setFontHeightInPoints((short) 10);
        subFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subStyle.setFont(subFont);
        subStyle.setAlignment(HorizontalAlignment.CENTER);

        Row subRow = sheet.createRow(1);
        Cell subCell = subRow.createCell(0);
        subCell.setCellValue("Generado el: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        subCell.setCellStyle(subStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

        // ===== Header =====
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(28);
        String[] cols = {"Código", "Nombre", "Tipo", "Stock Total", "Mínimo", "Estado Stock"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        // ===== Data =====
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        CellStyle altStyle = workbook.createCellStyle();
        altStyle.cloneStyleFrom(dataStyle);
        altStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int rowIdx = 4;
        for (Medicamento med : medicamentos) {
            Row row = sheet.createRow(rowIdx);
            CellStyle s = (rowIdx % 2 == 0) ? altStyle : dataStyle;

            Cell c0 = row.createCell(0); c0.setCellValue(med.getCodigo() != null ? med.getCodigo() : ""); c0.setCellStyle(s);
            Cell c1 = row.createCell(1); c1.setCellValue(med.getNombre() != null ? med.getNombre() : ""); c1.setCellStyle(s);
            Cell c2 = row.createCell(2); c2.setCellValue(med.getTipo() != null ? med.getTipo().getNombre() : "—"); c2.setCellStyle(s);
            Cell c3 = row.createCell(3); c3.setCellValue(med.getStockTotal()); c3.setCellStyle(s);
            Cell c4 = row.createCell(4); c4.setCellValue(med.getMinimo() != null ? med.getMinimo() : 0); c4.setCellStyle(s);
            Cell c5 = row.createCell(5); c5.setCellValue(med.isStockBajo() ? "⚠️ BAJO" : "✅ OK"); c5.setCellStyle(s);

            rowIdx++;
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3500));
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/export/lotes")
    public void exportLotesExcel(@RequestParam(required = false) String filtro, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=lotes_farmacia.xlsx");

        List<Lote> lotes = loteRepo.findAll();
        if ("poco_stock".equals(filtro)) {
            lotes = lotes.stream()
                .filter(l -> l.getMedicamento() != null && l.getMedicamento().isStockBajo())
                .toList();
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Lotes");

        // ===== Title =====
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(40);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📦 Reporte de Lotes - HGS Pharma");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // ===== Header =====
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(28);
        String[] cols = {"Nro Lote", "Nro Serie", "Medicamento", "Depósito", "Vencimiento", "Cantidad", "Estado"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        // ===== Data =====
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        int rowIdx = 3;
        for (Lote lote : lotes) {
            Row row = sheet.createRow(rowIdx);

            Cell c0 = row.createCell(0); c0.setCellValue(lote.getNroLote() != null ? lote.getNroLote() : ""); c0.setCellStyle(dataStyle);
            Cell c1 = row.createCell(1); c1.setCellValue(lote.getNroSerie() != null ? lote.getNroSerie() : ""); c1.setCellStyle(dataStyle);
            Cell c2 = row.createCell(2); c2.setCellValue(lote.getMedicamento() != null ? lote.getMedicamento().getNombre() : "—"); c2.setCellStyle(dataStyle);
            Cell c3 = row.createCell(3); c3.setCellValue(lote.getDeposito() != null ? lote.getDeposito().getNombre() : "—"); c3.setCellStyle(dataStyle);
            Cell c4 = row.createCell(4); c4.setCellValue(lote.getFechaVencimiento() != null ? lote.getFechaVencimiento().format(dateFmt) : ""); c4.setCellStyle(dataStyle);
            Cell c5 = row.createCell(5); c5.setCellValue(lote.getCantidadDisponible() != null ? lote.getCantidadDisponible() : 0); c5.setCellStyle(dataStyle);
            Cell c6 = row.createCell(6); c6.setCellValue(lote.getEstado() != null ? lote.getEstado() : ""); c6.setCellStyle(dataStyle);

            rowIdx++;
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3500));
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @Autowired
    private sistema_farmacia.demo.repository.SalidaRepository salidaRepo;

    @GetMapping("/export/salidas")
    public void exportSalidasExcel(@RequestParam(required = false) String filtro, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=salidas_farmacia.xlsx");

        List<sistema_farmacia.demo.model.Salida> salidas = salidaRepo.findAll();
        java.time.LocalDate now = java.time.LocalDate.now();
        
        if ("hoy".equals(filtro)) {
            salidas = salidas.stream()
                .filter(s -> s.getFechaSalida() != null && s.getFechaSalida().equals(now))
                .toList();
        } else if ("mes".equals(filtro)) {
            salidas = salidas.stream()
                .filter(s -> s.getFechaSalida() != null && s.getFechaSalida().getMonth() == now.getMonth() && s.getFechaSalida().getYear() == now.getYear())
                .toList();
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Salidas");

        // ===== Title =====
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setFont(titleFont);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(40);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("📤 Reporte de Salidas - HGS Pharma");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // ===== Header =====
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row headerRow = sheet.createRow(2);
        headerRow.setHeightInPoints(28);
        String[] cols = {"ID", "Medicamento", "Lote", "Cantidad", "Destino", "Fecha Salida"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

        // ===== Data =====
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        int rowIdx = 3;
        for (sistema_farmacia.demo.model.Salida sal : salidas) {
            Row row = sheet.createRow(rowIdx);

            Cell c0 = row.createCell(0); c0.setCellValue(sal.getId()); c0.setCellStyle(dataStyle);
            Cell c1 = row.createCell(1); c1.setCellValue(sal.getMedicamento() != null ? sal.getMedicamento().getNombre() : "—"); c1.setCellStyle(dataStyle);
            Cell c2 = row.createCell(2); c2.setCellValue(sal.getLote() != null ? sal.getLote().getNroLote() : "—"); c2.setCellStyle(dataStyle);
            Cell c3 = row.createCell(3); c3.setCellValue(sal.getCantidad() != null ? sal.getCantidad() : 0); c3.setCellStyle(dataStyle);
            Cell c4 = row.createCell(4); c4.setCellValue(sal.getDestino() != null ? sal.getDestino() : ""); c4.setCellStyle(dataStyle);
            Cell c5 = row.createCell(5); c5.setCellValue(sal.getFechaSalida() != null ? sal.getFechaSalida().format(dateFmt) : ""); c5.setCellStyle(dataStyle);

            rowIdx++;
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3500));
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}
