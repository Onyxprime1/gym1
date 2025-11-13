// LoginController.java
package com.example.gym1.Controller;

import com.example.gym1.Poo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@ModelAttribute Usuario usuario, Model model, HttpSession session) {
        Usuario u = em.createQuery("SELECT u FROM Usuario u WHERE u.correo = :correo", Usuario.class)
                .setParameter("correo", usuario.getCorreo())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (u != null && u.getContrasena().equals(usuario.getContrasena())) {
            session.setAttribute("uid", u.getId());      // <-- guardamos id_usuario
            session.setAttribute("unombre", u.getNombre()); // opcional
            return "redirect:/inicio"; // <-- panel del cliente
        }

        model.addAttribute("error", "Correo o contraseña incorrectos");
        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @Transactional
    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Usuario usuario) {
        em.persist(usuario);
        return "redirect:/inicio"; // después de crear, ir a login
    }
}
