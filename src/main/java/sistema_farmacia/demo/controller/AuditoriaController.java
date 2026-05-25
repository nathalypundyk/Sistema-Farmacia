package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sistema_farmacia.demo.model.AuditoriaAjuste;
import sistema_farmacia.demo.repository.AuditoriaAjusteRepository;
import org.springframework.data.domain.Sort;

import java.util.List;

@Controller
public class AuditoriaController {

    @Autowired
    private AuditoriaAjusteRepository auditoriaAjusteRepository;

    @GetMapping("/auditoria")
    public String verAuditoria(Model model) {
        List<AuditoriaAjuste> auditorias = auditoriaAjusteRepository.findAll(Sort.by(Sort.Direction.DESC, "fecha"));
        model.addAttribute("auditorias", auditorias);
        model.addAttribute("activePage", "auditoria");
        return "auditoria";
    }
}
