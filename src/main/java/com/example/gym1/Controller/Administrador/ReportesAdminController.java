package com.example.gym1.Controller.Administrador;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/reportes")
public class ReportesAdminController {

    @PersistenceContext
    private EntityManager em;

    // DTO para últimas membresías vendidas
    public static class UltimaMembresiaView {
        private String cliente;
        private String plan;
        private BigDecimal monto;
        private Date fechaInicio;
        private Date fechaFin;

        public UltimaMembresiaView(String cliente, String plan,
                                   BigDecimal monto, Date fechaInicio, Date fechaFin) {
            this.cliente = cliente;
            this.plan = plan;
            this.monto = monto;
            this.fechaInicio = fechaInicio;
            this.fechaFin = fechaFin;
        }

        public String getCliente() { return cliente; }
        public String getPlan() { return plan; }
        public BigDecimal getMonto() { return monto; }
        public Date getFechaInicio() { return fechaInicio; }
        public Date getFechaFin() { return fechaFin; }
    }

    // DTO para top clientes por uso
    public static class ClienteUsoView {
        private String cliente;
        private long rutinas;
        private long dietas;
        private long total;

        public ClienteUsoView(String cliente, long rutinas, long dietas, long total) {
            this.cliente = cliente;
            this.rutinas = rutinas;
            this.dietas = dietas;
            this.total = total;
        }

        public String getCliente() { return cliente; }
        public long getRutinas() { return rutinas; }
        public long getDietas() { return dietas; }
        public long getTotal() { return total; }
    }

    @GetMapping
    public String verReportes(Model model) {

        // === KPIs principales ===

        // Total de clientes
        Number totalClientes = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM clientes"
        ).getSingleResult();

        // Clientes con membresía activa hoy
        Number clientesActivos = (Number) em.createNativeQuery(
                "SELECT COUNT(DISTINCT cm.id_cliente) " +
                        "FROM cliente_membresia cm " +
                        "WHERE CURRENT_DATE BETWEEN cm.fecha_inicio AND cm.fecha_fin"
        ).getSingleResult();

        int activos = clientesActivos.intValue();
        int totCli  = totalClientes.intValue();
        int inactivos = Math.max(totCli - activos, 0);

        // Total de rutinas y dietas
        Number totalRutinas = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM rutinas"
        ).getSingleResult();

        Number totalDietas = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM dietas"
        ).getSingleResult();

        // Ingresos del mes actual (sumando precio de membresías por fecha_inicio)
        BigDecimal ingresosMes = (BigDecimal) em.createNativeQuery(
                "SELECT COALESCE(SUM(m.precio), 0) " +
                        "FROM cliente_membresia cm " +
                        "JOIN membresias m ON m.id_membresia = cm.id_membresia " +
                        "WHERE date_trunc('month', cm.fecha_inicio) = date_trunc('month', CURRENT_DATE)"
        ).getSingleResult();

        model.addAttribute("totalClientes", totCli);
        model.addAttribute("clientesActivos", activos);
        model.addAttribute("clientesInactivos", inactivos);
        model.addAttribute("totalRutinas", ((Number) totalRutinas).intValue());
        model.addAttribute("totalDietas", ((Number) totalDietas).intValue());
        model.addAttribute("ingresosMes", ingresosMes);

        // === Tabla: últimas 10 membresías vendidas ===
        @SuppressWarnings("unchecked")
        List<Object[]> rowsMem = em.createNativeQuery(
                "SELECT c.nombre AS cliente, " +
                        "       COALESCE(mp.nombre_comercial, me.tipo) AS plan, " +
                        "       me.precio, cm.fecha_inicio, cm.fecha_fin " +
                        "FROM cliente_membresia cm " +
                        "JOIN clientes c ON c.id_cliente = cm.id_cliente " +
                        "JOIN membresias me ON me.id_membresia = cm.id_membresia " +
                        "LEFT JOIN membresia_plan mp ON mp.id_membresia = me.id_membresia " +
                        "ORDER BY cm.fecha_inicio DESC " +
                        "LIMIT 10"
        ).getResultList();

        List<UltimaMembresiaView> ultimas = new ArrayList<>();
        for (Object[] r : rowsMem) {
            String cliente = (String) r[0];
            String plan    = (String) r[1];
            BigDecimal monto = (BigDecimal) r[2];
            Date fecIni    = (Date) r[3];
            Date fecFin    = (Date) r[4];

            ultimas.add(new UltimaMembresiaView(cliente, plan, monto, fecIni, fecFin));
        }

        model.addAttribute("ultimasMembresias", ultimas);

        // === Tabla: top 10 clientes por uso (rutinas + dietas) ===
        @SuppressWarnings("unchecked")
        List<Object[]> rowsTop = em.createNativeQuery(
                "SELECT c.nombre, " +
                        "       COUNT(DISTINCT r.id_rutina) AS rutinas, " +
                        "       COUNT(DISTINCT d.id_dieta)   AS dietas " +
                        "FROM clientes c " +
                        "LEFT JOIN rutinas r ON r.id_cliente = c.id_cliente " +
                        "LEFT JOIN dietas  d ON d.id_cliente = c.id_cliente " +
                        "GROUP BY c.id_cliente, c.nombre " +
                        "ORDER BY (COUNT(DISTINCT r.id_rutina) + COUNT(DISTINCT d.id_dieta)) DESC " +
                        "LIMIT 10"
        ).getResultList();

        List<ClienteUsoView> topClientes = new ArrayList<>();
        for (Object[] r : rowsTop) {
            String nombreCli = (String) r[0];
            long rutinas = ((Number) r[1]).longValue();
            long dietas  = ((Number) r[2]).longValue();
            long total   = rutinas + dietas;

            topClientes.add(new ClienteUsoView(nombreCli, rutinas, dietas, total));
        }

        model.addAttribute("topClientes", topClientes);

        // Vista
        return "Administrador/reportes";
    }
}
