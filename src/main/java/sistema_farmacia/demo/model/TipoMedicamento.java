package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_medicamento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoMedicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
}
