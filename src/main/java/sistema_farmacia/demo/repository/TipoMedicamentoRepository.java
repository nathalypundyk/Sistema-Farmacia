package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.TipoMedicamento;

public interface TipoMedicamentoRepository extends JpaRepository<TipoMedicamento, Long> {
}
