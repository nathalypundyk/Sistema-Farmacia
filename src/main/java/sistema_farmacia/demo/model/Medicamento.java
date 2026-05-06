package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medicamento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String codigo;

    private String nombre;

    @Builder.Default
    private Boolean esPrioridad = false;

    // LASA (Look-Alike/Sound-Alike) Alerts
    @Builder.Default
    private Boolean esLasa = false;
    private String similarA;

    @Builder.Default
    private Boolean esEstacional = false;

    // Stock levels as per business rules
    private Integer minimo;
    private Integer deseable;
    private Integer recomendado;
    
    // Blister integrity rule
    private Integer unidadesPorBlister;

    @ManyToOne
    @JoinColumn(name = "tipo_id")
    private TipoMedicamento tipo;

    @ManyToOne
    @JoinColumn(name = "presentacion_id")
    private Presentacion presentacion;

    @OneToMany(mappedBy = "medicamento", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Lote> lotes = new ArrayList<>();

    public int getStockTotal() {
        if (lotes == null) return 0;
        return lotes.stream().mapToInt(Lote::getCantidadDisponible).sum();
    }

    public boolean isStockBajo() {
        return getMinimoAjustado() != null && getStockTotal() < getMinimoAjustado();
    }

    public Integer getMinimoAjustado() {
        if (minimo == null) return 0;
        if (esEstacional == null || !esEstacional) return minimo;
        
        int mes = LocalDate.now().getMonthValue();
        // Ejemplo: Invierno (Junio, Julio, Agosto) incrementa el mínimo en 50%
        if (mes >= 6 && mes <= 8) {
            return (int) (minimo * 1.5);
        }
        return minimo;
    }

    public boolean isProximoAVencer() {
        if (lotes == null) return false;
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(30);
        return lotes.stream().anyMatch(lote ->
            lote.getFechaVencimiento() != null &&
            lote.getFechaVencimiento().isAfter(hoy) &&
            lote.getFechaVencimiento().isBefore(limite)
        );
    }
}
