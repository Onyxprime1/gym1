package com.example.gym1.Poo;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "platillos")
public class Platillo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPlatillo;

    private String nombre;

    // columnas reales de tu BD
    private String ingredientes;

    @Column(columnDefinition = "TEXT")
    private String preparacion;

    // Relación inversa con Dieta (opcional pero útil)
    @ManyToMany(mappedBy = "platillos")
    private Set<Dieta> dietas;

    // ===== GETTERS y SETTERS =====

    public Integer getIdPlatillo() {
        return idPlatillo;
    }

    public void setIdPlatillo(Integer idPlatillo) {
        this.idPlatillo = idPlatillo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(String ingredientes) {
        this.ingredientes = ingredientes;
    }

    public String getPreparacion() {
        return preparacion;
    }

    public void setPreparacion(String preparacion) {
        this.preparacion = preparacion;
    }

    public Set<Dieta> getDietas() {
        return dietas;
    }

    public void setDietas(Set<Dieta> dietas) {
        this.dietas = dietas;
    }
}
