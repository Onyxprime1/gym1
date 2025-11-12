package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "platillos")
public class Platillo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_platillo")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(columnDefinition = "text")
    private String ingredientes;

    @Column(columnDefinition = "text")
    private String preparacion;
}
