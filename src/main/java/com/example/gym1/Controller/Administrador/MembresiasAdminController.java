package com.example.gym1.Controller.Administrador;

import com.example.gym1.Poo.Membresia;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/membresias")
public class MembresiasAdminController {

    @PersistenceContext
    private EntityManager em;

    // -------- DTO para la tabla de membresías --------
    public static class MembresiaView {
        private Integer id;
        private String tipo;
        private BigDecimal precio;
        private String plan;

        public MembresiaView(Integer id, String tipo, BigDecimal precio, String plan) {
            this.id = id;
            this.tipo = tipo;
            this.precio = precio;
            this.plan = plan;
        }

        public Integer getId() { return id; }
        public String getTipo() { return tipo; }
        public BigDecimal getPrecio() { return precio; }
        public String getPlan() { return plan; }
    }

    // -------- DTO simple para "pagos" (historial) --------
    public static class PagoView {
        private Integer id;      // solo un correlativo visual
        private String cliente;
        private String plan;
        private BigDecimal monto;
        private Date fecha;

        public PagoView(Integer id, String cliente, String plan,
                        BigDecimal monto, Date fecha) {
            this.id = id;
            this.cliente = cliente;
            this.plan = plan;
            this.monto = monto;
            this.fecha = fecha;
        }

        public Integer getId() { return id; }
        public String getCliente() { return cliente; }
        public String getPlan() { return plan; }
        public BigDecimal getMonto() { return monto; }
        public Date getFecha() { return fecha; }
    }

    // -------- GET: /admin/membresias  (lista + historial) --------
    @GetMapping
    public String verMembresias(Model model) {

        // 1) Membresías con su plan (básico/premium) desde membresia_plan
        @SuppressWarnings("unchecked")
        List<Object[]> rowsMem = em.createNativeQuery(
                "SELECT m.id_membresia, m.tipo, m.precio, " +
                        "       COALESCE(mp.plan, 'basico') AS plan " +
                        "FROM membresias m " +
                        "LEFT JOIN membresia_plan mp ON mp.id_membresia = m.id_membresia " +
                        "ORDER BY m.id_membresia"
        ).getResultList();

        List<MembresiaView> membresias = new ArrayList<>();
        for (Object[] r : rowsMem) {
            Integer id = ((Number) r[0]).intValue();
            String tipo = (String) r[1];
            BigDecimal precio = (BigDecimal) r[2];
            String plan = (String) r[3];

            membresias.add(new MembresiaView(id, tipo, precio, plan));
        }

        // 2) Historial de "pagos" a partir de cliente_membresia
        @SuppressWarnings("unchecked")
        List<Object[]> rowsPagos = em.createNativeQuery(
                "SELECT c.nombre AS cliente, " +
                        "       COALESCE(mp.nombre_comercial, me.tipo) AS plan, " +
                        "       me.precio, " +
                        "       cm.fecha_fin " +
                        "FROM cliente_membresia cm " +
                        "JOIN clientes c ON c.id_cliente = cm.id_cliente " +
                        "JOIN membresias me ON me.id_membresia = cm.id_membresia " +
                        "LEFT JOIN membresia_plan mp ON mp.id_membresia = me.id_membresia " +
                        "ORDER BY cm.fecha_fin DESC " +
                        "LIMIT 20"
        ).getResultList();

        List<PagoView> pagos = new ArrayList<>();
        int correlativo = 1;
        for (Object[] r : rowsPagos) {
            String cliente = (String) r[0];
            String plan = (String) r[1];
            BigDecimal monto = (BigDecimal) r[2];
            Date fecha = (Date) r[3];

            pagos.add(new PagoView(correlativo++, cliente, plan, monto, fecha));
        }

        model.addAttribute("membresias", membresias);
        model.addAttribute("pagos", pagos);

        return "Administrador/membresias";
    }

    // -------- NUEVA MEMBRESÍA (FORM) --------
    @GetMapping("/nueva")
    public String nuevaMembresia(Model model) {
        model.addAttribute("membresia", new Membresia());
        model.addAttribute("modo", "nueva");
        return "Administrador/membresias-form";
    }

    // -------- EDITAR MEMBRESÍA (FORM) --------
    @GetMapping("/{id}/editar")
    public String editarMembresia(@PathVariable Integer id, Model model) {
        Membresia m = em.find(Membresia.class, id);
        if (m == null) {
            return "redirect:/admin/membresias";
        }
        model.addAttribute("membresia", m);
        model.addAttribute("modo", "editar");
        return "Administrador/membresias-form";
    }

    // -------- GUARDAR (CREAR / EDITAR) --------
    @Transactional
    @PostMapping("/guardar")
    public String guardarMembresia(
            @ModelAttribute Membresia membresia
    ) {
        if (membresia.getId() == null) {
            // nueva
            em.persist(membresia);
        } else {
            // edición
            em.merge(membresia);
        }
        return "redirect:/admin/membresias";
    }

    // -------- ELIMINAR --------
    @Transactional
    @PostMapping("/{id}/eliminar")
    public String eliminarMembresia(@PathVariable Integer id) {
        Membresia m = em.find(Membresia.class, id);
        if (m != null) {
            em.remove(m);
        }
        return "redirect:/admin/membresias";
    }
}
