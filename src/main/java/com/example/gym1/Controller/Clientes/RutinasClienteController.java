package com.example.gym1.Controller.Clientes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class RutinasClienteController {

    @PersistenceContext
    private EntityManager em;

    // DTO para mostrar cada ejercicio por día
    public static class RutinaDiaItem {
        private String diaSemana;
        private String rutina;
        private String ejercicio;
        private Integer series;
        private Integer repeticiones;
        private String musculo;
        private String linkVideo;

        public RutinaDiaItem(String diaSemana, String rutina, String ejercicio,
                             Integer series, Integer repeticiones,
                             String musculo, String linkVideo) {
            this.diaSemana = diaSemana;
            this.rutina = rutina;
            this.ejercicio = ejercicio;
            this.series = series;
            this.repeticiones = repeticiones;
            this.musculo = musculo;
            this.linkVideo = linkVideo;
        }

        public String getDiaSemana() { return diaSemana; }
        public String getRutina() { return rutina; }
        public String getEjercicio() { return ejercicio; }
        public Integer getSeries() { return series; }
        public Integer getRepeticiones() { return repeticiones; }
        public String getMusculo() { return musculo; }
        public String getLinkVideo() { return linkVideo; }
    }

    @GetMapping("/rutinas")
    public String verRutinas(HttpSession session, Model model) {

        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) {
            return "redirect:/login";
        }

        Integer idCliente = prepararModeloCliente(uid, session, model);
        if (idCliente == null) {
            model.addAttribute("rutinaDias", new ArrayList<RutinaDiaItem>());
            return "Clientes/rutinaCliente";
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                        "SELECT ru.nombre        AS rutina, " +
                                "       rd.dia_semana    AS dia, " +
                                "       e.nombre         AS ejercicio, " +
                                "       rd.series, rd.repeticiones, " +
                                "       e.musculo, e.link_video " +
                                "FROM rutinas ru " +
                                "JOIN rutina_detalle rd ON rd.id_rutina = ru.id_rutina " +
                                "JOIN ejercicios e      ON e.id_ejercicio = rd.id_ejercicio " +
                                "WHERE ru.id_cliente = :cid " +
                                "ORDER BY " +
                                "  CASE rd.dia_semana " +
                                "    WHEN 'Lunes' THEN 1 " +
                                "    WHEN 'Martes' THEN 2 " +
                                "    WHEN 'Miércoles' THEN 3 " +
                                "    WHEN 'Miercoles' THEN 3 " +
                                "    WHEN 'Jueves' THEN 4 " +
                                "    WHEN 'Viernes' THEN 5 " +
                                "    WHEN 'Sábado' THEN 6 " +
                                "    WHEN 'Sabado' THEN 6 " +
                                "    WHEN 'Domingo' THEN 7 " +
                                "    ELSE 8 END, " +
                                "  e.nombre"
                ).setParameter("cid", idCliente)
                .getResultList();

        List<RutinaDiaItem> lista = new ArrayList<>();
        for (Object[] r : rows) {
            String rutina   = (String) r[0];
            String dia      = (String) r[1];
            String ejercicio= (String) r[2];
            Integer series  = r[3] != null ? ((Number) r[3]).intValue() : null;
            Integer reps    = r[4] != null ? ((Number) r[4]).intValue() : null;
            String musculo  = (String) r[5];
            String link     = (String) r[6];

            lista.add(new RutinaDiaItem(dia, rutina, ejercicio, series, reps, musculo, link));
        }

        model.addAttribute("rutinaDias", lista);
        return "Clientes/rutinaCliente";
    }

    // ===== helper para nombre, plan, vigencia, etc. =====
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
        String estado   = "Sin membresía";
        boolean vigente = false;

        if (mem != null) {
            Date fin = (Date) mem[2];
            if (fin != null) {
                LocalDate fechaFin = fin.toLocalDate();
                vigente  = !fechaFin.isBefore(LocalDate.now());
                proxPago = fechaFin.toString();
                estado   = vigente ? "Activa" : "Vencida";
            }
        }

        model.addAttribute("planNivel", "Básico"); // o ajustar si usas premium
        model.addAttribute("vigente", vigente);
        model.addAttribute("proxPago", proxPago);
        model.addAttribute("estado", estado);

        return idCliente;
    }
}
