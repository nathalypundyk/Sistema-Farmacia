package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sistema_farmacia.demo.model.Alerta;
import sistema_farmacia.demo.model.Medicamento;

import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findAllByMedicamento(Medicamento medicamento);
    List<Alerta> findByTipo(String tipo);
}
