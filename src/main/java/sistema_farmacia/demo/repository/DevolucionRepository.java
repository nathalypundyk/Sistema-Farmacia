package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Devolucion;
import java.util.List;

public interface DevolucionRepository extends JpaRepository<Devolucion, Long> {
    List<Devolucion> findAllByOrderByFechaDesc();
}
