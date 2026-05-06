package sistema_farmacia.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "presentacion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Presentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private Integer unidades;
}
