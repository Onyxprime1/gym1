package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "rutina_ejercicio")
public class RutinaEjercicio {
    @EmbeddedId
    private RutinaEjercicioId id;
}
