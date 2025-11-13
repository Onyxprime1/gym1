// InicioController.java
package com.example.gym1.Controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class InicioController {

    @PersistenceContext
    private EntityManager em;

    @GetMapping("/inicio")
    public String inicio(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        // 1) Cliente por usuario
        Object[] cli = (Object[]) em.createNativeQuery(
                        "SELECT c.id_cliente, c.nombre " +
                                "FROM clientes c WHERE c.id_usuario = :uid")
                .setParameter("uid", uid)
                .getResultStream().findFirst().orElse(null);

        if (cli == null) {
            // Si el usuario aún no está vinculado a 'clientes', tratamos el nombre de sesión
            model.addAttribute("nombre", Optional.ofNullable((String)session.getAttribute("unombre")).orElse("Atleta"));
            // Sin cliente: plan básico sin datos de pago
            model.addAttribute("planNivel", "Básico");
            model.addAttribute("vigente", false);
            model.addAttribute("proxPago", "-");
            model.addAttribute("estado", "Sin membresía");
            model.addAttribute("rutinasCount", 0);
            model.addAttribute("dietasCount", 0);
            return "inicio";
        }

        Integer idCliente = ((Number) cli[0]).intValue();
        String nombre = (String) cli[1];

        // 2) Última membresía por fecha_fin
        Object[] mem = (Object[]) em.createNativeQuery(
                        "SELECT m.tipo, cm.fecha_inicio, cm.fecha_fin, m.precio " +
                                "FROM cliente_membresia cm " +
                                "JOIN membresias m ON m.id_membresia = cm.id_membresia " +
                                "WHERE cm.id_cliente = :cid " +
                                "ORDER BY cm.fecha_fin DESC LIMIT 1")
                .setParameter("cid", idCliente)
                .getResultStream().findFirst().orElse(null);

        String proxPago = "-";
        String estado = "Sin membresía";
        boolean vigente = false;

        if (mem != null) {
            Date fin = (Date) mem[2];
            if (fin != null) {
                vigente = !fin.toLocalDate().isBefore(LocalDate.now());
                proxPago = fin.toLocalDate().toString(); // formateo simple (puedes usar thymeleaf para formatear)
                estado = vigente ? "Activa" : "Vencida";
            }
        }

        // 3) ¿Tiene asignaciones personalizadas (rutinas/dietas)?
        Number rutinas = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM rutinas WHERE id_cliente = :cid")
                .setParameter("cid", idCliente).getSingleResult();

        Number dietas = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dietas WHERE id_cliente = :cid")
                .setParameter("cid", idCliente).getSingleResult();

        boolean premium = (rutinas.intValue() + dietas.intValue()) > 0;
        String planNivel = premium ? "El Gran Machote (Premium)" : "Básico";

        // 4) Modelo para la vista
        model.addAttribute("nombre", nombre);
        model.addAttribute("planNivel", planNivel);
        model.addAttribute("vigente", vigente);
        model.addAttribute("proxPago", proxPago);
        model.addAttribute("estado", estado);
        model.addAttribute("rutinasCount", rutinas.intValue());
        model.addAttribute("dietasCount", dietas.intValue());

        return "inicio";
    }
}
