package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.model.Usuario;
import sistema_farmacia.demo.repository.AuditoriaAjusteRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.repository.UsuarioRepository;
import sistema_farmacia.demo.service.InventarioService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private AuditoriaAjusteRepository auditoriaRepo;
    @Autowired private InventarioService inventarioService;
    @Autowired private UsuarioRepository usuarioRepo;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Medicamento> medicamentos = medicamentoRepo.findAll();
        
        long cantidadCritico = medicamentos.stream().filter(Medicamento::isStockBajo).count();
        long cantidadVencer = medicamentos.stream().filter(Medicamento::isProximoAVencer).count();
        
        List<Medicamento> medicamentosConAlerta = medicamentos.stream()
                .filter(m -> m.isStockBajo() || Boolean.TRUE.equals(m.getEsLasa()))
                .collect(Collectors.toList());
        
        model.addAttribute("medicamentos", medicamentos);
        model.addAttribute("cantidadCritico", cantidadCritico);
        model.addAttribute("cantidadVencer", cantidadVencer);
        model.addAttribute("ajustesHoy", auditoriaRepo.count());
        model.addAttribute("auditorias", auditoriaRepo.findAllByOrderByFechaDesc());
        model.addAttribute("medicamentosConAlerta", medicamentosConAlerta);
        
        // Datos dinámicos para los gráficos
        model.addAttribute("consumoData", inventarioService.getConsumoUltimos7Dias());
        model.addAttribute("stockDistribucionData", inventarioService.getDistribucionStockPorTipo());
        
        return "index";
    }

    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("medicamentos", medicamentoRepo.findAll());
        return "config";
    }

    @PostMapping("/config/save")
    public String saveConfig(@RequestParam Long medicamentoId,
                             @RequestParam Integer minimo,
                             @RequestParam Integer deseable,
                             @RequestParam Integer recomendado,
                             @RequestParam Integer unidadesPorBlister,
                             @RequestParam String pin) {
        try {
            Medicamento med = medicamentoRepo.findById(medicamentoId).orElseThrow();
            
            String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            Usuario currentUser = usuarioRepo.findByUsername(currentUsername).orElseThrow();

            // Validar PIN personal para ajuste de configuración
            if (currentUser.getPin() == null || !currentUser.getPin().equals(pin)) {
                return "redirect:/config?error=pin_invalido";
            }
            
            med.setMinimo(minimo);
            med.setDeseable(deseable);
            med.setRecomendado(recomendado);
            med.setUnidadesPorBlister(unidadesPorBlister);
            
            medicamentoRepo.save(med);
            inventarioService.verificarAlertas(med);
            
            return "redirect:/config?success=actualizado";
        } catch (Exception e) {
            return "redirect:/config?error=" + e.getMessage();
        }
    }
}
