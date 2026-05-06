package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
}
