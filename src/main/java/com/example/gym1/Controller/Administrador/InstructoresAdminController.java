package com.example.gym1.Controller.Administrador;

import com.example.gym1.Poo.Instructor;
import com.example.gym1.Poo.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/instructores")
public class InstructoresAdminController {

    @PersistenceContext
    private EntityManager em;

    // LISTAR INSTRUCTORES
    @GetMapping
    public String listarInstructores(Model model) {

        List<Instructor> instructores = em.createQuery(
                "SELECT i FROM Instructor i", Instructor.class
        ).getResultList();

        model.addAttribute("instructores", instructores);
        return "Administrador/instructores";   // tu HTML de lista
    }

    // MOSTRAR FORMULARIO NUEVO INSTRUCTOR
    @GetMapping("/nuevo")
    public String nuevoInstructor(Model model) {

        // Instructor vacío para el form
        model.addAttribute("instructor", new Instructor());

        // Opcional: lista de usuarios para vincular (si quieres elegir usuario desde combo)
        List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u", Usuario.class
        ).getResultList();
        model.addAttribute("usuarios", usuarios);

        return "Administrador/instructor-form"; // luego te puedo dar este HTML
    }

    // MOSTRAR FORMULARIO EDITAR INSTRUCTOR
    @GetMapping("/{id}/editar")
    public String editarInstructor(@PathVariable Integer id, Model model) {

        Instructor instructor = em.find(Instructor.class, id);
        if (instructor == null) {
            // Si no existe, regreso a la lista
            return "redirect:/admin/instructores";
        }

        model.addAttribute("instructor", instructor);

        List<Usuario> usuarios = em.createQuery(
                "SELECT u FROM Usuario u", Usuario.class
        ).getResultList();
        model.addAttribute("usuarios", usuarios);

        return "Administrador/instructor-form";
    }

    // GUARDAR (CREAR / EDITAR)
    @PostMapping("/guardar")
    @Transactional
    public String guardarInstructor(
            @RequestParam(required = false) Integer id,
            @RequestParam String nombre,
            @RequestParam(required = false) String especialidad,
            @RequestParam(required = false) Integer idUsuario
    ) {

        Instructor instructor;

        if (id != null) {
            // Editar existente
            instructor = em.find(Instructor.class, id);
            if (instructor == null) {
                return "redirect:/admin/instructores";
            }
        } else {
            // Nuevo
            instructor = new Instructor();
        }

        instructor.setNombre(nombre);
        instructor.setEspecialidad(especialidad);

        // Vincular usuario si se envía idUsuario
        if (idUsuario != null) {
            Usuario u = em.find(Usuario.class, idUsuario);
            instructor.setUsuario(u);
        } else {
            instructor.setUsuario(null);
        }

        if (instructor.getId() == null) {
            em.persist(instructor);
        } else {
            em.merge(instructor);
        }

        return "redirect:/admin/instructores";
    }

    // ELIMINAR
    @PostMapping("/{id}/eliminar")
    @Transactional
    public String eliminarInstructor(@PathVariable Integer id) {

        Instructor instructor = em.find(Instructor.class, id);
        if (instructor != null) {
            em.remove(instructor);
        }
        return "redirect:/admin/instructores";
    }
}
