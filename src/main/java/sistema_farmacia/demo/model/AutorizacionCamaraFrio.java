package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Permiso individual de ingreso a la cámara de frío y foto de referencia del rostro.
 * Se guarda en una tabla aparte (y no como columnas de "usuarios") para no tocar
 * el formulario de usuarios existente ni cargar la foto cada vez que se autentica alguien.
 */
@Entity
@Table(name = "camara_frio_autorizaciones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutorizacionCamaraFrio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", unique = true, nullable = false)
    private Usuario usuario;

    @Builder.Default
    private Boolean autorizado = false;

    /** Foto frontal del rostro (JPEG/PNG). En PostgreSQL se guarda como bytea. */
    @ToString.Exclude
    @Column(name = "foto_rostro")
    private byte[] fotoRostro;

    private String fotoContentType;

    private LocalDateTime fechaActualizacion;

    /** Username del administrador que hizo el último cambio. */
    private String actualizadoPor;

    public boolean tieneFoto() {
        return fotoRostro != null && fotoRostro.length > 0;
    }
}
