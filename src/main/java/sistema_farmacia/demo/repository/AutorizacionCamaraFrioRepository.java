package sistema_farmacia.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sistema_farmacia.demo.model.AutorizacionCamaraFrio;
import sistema_farmacia.demo.model.Usuario;

import java.util.Optional;

public interface AutorizacionCamaraFrioRepository extends JpaRepository<AutorizacionCamaraFrio, Long> {
    Optional<AutorizacionCamaraFrio> findByUsuario(Usuario usuario);
    Optional<AutorizacionCamaraFrio> findByUsuarioId(Long usuarioId);
}
