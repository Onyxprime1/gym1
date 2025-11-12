package com.example.gym1.Poo;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class RutinaDetalleId implements Serializable {
    private Integer idRutina;
    private Integer idEjercicio;
    private String  diaSemana;   // lunes, martes, etc.
}
