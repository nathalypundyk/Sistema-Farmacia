package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "importacion_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportacionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fecha;
    private String usuario;
    private String tipo;               // STOCK | IMI
    private Integer totalItems;
    private Integer aplicados;
    private Integer equivalenciasGuardadas;
    private Integer medicamentosCreados;   // solo para tipo IMI

    @PrePersist
    void stamp() { if (fecha == null) fecha = LocalDateTime.now(); }
}
