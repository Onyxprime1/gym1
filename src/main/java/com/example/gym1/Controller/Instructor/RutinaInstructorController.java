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
import java.util.stream.Collectors;

/**
 * Controlador para gestión de rutinas (crear/editar/listar/eliminar/asignar).
 * Actualizado: el formulario de crear/editar no contiene selección de cliente;
 * muestra todos los ejercicios y lista de músculos para filtrar.
 */
@Controller
@RequestMapping("/instructor/rutinas")
public class RutinaInstructorController {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper mapper = new ObjectMapper();

    // LISTAR rutinas del instructor (sin cambios)
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

        List<Rutina> rutinas = em.createQuery(
                        "SELECT r FROM Rutina r WHERE r.instructor.id = :iid ORDER BY r.id DESC", Rutina.class)
                .setParameter("iid", instructor.getId())
                .getResultList();

        model.addAttribute("rutinas", rutinas);
        return "instructor/rutinas";
    }

    // FORM crear: NO devolvemos lista de clientes, sí ejercicios y lista de músculos
    @GetMapping("/crear")
    public String crearForm(HttpSession session, Model model,
                            @RequestParam(value = "clienteId", required = false) Integer clienteId) {
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

        // Todos los ejercicios
        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();

        // Lista de músculos distintos (para el filtro)
        List<String> musculos = ejercicios.stream()
                .map(Ejercicio::getMusculo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        model.addAttribute("ejercicios", ejercicios);
        model.addAttribute("musculos", musculos);
        model.addAttribute("rutina", new Rutina());
        model.addAttribute("detallesJson", "[]"); // JS espera JSON
        return "instructor/rutina-form";
    }

    // FORM editar (mantiene comportamiento anterior, pero también provee músculos y ejercicios)
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

        // protección: solo el instructor dueño puede editar la rutina
        if (rutina.getInstructor() == null || !Objects.equals(rutina.getInstructor().getId(), instructor.getId())) {
            ra.addFlashAttribute("error", "No tienes permiso para editar esta rutina");
            return "redirect:/instructor/rutinas";
        }

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();

        List<String> musculos = ejercicios.stream()
                .map(Ejercicio::getMusculo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        // Cargar detalles desde la tabla rutina_detalle
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
        model.addAttribute("musculos", musculos);
        model.addAttribute("rutina", rutina);
        model.addAttribute("detallesJson", detallesJson);
        return "instructor/rutina-form";
    }

    // GUARDAR rutina + detalles (mantiene cliente opcional; formulario ya no envía cliente)
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

        rutina.setInstructor(instructor);

        // Si por alguna razón llega clienteId (ahora opcional), lo asociamos; normalmente no vendrá desde la UI
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

        if (rutina.getId() == null) {
            em.persist(rutina);
            em.flush();
        } else {
            rutina = em.merge(rutina);
        }
        Integer rutinaId = rutina.getId();

        em.createNativeQuery("DELETE FROM rutina_detalle WHERE id_rutina = ?")
                .setParameter(1, rutinaId)
                .executeUpdate();

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

    // ELIMINAR rutina (sin cambios)
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

        em.createNativeQuery("DELETE FROM rutina_detalle WHERE id_rutina = ?").setParameter(1, id).executeUpdate();
        em.remove(em.contains(r) ? r : em.merge(r));
        ra.addFlashAttribute("success", "Rutina eliminada");
        return "redirect:/instructor/rutinas";
    }
}