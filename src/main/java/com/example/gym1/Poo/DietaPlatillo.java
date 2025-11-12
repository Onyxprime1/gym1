package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "dieta_platillo")
public class DietaPlatillo {
    @EmbeddedId
    private DietaPlatilloId id;
}
