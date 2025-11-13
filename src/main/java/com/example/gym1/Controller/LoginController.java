package com.example.gym1.Controller;

import com.example.gym1.Poo.Usuario;
import com.example.gym1.Poo.Rol;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @PersistenceContext
    private EntityManager em;

    // GET: mostrar formulario de login
    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "login";
    }

    // POST: procesar login
    @PostMapping("/login")
    public String procesarLogin(@ModelAttribute Usuario usuario,
                                Model model,
                                HttpSession session) {
        Usuario u = em.createQuery(
                        "SELECT u FROM Usuario u WHERE u.correo = :correo",
                        Usuario.class)
                .setParameter("correo", usuario.getCorreo())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (u != null && u.getContrasena().equals(usuario.getContrasena())) {
            // Guardas datos en sesión
            session.setAttribute("uid", u.getId());
            session.setAttribute("unombre", u.getNombre());
            session.setAttribute("rol", u.getRol() != null ? u.getRol().getNombre() : "Desconocido");

            // Ir al panel /inicio
            return "redirect:/inicio";
        }

        model.addAttribute("error", "Correo o contraseña incorrectos");
        return "login";
    }

    // GET: mostrar formulario de registro
    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    // POST: procesar registro  👉 AQUÍ se asigna rol 4 = Cliente
    @Transactional
    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Usuario usuario) {

        // Buscar el rol CLIENTE (id_rol = 4)
        Rol rolCliente = em.find(Rol.class, 4);
        if (rolCliente == null) {
            // Si por alguna razón no existe, mejor lanzar error claro
            throw new IllegalStateException("No existe el rol con id_rol = 4 (Cliente) en la tabla roles.");
        }

        // Asignar rol al usuario
        usuario.setRol(rolCliente);

        // Guardar en BD
        em.persist(usuario);

        // Después de registrarse, lo mandas al login
        return "redirect:/login";
    }
}
