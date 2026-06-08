package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Lote;
import sistema_farmacia.demo.repository.LoteRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;

@Controller
@RequestMapping("/lotes")
public class LoteController {

    private static final int PAGE_SIZE = 30;

    @Autowired private LoteRepository loteRepository;
    @Autowired private MedicamentoRepository medicamentoRepository;

    @GetMapping
    public String listarLotes(
            @RequestParam(required = false) String filtro,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        PageRequest pageable = PageRequest.of(page, PAGE_SIZE);
        Page<Lote> pagina = "poco_stock".equals(filtro)
                ? loteRepository.findPocoStock(pageable)
                : loteRepository.findAllPaginado(pageable);

        model.addAttribute("lotes", pagina.getContent());
        model.addAttribute("pagina", pagina);
        model.addAttribute("medicamentos", medicamentoRepository.findAll());
        model.addAttribute("activePage", "lotes");
        model.addAttribute("filtroActual", filtro);
        return "lotes";
    }

    @PostMapping("/save")
    public String guardarLote(@ModelAttribute Lote lote) {
        loteRepository.save(lote);
        return "redirect:/lotes?success";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarLote(@PathVariable Long id) {
        try {
            loteRepository.deleteById(id);
            return "redirect:/lotes?success";
        } catch (Exception e) {
            return "redirect:/lotes?error=No se puede eliminar el lote porque tiene registros asociados.";
        }
    }
}
