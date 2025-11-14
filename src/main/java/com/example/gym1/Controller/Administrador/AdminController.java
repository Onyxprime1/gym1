package com.example.gym1.Controller.Administrador;

import com.example.gym1.Poo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class AdminController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/admin")
    public String panelAdmin(Model model, HttpSession session) {
        String nombre = (String) session.getAttribute("unombre");
        model.addAttribute("nombre", nombre != null ? nombre : "Administrador");
        return "Administrador/admin";
    }

    // 🔹 AQUÍ ya cargamos los usuarios desde la BD
    @GetMapping("/admin/usuarios")
    public String adminUsuarios(Model model) {

        List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u", Usuario.class
        ).getResultList();

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("totalUsuarios", usuarios.size());

        // Por ahora dejamos estos en 0 (luego los llenamos bien con queries)
        model.addAttribute("totalClientes", 0);
        model.addAttribute("totalInstructores", 0);
        model.addAttribute("totalNutriologos", 0);

        return "Administrador/usuarios"; // tu HTML de lista
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
