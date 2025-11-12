package com.example.gym1.Poo;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class DietaDetalleId implements Serializable {
    private Integer idDieta;
    private Integer idPlatillo;
    private String  diaSemana;   // lunes, martes, etc.
    private LocalTime hora;      // TIME
}
