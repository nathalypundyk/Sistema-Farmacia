package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.LecturaTemperatura;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LecturaTemperaturaRepository extends JpaRepository<LecturaTemperatura, Long> {
    /** Última lectura recibida (por id, así no depende de la hora del servidor). */
    Optional<LecturaTemperatura> findFirstByOrderByIdDesc();
    List<LecturaTemperatura> findByFechaHoraBetweenOrderByFechaHoraAsc(LocalDateTime desde, LocalDateTime hasta);
    long countByFechaHoraAfterAndFueraDeRangoTrue(LocalDateTime desde);
}
