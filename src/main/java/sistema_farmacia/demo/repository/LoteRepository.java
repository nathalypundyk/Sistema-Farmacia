package sistema_farmacia.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sistema_farmacia.demo.model.Lote;
import sistema_farmacia.demo.model.Medicamento;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByMedicamento(Medicamento medicamento);

    @Query(value = "SELECT l FROM Lote l ORDER BY l.medicamento.nombre ASC, l.fechaVencimiento ASC",
           countQuery = "SELECT COUNT(l) FROM Lote l")
    Page<Lote> findAllPaginado(Pageable pageable);

    @Query(value = "SELECT l FROM Lote l WHERE " +
                   "l.medicamento.minimo IS NOT NULL AND l.medicamento.minimo > 0 AND " +
                   "(SELECT COALESCE(SUM(l2.cantidadDisponible), 0) FROM Lote l2 WHERE l2.medicamento = l.medicamento) < l.medicamento.minimo " +
                   "ORDER BY l.medicamento.nombre ASC",
           countQuery = "SELECT COUNT(l) FROM Lote l WHERE " +
                        "l.medicamento.minimo IS NOT NULL AND l.medicamento.minimo > 0 AND " +
                        "(SELECT COALESCE(SUM(l2.cantidadDisponible), 0) FROM Lote l2 WHERE l2.medicamento = l.medicamento) < l.medicamento.minimo")
    Page<Lote> findPocoStock(Pageable pageable);
}
