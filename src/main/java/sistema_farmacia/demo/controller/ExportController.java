package sistema_farmacia.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sistema_farmacia.demo.model.ImportacionLog;
import sistema_farmacia.demo.model.Lote;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.ImportacionLogRepository;
import sistema_farmacia.demo.repository.LoteRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ExportController {

    @Autowired private ImportacionLogRepository importacionLogRepo;
    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private LoteRepository loteRepo;

    @GetMapping("/export/auditoria")
    public void exportAuditoria(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=auditoria_importaciones.xlsx");

        List<ImportacionLog> logs = importacionLogRepo.findAllByOrderByFechaDesc();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Importaciones");

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
        titleCell.setCellValue("📥 Historial de Importaciones - HGS Pharma");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

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
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        Row headerRow = sheet.createRow(3);
        headerRow.setHeightInPoints(28);
        String[] columns = {"Fecha", "Hora", "Usuario", "Tipo", "Items Totales", "Aplicados", "Equiv. Guardadas"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        CellStyle altStyle = workbook.createCellStyle();
        altStyle.cloneStyleFrom(dataStyle);
        altStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        int rowIdx = 4;
        for (ImportacionLog log : logs) {
            Row row = sheet.createRow(rowIdx);
            CellStyle s = (rowIdx % 2 == 0) ? altStyle : dataStyle;

            Cell c0 = row.createCell(0); c0.setCellValue(log.getFecha() != null ? log.getFecha().format(dateFmt) : ""); c0.setCellStyle(s);
            Cell c1 = row.createCell(1); c1.setCellValue(log.getFecha() != null ? log.getFecha().format(timeFmt) : ""); c1.setCellStyle(s);
            Cell c2 = row.createCell(2); c2.setCellValue(log.getUsuario() != null ? log.getUsuario() : ""); c2.setCellStyle(s);
            Cell c3 = row.createCell(3); c3.setCellValue(log.getTipo() != null ? log.getTipo() : ""); c3.setCellStyle(s);
            Cell c4 = row.createCell(4); c4.setCellValue(log.getTotalItems() != null ? log.getTotalItems() : 0); c4.setCellStyle(s);
            Cell c5 = row.createCell(5); c5.setCellValue(log.getAplicados() != null ? log.getAplicados() : 0); c5.setCellStyle(s);
            Cell c6 = row.createCell(6); c6.setCellValue(log.getEquivalenciasGuardadas() != null ? log.getEquivalenciasGuardadas() : 0); c6.setCellStyle(s);

            rowIdx++;
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 3500));
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/export/pdf")
    public void exportMedicamentos(HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=inventario_medicamentos.xlsx");

        List<Medicamento> medicamentos = medicamentoRepo.findAll();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Inventario");

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
        String[] cols = {"Código", "Nombre", "F. Farmacéutica", "Stock Total", "Mínimo", "Estado Stock"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

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
            Cell c2 = row.createCell(2); c2.setCellValue(med.getFormaFarmaceutica() != null ? med.getFormaFarmaceutica() : "—"); c2.setCellStyle(s);
            Cell c3 = row.createCell(3); c3.setCellValue(med.getStockTotal()); c3.setCellStyle(s);
            Cell c4 = row.createCell(4); c4.setCellValue(med.getMinimo() != null ? med.getMinimo() : 0); c4.setCellStyle(s);
            Cell c5 = row.createCell(5); c5.setCellValue(med.isStockBajo() ? "BAJO" : "OK"); c5.setCellStyle(s);

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
    public void exportLotes(@RequestParam(required = false) String filtro, HttpServletResponse response) throws Exception {
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
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

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
        String[] cols = {"Nro Lote", "Medicamento", "Vencimiento", "Cantidad"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(cols[i]);
            cell.setCellStyle(headerStyle);
        }

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
            Cell c1 = row.createCell(1); c1.setCellValue(lote.getMedicamento() != null ? lote.getMedicamento().getNombre() : "—"); c1.setCellStyle(dataStyle);
            Cell c2 = row.createCell(2); c2.setCellValue(lote.getFechaVencimiento() != null ? lote.getFechaVencimiento().format(dateFmt) : ""); c2.setCellStyle(dataStyle);
            Cell c3 = row.createCell(3); c3.setCellValue(lote.getCantidadDisponible() != null ? lote.getCantidadDisponible() : 0); c3.setCellStyle(dataStyle);
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
