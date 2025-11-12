package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "clientes")
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    private Integer edad;

    @Column(precision = 5, scale = 2)
    private BigDecimal peso;

    @Column(precision = 5, scale = 2)
    private BigDecimal altura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;
}
