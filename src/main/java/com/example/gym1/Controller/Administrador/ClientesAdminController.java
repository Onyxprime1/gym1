package com.example.gym1.Controller.Administrador;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/clientes")
public class ClientesAdminController {

    @PersistenceContext
    private EntityManager em;

    // DTO para la vista
    public static class ClienteAdminView {
        private Integer id;
        private String nombre;
        private Integer edad;
        private BigDecimal peso;
        private BigDecimal altura;
        private String correo;
        private String plan;
        private boolean vigente;

        public ClienteAdminView(Integer id, String nombre, Integer edad,
                                BigDecimal peso, BigDecimal altura,
                                String correo, String plan, boolean vigente) {
            this.id = id;
            this.nombre = nombre;
            this.edad = edad;
            this.peso = peso;
            this.altura = altura;
            this.correo = correo;
            this.plan = plan;
            this.vigente = vigente;
        }

        public Integer getId() { return id; }
        public String getNombre() { return nombre; }
        public Integer getEdad() { return edad; }
        public BigDecimal getPeso() { return peso; }
        public BigDecimal getAltura() { return altura; }
        public String getCorreo() { return correo; }
        public String getPlan() { return plan; }
        public boolean isVigente() { return vigente; }
    }

    @GetMapping
    public String listarClientes(Model model) {

        // Totales
        Number totalClientes = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM clientes"
        ).getSingleResult();

        // Clientes con membresía activa (vista v_membresia_activa que ya creaste)
        Number totalActivos = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM v_membresia_activa"
        ).getSingleResult();

        int activos = totalActivos.intValue();
        int sinPlan = totalClientes.intValue() - activos;

        model.addAttribute("totalClientes", totalClientes.intValue());
        model.addAttribute("totalActivos", activos);
        model.addAttribute("totalSinPlan", sinPlan);

        // Lista de clientes con datos básicos + plan
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT c.id_cliente, c.nombre, c.edad, c.peso, c.altura, " +
                        "       u.correo, " +
                        "       COALESCE(v.nombre_plan, 'Sin plan') AS plan, " +
                        "       CASE WHEN v.id_cliente IS NOT NULL THEN TRUE ELSE FALSE END AS vigente " +
                        "FROM clientes c " +
                        "LEFT JOIN usuarios u ON u.id_usuario = c.id_usuario " +
                        "LEFT JOIN v_membresia_activa v ON v.id_cliente = c.id_cliente " +
                        "ORDER BY c.id_cliente"
        ).getResultList();

        List<ClienteAdminView> clientes = new ArrayList<>();

        for (Object[] r : rows) {
            Integer id        = ((Number) r[0]).intValue();
            String  nombre    = (String) r[1];
            Integer edad      = (r[2] != null) ? ((Number) r[2]).intValue() : null;
            BigDecimal peso   = (BigDecimal) r[3];
            BigDecimal altura = (BigDecimal) r[4];
            String  correo    = (String) r[5];
            String  plan      = (String) r[6];
            boolean vigente   = (r[7] != null) && (Boolean) r[7];

            clientes.add(new ClienteAdminView(
                    id, nombre, edad, peso, altura, correo, plan, vigente
            ));
        }

        model.addAttribute("clientes", clientes);

        // Vista: src/main/resources/templates/Administrador/clientes.html
        return "Administrador/clientes";
    }
}
