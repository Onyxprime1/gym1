package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.Instructor;
import com.example.gym1.Poo.Ejercicio;
import com.example.gym1.Poo.Rutina;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

        model.addAttribute("nombre", nombreSesion != null ? nombreSesion : "Instructor");

        // Conteo total de ejercicios
        Number ejerciciosCountN = (Number) em.createQuery("SELECT COUNT(e) FROM Ejercicio e").getSingleResult();
        int ejerciciosCount = ejerciciosCountN != null ? ejerciciosCountN.intValue() : 0;
        model.addAttribute("ejerciciosCount", ejerciciosCount);

        // Conteo de rutinas del instructor (si existe instructor para este usuario)
        Instructor instructor = em.createQuery(
                        "SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream()
                .findFirst()
                .orElse(null);

        int rutinasCount = 0;
        if (instructor != null) {
            Number rutinasCountN = (Number) em.createQuery(
                            "SELECT COUNT(r) FROM Rutina r WHERE r.instructor.id = :iid")
                    .setParameter("iid", instructor.getId())
                    .getSingleResult();
            rutinasCount = rutinasCountN != null ? rutinasCountN.intValue() : 0;
        }
        model.addAttribute("rutinasCount", rutinasCount);

        // Conteo total de clientes (para la tarjeta Clientes)
        Number clientesCountN = (Number) em.createQuery("SELECT COUNT(c) FROM Cliente c").getSingleResult();
        int clientesCount = clientesCountN != null ? clientesCountN.intValue() : 0;
        model.addAttribute("clientesCount", clientesCount);

        return "instructor/instructor-inicio";
    }
}