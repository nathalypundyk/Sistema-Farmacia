package sistema_farmacia.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sistema_farmacia.demo.model.*;
import sistema_farmacia.demo.repository.*;

import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private RolRepository rolRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        cargarRolesYUsuarios();
    }

    private void cargarRolesYUsuarios() {
        if (rolRepo.count() > 0) return;

        Rol adminRol = rolRepo.save(Rol.builder().nombre("ADMIN").descripcion("Administrador Total").build());
        Rol farmaRol = rolRepo.save(Rol.builder().nombre("FARMACEUTICO").descripcion("Gestion de Farmacia").build());

        // Admin con PIN hasheado (BCrypt). La administradora puede cambiarlo luego desde Usuarios.
        usuarioRepo.save(Usuario.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .nombre("Mario").apellido("Administrador")
            .pin(passwordEncoder.encode("1111")).activo(true)
            .roles(Set.of(adminRol)).build());

        // El farmacéutico no requiere PIN (no modifica stock mínimo/deseable).
        usuarioRepo.save(Usuario.builder()
            .username("farma")
            .password(passwordEncoder.encode("farma123"))
            .nombre("Lucia").apellido("Farmaceutica")
            .activo(true)
            .roles(Set.of(farmaRol)).build());
    }
}
