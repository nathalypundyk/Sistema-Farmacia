package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "lotes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nroLote;
    private String nroSerie; // Para trazabilidad nacional (ANMAT/COFEPRIS)
    private LocalDate fechaVencimiento;
    private Integer cantidadDisponible;
    private String estado;

    @ManyToOne
    @JoinColumn(name = "medicamento_id")
    @ToString.Exclude
    private Medicamento medicamento;

    @ManyToOne
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;
}
