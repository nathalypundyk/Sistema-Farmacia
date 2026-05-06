package sistema_farmacia.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import sistema_farmacia.demo.model.AuditoriaAjuste;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.AuditoriaAjusteRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;

import java.io.IOException;
import java.util.List;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ExportController {

    @Autowired
    private AuditoriaAjusteRepository auditoriaRepo;

    @Autowired
    private MedicamentoRepository medicamentoRepo;

    @GetMapping("/export/auditoria")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=auditoria_farmacia.xlsx");

        List<AuditoriaAjuste> auditorias = auditoriaRepo.findAllByOrderByFechaDesc();

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Auditoria");

        // Header
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Fecha", "Usuario", "Medicamento", "Accion", "Anterior", "Nuevo", "PIN Verificado"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        // Data
        int rowIdx = 1;
        for (AuditoriaAjuste audit : auditorias) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(audit.getFecha().toString());
            row.createCell(1).setCellValue(audit.getUsuario());
            row.createCell(2).setCellValue(audit.getMedicamentoNombre());
            row.createCell(3).setCellValue(audit.getAccion());
            row.createCell(4).setCellValue(audit.getCantidadAnterior() != null ? audit.getCantidadAnterior() : 0);
            row.createCell(5).setCellValue(audit.getCantidadNueva() != null ? audit.getCantidadNueva() : 0);
            row.createCell(6).setCellValue(audit.getPinVerificado() ? "SI" : "NO");
        }

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    @GetMapping("/export/pdf")
    public void exportToPdf(HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=reporte_inventario.pdf");

        List<Medicamento> medicamentos = medicamentoRepo.findAll();
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(medicamentos);

        // En un entorno real usaríamos un archivo .jrxml
        // Por simplicidad para este roadmap, simulamos la generación del reporte
        JasperReport jasperReport = JasperCompileManager.compileReport(
            getClass().getResourceAsStream("/reports/inventario.jrxml"));
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("titulo", "Reporte de Inventario - HGS Pharma");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        JasperExportManager.exportReportToPdfStream(jasperPrint, response.getOutputStream());
    }
}
