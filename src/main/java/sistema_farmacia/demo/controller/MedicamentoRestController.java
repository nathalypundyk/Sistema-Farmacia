package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.MedicamentoRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/medicamentos")
public class MedicamentoRestController {

    @Autowired
    private MedicamentoRepository medicamentoRepo;

    @GetMapping
    public List<MedicamentoDTO> getAll() {
        return medicamentoRepo.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{codigo}")
    public MedicamentoDTO getByCodigo(@PathVariable String codigo) {
        return medicamentoRepo.findByCodigo(codigo)
                .map(this::convertToDTO)
                .orElse(null);
    }

    private MedicamentoDTO convertToDTO(Medicamento med) {
        return MedicamentoDTO.builder()
                .id(med.getId())
                .codigo(med.getCodigo())
                .nombre(med.getNombre())
                .stockTotal(med.getStockTotal())
                .esPrioridad(med.getEsPrioridad())
                .esStockBajo(med.isStockBajo())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class MedicamentoDTO {
        private Long id;
        private String codigo;
        private String nombre;
        private Integer stockTotal;
        private Boolean esPrioridad;
        private Boolean esStockBajo;
    }
}
