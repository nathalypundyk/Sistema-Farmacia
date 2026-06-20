package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.MedicamentoRepository;

@Controller
@RequestMapping("/medicamentos")
public class MedicamentoController {

    private static final int PAGE_SIZE = 25;

    @Autowired
    private MedicamentoRepository medicamentoRepository;

    @GetMapping
    public String listarMedicamentos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "false") boolean soloPrioridad,
            Model model) {

        Page<Medicamento> pagina = medicamentoRepository.buscarPaginado(
                buscar.trim(), soloPrioridad,
                PageRequest.of(page, PAGE_SIZE));

        model.addAttribute("medicamentos", pagina.getContent());
        model.addAttribute("pagina", pagina);
        model.addAttribute("buscar", buscar);
        model.addAttribute("soloPrioridad", soloPrioridad);
        model.addAttribute("nuevoMedicamento", new Medicamento());
        return "medicamentos";
    }

    @PostMapping("/guardar")
    public String guardarMedicamento(@ModelAttribute Medicamento medicamento) {
        try {
            if (medicamento.getCodigo() == null || medicamento.getCodigo().isEmpty()) {
                medicamento.setCodigo("MED-" + System.currentTimeMillis());
            }
            // Normalizar código SICIAP vacío a null (la columna es única; null permite varios)
            if (medicamento.getCodigoSiciap() != null && medicamento.getCodigoSiciap().isBlank()) {
                medicamento.setCodigoSiciap(null);
            }
            // Stock mínimo/deseable por defecto: 100
            if (medicamento.getMinimo() == null)   medicamento.setMinimo(100);
            if (medicamento.getDeseable() == null) medicamento.setDeseable(100);
            medicamentoRepository.save(medicamento);
            return "redirect:/medicamentos?success";
        } catch (Exception e) {
            return "redirect:/medicamentos?error=" + e.getMessage();
        }
    }

    @PostMapping("/actualizar")
    public String actualizarMedicamento(
            @RequestParam Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) String concentracion,
            @RequestParam(required = false) String codigoSiciap,
            @RequestParam(defaultValue = "false") boolean esPrioridad,
            @RequestParam(defaultValue = "false") boolean esLasa,
            @RequestParam(required = false) String similarA) {
        try {
            Medicamento med = medicamentoRepository.findById(id).orElseThrow();
            med.setNombre(nombre);
            med.setConcentracion(concentracion);
            // Asociación manual de código SICIAP (vacío = quitar). NO se toca el stock mínimo aquí:
            // eso se gestiona solo en el apartado Configuración protegido con PIN.
            med.setCodigoSiciap((codigoSiciap == null || codigoSiciap.isBlank()) ? null : codigoSiciap.trim());
            med.setEsPrioridad(esPrioridad);
            med.setEsLasa(esLasa);
            med.setSimilarA(esLasa ? similarA : null);
            medicamentoRepository.save(med);
            return "redirect:/medicamentos?success";
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return "redirect:/medicamentos?error=Ese código SICIAP ya está asignado a otro medicamento.";
        } catch (Exception e) {
            return "redirect:/medicamentos?error=" + e.getMessage();
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarMedicamento(@PathVariable Long id) {
        try {
            medicamentoRepository.deleteById(id);
            return "redirect:/medicamentos?success";
        } catch (Exception e) {
            return "redirect:/medicamentos?error=No se puede eliminar el medicamento porque tiene lotes o registros asociados.";
        }
    }
}
