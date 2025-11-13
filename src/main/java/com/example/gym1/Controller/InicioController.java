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
        // 1) Verificar usuario en sesión
        Integer uid = (Integer) session.getAttribute("uid");
        String nombreUsuario = Optional.ofNullable((String) session.getAttribute("unombre"))
                .orElse("Atleta");

        if (uid == null) {
            return "redirect:/login";
        }

        // REDIRECCIÓN: si el usuario es INSTRUCTOR, mandarlo al panel de instructor
        String rolNombreSession = Optional.ofNullable((String) session.getAttribute("rolNombre"))
                .orElse("").toLowerCase();
        Integer rolId = (Integer) session.getAttribute("rolId");
        if (rolNombreSession.contains("instructor")) {
            return "redirect:/instructor/ejercicios";
        }

        // 1.1) Verificar rolId → si es 1 (ADMIN), ir a admin.html
        if (rolId != null && rolId == 1) {   // admin
            model.addAttribute("nombre", nombreUsuario);
            return "Administrador/admin";
        }

        // Siempre mandamos el nombre del USUARIO a la vista inicio.html
        model.addAttribute("nombre", nombreUsuario);

        // 2) Buscar si este usuario tiene un CLIENTE asociado
        Number idClienteNum = (Number) em.createNativeQuery(
                        "SELECT c.id_cliente FROM clientes c WHERE c.id_usuario = ?")
                .setParameter(1, uid)
                .getResultStream()
                .findFirst()
                .orElse(null);

        // Si NO es cliente aún → sin membresía, sin rutinas, sin dietas
        if (idClienteNum == null) {
            model.addAttribute("planNivel", "Sin plan");
            model.addAttribute("vigente", false);
            model.addAttribute("proxPago", "-");
            model.addAttribute("estado", "Sin membresía");
            model.addAttribute("rutinasCount", 0);
            model.addAttribute("dietasCount", 0);
            return "inicio";
        }

        Integer idCliente = idClienteNum.intValue();

        // 3) Última membresía del cliente (native query con parámetro posicional y setMaxResults(1))
        Object[] mem = (Object[]) em.createNativeQuery(
                        "SELECT m.tipo, cm.fecha_inicio, cm.fecha_fin, m.precio " +
                                "FROM cliente_membresia cm " +
                                "JOIN membresias m ON m.id_membresia = cm.id_membresia " +
                                "WHERE cm.id_cliente = ? " +
                                "ORDER BY cm.fecha_fin DESC")
                .setParameter(1, idCliente)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        String proxPago = "-";
        String estado = "Sin membresía";
        boolean vigente = false;

        if (mem != null) {
            Date fin = (Date) mem[2];
            if (fin != null) {
                LocalDate fechaFin = fin.toLocalDate();
                vigente = !fechaFin.isBefore(LocalDate.now());
                proxPago = fechaFin.toString();
                estado = vigente ? "Activa" : "Vencida";
            }
        }

        // 4) Contar rutinas y dietas asignadas al cliente (native queries con parámetro posicional)
        Number rutinasNum = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM rutinas WHERE id_cliente = ?")
                .setParameter(1, idCliente)
                .getSingleResult();

        Number dietasNum = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dietas WHERE id_cliente = ?")
                .setParameter(1, idCliente)
                .getSingleResult();

        int rutinas = rutinasNum != null ? rutinasNum.intValue() : 0;
        int dietas = dietasNum != null ? dietasNum.intValue() : 0;

        boolean premium = (rutinas + dietas) > 0;
        String planNivel = premium ? "El Gran Machote (Premium)" : "Básico";

        // 5) Poner todo en el modelo para la vista
        model.addAttribute("planNivel", planNivel);
        model.addAttribute("vigente", vigente);
        model.addAttribute("proxPago", proxPago);
        model.addAttribute("estado", estado);
        model.addAttribute("rutinasCount", rutinas);
        model.addAttribute("dietasCount", dietas);

        return "inicio";
    }
}