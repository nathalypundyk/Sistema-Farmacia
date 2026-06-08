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
    private LocalDate fechaVencimiento;
    private Integer cantidadDisponible;

    @ManyToOne
    @JoinColumn(name = "medicamento_id")
    @ToString.Exclude
    private Medicamento medicamento;

    public long getDiasHastaVencimiento() {
        if (fechaVencimiento == null) return Long.MAX_VALUE;
        return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), fechaVencimiento);
    }
}
