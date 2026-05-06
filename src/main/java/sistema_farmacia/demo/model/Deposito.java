package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deposito")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Deposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String ubicacion;
}
