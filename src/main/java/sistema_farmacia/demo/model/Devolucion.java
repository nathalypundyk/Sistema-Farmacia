package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "devoluciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private String motivo;

    @Column(nullable = false)
    private String usuario;

    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        fecha = LocalDateTime.now();
    }
}
