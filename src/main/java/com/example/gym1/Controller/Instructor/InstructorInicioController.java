package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.Ejercicio;
import com.example.gym1.Poo.Instructor;
import com.example.gym1.Poo.Rutina;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador del panel del instructor.
 * Añade los datos necesarios para mostrar por cliente las rutinas asignadas
 * y para permitir asignar una rutina existente al cliente.
 */
@Controller
public class InstructorInicioController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/instructor")
    public String inicioInstructor(@RequestParam(value = "clienteId", required = false) Integer clienteId,
                                   HttpSession session, Model model) {
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

        // Lista de clientes del sistema (para el instructor poder ver y gestionar)
        List<Cliente> clientes = em.createQuery("SELECT c FROM Cliente c ORDER BY c.nombre", Cliente.class)
                .getResultList();

        // Construir lista por cliente con sus rutinas asignadas por este instructor
        List<ClientRutinas> clientRutinasList = new ArrayList<>();
        for (Cliente c : clientes) {
            List<Rutina> rutinasDelCliente = em.createQuery(
                            "SELECT r FROM Rutina r WHERE r.instructor.id = :iid AND r.cliente.id = :cid ORDER BY r.id DESC",
                            Rutina.class)
                    .setParameter("iid", instructor.getId())
                    .setParameter("cid", getClienteIdSafe(c))
                    .getResultList();
            clientRutinasList.add(new ClientRutinas(c, rutinasDelCliente));
        }

        // Plantillas / rutinas "generales" del instructor (rutinas creadas sin cliente asignado)
        List<Rutina> plantillaRutinas = em.createQuery(
                        "SELECT r FROM Rutina r WHERE r.instructor.id = :iid AND r.cliente IS NULL ORDER BY r.nombre",
                        Rutina.class)
                .setParameter("iid", instructor.getId())
                .getResultList();

        model.addAttribute("nombre", nombreSesion != null ? nombreSesion : instructor.getNombre());
        model.addAttribute("instructor", instructor);
        model.addAttribute("ejerciciosCount", ejerciciosCount);
        model.addAttribute("rutinasCount", rutinasCount);
        model.addAttribute("recentEjercicios", recentEjercicios);
        model.addAttribute("recentRutinas", recentRutinas);
        model.addAttribute("clientes", clientes);
        model.addAttribute("clientRutinasList", clientRutinasList);
        model.addAttribute("plantillaRutinas", plantillaRutinas);
        model.addAttribute("selectedClienteId", clienteId);

        return "instructor/instructor-inicio";
    }

    // Helper para obtener el id del cliente sin lanzar NullPointer (asume getter getId o getIdCliente)
    private Integer getClienteIdSafe(Cliente c) {
        if (c == null) return null;
        try {
            // lo habitual es getId()
            return (Integer) c.getClass().getMethod("getId").invoke(c);
        } catch (Exception ex) {
            try {
                return (Integer) c.getClass().getMethod("getIdCliente").invoke(c);
            } catch (Exception ex2) {
                // fallback a null (no debería ocurrir si la entidad está bien definida)
                return null;
            }
        }
    }

    // DTO simple para enviar Cliente + sus Rutinas al template
    public static class ClientRutinas {
        private final Cliente cliente;
        private final List<Rutina> rutinas;

        public ClientRutinas(Cliente cliente, List<Rutina> rutinas) {
            this.cliente = cliente;
            this.rutinas = rutinas;
        }

        public Cliente getCliente() {
            return cliente;
        }

        public List<Rutina> getRutinas() {
            return rutinas;
        }
    }
}