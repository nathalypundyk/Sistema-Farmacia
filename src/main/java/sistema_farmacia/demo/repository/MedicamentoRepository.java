package sistema_farmacia.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sistema_farmacia.demo.model.Medicamento;

import java.util.Optional;

public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {

    Optional<Medicamento> findByCodigo(String codigo);

    @Query("SELECT m FROM Medicamento m WHERE " +
           "(:buscar = '' OR LOWER(m.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')) OR " +
           "LOWER(COALESCE(m.concentracion, '')) LIKE LOWER(CONCAT('%', :buscar, '%'))) " +
           "AND (:soloPrioridad = false OR m.esPrioridad = true) " +
           "ORDER BY m.nombre ASC")
    Page<Medicamento> buscarPaginado(
            @Param("buscar") String buscar,
            @Param("soloPrioridad") boolean soloPrioridad,
            Pageable pageable);
}
