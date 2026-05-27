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
}
