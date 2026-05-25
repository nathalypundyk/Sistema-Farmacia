package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Lote;
import sistema_farmacia.demo.repository.LoteRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.repository.DepositoRepository;

import java.util.List;

@Controller
@RequestMapping("/lotes")
public class LoteController {

    @Autowired
    private LoteRepository loteRepository;
    
    @Autowired
    private MedicamentoRepository medicamentoRepository;
    
    @Autowired
    private DepositoRepository depositoRepository;

    @GetMapping
    public String listarLotes(Model model) {
        List<Lote> lotes = loteRepository.findAll();
        model.addAttribute("lotes", lotes);
        model.addAttribute("medicamentos", medicamentoRepository.findAll());
        model.addAttribute("depositos", depositoRepository.findAll());
        model.addAttribute("activePage", "lotes");
        return "lotes";
    }

    @PostMapping("/save")
    public String guardarLote(@ModelAttribute Lote lote) {
        if (lote.getEstado() == null || lote.getEstado().isEmpty()) {
            lote.setEstado("ACTIVO");
        }
        loteRepository.save(lote);
        return "redirect:/lotes?success";
    }

    @PostMapping("/delete")
    public String eliminarLote(@RequestParam Long id) {
        loteRepository.deleteById(id);
        return "redirect:/lotes?success";
    }
}
