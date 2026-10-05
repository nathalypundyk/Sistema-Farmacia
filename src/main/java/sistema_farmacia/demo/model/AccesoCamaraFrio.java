package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Bitácora de accesos a la cámara de frío detectados por reconocimiento facial.
 * Es un registro de solo inserción: no se expone ningún endpoint para editarlo ni borrarlo.
 */
@Entity
@Table(name = "camara_frio_accesos", indexes = @Index(name = "idx_acceso_fecha", columnList = "fechaHora"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccesoCamaraFrio {

    public static final String AUTORIZADO = "AUTORIZADO";
    public static final String DENEGADO_SIN_PERMISO = "DENEGADO_SIN_PERMISO";
    public static final String DENEGADO_DESCONOCIDO = "DENEGADO_DESCONOCIDO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    /** Usuario reconocido. Es null cuando el rostro no coincide con ningún usuario registrado. */
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    /** Nombre mostrado por la cámara (nombre del usuario o "Desconocido"). */
    private String nombreDetectado;

    @Column(nullable = false, length = 30)
    private String resultado;

    private Boolean puertaAbierta;

    /** Distancia facial devuelta por face_recognition (menor = más parecido). Sirve para auditar. */
    private Double distancia;

    @PrePersist
    protected void onCreate() {
        if (fechaHora == null) fechaHora = LocalDateTime.now();
    }
}
