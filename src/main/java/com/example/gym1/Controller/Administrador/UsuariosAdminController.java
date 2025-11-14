package com.example.gym1.Controller.Administrador;

import com.example.gym1.Poo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UsuariosAdminController {

    @PersistenceContext
    private EntityManager em;

    public String listarUsuarios(Model model) {

        List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u", Usuario.class
        ).getResultList();

        model.addAttribute("usuarios", usuarios);

        return "Administrador/usuarios"; // tu HTML
    }
}
