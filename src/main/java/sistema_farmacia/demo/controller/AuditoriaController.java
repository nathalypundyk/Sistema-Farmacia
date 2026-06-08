package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sistema_farmacia.demo.repository.ImportacionLogRepository;

@Controller
public class AuditoriaController {

    @Autowired
    private ImportacionLogRepository importacionLogRepo;

    @GetMapping("/auditoria")
    public String verAuditoria(Model model) {
        model.addAttribute("importaciones", importacionLogRepo.findAllByOrderByFechaDesc());
        model.addAttribute("activePage", "auditoria");
        return "auditoria";
    }
}
