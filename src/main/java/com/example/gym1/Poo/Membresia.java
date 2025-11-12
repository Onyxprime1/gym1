package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "membresias")
public class Membresia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_membresia")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String tipo;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal precio;
}
