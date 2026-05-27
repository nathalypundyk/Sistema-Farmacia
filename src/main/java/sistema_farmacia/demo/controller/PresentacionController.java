package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Presentacion;
import sistema_farmacia.demo.repository.PresentacionRepository;

@Controller
@RequestMapping("/presentaciones")
public class PresentacionController {

    @Autowired
    private PresentacionRepository presentacionRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("activePage", "presentaciones");
        model.addAttribute("presentaciones", presentacionRepository.findAll());
        model.addAttribute("nuevaPresentacion", new Presentacion());
        return "presentaciones";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Presentacion presentacion) {
        try {
            presentacionRepository.save(presentacion);
            return "redirect:/presentaciones?success";
        } catch (Exception e) {
            return "redirect:/presentaciones?error=" + e.getMessage();
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        try {
            presentacionRepository.deleteById(id);
            return "redirect:/presentaciones?success";
        } catch (Exception e) {
            return "redirect:/presentaciones?error=No se puede eliminar porque tiene medicamentos asociados.";
        }
    }
}
