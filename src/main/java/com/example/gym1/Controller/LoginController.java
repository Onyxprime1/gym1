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
    public String procesarLogin(@ModelAttribute Usuario usuario, Model model, HttpSession session) {
        Usuario u = em.createQuery("SELECT u FROM Usuario u WHERE u.correo = :correo", Usuario.class)
                .setParameter("correo", usuario.getCorreo())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (u != null && u.getContrasena().equals(usuario.getContrasena())) {
            // Guardamos datos en sesión
            session.setAttribute("uid", u.getId());
            session.setAttribute("unombre", u.getNombre());

            // Guardamos el ID del rol y el nombre, para usarlo después
            if (u.getRol() != null) {
                session.setAttribute("rolId", u.getRol().getId());
                session.setAttribute("rolNombre", u.getRol().getNombre());
            }

            // Redirigir según rol:
            String rolNombre = (u.getRol() != null && u.getRol().getNombre() != null)
                    ? u.getRol().getNombre().trim().toLowerCase()
                    : "";

            // Preferible: usar rolNombre o rolId según cómo gestiones roles en la BD
            if (rolNombre.contains("instructor") || Integer.valueOf( u.getRol()!=null ? u.getRol().getId() : 0 ).equals( /* opcional: id del rol instructor */ 2 )) {
                // Redirige al mapping /instructor que ya atiende InstructorInicioController
                return "redirect:/instructor";
            } else if (rolNombre.contains("admin") || rolNombre.contains("administrador") || (u.getRol()!=null && u.getRol().getId()!=null && u.getRol().getId() == 1)) {
                return "redirect:/admin";
            } else if (rolNombre.contains("nutriologo") || rolNombre.contains("nutriólogo")) {
                return "redirect:/nutriologo/panel";
            } else {
                return "redirect:/inicio";
            }
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

    // POST: procesar registro → asigna rol 4 = Cliente
    @Transactional
    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Usuario usuario) {

        // Buscar el rol CLIENTE (id_rol = 4)
        Rol rolCliente = em.find(Rol.class, 4);
        if (rolCliente == null) {
            throw new IllegalStateException("No existe el rol con id_rol = 4 (Cliente) en la tabla roles.");
        }

        usuario.setRol(rolCliente);

        em.persist(usuario);

        return "redirect:/login";
    }
}