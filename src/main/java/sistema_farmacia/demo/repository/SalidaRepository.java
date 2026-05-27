package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Salida;

public interface SalidaRepository extends JpaRepository<Salida, Long> {
}
