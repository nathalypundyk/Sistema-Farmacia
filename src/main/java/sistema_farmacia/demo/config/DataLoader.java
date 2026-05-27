package sistema_farmacia.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sistema_farmacia.demo.model.*;
import sistema_farmacia.demo.repository.*;

import java.time.LocalDate;
import java.util.Set;

// @Component
public class DataLoader implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private RolRepository rolRepo;
    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private TipoMedicamentoRepository tipoRepo;
    @Autowired private PresentacionRepository presentacionRepo;
    @Autowired private DepositoRepository depositoRepo;
    @Autowired private LoteRepository loteRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 0. Limpiar datos viejos para asegurar consistencia
        System.out.println("DEBUG: Limpiando base de datos para recarga...");
        usuarioRepo.deleteAll();
        rolRepo.deleteAll();

        // 1. Roles
        Rol adminRol = rolRepo.save(Rol.builder().nombre("ADMIN").build());
        Rol farmaRol = rolRepo.save(Rol.builder().nombre("FARMACEUTICO").build());

        // 2. Usuarios
        System.out.println("DEBUG: Creando usuarios de prueba...");
        Usuario admin = Usuario.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .nombre("Mario")
                .apellido("Admin")
                .pin("1234")
                .roles(Set.of(adminRol))
                .activo(true)
                .build();
        usuarioRepo.save(admin);
        System.out.println("DEBUG: Usuario 'admin' creado con éxito (admin123).");

        Usuario farma = Usuario.builder()
                .username("farmacia")
                .password(passwordEncoder.encode("farma123"))
                .nombre("Lucía")
                .apellido("Farmacéutica")
                .pin("5678")
                .roles(Set.of(farmaRol))
                .activo(true)
                .build();
        usuarioRepo.save(farma);
        System.out.println("DEBUG: Usuario 'farmacia' creado con éxito.");

        // 3. Tipos y Presentaciones
        TipoMedicamento antibiotico = tipoRepo.findAll().stream()
                .filter(t -> t.getNombre().equalsIgnoreCase("Antibiótico"))
                .findFirst()
                .orElseGet(() -> tipoRepo.save(TipoMedicamento.builder().nombre("Antibiótico").build()));

        TipoMedicamento analgesico = tipoRepo.findAll().stream()
                .filter(t -> t.getNombre().equalsIgnoreCase("Analgésico"))
                .findFirst()
                .orElseGet(() -> tipoRepo.save(TipoMedicamento.builder().nombre("Analgésico").build()));

        Presentacion comprimidos = presentacionRepo.findAll().stream()
                .filter(p -> p.getNombre().equalsIgnoreCase("Comprimidos"))
                .findFirst()
                .orElseGet(() -> presentacionRepo.save(Presentacion.builder().nombre("Comprimidos").build()));

        // 4. Depósito
        Deposito central = depositoRepo.findAll().stream()
                .filter(d -> d.getNombre().equalsIgnoreCase("Depósito Central"))
                .findFirst()
                .orElseGet(() -> {
                    Deposito d = new Deposito();
                    d.setNombre("Depósito Central");
                    d.setUbicacion("Piso 1");
                    return depositoRepo.save(d);
                });

        // 5. Medicamentos con Alertas LASA y Stock
        if (medicamentoRepo.findByCodigo("IBU-600").isEmpty()) {
            Medicamento ibu = Medicamento.builder()
                    .codigo("IBU-600")
                    .nombre("Ibuprofeno 600mg")
                    .tipo(analgesico)
                    .presentacion(comprimidos)
                    .minimo(50)
                    .esLasa(true)
                    .similarA("Ibuprofeno 400mg")
                    .build();
            medicamentoRepo.save(ibu);
            
            // 6. Lotes para IBU
            loteRepo.save(Lote.builder()
                    .nroLote("L-1001")
                    .nroSerie("SN-0001-A")
                    .cantidadDisponible(10)
                    .fechaVencimiento(LocalDate.now().plusDays(15))
                    .medicamento(ibu)
                    .deposito(central)
                    .build());
        }

        if (medicamentoRepo.findByCodigo("AMO-500").isEmpty()) {
            Medicamento amo = Medicamento.builder()
                    .codigo("AMO-500")
                    .nombre("Amoxicilina 500mg")
                    .tipo(antibiotico)
                    .presentacion(comprimidos)
                    .minimo(100)
                    .esPrioridad(true)
                    .build();
            medicamentoRepo.save(amo);

            loteRepo.save(Lote.builder()
                    .nroLote("L-2002")
                    .nroSerie("SN-9999-B")
                    .cantidadDisponible(150)
                    .fechaVencimiento(LocalDate.now().plusMonths(6))
                    .medicamento(amo)
                    .deposito(central)
                    .build());
        }

        System.out.println("=== Datos de prueba cargados con éxito ===");
    }
}
