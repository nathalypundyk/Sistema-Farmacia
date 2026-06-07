package sistema_farmacia.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.service.ImportacionService;
import sistema_farmacia.demo.service.ImportacionService.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/importacion")
public class ImportacionController {

    @Autowired private ImportacionService importacionService;
    @Autowired private MedicamentoRepository medicamentoRepo;

    private static final String SESSION_KEY = "importPreview";

    @GetMapping
    public String mostrar(HttpSession session, Model model) {
        ResultadoPreview preview = (ResultadoPreview) session.getAttribute(SESSION_KEY);
        if (preview != null) {
            model.addAttribute("resultado", preview);
        }
        model.addAttribute("medicamentos", medicamentoRepo.findAll());
        return "importacion";
    }

    @PostMapping("/preview")
    public String preview(@RequestParam("archivo") MultipartFile archivo,
                          HttpSession session, Model model) {
        if (archivo.isEmpty()) {
            model.addAttribute("error", "Por favor seleccioná un archivo Excel.");
            model.addAttribute("medicamentos", medicamentoRepo.findAll());
            return "importacion";
        }
        try {
            ResultadoPreview resultado = importacionService.procesarExcel(archivo);
            session.setAttribute(SESSION_KEY, resultado);
            model.addAttribute("resultado", resultado);
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar el archivo: " + e.getMessage());
        }
        model.addAttribute("medicamentos", medicamentoRepo.findAll());
        return "importacion";
    }

    @PostMapping("/confirmar")
    public String confirmar(@RequestParam Map<String, String> params,
                            HttpSession session, Model model) {
        ResultadoPreview preview = (ResultadoPreview) session.getAttribute(SESSION_KEY);
        if (preview == null) {
            model.addAttribute("error", "La sesión expiró. Por favor volvé a subir el archivo.");
            model.addAttribute("medicamentos", medicamentoRepo.findAll());
            return "importacion";
        }

        // Construir lista completa: matched + unmatched con asignaciones manuales
        List<GrupoMedicamento> grupos = new ArrayList<>(preview.matched);

        for (int i = 0; i < preview.unmatched.size(); i++) {
            GrupoMedicamento g = preview.unmatched.get(i);
            String medIdStr = params.get("manual_" + i);
            if (medIdStr != null && !medIdStr.isEmpty() && !medIdStr.equals("-1")) {
                try {
                    g.medicamentoId = Long.parseLong(medIdStr);
                    g.tipoMatch = "MANUAL";
                } catch (NumberFormatException ignored) {}
            }
            grupos.add(g);
        }

        try {
            int lotesActualizados = importacionService.confirmarImportacion(grupos);
            session.removeAttribute(SESSION_KEY);

            long sinAsignar = preview.unmatched.stream()
                    .filter(g -> g.medicamentoId == null).count();

            model.addAttribute("exito", "Importación completada: " + lotesActualizados
                    + " lote(s) actualizados/creados."
                    + (sinAsignar > 0 ? " " + sinAsignar + " medicamento(s) sin asignar fueron ignorados." : ""));
        } catch (Exception e) {
            model.addAttribute("error", "Error al confirmar la importación: " + e.getMessage());
            model.addAttribute("resultado", preview);
        }

        model.addAttribute("medicamentos", medicamentoRepo.findAll());
        return "importacion";
    }

    @GetMapping("/cancelar")
    public String cancelar(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return "redirect:/importacion";
    }
}
