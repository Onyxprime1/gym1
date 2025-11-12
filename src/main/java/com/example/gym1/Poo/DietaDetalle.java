package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "dieta_detalle")
public class DietaDetalle {
    @EmbeddedId
    private DietaDetalleId id;

    @Column(name = "semana")
    private Integer semana;
}
