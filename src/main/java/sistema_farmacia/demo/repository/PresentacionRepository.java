package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Presentacion;

public interface PresentacionRepository extends JpaRepository<Presentacion, Long> {
}
