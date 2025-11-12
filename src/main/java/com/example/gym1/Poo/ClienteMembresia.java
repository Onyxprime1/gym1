package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "cliente_membresia")
public class ClienteMembresia {
    @EmbeddedId
    private ClienteMembresiaId id;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("idCliente")
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY) @MapsId("idMembresia")
    @JoinColumn(name = "id_membresia")
    private Membresia membresia;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;
}
