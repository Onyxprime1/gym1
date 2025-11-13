package com.example.gym1.Controller.Administrador;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String panelAdmin(Model model, HttpSession session) {

        // Traer el nombre del usuario (si lo deseas mostrar arriba)
        String nombre = (String) session.getAttribute("unombre");
        model.addAttribute("nombre", nombre != null ? nombre : "Administrador");

        return "Administrador/admin"; // 👈 IMPORTANTÍSIMO
    }

    @GetMapping("/admin/usuarios")
    public String adminUsuarios() {
        return "Administrador/usuarios";
    }

    @GetMapping("/admin/clientes")
    public String adminClientes() {
        return "Administrador/clientes";
    }

    @GetMapping("/admin/instructores")
    public String adminInstructores() {
        return "Administrador/instructores";
    }

    @GetMapping("/admin/nutriologos")
    public String adminNutriologos() {
        return "Administrador/nutriologos";
    }

    @GetMapping("/admin/membresias")
    public String adminMembresias() {
        return "Administrador/membresias";
    }

    @GetMapping("/admin/reportes")
    public String adminReportes() {
        return "Administrador/reportes";
    }
}
