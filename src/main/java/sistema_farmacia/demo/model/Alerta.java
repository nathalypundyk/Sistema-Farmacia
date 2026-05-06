package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo; // STOCK_BAJO, PROXIMO_VENCIMIENTO
    private String mensaje;
    private LocalDateTime fechaGeneracion;
    @Builder.Default
    private Boolean leida = false;

    @ManyToOne
    @JoinColumn(name = "medicamento_id")
    private Medicamento medicamento;

    @PrePersist
    protected void onCreate() {
        fechaGeneracion = LocalDateTime.now();
    }
}
