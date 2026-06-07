package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medicamento_equivalencias",
       uniqueConstraints = @UniqueConstraint(columnNames = {"nombre_externo", "concentracion_externa"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicamentoEquivalencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_externo", nullable = false)
    private String nombreExterno;

    @Column(name = "concentracion_externa")
    private String concentracionExterna;

    @ManyToOne
    @JoinColumn(name = "medicamento_id", nullable = false)
    @ToString.Exclude
    private Medicamento medicamento;
}
