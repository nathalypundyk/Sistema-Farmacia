package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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

    @Column(unique = true)
    private String codigoSiciap;

    private String nombre;

    private String concentracion;

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

    @Column(columnDefinition = "TEXT")
    private String formaFarmaceutica;

    @Column(columnDefinition = "TEXT")
    private String presentacion;

    @OneToMany(mappedBy = "medicamento", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<Lote> lotes = new ArrayList<>();

    public int getStockTotal() {
        if (lotes == null) return 0;
        return lotes.stream().mapToInt(Lote::getCantidadDisponible).sum();
    }

    public boolean isStockBajo() {
        if (getStockTotal() == 0) return true;
        Integer min = getMinimoAjustado();
        return min != null && min > 0 && getStockTotal() < min;
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
        LocalDate limite = LocalDate.now().plusDays(30);
        return lotes.stream().anyMatch(lote ->
            lote.getFechaVencimiento() != null &&
            lote.getFechaVencimiento().isBefore(limite)
        );
    }

    public java.util.List<Lote> getLotesProximosAVencer() {
        if (lotes == null) return new ArrayList<>();
        LocalDate limite = LocalDate.now().plusDays(30);
        return lotes.stream()
            .filter(l -> l.getFechaVencimiento() != null &&
                l.getFechaVencimiento().isBefore(limite))
            .sorted(java.util.Comparator.comparing(Lote::getFechaVencimiento))
            .collect(java.util.stream.Collectors.toList());
    }
}
