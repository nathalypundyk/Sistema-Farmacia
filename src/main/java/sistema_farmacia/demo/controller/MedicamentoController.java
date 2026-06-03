package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.repository.PresentacionRepository;
import sistema_farmacia.demo.repository.TipoMedicamentoRepository;

@Controller
@RequestMapping("/medicamentos")
public class MedicamentoController {

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @Autowired
    private TipoMedicamentoRepository tipoMedicamentoRepository;

    @Autowired
    private PresentacionRepository presentacionRepository;

    @GetMapping
    public String listarMedicamentos(Model model) {
        model.addAttribute("activePage", "medicamentos");
        model.addAttribute("medicamentos", medicamentoRepository.findAll());
        model.addAttribute("tipos", tipoMedicamentoRepository.findAll());
        model.addAttribute("presentaciones", presentacionRepository.findAll());
        model.addAttribute("nuevoMedicamento", new Medicamento());
        return "medicamentos";
    }

    @PostMapping("/guardar")
    public String guardarMedicamento(@ModelAttribute Medicamento medicamento) {
        try {
            // Generar código aleatorio si no se provee
            if (medicamento.getCodigo() == null || medicamento.getCodigo().isEmpty()) {
                medicamento.setCodigo("MED-" + System.currentTimeMillis());
            }
            medicamentoRepository.save(medicamento);
            return "redirect:/medicamentos?success";
        } catch (Exception e) {
            return "redirect:/medicamentos?error=" + e.getMessage();
        }
    }

    @PostMapping("/actualizar")
    public String actualizarMedicamento(
            @org.springframework.web.bind.annotation.RequestParam Long id,
            @org.springframework.web.bind.annotation.RequestParam String nombre,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String concentracion,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String codigo,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long tipoId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long presentacionId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") Integer minimo,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean esPrioridad,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean esLasa,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String similarA) {
        try {
            Medicamento med = medicamentoRepository.findById(id).orElseThrow();
            med.setNombre(nombre);
            med.setConcentracion(concentracion);
            if (codigo != null && !codigo.isBlank()) med.setCodigo(codigo);
            if (tipoId != null) tipoMedicamentoRepository.findById(tipoId).ifPresent(med::setTipo);
            if (presentacionId != null) presentacionRepository.findById(presentacionId).ifPresent(med::setPresentacion);
            med.setMinimo(minimo);
            med.setEsPrioridad(esPrioridad);
            med.setEsLasa(esLasa);
            med.setSimilarA(esLasa ? similarA : null);
            medicamentoRepository.save(med);
            return "redirect:/medicamentos?success";
        } catch (Exception e) {
            return "redirect:/medicamentos?error=" + e.getMessage();
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarMedicamento(@org.springframework.web.bind.annotation.PathVariable Long id) {
        try {
            medicamentoRepository.deleteById(id);
            return "redirect:/medicamentos?success";
        } catch (Exception e) {
            return "redirect:/medicamentos?error=No se puede eliminar el producto porque tiene lotes o registros asociados.";
        }
    }
}
