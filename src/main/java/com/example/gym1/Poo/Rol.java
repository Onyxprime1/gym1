package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "roles")
public class Rol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer id;

    @Column(nullable = false, length = 50)
    private String nombre;
}
