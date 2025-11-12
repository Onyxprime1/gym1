package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "instructores")
public class Instructor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_instructor")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String especialidad;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;
}
