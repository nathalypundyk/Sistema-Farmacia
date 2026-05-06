package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Rol;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(String nombre);
}
