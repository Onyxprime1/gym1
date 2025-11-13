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

        // Siempre mandamos el nombre del USUARIO
        model.addAttribute("nombre", nombreUsuario);

        // 2) Buscar si este usuario tiene un CLIENTE asociado
        //    Solo necesitamos el id_cliente
        Number idClienteNum = (Number) em.createNativeQuery(
                        "SELECT c.id_cliente " +
                                "FROM clientes c WHERE c.id_usuario = :uid")
                .setParameter("uid", uid)
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

        // 3) Última membresía del cliente
        Object[] mem = (Object[]) em.createNativeQuery(
                        "SELECT m.tipo, cm.fecha_inicio, cm.fecha_fin, m.precio " +
                                "FROM cliente_membresia cm " +
                                "JOIN membresias m ON m.id_membresia = cm.id_membresia " +
                                "WHERE cm.id_cliente = :cid " +
                                "ORDER BY cm.fecha_fin DESC LIMIT 1")
                .setParameter("cid", idCliente)
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
                proxPago = fechaFin.toString(); // luego lo puedes formatear más bonito
                estado = vigente ? "Activa" : "Vencida";
            }
        }

        // 4) Contar rutinas y dietas asignadas al cliente
        Number rutinas = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM rutinas WHERE id_cliente = :cid")
                .setParameter("cid", idCliente)
                .getSingleResult();

        Number dietas = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dietas WHERE id_cliente = :cid")
                .setParameter("cid", idCliente)
                .getSingleResult();

        boolean premium = (rutinas.intValue() + dietas.intValue()) > 0;
        String planNivel = premium ? "El Gran Machote (Premium)" : "Básico";


        // 5) Poner todo en el modelo para la vista
        model.addAttribute("planNivel", planNivel);
        model.addAttribute("vigente", vigente);
        model.addAttribute("proxPago", proxPago);
        model.addAttribute("estado", estado);
        model.addAttribute("rutinasCount", rutinas.intValue());
        model.addAttribute("dietasCount", dietas.intValue());

        return "inicio";
    }
}
