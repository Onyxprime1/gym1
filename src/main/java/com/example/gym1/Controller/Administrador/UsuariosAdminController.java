package com.example.gym1.Controller.Administrador;

import com.example.gym1.Poo.Rol;
import com.example.gym1.Poo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/usuarios")
public class UsuariosAdminController {

    @PersistenceContext
    private EntityManager em;

    // LISTAR
    @GetMapping
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u", Usuario.class
        ).getResultList();

        List<Rol> roles = em.createQuery(
                "SELECT r FROM Rol r", Rol.class
        ).getResultList();

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", roles);
        return "Administrador/usuarios";
    }

    // EDITAR (cargar formulario)
    @GetMapping("/{id}/editar")
    public String editarUsuario(@PathVariable Integer id, Model model) {
        Usuario usuario = em.find(Usuario.class, id);
        if (usuario == null) {
            // si no existe, regreso a la lista
            return "redirect:/admin/usuarios";
        }

        List<Rol> roles = em.createQuery(
                "SELECT r FROM Rol r", Rol.class
        ).getResultList();

        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", roles);
        return "Administrador/usuario-form";  // nuevo html
    }

    // GUARDAR CAMBIOS
    @PostMapping("/{id}/guardar")
    @Transactional
    public String guardarUsuario(
            @PathVariable Integer id,
            @RequestParam String nombre,
            @RequestParam String correo,
            @RequestParam Integer idRol
    ) {
        Usuario usuario = em.find(Usuario.class, id);
        if (usuario == null) {
            return "redirect:/admin/usuarios";
        }

        usuario.setNombre(nombre);
        usuario.setCorreo(correo);

        Rol rol = em.find(Rol.class, idRol);
        usuario.setRol(rol);

        em.merge(usuario);
        return "redirect:/admin/usuarios";
    }

    // ELIMINAR
    @PostMapping("/{id}/eliminar")
    @Transactional
    public String eliminarUsuario(@PathVariable Integer id) {
        Usuario usuario = em.find(Usuario.class, id);
        if (usuario != null) {
            em.remove(usuario);
        }
        return "redirect:/admin/usuarios";
    }
}
