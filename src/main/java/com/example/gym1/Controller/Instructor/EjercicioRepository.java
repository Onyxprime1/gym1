package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Ejercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EjercicioRepository extends JpaRepository<Ejercicio, Integer> {
    // Búsqueda útil por nombre o descripción directamente en la BD
    List<Ejercicio> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(String nombre, String descripcion);
}