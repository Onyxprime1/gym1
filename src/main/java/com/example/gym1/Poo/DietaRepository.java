package com.example.gym1.Poo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DietaRepository extends JpaRepository<Dieta, Long> {

    // Usamos el nombre real del ID de Cliente: "id"
    List<Dieta> findByCliente_Id(Integer idCliente);

    long countByCliente_Id(Integer clienteId);

}
