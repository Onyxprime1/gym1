package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.Rutina;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Controlador de clientes: lista y detalle. Añade 'membresia' calculada seguro.
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

        System.out.println("[DEBUG] clientes encontrados: " + (clientes != null ? clientes.size() : 0));

        model.addAttribute("clientes", clientes);
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

        List<Rutina> plantillas = em.createQuery("SELECT r FROM Rutina r WHERE r.cliente IS NULL ORDER BY r.nombre", Rutina.class)
                .getResultList();

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

    private String safeGetMembership(Cliente c) {
        if (c == null) return "—";
        // Intentar detectar getters / ids / objetos (mismos pasos previos)
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