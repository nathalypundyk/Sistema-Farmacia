package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_ajuste")
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaAjuste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, updatable = false)
    private String usuario;

    @Column(nullable = false, updatable = false)
    private String accion;

    @Column(nullable = false, updatable = false)
    private String medicamentoNombre;

    @Column(updatable = false)
    private Integer cantidadAnterior;

    @Column(updatable = false)
    private Integer cantidadNueva;

    @Column(updatable = false)
    private String authPin;

    @Column(updatable = false)
    private Boolean pinVerificado;

    @PrePersist
    protected void onCreate() {
        fecha = LocalDateTime.now();
    }
}
