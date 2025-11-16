package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Ejercicio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controlador de ejercicios — usa EntityManager.
 * NO MODIFICA com.example.gym1.Poo ni com.example.gym1.Service.YouTubeService.
 */
@Controller
@RequestMapping("/instructor/ejercicios")
public class EjercicioInstructorController {

    private static final Logger log = LoggerFactory.getLogger(EjercicioInstructorController.class);

    @PersistenceContext
    private EntityManager em;

    // Inyecta YouTubeService si existe; no se modifica el bean ni su código.
    @Autowired(required = false)
    private com.example.gym1.Service.YouTubeService yt;

    @Value("${youtube.channel.id:}")
    private String youtubeChannelId;

    // ------------------------------
    // LISTAR
    // ------------------------------
    @GetMapping
    public String listar(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        List<Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e ORDER BY e.nombre", Ejercicio.class)
                .getResultList();

        model.addAttribute("ejercicios", ejercicios);
        addChannelVideosToModel(model);
        return "instructor/ejercicios";
    }

    // ------------------------------
    // NUEVO / CREAR (alias añadido para /crear)
    // ------------------------------
    @GetMapping({"/nuevo", "/crear"})
    public String nuevo(HttpSession session, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        model.addAttribute("ejercicio", new Ejercicio());
        addChannelVideosToModel(model);
        return "instructor/ejercicio-form";
    }

    // ------------------------------
    // GUARDAR (create/update) - normaliza linkVideo usando YouTubeService helper
    // ------------------------------
    @PostMapping("/guardar")
    @Transactional
    public String guardar(@ModelAttribute Ejercicio ejercicio, HttpSession session, RedirectAttributes ra, Model model) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        try {
            if (ejercicio.getLinkVideo() != null && !ejercicio.getLinkVideo().isBlank()) {
                String maybe = ejercicio.getLinkVideo().trim();
                // Usa el helper estático de YouTubeService; no modifica YouTubeService
                String videoId = com.example.gym1.Service.YouTubeService.extractYouTubeId(maybe);
                if (videoId != null) {
                    ejercicio.setLinkVideo(videoId);
                    if (yt != null) {
                        try {
                            String title = yt.getVideoTitle(videoId);
                            if (title != null) log.debug("YouTube video found: {} - {}", videoId, title);
                        } catch (Exception ex) {
                            log.warn("YouTube title lookup failed for {}: {}", videoId, ex.getMessage());
                        }
                    }
                } else {
                    log.debug("No se pudo extraer videoId de '{}'", ejercicio.getLinkVideo());
                }
            }

            if (ejercicio.getId() == null) {
                em.persist(ejercicio);
            } else {
                em.merge(ejercicio);
            }
            ra.addFlashAttribute("success", "Ejercicio guardado correctamente.");
        } catch (Exception ex) {
            log.error("Error guardando ejercicio", ex);
            ra.addFlashAttribute("error", "Error guardando ejercicio: " + ex.getMessage());
            addChannelVideosToModel(model);
            return "instructor/ejercicio-form";
        }
        return "redirect:/instructor/ejercicios";
    }

    // ------------------------------
    // EDITAR (form)
    // ------------------------------
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, HttpSession session, Model model, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        Ejercicio e = em.find(Ejercicio.class, id);
        if (e == null) {
            ra.addFlashAttribute("error", "Ejercicio no encontrado");
            return "redirect:/instructor/ejercicios";
        }
        model.addAttribute("ejercicio", e);
        addChannelVideosToModel(model);
        return "instructor/ejercicio-form";
    }

    // ------------------------------
    // ELIMINAR (limpieza de rutina_detalle y borrado) — POST por seguridad
    // ------------------------------
    @PostMapping("/eliminar/{id}")
    @Transactional
    public String eliminar(@PathVariable Integer id, HttpSession session, RedirectAttributes ra) {
        Integer uid = (Integer) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";

        try {
            Number refsN = (Number) em.createNativeQuery("SELECT COUNT(1) FROM rutina_detalle WHERE id_ejercicio = ?")
                    .setParameter(1, id)
                    .getSingleResult();
            int refs = refsN != null ? refsN.intValue() : 0;

            if (refs > 0) {
                em.createNativeQuery("DELETE FROM rutina_detalle WHERE id_ejercicio = ?")
                        .setParameter(1, id)
                        .executeUpdate();
            }

            Ejercicio ejercicio = em.find(Ejercicio.class, id);
            if (ejercicio == null) {
                ra.addFlashAttribute("error", "Ejercicio no encontrado");
                return "redirect:/instructor/ejercicios";
            }
            em.remove(em.contains(ejercicio) ? ejercicio : em.merge(ejercicio));

            if (refs > 0) {
                ra.addFlashAttribute("success", "Ejercicio eliminado y " + refs + " referencia(s) en rutinas fueron limpiadas.");
            } else {
                ra.addFlashAttribute("success", "Ejercicio eliminado correctamente.");
            }
        } catch (Exception ex) {
            log.error("Error al eliminar ejercicio id={}", id, ex);
            ra.addFlashAttribute("error", "Error al eliminar ejercicio: " + ex.getMessage());
        }

        return "redirect:/instructor/ejercicios";
    }

    // ------------------------------
    // Helper: añadir videos del canal al model (si está configurado)
    // ------------------------------
    private void addChannelVideosToModel(Model model) {
        if (yt == null || youtubeChannelId == null || youtubeChannelId.isBlank()) {
            model.addAttribute("channelVideos", Collections.emptyList());
            return;
        }
        try {
            List<Map<String, Object>> channelVideos = yt.listChannelUploads(youtubeChannelId, 200);
            model.addAttribute("channelVideos", channelVideos != null ? channelVideos : Collections.emptyList());
        } catch (Exception ex) {
            log.warn("No se pudieron obtener vídeos del canal: {}", ex.getMessage());
            model.addAttribute("channelVideos", Collections.emptyList());
        }
    }
}