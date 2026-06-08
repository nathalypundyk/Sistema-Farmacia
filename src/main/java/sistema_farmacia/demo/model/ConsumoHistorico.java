package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "consumo_historico")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    private LocalDate fecha;

    // stockAnterior - stockNuevo del import. Positivo = consumo real. Negativo = reposición.
    private Integer consumo;

    private Integer stockAnterior;
    private Integer stockNuevo;
}
