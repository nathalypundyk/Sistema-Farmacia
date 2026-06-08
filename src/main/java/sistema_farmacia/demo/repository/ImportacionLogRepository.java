package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.ImportacionLog;
import java.util.List;
import java.util.Optional;

public interface ImportacionLogRepository extends JpaRepository<ImportacionLog, Long> {
    List<ImportacionLog> findAllByOrderByFechaDesc();
    Optional<ImportacionLog> findFirstByOrderByFechaDesc();
    Optional<ImportacionLog> findFirstByTipoOrderByFechaDesc(String tipo);
}
