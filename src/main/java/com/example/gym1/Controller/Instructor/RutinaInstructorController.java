package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Ejercicio;
import com.example.gym1.Poo.Instructor;
import com.example.gym1.Poo.Rutina;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.hibernate.exception.SQLGrammarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Controlador completo para manejo de rutinas e detalles.
 * - Detecta dinámicamente el nombre de la columna PK en rutina_detalle para evitar referencias a "id" que no existan.
 * - Usa consultas nativas con mapeo defensivo.
 * - Todos los endpoints usan POST donde tu app ya espera POST (no PUT/DELETE desde JS).
 */
@Controller
@RequestMapping("/instructor/rutinas")
public class RutinaInstructorController {

    private static final Logger logger = LoggerFactory.getLogger(RutinaInstructorController.class);

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate txTemplate;
    private final PlatformTransactionManager txManager;

    // cache para el nombre de la columna id en rutina_detalle
    private volatile String cachedDetalleIdCol = null;

    @Autowired
    public RutinaInstructorController(PlatformTransactionManager txManager) {
        this.txManager = txManager;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    // Ejecuta una acción usando un EntityManager nuevo y su propia transacción local.
    private void runInIsolatedEm(Consumer<EntityManager> action) {
        EntityManagerFactory emf;
        try {
            emf = em.getEntityManagerFactory();
        } catch (Exception ex) {
            logger.warn("EntityManagerFactory no disponible, fallback a txTemplate: {}", ex.getMessage());
            txTemplate.execute(status -> {
                try {
                    action.accept(em);
                } catch (RuntimeException rex) {
                    logger.warn("Fallback action failed: {}", rex.getMessage(), rex);
                    throw rex;
                }
                return null;
            });
            return;
        }

        EntityManager emIsolated = null;
        EntityTransaction tx = null;
        try {
            emIsolated = emf.createEntityManager();
            tx = emIsolated.getTransaction();
            tx.begin();
            action.accept(emIsolated);
            tx.commit();
        } catch (RuntimeException ex) {
            try {
                if (tx != null && tx.isActive()) tx.rollback();
            } catch (Exception rbEx) {
                logger.warn("rollback failed on isolated tx: {}", rbEx.getMessage(), rbEx);
            }
            logger.warn("Isolated EM action failed: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            if (emIsolated != null && emIsolated.isOpen()) {
                try { emIsolated.close(); } catch (Exception ignored) {}
            }
        }
    }

    // Detecta la columna id de rutina_detalle (cacheada). Usa parámetro posicional para evitar problemas.
    private String getDetalleIdColumn() {
        if (cachedDetalleIdCol != null) return cachedDetalleIdCol;
        synchronized (this) {
            if (cachedDetalleIdCol != null) return cachedDetalleIdCol;
            cachedDetalleIdCol = detectIdColumn(em, "rutina_detalle");
            if (cachedDetalleIdCol != null) {
                logger.info("Columna ID detectada para rutina_detalle: {}", cachedDetalleIdCol);
            } else {
                logger.info("No se detectó columna ID 'convencional' en rutina_detalle; se usará mapeo flexible.");
            }
            return cachedDetalleIdCol;
        }
    }

    // Detecta nombre de columna posible en información del esquema (usa parámetro posicional)
    private String detectIdColumn(EntityManager emLocal, String tableName) {
        List<String> candidates = Arrays.asList(
                "id", "detalle_id", "id_detalle", "id_rutina_detalle", "rutina_detalle_id", "pk", "idrutina_detalle", "id_rutina"
        );
        try {
            @SuppressWarnings("unchecked")
            List<Object> cols = emLocal.createNativeQuery(
                    "SELECT column_name FROM information_schema.columns WHERE table_name = ?1 AND table_schema = current_schema()"
            ).setParameter(1, tableName).getResultList();
            Set<String> colSet = new HashSet<>();
            for (Object c : cols) if (c != null) colSet.add(c.toString().toLowerCase(Locale.ROOT));

            for (String cand : candidates) {
                if (colSet.contains(cand.toLowerCase(Locale.ROOT))) {
                    logger.debug("Found id column '{}' for table {}", cand, tableName);
                    return cand;
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not detect columns for {}: {}", tableName, ex.getMessage());
        }
        return null;
    }

    @GetMapping
    public String listarRutinas(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Instructor instructor = em.createQuery("SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                .setParameter("uid", uid)
                .getResultStream().findFirst().orElse(null);
        if (instructor == null) return "redirect:/instructor";

        List<Rutina> rutinas = em.createQuery("SELECT r FROM Rutina r WHERE r.instructor.id = :iid ORDER BY r.nombre", Rutina.class)
                .setParameter("iid", instructor.getId())
                .getResultList();

        List<RutinaView> views = new ArrayList<>();

        // Obtener (y cachear) la columna id de rutina_detalle
        String detalleIdCol = getDetalleIdColumn();

        for (Rutina r : rutinas) {
            RutinaView rv = new RutinaView();
            rv.rutina = r;
            rv.detalles = new ArrayList<>();

            boolean success = false;

            // candidate orders: priorizar id_ejercicio y fallback si detectamos id de detalle
            List<String> candidateOrders = new ArrayList<>();
            candidateOrders.add("rd.id_ejercicio");
            if (detalleIdCol != null && !detalleIdCol.isEmpty()) candidateOrders.add("rd." + detalleIdCol);
            candidateOrders.add("");

            for (String orderClause : candidateOrders) {
                String orderSql = (orderClause != null && !orderClause.trim().isEmpty()) ? (" ORDER BY " + orderClause) : "";
                String selectIdPart = (detalleIdCol != null && !detalleIdCol.isEmpty()) ? ("rd." + detalleIdCol + " as detalle_id, ") : "";
                String sql = "SELECT " + selectIdPart +
                        "rd.id_ejercicio, rd.dia_semana, rd.series, rd.repeticiones, e.nombre as ejercicio_nombre " +
                        "FROM rutina_detalle rd LEFT JOIN ejercicios e ON e.id_ejercicio = rd.id_ejercicio " +
                        "WHERE rd.id_rutina = ?1 " + orderSql;
                try {
                    @SuppressWarnings("unchecked")
                    List<Object[]> rows = em.createNativeQuery(sql)
                            .setParameter(1, r.getId())
                            .getResultList();

                    if (rows != null) {
                        for (Object[] row : rows) {
                            Detalle d = new Detalle();
                            int baseIdx = 0;
                            if (detalleIdCol != null && !detalleIdCol.isEmpty()) {
                                d.detalleId = (row.length > 0 && row[0] instanceof Number) ? ((Number) row[0]).intValue() : null;
                                baseIdx = 1;
                            } else {
                                d.detalleId = null;
                                baseIdx = 0;
                            }
                            d.ejercicioId = (row.length > baseIdx && row[baseIdx] instanceof Number) ? ((Number) row[baseIdx]).intValue() : null;
                            d.diaSemana = (row.length > baseIdx + 1 && row[baseIdx + 1] != null) ? row[baseIdx + 1].toString() : "";
                            d.series = (row.length > baseIdx + 2 && row[baseIdx + 2] instanceof Number) ? ((Number) row[baseIdx + 2]).intValue() : null;
                            d.repeticiones = (row.length > baseIdx + 3 && row[baseIdx + 3] instanceof Number) ? ((Number) row[baseIdx + 3]).intValue() : null;
                            d.ejercicioNombre = (row.length > baseIdx + 4 && row[baseIdx + 4] != null) ? row[baseIdx + 4].toString() : "Ejercicio";
                            rv.detalles.add(d);
                        }
                    }
                    success = true;
                    break;
                } catch (SQLGrammarException sge) {
                    logger.debug("ORDER BY no válido (intentando siguiente): {} -> {}", orderClause, sge.getMessage());
                } catch (Exception ex) {
                    logger.debug("Ignored exception while trying ordering variant: {}", ex.getMessage());
                }
            }

            if (!success) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Object[]> rows2 = em.createNativeQuery("SELECT * FROM rutina_detalle WHERE id_rutina = ?1")
                            .setParameter(1, r.getId())
                            .getResultList();
                    if (rows2 != null) {
                        for (Object[] row : rows2) {
                            Detalle d = mapRowToDetalleFlexible(row, r.getId());
                            rv.detalles.add(d);
                        }
                        success = true;
                    }
                } catch (Exception ex) {
                    logger.debug("Fallback SELECT * failed: {}", ex.getMessage());
                }
            }

            StringJoiner sj = new StringJoiner(" ");
            if (r.getNombre() != null) sj.add(r.getNombre());
            if (r.getDescripcion() != null) sj.add(r.getDescripcion());
            if (rv.detalles != null) {
                for (Detalle d : rv.detalles) {
                    if (d.ejercicioNombre != null && !d.ejercicioNombre.isEmpty()) sj.add(d.ejercicioNombre);
                }
            }
            rv.searchText = sj.toString();
            views.add(rv);
        }

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();

        model.addAttribute("rutinasViews", views);
        model.addAttribute("ejercicios", ejercicios);
        model.addAttribute("nombre", session.getAttribute("unombre"));
        return "instructor/rutinas";
    }

    @GetMapping("/crear")
    public String crearForm(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();
        List<String> musculos = new ArrayList<>();
        for (Ejercicio e : ejercicios) {
            if (e.getMusculo() != null && !e.getMusculo().isBlank() && !musculos.contains(e.getMusculo())) musculos.add(e.getMusculo());
        }

        model.addAttribute("rutina", new Rutina());
        model.addAttribute("ejercicios", ejercicios);
        model.addAttribute("musculos", musculos);
        model.addAttribute("detallesJson", "[]");
        return "instructor/rutina-form";
    }

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Integer id, HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";
        Rutina rutina = em.find(Rutina.class, id);
        if (rutina == null) return "redirect:/instructor/rutinas";

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();
        List<String> musculos = new ArrayList<>();
        for (Ejercicio e : ejercicios) {
            if (e.getMusculo() != null && !e.getMusculo().isBlank() && !musculos.contains(e.getMusculo())) musculos.add(e.getMusculo());
        }

        List<Map<String,Object>> detalles = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            List<Object[]> rows = em.createNativeQuery(
                            "SELECT rd.*, e.nombre as ejercicio_nombre FROM rutina_detalle rd LEFT JOIN ejercicios e ON e.id_ejercicio = rd.id_ejercicio WHERE rd.id_rutina = ?1 ORDER BY rd.id_ejercicio")
                    .setParameter(1, id)
                    .getResultList();

            if (rows != null) {
                for (Object[] row : rows) {
                    Integer detalleId = null;
                    Integer ejercicioId = null;
                    String dia = "";
                    Integer series = null;
                    Integer repes = null;

                    // intentar mapear conservadoramente
                    if (row.length >= 5) {
                        // buscar primero una columna numérica plausible para ejercicioId
                        // (no asumimos posición fija en este fallback)
                        for (Object col : row) {
                            if (col instanceof Number && ejercicioId == null) {
                                ejercicioId = ((Number) col).intValue();
                            }
                        }
                        // intenta asignaciones por posición si no están vacías
                        // (este bloque es un simple fallback para mostrar datos en el form de edición)
                        if (row.length >= 2) {
                            ejercicioId = (row[1] instanceof Number) ? ((Number) row[1]).intValue() : ejercicioId;
                        }
                        dia = row.length > 2 && row[2] != null ? row[2].toString() : dia;
                        series = row.length > 3 && row[3] instanceof Number ? ((Number) row[3]).intValue() : series;
                        repes = row.length > 4 && row[4] instanceof Number ? ((Number) row[4]).intValue() : repes;
                    }

                    String nombreEj = "";
                    String musculo = "";
                    if (ejercicioId != null) {
                        try {
                            Ejercicio ej = em.find(Ejercicio.class, ejercicioId);
                            if (ej != null) {
                                nombreEj = ej.getNombre();
                                musculo = ej.getMusculo();
                            }
                        } catch (Exception ignored) { }
                    }

                    Map<String,Object> m = new HashMap<>();
                    m.put("detalleId", detalleId);
                    m.put("id", ejercicioId);
                    m.put("nombre", nombreEj);
                    m.put("musculo", musculo);
                    m.put("serie", series);
                    m.put("repeticiones", repes);
                    m.put("dia", dia);
                    detalles.add(m);
                }
            }
        } catch (Exception ex) {
            logger.debug("Could not load detalles for rutina {}: {}", id, ex.getMessage());
        }

        String detallesJson = toJsonArray(detalles);

        model.addAttribute("rutina", rutina);
        model.addAttribute("ejercicios", ejercicios);
        model.addAttribute("musculos", musculos);
        model.addAttribute("detallesJson", detallesJson);
        return "instructor/rutina-form";
    }

    @PostMapping("/guardar")
    public String guardarRutina(@ModelAttribute Rutina rutina,
                                @RequestParam(value = "ejercicioId", required = false) List<Integer> ejercicioIds,
                                @RequestParam(value = "series", required = false) List<Integer> seriesList,
                                @RequestParam(value = "repeticiones", required = false) List<Integer> repesList,
                                @RequestParam(value = "dia", required = false) List<String> dias,
                                HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        final Rutina rutinaToPersist = rutina;

        Integer rutinaId = txTemplate.execute((TransactionCallback<Integer>) status -> {
            if (rutinaToPersist.getId() == null) {
                try {
                    Instructor instr = em.createQuery("SELECT i FROM Instructor i WHERE i.usuario.id = :uid", Instructor.class)
                            .setParameter("uid", uid)
                            .getResultStream().findFirst().orElse(null);
                    if (instr != null) rutinaToPersist.setInstructor(instr);
                } catch (Exception ignored) {}
                em.persist(rutinaToPersist);
                em.flush();
            } else {
                em.merge(rutinaToPersist);
                em.flush();
            }

            try {
                em.createNativeQuery("DELETE FROM rutina_detalle WHERE id_rutina = ?1")
                        .setParameter(1, rutinaToPersist.getId())
                        .executeUpdate();
            } catch (Exception ignored) {}

            return rutinaToPersist.getId();
        });

        if (ejercicioIds != null && !ejercicioIds.isEmpty() && rutinaId != null) {
            final Integer finalRutinaId = rutinaId;
            int n = ejercicioIds.size();
            for (int i = 0; i < n; i++) {
                final Integer ejId = ejercicioIds.get(i);
                final Integer s = (seriesList != null && seriesList.size() > i) ? seriesList.get(i) : null;
                final Integer r = (repesList != null && repesList.size() > i) ? repesList.get(i) : null;
                final String dia = (dias != null && dias.size() > i) ? dias.get(i) : null;

                try {
                    txTemplate.execute(status -> {
                        em.createNativeQuery("INSERT INTO rutina_detalle (id_rutina, id_ejercicio, dia_semana, series, repeticiones) VALUES (?1, ?2, ?3, ?4, ?5)")
                                .setParameter(1, finalRutinaId)
                                .setParameter(2, ejId)
                                .setParameter(3, dia)
                                .setParameter(4, s)
                                .setParameter(5, r)
                                .executeUpdate();
                        return null;
                    });
                } catch (Exception ex) {
                    logger.warn("no se pudo insertar detalle (ejercicioId={}): {}", ejId, ex.getMessage(), ex);
                }
            }
        }

        return "redirect:/instructor/rutinas";
    }

    @PostMapping("/{rutinaId}/detalles/agregar")
    public String agregarDetalle(@PathVariable Integer rutinaId,
                                 @RequestParam("ejercicioId") Integer ejercicioId,
                                 @RequestParam(value = "diaSemana", required = false) String diaSemana,
                                 @RequestParam(value = "series", required = false) Integer series,
                                 @RequestParam(value = "repeticiones", required = false) Integer repeticiones,
                                 HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";
        final Integer rid = rutinaId;
        final Integer ejId = ejercicioId;
        final Integer s = series;
        final Integer r = repeticiones;
        final String dia = diaSemana;

        runInIsolatedEm(emIso -> {
            emIso.createNativeQuery("INSERT INTO rutina_detalle (id_rutina, id_ejercicio, dia_semana, series, repeticiones) VALUES (?1, ?2, ?3, ?4, ?5)")
                    .setParameter(1, rid)
                    .setParameter(2, ejId)
                    .setParameter(3, dia)
                    .setParameter(4, s)
                    .setParameter(5, r)
                    .executeUpdate();
        });

        return "redirect:/instructor/rutinas";
    }

    @PostMapping("/{rutinaId}/detalles/eliminar/{detalleId}")
    public String eliminarDetallePorId(@PathVariable Integer rutinaId, @PathVariable Integer detalleId, HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";
        final Integer did = detalleId;

        runInIsolatedEm(emIso -> {
            String detCol = getDetalleIdColumn();
            if (detCol != null && !detCol.isEmpty()) {
                emIso.createNativeQuery("DELETE FROM rutina_detalle WHERE " + detCol + " = ?1")
                        .setParameter(1, did)
                        .executeUpdate();
            } else {
                // fallback: intentar con 'id'
                emIso.createNativeQuery("DELETE FROM rutina_detalle WHERE id = ?1").setParameter(1, did).executeUpdate();
            }
        });

        return "redirect:/instructor/rutinas";
    }

    @PostMapping("/{rutinaId}/detalles/eliminar")
    public String eliminarDetalleFallback(@PathVariable Integer rutinaId,
                                          @RequestParam(value = "ejercicioId", required = false) Integer ejercicioId,
                                          @RequestParam(value = "diaSemana", required = false) String diaSemana,
                                          HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";
        final Integer rid = rutinaId;
        final Integer ejId = ejercicioId;
        final String dia = diaSemana;

        runInIsolatedEm(emIso -> {
            if (ejId != null && dia != null) {
                emIso.createNativeQuery("DELETE FROM rutina_detalle WHERE id_rutina = ?1 AND id_ejercicio = ?2 AND dia_semana = ?3")
                        .setParameter(1, rid)
                        .setParameter(2, ejId)
                        .setParameter(3, dia)
                        .executeUpdate();
            }
        });

        return "redirect:/instructor/rutinas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarRutina(@PathVariable Integer id, HttpSession session) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";
        final Integer rid = id;

        runInIsolatedEm(emIso -> {
            emIso.createNativeQuery("DELETE FROM rutina_detalle WHERE id_rutina = ?1").setParameter(1, rid).executeUpdate();
            emIso.createQuery("DELETE FROM Rutina r WHERE r.id = :id").setParameter("id", rid).executeUpdate();
        });

        return "redirect:/instructor/rutinas";
    }

    @PostMapping("/{rutinaId}/detalles/guardar")
    public String guardarDetallesYRutina(
            @PathVariable Integer rutinaId,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "detalleId", required = false) List<Integer> detalleIds,
            @RequestParam(value = "ejercicioId", required = false) List<Integer> ejercicioIds,
            @RequestParam(value = "dia", required = false) List<String> dias,
            @RequestParam(value = "series", required = false) List<Integer> seriesList,
            @RequestParam(value = "repeticiones", required = false) List<Integer> repesList,
            HttpSession session
    ) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        final Integer rid = rutinaId;
        final String n = nombre;
        final String d = descripcion;
        final String detCol = getDetalleIdColumn();

        if ((n != null && !n.isBlank()) || (d != null)) {
            txTemplate.execute(status -> {
                em.createQuery("UPDATE Rutina r SET r.nombre = :n, r.descripcion = :d WHERE r.id = :id")
                        .setParameter("n", n != null ? n : "")
                        .setParameter("d", d != null ? d : "")
                        .setParameter("id", rid)
                        .executeUpdate();
                return null;
            });
        }

        if (detalleIds != null && !detalleIds.isEmpty()) {
            int nRows = detalleIds.size();
            for (int i = 0; i < nRows; i++) {
                final int idx = i;
                final Integer did = detalleIds.get(i);
                final Integer ejId = (ejercicioIds != null && ejercicioIds.size() > i) ? ejercicioIds.get(i) : null;
                final String dia = (dias != null && dias.size() > i) ? dias.get(i) : null;
                final Integer s = (seriesList != null && seriesList.size() > i) ? seriesList.get(i) : null;
                final Integer rVal = (repesList != null && repesList.size() > i) ? repesList.get(i) : null;

                try {
                    txTemplate.execute(status -> {
                        if (did != null) {
                            // usar la columna detectada si existe
                            if (detCol != null && !detCol.isEmpty()) {
                                em.createNativeQuery("UPDATE rutina_detalle SET series = ?1, repeticiones = ?2 WHERE " + detCol + " = ?3")
                                        .setParameter(1, s)
                                        .setParameter(2, rVal)
                                        .setParameter(3, did)
                                        .executeUpdate();
                            } else {
                                em.createNativeQuery("UPDATE rutina_detalle SET series = ?1, repeticiones = ?2 WHERE id = ?3")
                                        .setParameter(1, s)
                                        .setParameter(2, rVal)
                                        .setParameter(3, did)
                                        .executeUpdate();
                            }
                        } else if (ejId != null && dia != null) {
                            em.createNativeQuery("UPDATE rutina_detalle SET series = ?1, repeticiones = ?2 WHERE id_rutina = ?3 AND id_ejercicio = ?4 AND dia_semana = ?5")
                                    .setParameter(1, s)
                                    .setParameter(2, rVal)
                                    .setParameter(3, rid)
                                    .setParameter(4, ejId)
                                    .setParameter(5, dia)
                                    .executeUpdate();
                        }
                        return null;
                    });
                } catch (Exception ex) {
                    logger.warn("fallo tx al actualizar detalle (fila {}): {}", idx, ex.getMessage(), ex);
                }
            }
        }

        return "redirect:/instructor/rutinas";
    }

    // Mapeo flexible cuando la estructura de columnas varía
    private Detalle mapRowToDetalleFlexible(Object[] row, Integer rutinaId) {
        Detalle d = new Detalle();
        try {
            if (row.length >= 5) {
                Integer possibleId = null;
                if (row[0] instanceof Number) {
                    int v = ((Number) row[0]).intValue();
                    if (v > 0 && v != rutinaId) possibleId = v;
                }
                if (possibleId != null) {
                    d.detalleId = possibleId;
                    d.ejercicioId = (row[1] instanceof Number) ? ((Number) row[1]).intValue() : null;
                    d.diaSemana = row.length > 2 && row[2] != null ? row[2].toString() : "";
                    d.series = row.length > 3 && row[3] instanceof Number ? ((Number) row[3]).intValue() : null;
                    d.repeticiones = row.length > 4 && row[4] instanceof Number ? ((Number) row[4]).intValue() : null;
                } else {
                    d.detalleId = null;
                    d.ejercicioId = (row[1] instanceof Number) ? ((Number) row[1]).intValue() : null;
                    d.diaSemana = row.length > 2 && row[2] != null ? row[2].toString() : "";
                    d.series = row.length > 3 && row[3] instanceof Number ? ((Number) row[3]).intValue() : null;
                    d.repeticiones = row.length > 4 && row[4] instanceof Number ? ((Number) row[4]).intValue() : null;
                }
            } else {
                d.detalleId = null;
                d.ejercicioId = null;
                d.diaSemana = "";
            }
        } catch (Exception ex) {
            d.detalleId = null;
            d.ejercicioId = null;
            d.diaSemana = "";
        }
        d.ejercicioNombre = (d.ejercicioId != null) ? tryFindEjercicioNombre(d.ejercicioId) : "Ejercicio";
        return d;
    }

    private String tryFindEjercicioNombre(Integer id) {
        if (id == null) return "Ejercicio";
        try {
            Ejercicio ej = em.find(Ejercicio.class, id);
            if (ej != null && ej.getNombre() != null) return ej.getNombre();
        } catch (Exception ignored) {}
        return "Ejercicio";
    }

    // Clases internas usadas por la vista
    public static class RutinaView {
        public Rutina rutina;
        public List<Detalle> detalles;
        public String searchText;
        public boolean editing = false;
    }

    public static class Detalle {
        public Integer detalleId;
        public Integer ejercicioId;
        public String ejercicioNombre;
        public String diaSemana;
        public Integer series;
        public Integer repeticiones;
    }

    private String toJsonArray(List<Map<String,Object>> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Map<String,Object> m : list) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            boolean firstField = true;
            for (Map.Entry<String,Object> e : m.entrySet()) {
                if (!firstField) sb.append(",");
                firstField = false;
                sb.append("\"").append(escapeJson(e.getKey())).append("\":");
                Object v = e.getValue();
                if (v == null) {
                    sb.append("null");
                } else if (v instanceof Number) {
                    sb.append(v.toString());
                } else {
                    sb.append("\"").append(escapeJson(v.toString())).append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}