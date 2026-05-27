package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import sistema_farmacia.demo.model.Deposito;
import sistema_farmacia.demo.repository.DepositoRepository;

@Controller
@RequestMapping("/depositos")
public class DepositoController {

    @Autowired
    private DepositoRepository depositoRepository;

    @GetMapping
    public String listarDepositos(Model model) {
        model.addAttribute("activePage", "depositos");
        model.addAttribute("depositos", depositoRepository.findAll());
        model.addAttribute("nuevoDeposito", new Deposito());
        return "depositos";
    }

    @PostMapping("/guardar")
    public String guardarDeposito(@ModelAttribute Deposito deposito) {
        try {
            depositoRepository.save(deposito);
            return "redirect:/depositos?success";
        } catch (Exception e) {
            return "redirect:/depositos?error=" + e.getMessage();
        }
    }
}
