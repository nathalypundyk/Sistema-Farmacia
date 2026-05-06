package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Medicamento;

import java.util.Optional;

// Spring Data JPA genera automaticamente: findAll, findById, save, deleteById, etc.
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
    Optional<Medicamento> findByCodigo(String codigo);
}
