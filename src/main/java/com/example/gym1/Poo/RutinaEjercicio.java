package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "rutina_ejercicio")
public class RutinaEjercicio {

    @EmbeddedId
    private RutinaEjercicioId id;

    @MapsId("idRutina")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rutina")
    private Rutina rutina;

    @MapsId("idEjercicio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ejercicio")
    private Ejercicio ejercicio;

    // Si necesitas campos extra en la relación (orden, series, repeticiones), agrégalos aquí:
    // private Integer orden;
    // private Integer series;
    // private Integer repeticiones;
}