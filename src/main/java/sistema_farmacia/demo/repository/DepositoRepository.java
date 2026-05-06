package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Deposito;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {
}
