package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.MedicamentoEquivalencia;

import java.util.Optional;

public interface MedicamentoEquivalenciaRepository extends JpaRepository<MedicamentoEquivalencia, Long> {
    Optional<MedicamentoEquivalencia> findByNombreExternoAndConcentracionExterna(String nombreExterno, String concentracionExterna);
}
