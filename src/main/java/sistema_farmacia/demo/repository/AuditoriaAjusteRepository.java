package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sistema_farmacia.demo.model.AuditoriaAjuste;

import java.util.List;

@Repository
public interface AuditoriaAjusteRepository extends JpaRepository<AuditoriaAjuste, Long> {
    List<AuditoriaAjuste> findAllByOrderByFechaDesc();
}
