package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.ConsumoHistorico;
import sistema_farmacia.demo.model.Medicamento;
import java.util.List;

public interface ConsumoHistoricoRepository extends JpaRepository<ConsumoHistorico, Long> {
    List<ConsumoHistorico> findByMedicamentoOrderByFechaAsc(Medicamento medicamento);
}
