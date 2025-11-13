package com.example.gym1.Poo;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "rutina_detalle")
public class RutinaDetalle {

    @EmbeddedId
    private RutinaDetalleId id;

    // Mapear relaciones opcionalmente para facilitar operaciones JPA
    @MapsId("idRutina")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rutina", insertable = false, updatable = false)
    private Rutina rutina;

    @MapsId("idEjercicio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ejercicio", insertable = false, updatable = false)
    private Ejercicio ejercicio;

    @Column(name = "series")
    private Integer series;

    @Column(name = "repeticiones")
    private Integer repeticiones;
}