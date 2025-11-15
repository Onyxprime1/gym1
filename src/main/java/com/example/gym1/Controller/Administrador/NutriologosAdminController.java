package com.example.gym1.Controller.Administrador;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/nutriologos")
public class NutriologosAdminController {

    @PersistenceContext
    private EntityManager em;

    // DTO para la vista
    public static class NutriologoAdminView {
        private Integer id;
        private String nombre;
        private String especialidad;
        private String correo;
        private int totalDietas;
        private boolean tieneUsuario;

        public NutriologoAdminView(Integer id, String nombre, String especialidad,
                                   String correo, int totalDietas, boolean tieneUsuario) {
            this.id = id;
            this.nombre = nombre;
            this.especialidad = especialidad;
            this.correo = correo;
            this.totalDietas = totalDietas;
            this.tieneUsuario = tieneUsuario;
        }

        public Integer getId() { return id; }
        public String getNombre() { return nombre; }
        public String getEspecialidad() { return especialidad; }
        public String getCorreo() { return correo; }
        public int getTotalDietas() { return totalDietas; }
        public boolean isTieneUsuario() { return tieneUsuario; }
    }

    @GetMapping
    public String listarNutriologos(Model model) {

        // Totales generales
        Number totalNutriologos = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM nutriologos"
        ).getSingleResult();

        Number totalConUsuario = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM nutriologos WHERE id_usuario IS NOT NULL"
        ).getSingleResult();

        Number totalDietas = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM dietas"
        ).getSingleResult();

        int conUsuario = totalConUsuario.intValue();
        int sinUsuario = totalNutriologos.intValue() - conUsuario;

        model.addAttribute("totalNutriologos", totalNutriologos.intValue());
        model.addAttribute("totalConUsuario", conUsuario);
        model.addAttribute("totalSinUsuario", sinUsuario);
        model.addAttribute("totalDietas", totalDietas.intValue());

        // Lista detallada
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT n.id_nutriologo, n.nombre, n.especialidad, " +
                        "       u.correo, " +
                        "       COUNT(d.id_dieta) AS total_dietas " +
                        "FROM nutriologos n " +
                        "LEFT JOIN usuarios u ON u.id_usuario = n.id_usuario " +
                        "LEFT JOIN dietas d ON d.id_nutriologo = n.id_nutriologo " +
                        "GROUP BY n.id_nutriologo, n.nombre, n.especialidad, u.correo " +
                        "ORDER BY n.id_nutriologo"
        ).getResultList();

        List<NutriologoAdminView> nutriologos = new ArrayList<>();

        for (Object[] r : rows) {
            Integer id          = ((Number) r[0]).intValue();
            String  nombre      = (String) r[1];
            String  especialidad= (String) r[2];
            String  correo      = (String) r[3]; // puede ser null
            int     totalD      = ((Number) r[4]).intValue();

            boolean tieneUsuario = (correo != null);

            nutriologos.add(
                    new NutriologoAdminView(
                            id, nombre, especialidad, correo, totalD, tieneUsuario
                    )
            );
        }

        model.addAttribute("nutriologos", nutriologos);

        // Vista: templates/Administrador/nutriologos.html
        return "Administrador/nutriologos";
    }
}
