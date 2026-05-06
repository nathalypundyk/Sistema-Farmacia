package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Lote;
import sistema_farmacia.demo.model.Medicamento;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    // Spring Data JPA genera la consulta SQL automaticamente por el nombre del metodo
    List<Lote> findByMedicamento(Medicamento medicamento);
}
