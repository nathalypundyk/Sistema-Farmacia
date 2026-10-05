package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.AccesoCamaraFrio;

import java.time.LocalDateTime;
import java.util.List;

public interface AccesoCamaraFrioRepository extends JpaRepository<AccesoCamaraFrio, Long> {
    List<AccesoCamaraFrio> findTop50ByOrderByFechaHoraDesc();
    long countByFechaHoraAfterAndResultadoNot(LocalDateTime desde, String resultado);
}
