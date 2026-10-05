package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lectura enviada por el Arduino de la cámara de frío (a través del script puente en Python).
 * Formato serial original del docente: TEMP:xx.xx,PUERTA:ESTADO,TIEMPO:HH:MM:SS,EST:ESTADO
 */
@Entity
@Table(name = "camara_frio_lecturas", indexes = @Index(name = "idx_lectura_fecha", columnList = "fechaHora"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaTemperatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    @Column(nullable = false)
    private Double temperatura;

    private Boolean puertaAbierta;

    /** Tiempo que la puerta lleva abierta, tal como lo cuenta el Arduino (HH:MM:SS). */
    private String tiempoPuerta;

    /** Estado informado por el Arduino: ok, alert temp, tiempo max-exed, ALERTA INTRUSO! */
    private String estadoDispositivo;

    /** Calculado por el servidor según los umbrales configurados (camarafrio.temp.min / max). */
    private Boolean fueraDeRango;

    @PrePersist
    protected void onCreate() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }
}
