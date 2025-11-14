package com.example.gym1.Controller.Administrador;

import com.example.gym1.Poo.Usuario;
import com.example.gym1.Poo.Rol;
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

    // ============================
    // LISTAR USUARIOS
    // ============================
    @GetMapping
    public String listarUsuarios(Model model) {

        List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u", Usuario.class
        ).getResultList();

        model.addAttribute("usuarios", usuarios);

        return "Administrador/usuarios";
    }


    // ============================
    // VER DETALLE DE USUARIO
    // ============================
    @GetMapping("/{id}")
    public String verUsuario(@PathVariable Integer id, Model model) {

        Usuario u = em.find(Usuario.class, id);
        if (u == null) {
            return "redirect:/admin/usuarios";
        }

        model.addAttribute("usuario", u);

        return "Administrador/usuario_detalle";
    }


    // ============================
    // FORMULARIO CREAR USUARIO
    // ============================
    @GetMapping("/nuevo")
    public String nuevoUsuario(Model model) {
        model.addAttribute("usuario", new Usuario());

        List<Rol> roles = em.createQuery("SELECT r FROM Rol r", Rol.class).getResultList();
        model.addAttribute("roles", roles);

        return "Administrador/usuario_form";
    }


    // ============================
    // GUARDAR USUARIO NUEVO
    // ============================
    @Transactional
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario) {

        em.persist(usuario);
        return "redirect:/admin/usuarios";
    }


    // ============================
    // EDITAR USUARIO
    // ============================
    @GetMapping("/editar/{id}")
    public String editarUsuario(@PathVariable Integer id, Model model) {

        Usuario u = em.find(Usuario.class, id);
        if (u == null) {
            return "redirect:/admin/usuarios";
        }

        model.addAttribute("usuario", u);

        List<Rol> roles = em.createQuery("SELECT r FROM Rol r", Rol.class).getResultList();
        model.addAttribute("roles", roles);

        return "Administrador/usuario_form";
    }


    // ============================
    // ACTUALIZAR USUARIO
    // ============================
    @Transactional
    @PostMapping("/actualizar/{id}")
    public String actualizarUsuario(@PathVariable Integer id,
                                    @ModelAttribute Usuario usuarioForm) {

        Usuario u = em.find(Usuario.class, id);
        if (u == null) {
            return "redirect:/admin/usuarios";
        }

        u.setNombre(usuarioForm.getNombre());
        u.setCorreo(usuarioForm.getCorreo());
        u.setContrasena(usuarioForm.getContrasena());
        u.setRol(usuarioForm.getRol());

        em.merge(u);
        return "redirect:/admin/usuarios";
    }


    // ============================
    // ELIMINAR USUARIO
    // ============================
    @Transactional
    @GetMapping("/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Integer id) {

        Usuario u = em.find(Usuario.class, id);
        if (u != null) {
            em.remove(u);
        }

        return "redirect:/admin/usuarios";
    }
}
