package com.example.gym1.Controller.Nutriologo;

import com.example.gym1.Poo.Platillo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/nutriologo/platillos")
public class PlatilloNutriologoController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    public String listarPlatillos(Model model) {
        // Traer todos los platillos de la BD
        List<Platillo> platillos = em.createQuery(
                "SELECT p FROM Platillo p", Platillo.class
        ).getResultList();

        // Mandarlos a la vista
        model.addAttribute("platillos", platillos);

        return "Nutriologo/platillos";   // nombre del HTML que estés usando
    }
}
