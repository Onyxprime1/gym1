package com.example.gym1.Poo;

import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;

@Data @NoArgsConstructor @AllArgsConstructor
@Embeddable
public class DietaPlatilloId implements Serializable {
    private Integer idDieta;
    private Integer idPlatillo;
}
