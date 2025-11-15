package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Ejercicio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controlador para gestionar ejercicios (listar, crear, editar, eliminar).
 * Eliminar ahora limpia referencias en rutina_detalle antes de borrar el ejercicio
 * para evitar violaciones de FK con rutinas antiguas.
 */
@Controller
@RequestMapping("/instructor/ejercicios")
public class EjercicioInstructorController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    public String listar(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();

        model.addAttribute("ejercicios", ejercicios);
        return "instructor/ejercicios";
    }

    @GetMapping("/nuevo")
    public String nuevo(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        model.addAttribute("ejercicio", new Ejercicio());
        return "instructor/ejercicio-form";
    }

    @PostMapping("/guardar")
    @Transactional
    public String guardar(@ModelAttribute Ejercicio ejercicio, HttpSession session, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        try {
            if (ejercicio.getId() == null) {
                em.persist(ejercicio);
            } else {
                em.merge(ejercicio);
            }
            ra.addFlashAttribute("success", "Ejercicio guardado correctamente.");
        } catch (Exception ex) {
            ex.printStackTrace();
            ra.addFlashAttribute("error", "Error guardando ejercicio: " + ex.getMessage());
        }
        return "redirect:/instructor/ejercicios";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Ejercicio e = em.find(Ejercicio.class, id);
        if (e == null) {
            ra.addFlashAttribute("error", "Ejercicio no encontrado");
            return "redirect:/instructor/ejercicios";
        }
        model.addAttribute("ejercicio", e);
        return "instructor/ejercicio-form";
    }

    /**
     * Eliminar ejercicio: limpieza de referencias antigua y eliminación.
     * - Borra filas en rutina_detalle que referencien el ejercicio,
     * - Luego elimina la entidad ejercicio de la tabla ejercicios.
     * Esto evita el error de FK que veías al intentar borrar ejercicios usados en rutinas antiguas.
     */
    @PostMapping("/eliminar/{id}")
    @Transactional
    public String eliminar(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        try {
            // 1) comprobar cuántas referencias hay (opcional información)
            Number refsN = (Number) em.createNativeQuery("SELECT COUNT(1) FROM rutina_detalle WHERE id_ejercicio = ?")
                    .setParameter(1, id)
                    .getSingleResult();
            int refs = refsN != null ? refsN.intValue() : 0;

            // 2) borrar referencias en rutina_detalle (si existen)
            if (refs > 0) {
                em.createNativeQuery("DELETE FROM rutina_detalle WHERE id_ejercicio = ?")
                        .setParameter(1, id)
                        .executeUpdate();
            }

            // 3) borrar el ejercicio
            Ejercicio ejercicio = em.find(Ejercicio.class, id);
            if (ejercicio == null) {
                ra.addFlashAttribute("error", "Ejercicio no encontrado");
                return "redirect:/instructor/ejercicios";
            }
            em.remove(em.contains(ejercicio) ? ejercicio : em.merge(ejercicio));

            // 4) feedback al usuario
            if (refs > 0) {
                ra.addFlashAttribute("success", "Ejercicio eliminado y " + refs + " referencia(s) en rutinas fueron limpiadas.");
            } else {
                ra.addFlashAttribute("success", "Ejercicio eliminado correctamente.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            ra.addFlashAttribute("error", "Error al eliminar ejercicio: " + ex.getMessage());
        }

        return "redirect:/instructor/ejercicios";
    }
}