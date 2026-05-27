package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Salida;
import sistema_farmacia.demo.repository.SalidaRepository;
import sistema_farmacia.demo.repository.MedicamentoRepository;
import sistema_farmacia.demo.repository.LoteRepository;

@Controller
@RequestMapping("/salidas")
public class SalidaController {

    @Autowired
    private SalidaRepository salidaRepository;
    @Autowired
    private MedicamentoRepository medicamentoRepository;
    @Autowired
    private LoteRepository loteRepository;

    @GetMapping
    public String listar(@RequestParam(required = false) String filtro, Model model) {
        java.util.List<Salida> salidas = salidaRepository.findAll();
        java.time.LocalDate now = java.time.LocalDate.now();
        
        if ("hoy".equals(filtro)) {
            salidas = salidas.stream()
                .filter(s -> s.getFechaSalida() != null && s.getFechaSalida().equals(now))
                .toList();
        } else if ("mes".equals(filtro)) {
            salidas = salidas.stream()
                .filter(s -> s.getFechaSalida() != null && s.getFechaSalida().getMonth() == now.getMonth() && s.getFechaSalida().getYear() == now.getYear())
                .toList();
        }
        
        model.addAttribute("activePage", "salidas");
        model.addAttribute("salidas", salidas);
        model.addAttribute("filtro", filtro);
        model.addAttribute("medicamentos", medicamentoRepository.findAll());
        model.addAttribute("lotes", loteRepository.findAll());
        model.addAttribute("nuevaSalida", new Salida());
        return "salidas";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Salida salida) {
        try {
            sistema_farmacia.demo.model.Lote lote = loteRepository.findById(salida.getLote().getId())
                .orElseThrow(() -> new RuntimeException("Lote no encontrado"));
                
            if (lote.getCantidadDisponible() < salida.getCantidad()) {
                throw new RuntimeException("Stock insuficiente en el lote seleccionado (Disponible: " + lote.getCantidadDisponible() + ")");
            }
            
            // Descontar stock
            lote.setCantidadDisponible(lote.getCantidadDisponible() - salida.getCantidad());
            loteRepository.save(lote);
            
            // Guardar salida
            salidaRepository.save(salida);
            return "redirect:/salidas?success";
        } catch (Exception e) {
            return "redirect:/salidas?error=" + e.getMessage();
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id) {
        try {
            salidaRepository.deleteById(id);
            return "redirect:/salidas?success";
        } catch (Exception e) {
            return "redirect:/salidas?error=No se puede eliminar porque está asociada a otros registros.";
        }
    }
}
