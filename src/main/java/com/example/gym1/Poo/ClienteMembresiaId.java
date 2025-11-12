package com.example.gym1.Poo;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class ClienteMembresiaId implements Serializable {
    private Integer idCliente;
    private Integer idMembresia;
}
