package com.example.gym1.Poo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class RutinaEjercicioId implements Serializable {

    @Column(name = "id_rutina")
    private Integer idRutina;

    @Column(name = "id_ejercicio")
    private Integer idEjercicio;
}