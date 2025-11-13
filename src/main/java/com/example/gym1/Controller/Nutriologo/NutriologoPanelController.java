package com.example.gym1.Controller.Nutriologo;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NutriologoPanelController {

    @GetMapping("/nutriologo/panel")
    public String panelNutriologo(Model model, HttpSession session) {

        String nombre = (String) session.getAttribute("unombre");
        model.addAttribute("nombreNutriologo", nombre != null ? nombre : "Nutriólogo");

        // templates/Nutriologo/NutriologoInicio.html
        return "Nutriologo/NutriologoInicio";
    }
}
