package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.Rutina;
import com.example.gym1.Poo.Instructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controlador de clientes: lista y detalle.
 *
 * Cambios principales en esta versión:
 * - Detección robusta del Instructor autenticado (intenta i.usuario.id = uid y también i.id = uid).
 * - Mensajes flash más informativos sobre asignaciones (cuántas asignadas y razones de fallos).
 * - Seguimos obligando que sólo se puedan asignar rutinas cuya rutina.instructor.id == instructorId.
 * - Registro (System.out) de información útil para depuración local.
 */
@Controller
@RequestMapping("/instructor/clientes")
public class InstructorClienteController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping
    public String listarClientes(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        List<Cliente> clientes = em.createQuery("SELECT c FROM Cliente c ORDER BY c.nombre", Cliente.class)
                .getResultList();

        List<Object> rows = new ArrayList<>();
        for (Cliente c : clientes) {
            String memb = safeGetMembership(c);
            rows.add(new Object() {
                public Cliente cliente = c;
                public String membresia = memb;
            });
        }

        model.addAttribute("clientesRows", rows);
        model.addAttribute("nombre", session.getAttribute("unombre"));
        return "instructor/instructor-clientes";
    }

    @GetMapping("/{id}")
    public String clienteDetalle(@PathVariable Integer id, HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Cliente cliente = em.find(Cliente.class, id);
        if (cliente == null) {
            return "redirect:/instructor/clientes";
        }

        Integer instructorId = resolveInstructorId(uid);

        System.out.println("[DEBUG] clienteDetalle: session.uid=" + uid + " resolvedInstructorId=" + instructorId);

        // PLANTILLAS — sólo rutinas del instructor autenticado, y que no pertenezcan al cliente mostrado
        List<Rutina> plantillas;
        if (instructorId != null) {
            plantillas = em.createQuery(
                            "SELECT DISTINCT r FROM Rutina r " +
                                    "WHERE r.instructor IS NOT NULL AND r.instructor.id = :iid " +
                                    "AND (r.cliente IS NULL OR r.cliente.id <> :cid) " +
                                    "ORDER BY r.nombre",
                            Rutina.class)
                    .setParameter("iid", instructorId)
                    .setParameter("cid", id)
                    .getResultList();
        } else {
            plantillas = Collections.emptyList();
        }

        // assigned: rutinas que pertenecen a este cliente
        List<Rutina> assigned = em.createQuery("SELECT r FROM Rutina r WHERE r.cliente.id = :cid ORDER BY r.id DESC", Rutina.class)
                .setParameter("cid", id)
                .getResultList();

        model.addAttribute("cliente", cliente);
        model.addAttribute("plantillas", plantillas);
        model.addAttribute("assignedRutinas", assigned);
        model.addAttribute("nombre", session.getAttribute("unombre"));
        model.addAttribute("membresia", safeGetMembership(cliente));
        return "instructor/cliente-asignar";
    }

    /**
     * Asigna plantillas al cliente sin clonar: se marca la rutina existente con el cliente dado.
     * Comprueba:
     *  - que la rutina exista,
     *  - que la rutina fue creada por el instructor autenticado,
     *  - que la rutina no esté ya asignada a otro cliente.
     *
     * Ahora acumula razones de fallo y las muestra en flash para depuración / UX.
     */
    @PostMapping("/{id}/asignar")
    @Transactional
    public String asignarRutinasAlCliente(@PathVariable Integer id,
                                          @RequestParam(value = "rutinaIds", required = false) List<Integer> rutinaIds,
                                          RedirectAttributes ra,
                                          HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Cliente cliente = em.find(Cliente.class, id);
        if (cliente == null) return "redirect:/instructor/clientes";

        Integer instructorId = resolveInstructorId(uid);
        System.out.println("[DEBUG] asignarRutinasAlCliente: uid=" + uid + " resolvedInstructorId=" + instructorId);

        if (instructorId == null) {
            ra.addFlashAttribute("error", "No se pudo detectar el instructor autenticado (imposible asignar).");
            return "redirect:/instructor/clientes/" + id;
        }

        int assignedCount = 0;
        List<String> failures = new ArrayList<>();

        try {
            if (rutinaIds == null || rutinaIds.isEmpty()) {
                ra.addFlashAttribute("error", "No seleccionaste ninguna rutina para asignar.");
                return "redirect:/instructor/clientes/" + id;
            }

            for (Integer rid : rutinaIds) {
                if (rid == null) {
                    failures.add("Id nulo en selección");
                    continue;
                }
                Rutina r = em.find(Rutina.class, rid);
                if (r == null) {
                    failures.add("Rutina id=" + rid + " no encontrada");
                    continue;
                }

                // comprobar instructor propietario (r.instructor.id == instructorId)
                boolean okInstructor = false;
                try {
                    Method gm = r.getClass().getMethod("getInstructor");
                    Object owner = gm.invoke(r);
                    if (owner instanceof Instructor) {
                        Instructor ownerInstr = (Instructor) owner;
                        okInstructor = ownerInstr != null && ownerInstr.getId() != null && ownerInstr.getId().equals(instructorId);
                    }
                } catch (NoSuchMethodException ignored) {
                    failures.add("Rutina id=" + rid + " no expone getInstructor()");
                    okInstructor = false;
                } catch (Exception ex) {
                    failures.add("Rutina id=" + rid + " error al leer instructor: " + ex.getMessage());
                    okInstructor = false;
                }
                if (!okInstructor) {
                    failures.add("Rutina id=" + rid + " no pertenece al instructor autenticado");
                    continue;
                }

                // comprobar que no esté asignada ya a otro cliente
                boolean alreadyAssignedToOther = false;
                try {
                    Method gm2 = r.getClass().getMethod("getCliente");
                    Object c = gm2.invoke(r);
                    if (c instanceof Cliente) {
                        Cliente cobj = (Cliente) c;
                        if (cobj != null && cobj.getId() != null && !cobj.getId().equals(id)) {
                            alreadyAssignedToOther = true;
                        } else if (cobj != null && cobj.getId() != null && cobj.getId().equals(id)) {
                            // ya asignada al mismo cliente → nada que hacer
                            failures.add("Rutina id=" + rid + " ya asignada a este cliente");
                            continue;
                        }
                    }
                } catch (NoSuchMethodException ignored) {
                    // si no existe getCliente, no podemos verificar; saltamos por seguridad
                    failures.add("Rutina id=" + rid + " no expone getCliente()");
                    continue;
                } catch (Exception ex) {
                    failures.add("Rutina id=" + rid + " error al leer cliente actual: " + ex.getMessage());
                    continue;
                }
                if (alreadyAssignedToOther) {
                    failures.add("Rutina id=" + rid + " ya asignada a otro cliente");
                    continue;
                }

                // asignar: set cliente en la entidad existente
                try {
                    Method sc = r.getClass().getMethod("setCliente", Cliente.class);
                    sc.invoke(r, cliente);
                    // r es entidad gestionada si proviene de em.find -> no hace falta merge, pero lo hacemos por seguridad
                    em.merge(r);
                    assignedCount++;
                } catch (NoSuchMethodException ex) {
                    // intentar setClienteId si existe
                    try {
                        Method scId = r.getClass().getMethod("setClienteId", Integer.class);
                        scId.invoke(r, cliente.getId());
                        em.merge(r);
                        assignedCount++;
                    } catch (Exception ex2) {
                        failures.add("Rutina id=" + rid + " no tiene setter para cliente");
                        continue;
                    }
                } catch (Exception ex) {
                    failures.add("Rutina id=" + rid + " fallo al asignar: " + ex.getMessage());
                    continue;
                }
            }

            StringBuilder msg = new StringBuilder();
            msg.append("Asignadas: ").append(assignedCount);
            if (!failures.isEmpty()) {
                msg.append(". Errores: ").append(failures.size()).append(". ");
                // concatenar primeras 6 errores
                int max = Math.min(failures.size(), 6);
                for (int i = 0; i < max; i++) {
                    msg.append(failures.get(i));
                    if (i < max - 1) msg.append("; ");
                }
            }
            ra.addFlashAttribute("success", msg.toString());
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Error general al asignar rutinas: " + ex.getMessage());
        }
        return "redirect:/instructor/clientes/" + id;
    }

    /**
     * Desasigna (quita) la rutina del cliente: en lugar de eliminar la rutina,
     * simplemente se pone r.setCliente(null).
     */
    @PostMapping("/{id}/desasignar/{rutinaId}")
    @Transactional
    public String desasignarRutina(@PathVariable Integer id,
                                   @PathVariable Integer rutinaId,
                                   RedirectAttributes ra,
                                   HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        try {
            Rutina r = em.find(Rutina.class, rutinaId);
            if (r == null) {
                ra.addFlashAttribute("error", "Rutina no encontrada.");
                return "redirect:/instructor/clientes/" + id;
            }

            // verificar que pertenece a este cliente
            boolean belongsToClient = false;
            try {
                Method gm = r.getClass().getMethod("getCliente");
                Object c = gm.invoke(r);
                if (c instanceof Cliente) {
                    Cliente cobj = (Cliente) c;
                    if (cobj != null && cobj.getId() != null && cobj.getId().equals(id)) {
                        belongsToClient = true;
                    }
                }
            } catch (NoSuchMethodException ignored) { }

            if (!belongsToClient) {
                ra.addFlashAttribute("error", "La rutina no está asignada a este cliente.");
                return "redirect:/instructor/clientes/" + id;
            }

            // quitar asignación: set cliente = null
            try {
                Method sc = r.getClass().getMethod("setCliente", Cliente.class);
                sc.invoke(r, new Object[] { null });
                em.merge(r);
            } catch (NoSuchMethodException ex) {
                // intentar setClienteId(null)
                try {
                    Method scId = r.getClass().getMethod("setClienteId", Integer.class);
                    scId.invoke(r, new Object[] { null });
                    em.merge(r);
                } catch (Exception ex2) {
                    ra.addFlashAttribute("error", "No se pudo quitar la asignación (setter ausente).");
                    return "redirect:/instructor/clientes/" + id;
                }
            } catch (Exception ex) {
                ra.addFlashAttribute("error", "Error quitando asignación: " + ex.getMessage());
                return "redirect:/instructor/clientes/" + id;
            }

            ra.addFlashAttribute("success", "Rutina desasignada correctamente.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Error desasignando rutina: " + ex.getMessage());
        }

        return "redirect:/instructor/clientes/" + id;
    }

    // ---------- Helpers ----------

    /**
     * Resuelve el id del Instructor autenticado a partir del session.uid.
     * Intentos:
     *  - SELECT i FROM Instructor i WHERE i.usuario.id = :uid
     *  - em.find(Instructor.class, uid)  (por si session uid ya es instructor id)
     */
    private Integer resolveInstructorId(Integer uid) {
        if (uid == null) return null;
        try {
            Instructor instr = em.createQuery("SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                    .setParameter("uid", uid)
                    .getResultStream().findFirst().orElse(null);
            if (instr != null) return instr.getId();
        } catch (Exception ignored) {}

        try {
            Instructor instr2 = em.find(Instructor.class, uid);
            if (instr2 != null) return instr2.getId();
        } catch (Exception ignored) {}

        return null;
    }

    private String safeGetMembership(Cliente c) {
        if (c == null) return "—";
        try {
            Method gm = c.getClass().getMethod("getMembresia");
            Object memb = gm.invoke(c);
            if (memb != null) {
                try {
                    Method mname = memb.getClass().getMethod("getNombre");
                    Object v = mname.invoke(memb);
                    if (v != null) return v.toString();
                } catch (NoSuchMethodException ignored) {}
                try {
                    Method mtype = memb.getClass().getMethod("getTipo");
                    Object v = mtype.invoke(memb);
                    if (v != null) return v.toString();
                } catch (NoSuchMethodException ignored) {}
                return memb.toString();
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception ignored) {}

        String[] idGetters = new String[]{"getMembresiaId","getPlanId","getIdMembresia","getTipoMembresiaId","getIdPlan"};
        for (String name : idGetters) {
            try {
                Method m = c.getClass().getMethod(name);
                Object val = m.invoke(c);
                if (val != null) {
                    Integer id = null;
                    if (val instanceof Number) id = ((Number) val).intValue();
                    else {
                        try { id = Integer.valueOf(val.toString()); } catch (Exception ex) { id = null; }
                    }
                    if (id != null) {
                        switch (id) {
                            case 1: return "Mensual";
                            case 2: return "Trimestral";
                            case 3: return "Anual";
                            default: return "Plan " + id;
                        }
                    }
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {}
        }

        String[] textGetters = new String[]{"getTipoMembresia","getPlan","getTipo","getNombreMembresia","getEstado"};
        for (String name : textGetters) {
            try {
                Method m = c.getClass().getMethod(name);
                Object v = m.invoke(c);
                if (v != null) return v.toString();
            } catch (NoSuchMethodException ignored) {
            } catch (Exception ignored) {}
        }

        return "—";
    }
}