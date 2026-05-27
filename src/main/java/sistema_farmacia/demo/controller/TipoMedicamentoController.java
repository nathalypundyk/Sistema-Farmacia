package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.TipoMedicamento;
import sistema_farmacia.demo.repository.TipoMedicamentoRepository;

@Controller
@RequestMapping("/tipo-medicamentos")
public class TipoMedicamentoController {

    @Autowired
    private TipoMedicamentoRepository tipoMedicamentoRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "tipo-medicamentos");
        model.addAttribute("tipos", tipoMedicamentoRepository.findAll());
        model.addAttribute("nuevoTipo", new TipoMedicamento());
        return "tipo-medicamentos";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute TipoMedicamento tipo) {
        try {
            tipoMedicamentoRepository.save(tipo);
            return "redirect:/tipo-medicamentos?success";
        } catch (Exception e) {
            return "redirect:/tipo-medicamentos?error=" + e.getMessage();
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        try {
            tipoMedicamentoRepository.deleteById(id);
            return "redirect:/tipo-medicamentos?success";
        } catch (Exception e) {
            return "redirect:/tipo-medicamentos?error=No se puede eliminar porque tiene medicamentos asociados.";
        }
    }
}
