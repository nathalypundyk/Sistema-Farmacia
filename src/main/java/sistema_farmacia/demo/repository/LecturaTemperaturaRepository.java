package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.LecturaTemperatura;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LecturaTemperaturaRepository extends JpaRepository<LecturaTemperatura, Long> {
    Optional<LecturaTemperatura> findFirstByOrderByFechaHoraDesc();
    List<LecturaTemperatura> findByFechaHoraAfterOrderByFechaHoraAsc(LocalDateTime desde);
    long countByFechaHoraAfterAndFueraDeRangoTrue(LocalDateTime desde);
}
