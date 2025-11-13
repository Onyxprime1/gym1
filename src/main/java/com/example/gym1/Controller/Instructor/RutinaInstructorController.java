package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.Ejercicio;
import com.example.gym1.Poo.Instructor;
import com.example.gym1.Poo.Rutina;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Controlador para gestión de rutinas (crear/editar/listar/eliminar).
 * Respeta los POJOs existentes y usa consultas nativas para la tabla rutina_detalle.
 */
@Controller
@RequestMapping("/instructor/rutinas")
public class RutinaInstructorController {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper mapper = new ObjectMapper();

    // LISTAR rutinas del instructor
    @GetMapping
    public String listar(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Instructor instructor = em.createQuery(
                        "SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream().findFirst().orElse(null);

        if (instructor == null) {
            model.addAttribute("error", "Instructor no encontrado");
            return "instructor/rutinas";
        }

        // Obtenemos rutinas vía JPQL (la entidad Rutina ya mapea la columna id_instructor)
        List<Rutina> rutinas = em.createQuery(
                        "SELECT r FROM Rutina r WHERE r.instructor.id = :iid ORDER BY r.id DESC", Rutina.class)
                .setParameter("iid", instructor.getId())
                .getResultList();

        model.addAttribute("rutinas", rutinas);
        return "instructor/rutinas";
    }

    // FORM crear
    @GetMapping("/crear")
    public String crearForm(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Instructor instructor = em.createQuery(
                        "SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream().findFirst().orElse(null);
        if (instructor == null) {
            model.addAttribute("error", "Instructor no encontrado");
            return "instructor/rutina-form";
        }

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();

        // Intentamos obtener clientes relacionados mediante JOIN con rutinas (si existen)
        List<Cliente> clientes;
        try {
            clientes = em.createNativeQuery(
                            "SELECT DISTINCT c.* FROM clientes c JOIN rutinas r ON c.id_cliente = r.id_cliente WHERE r.id_instructor = ? ORDER BY c.nombre",
                            Cliente.class)
                    .setParameter(1, instructor.getId())
                    .getResultList();

            if (clientes == null || clientes.isEmpty()) {
                clientes = em.createQuery("SELECT c FROM Cliente c ORDER BY c.nombre", Cliente.class).getResultList();
            }
        } catch (Exception ex) {
            clientes = em.createQuery("SELECT c FROM Cliente c ORDER BY c.nombre", Cliente.class).getResultList();
        }

        model.addAttribute("ejercicios", ejercicios);
        model.addAttribute("clientes", clientes);
        model.addAttribute("rutina", new Rutina());
        model.addAttribute("detallesJson", "[]"); // JS espera JSON
        return "instructor/rutina-form";
    }

    // FORM editar (carga la rutina y sus detalles desde rutina_detalle)
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Rutina rutina = em.find(Rutina.class, id);
        if (rutina == null) {
            ra.addFlashAttribute("error", "Rutina no encontrada");
            return "redirect:/instructor/rutinas";
        }

        Instructor instructor = em.createQuery(
                        "SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream().findFirst().orElse(null);
        if (instructor == null) {
            ra.addFlashAttribute("error", "Instructor no encontrado");
            return "redirect:/instructor/rutinas";
        }

        // Verificar propietario (protección básica)
        if (rutina.getInstructor() == null || !Objects.equals(rutina.getInstructor().getId(), instructor.getId())) {
            ra.addFlashAttribute("error", "No tienes permiso para editar esta rutina");
            return "redirect:/instructor/rutinas";
        }

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();

        // Clientes (los que han recibido rutinas de este instructor; fallback a todos)
        List<Cliente> clientes;
        try {
            clientes = em.createNativeQuery(
                            "SELECT DISTINCT c.* FROM clientes c JOIN rutinas r ON c.id_cliente = r.id_cliente WHERE r.id_instructor = ? ORDER BY c.nombre",
                            Cliente.class)
                    .setParameter(1, instructor.getId())
                    .getResultList();
            if (clientes == null || clientes.isEmpty()) {
                clientes = em.createQuery("SELECT c FROM Cliente c ORDER BY c.nombre", Cliente.class).getResultList();
            }
        } catch (Exception ex) {
            clientes = em.createQuery("SELECT c FROM Cliente c ORDER BY c.nombre", Cliente.class).getResultList();
        }

        // Cargar detalles desde la tabla rutina_detalle (nombres reales en la BD)
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT id_ejercicio, dia_semana, series, repeticiones FROM rutina_detalle WHERE id_rutina = ? ORDER BY id_ejercicio")
                .setParameter(1, rutina.getId())
                .getResultList();

        List<Map<String, Object>> dto = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> m = new HashMap<>();
            Integer idEj = row[0] != null ? ((Number) row[0]).intValue() : null;
            String dia = row[1] != null ? row[1].toString() : null;
            Integer series = row[2] != null ? ((Number) row[2]).intValue() : null;
            Integer reps = row[3] != null ? ((Number) row[3]).intValue() : null;
            m.put("id", idEj);
            m.put("dia", dia);
            m.put("serie", series);
            m.put("repeticiones", reps);
            if (idEj != null) {
                Ejercicio e = em.find(Ejercicio.class, idEj);
                if (e != null) {
                    m.put("nombre", e.getNombre());
                    m.put("musculo", e.getMusculo());
                }
            }
            dto.add(m);
        }

        String detallesJson = "[]";
        try {
            detallesJson = mapper.writeValueAsString(dto);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        model.addAttribute("ejercicios", ejercicios);
        model.addAttribute("clientes", clientes);
        model.addAttribute("rutina", rutina);
        model.addAttribute("detallesJson", detallesJson);
        return "instructor/rutina-form";
    }

    // GUARDAR rutina + detalles (crear o actualizar) usando SQL nativo para rutina_detalle
    @PostMapping("/guardar")
    @Transactional
    public String guardar(@ModelAttribute Rutina rutina,
                          @RequestParam(value = "clienteId", required = false) Integer clienteId,
                          @RequestParam(value = "ejercicioId", required = false) List<Integer> ejercicioIds,
                          @RequestParam(value = "serie", required = false) List<Integer> series,
                          @RequestParam(value = "repeticiones", required = false) List<Integer> repeticiones,
                          @RequestParam(value = "dia", required = false) List<String> dias,
                          HttpSession session,
                          RedirectAttributes ra) {

        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Instructor instructor = em.createQuery(
                        "SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream().findFirst().orElse(null);
        if (instructor == null) {
            ra.addFlashAttribute("error", "Instructor no encontrado");
            return "redirect:/instructor/rutinas";
        }

        // Asignar instructor en la entidad Rutina (POJO existente)
        rutina.setInstructor(instructor);

        // Asociar cliente de forma segura: obtener referencia (no persistir ni eliminar cliente)
        if (clienteId != null) {
            try {
                Cliente cRef = em.getReference(Cliente.class, clienteId);
                rutina.setCliente(cRef);
            } catch (Exception ex) {
                ra.addFlashAttribute("error", "Cliente no encontrado");
                return "redirect:/instructor/rutinas/crear";
            }
        } else {
            rutina.setCliente(null);
        }

        // Persistir o mergear rutina
        if (rutina.getId() == null) {
            em.persist(rutina);
            em.flush(); // asegurar id
        } else {
            rutina = em.merge(rutina);
        }
        Integer rutinaId = rutina.getId();

        // Eliminar detalles previos en la tabla rutina_detalle (SQL nativo)
        em.createNativeQuery("DELETE FROM rutina_detalle WHERE id_rutina = ?")
                .setParameter(1, rutinaId)
                .executeUpdate();

        // Insertar nuevos detalles enviados
        if (ejercicioIds != null && !ejercicioIds.isEmpty()) {
            for (int i = 0; i < ejercicioIds.size(); i++) {
                Integer eid = ejercicioIds.get(i);
                if (eid == null) continue;
                Integer s = (series != null && series.size() > i) ? series.get(i) : null;
                Integer rps = (repeticiones != null && repeticiones.size() > i) ? repeticiones.get(i) : null;
                String d = (dias != null && dias.size() > i) ? dias.get(i) : null;

                em.createNativeQuery("INSERT INTO rutina_detalle (id_rutina, id_ejercicio, dia_semana, series, repeticiones) VALUES (?, ?, ?, ?, ?)")
                        .setParameter(1, rutinaId)
                        .setParameter(2, eid)
                        .setParameter(3, d)
                        .setParameter(4, s)
                        .setParameter(5, rps)
                        .executeUpdate();
            }
        }

        ra.addFlashAttribute("success", "Rutina guardada correctamente");
        return "redirect:/instructor/rutinas";
    }

    // ELIMINAR rutina
    @PostMapping("/eliminar/{id}")
    @Transactional
    public String eliminar(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Rutina r = em.find(Rutina.class, id);
        if (r == null) {
            ra.addFlashAttribute("error", "Rutina no encontrada");
            return "redirect:/instructor/rutinas";
        }

        Instructor inst = em.createQuery(
                        "SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream().findFirst().orElse(null);
        if (inst == null || r.getInstructor() == null || !inst.getId().equals(r.getInstructor().getId())) {
            ra.addFlashAttribute("error", "No tienes permiso para eliminar esta rutina");
            return "redirect:/instructor/rutinas";
        }

        // Borrar detalles (tabla rutina_detalle) y luego la rutina
        em.createNativeQuery("DELETE FROM rutina_detalle WHERE id_rutina = ?").setParameter(1, id).executeUpdate();
        em.remove(em.contains(r) ? r : em.merge(r));
        ra.addFlashAttribute("success", "Rutina eliminada");
        return "redirect:/instructor/rutinas";
    }
}