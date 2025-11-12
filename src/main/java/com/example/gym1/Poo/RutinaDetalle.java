package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "rutina_detalle")
public class RutinaDetalle {
    @EmbeddedId
    private RutinaDetalleId id;

    private Integer series;
    private Integer repeticiones;
}
