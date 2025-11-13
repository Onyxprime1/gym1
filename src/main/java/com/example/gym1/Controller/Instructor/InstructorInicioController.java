package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Ejercicio;
import com.example.gym1.Poo.Instructor;
import com.example.gym1.Poo.Rutina;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class InstructorInicioController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/instructor")
    public String inicioInstructor(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        String nombreSesion = (String) session.getAttribute("unombre");

        if (uid == null) {
            return "redirect:/login";
        }

        // Buscar instructor asociado al usuario actual
        Instructor instructor = em.createQuery(
                        "SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (instructor == null) {
            // si no es instructor, redirigir o mostrar inicio genérico
            model.addAttribute("error", "No se encontró un instructor para este usuario.");
            return "redirect:/inicio";
        }

        // Contadores y resúmenes
        Number ejerciciosCountN = (Number) em.createQuery("SELECT COUNT(e) FROM Ejercicio e").getSingleResult();
        int ejerciciosCount = ejerciciosCountN != null ? ejerciciosCountN.intValue() : 0;

        Number rutinasCountN = (Number) em.createQuery(
                        "SELECT COUNT(r) FROM Rutina r WHERE r.instructor.id = :iid")
                .setParameter("iid", instructor.getId())
                .getSingleResult();
        int rutinasCount = rutinasCountN != null ? rutinasCountN.intValue() : 0;

        // Listas recientes para mostrar en el dashboard (máx 5)
        List<Ejercicio> recentEjercicios = em.createQuery(
                        "SELECT e FROM Ejercicio e ORDER BY e.id DESC", Ejercicio.class)
                .setMaxResults(5)
                .getResultList();

        List<Rutina> recentRutinas = em.createQuery(
                        "SELECT r FROM Rutina r WHERE r.instructor.id = :iid ORDER BY r.id DESC", Rutina.class)
                .setParameter("iid", instructor.getId())
                .setMaxResults(5)
                .getResultList();

        model.addAttribute("nombre", nombreSesion != null ? nombreSesion : instructor.getNombre());
        model.addAttribute("instructor", instructor);
        model.addAttribute("ejerciciosCount", ejerciciosCount);
        model.addAttribute("rutinasCount", rutinasCount);
        model.addAttribute("recentEjercicios", recentEjercicios);
        model.addAttribute("recentRutinas", recentRutinas);

        return "instructor/instructor-inicio";
    }
}