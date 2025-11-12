package com.example.gym1.Controller;

import com.example.gym1.Poo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @PersistenceContext
    private EntityManager em;

    // Mostrar login
    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "login";
    }

    // Procesar login
    @PostMapping("/login")
    public String procesarLogin(@ModelAttribute Usuario usuario, Model model) {
        Usuario u = em.createQuery("SELECT u FROM Usuario u WHERE u.correo = :correo", Usuario.class)
                .setParameter("correo", usuario.getCorreo())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (u != null && u.getContrasena().equals(usuario.getContrasena())) {
            model.addAttribute("nombre", u.getNombre());
            return "index"; // ⬅️ ahora carga tu vista principal
        }

        model.addAttribute("error", "Correo o contraseña incorrectos");
        return "login";
    }

    // Mostrar registro
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    // Procesar registro
    @Transactional
    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Usuario usuario, Model model) {
        // Si deseas asignarle un rol por defecto (por ejemplo, "cliente")
        usuario.setRol(em.find(com.example.gym1.Poo.Rol.class, 1)); // rol con ID=1
        em.persist(usuario);

        model.addAttribute("nombre", usuario.getNombre());
        return "index"; // ⬅️ redirige al dashboard
    }

    // Redirigir raíz a /login
    @GetMapping("/")
    public String raiz() {
        return "redirect:/login";
    }
}
