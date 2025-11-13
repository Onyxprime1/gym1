package com.example.gym1.Poo;

import jakarta.persistence.*;

@Entity
@Table(name = "dietas")
public class Dieta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDieta;

    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "id_nutriologo")
    private Nutriologo nutriologo;

    // ============ GETTERS Y SETTERS ============

    public Long getIdDieta() {
        return idDieta;
    }

    public void setIdDieta(Long idDieta) {
        this.idDieta = idDieta;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Nutriologo getNutriologo() {
        return nutriologo;
    }

    public void setNutriologo(Nutriologo nutriologo) {
        this.nutriologo = nutriologo;
    }
}
