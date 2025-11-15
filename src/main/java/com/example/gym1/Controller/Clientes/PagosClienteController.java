package com.example.gym1.Controller.Cliente;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class PagosClienteController {

    @PersistenceContext
    private EntityManager em;

    // DTO para el historial de pagos del cliente
    public static class PagoClienteItem {
        private Date fecha;
        private String plan;
        private BigDecimal monto;
        private String estado;

        public PagoClienteItem(Date fecha, String plan, BigDecimal monto, String estado) {
            this.fecha = fecha;
            this.plan = plan;
            this.monto = monto;
            this.estado = estado;
        }

        public Date getFecha() { return fecha; }
        public String getPlan() { return plan; }
        public BigDecimal getMonto() { return monto; }
        public String getEstado() { return estado; }
    }

    @GetMapping("/pagos")
        public String verPagos(HttpSession session, Model model) {

        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) {
            return "redirect:/login";
        }

        Integer idCliente = prepararModeloCliente(uid, session, model);

        if (idCliente == null) {
            model.addAttribute("pagosCliente", new ArrayList<PagoClienteItem>());
            // ⬇⬇⬇ nombre real del HTML
            return "Clientes/pagosCliente";
        }

            // Usamos cliente_membresia como "historial de pagos" del cliente
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                            "SELECT COALESCE(mp.nombre_comercial, me.tipo) AS plan, " +
                                    "       me.precio, " +
                                    "       cm.fecha_fin " +
                                    "FROM cliente_membresia cm " +
                                    "JOIN membresias me ON me.id_membresia = cm.id_membresia " +
                                    "LEFT JOIN membresia_plan mp ON mp.id_membresia = me.id_membresia " +
                                    "WHERE cm.id_cliente = :cid " +
                                    "ORDER BY cm.fecha_fin DESC"
                    ).setParameter("cid", idCliente)
                    .getResultList();

            List<PagoClienteItem> pagos = new ArrayList<>();
            for (Object[] r : rows) {
                String plan = (String) r[0];
                BigDecimal monto = (BigDecimal) r[1];
                Date fechaFin = (Date) r[2];

                // asumimos "Pagado" siempre; podrías complicarlo más si quieres
                pagos.add(new PagoClienteItem(
                        fechaFin, plan, monto, "Pagado"
                ));
            }

        model.addAttribute("pagosCliente", pagos);
        // ⬇⬇⬇ nombre real del HTML
        return "Clientes/pagosCliente";
        }

    // Acción del botón "Renovar / Pagar ahora" (por ahora solo regresa a la vista)
    @PostMapping("/pagos/renovar")
    public String renovarMembresia(HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) {
            return "redirect:/login";
        }

        // Aquí podrías:
        //  - insertar un nuevo registro en cliente_membresia
        //  - elegir el plan desde un formulario
        //  - etc.
        // Por ahora, simplemente redirigimos a /pagos.
        return "redirect:/pagos";
    }

    private Integer prepararModeloCliente(Integer uid, HttpSession session, Model model) {
        String nombreUsuario = (String) session.getAttribute("unombre");
        if (nombreUsuario == null) {
            nombreUsuario = "Atleta";
        }
        model.addAttribute("nombre", nombreUsuario);

        Number idClienteNum = (Number) em.createNativeQuery(
                        "SELECT c.id_cliente FROM clientes c WHERE c.id_usuario = :uid"
                ).setParameter("uid", uid)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (idClienteNum == null) {
            model.addAttribute("planNivel", "Sin plan");
            model.addAttribute("vigente", false);
            model.addAttribute("proxPago", "-");
            model.addAttribute("estado", "Sin membresía");
            return null;
        }

        Integer idCliente = idClienteNum.intValue();

        Object[] mem = (Object[]) em.createNativeQuery(
                        "SELECT m.tipo, cm.fecha_inicio, cm.fecha_fin, m.precio " +
                                "FROM cliente_membresia cm " +
                                "JOIN membresias m ON m.id_membresia = cm.id_membresia " +
                                "WHERE cm.id_cliente = :cid " +
                                "ORDER BY cm.fecha_fin DESC LIMIT 1"
                ).setParameter("cid", idCliente)
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

        Number rutinas = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM rutinas WHERE id_cliente = :cid"
                ).setParameter("cid", idCliente)
                .getSingleResult();

        Number dietas = (Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM dietas WHERE id_cliente = :cid"
                ).setParameter("cid", idCliente)
                .getSingleResult();

        boolean premium = (rutinas.intValue() + dietas.intValue()) > 0;
        String planNivel = premium ? "El Gran Machote (Premium)" : "Básico";

        model.addAttribute("planNivel", planNivel);
        model.addAttribute("vigente", vigente);
        model.addAttribute("proxPago", proxPago);
        model.addAttribute("estado", estado);

        return idCliente;
    }
}
