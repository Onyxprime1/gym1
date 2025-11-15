package com.example.gym1.Controller.Clientes;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NutricionClienteController {

    @GetMapping("/dietas")
    public String verDietas(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) {
            return "redirect:/login";
        }

        String nombre = (String) session.getAttribute("unombre");
        model.addAttribute("nombre", nombre != null ? nombre : "Atleta");

        // aquí luego puedes cargar la dieta desde la BD
        return "Clientes/nutricionCliente";
    }
}
