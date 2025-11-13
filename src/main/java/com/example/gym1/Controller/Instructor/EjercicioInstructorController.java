package com.example.gym1.Controller.Instructor;

import com.example.gym1.Poo.Ejercicio;
import com.example.gym1.Controller.Instructor.EjercicioRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/instructor/ejercicios")
public class EjercicioInstructorController {

    private final EjercicioRepository ejercicioRepository;

    public EjercicioInstructorController(EjercicioRepository ejercicioRepository) {
        this.ejercicioRepository = ejercicioRepository;
    }

    /**
     * Garantiza que la plantilla siempre tenga un atributo "ejercicio" para el binding.
     * Evita el error: "Neither BindingResult nor plain target object for bean name 'ejercicio' available".
     */
    @ModelAttribute("ejercicio")
    public Ejercicio ejercicioModel() {
        return new Ejercicio();
    }

    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String q, Model model) {
        List<Ejercicio> lista = ejercicioRepository.findAll();
        if (q != null && !q.isBlank()) {
            String qLower = q.toLowerCase(Locale.ROOT);
            lista = lista.stream()
                    .filter(e ->
                            (e.getNombre() != null && e.getNombre().toLowerCase(Locale.ROOT).contains(qLower)) ||
                                    (e.getDescripcion() != null && e.getDescripcion().toLowerCase(Locale.ROOT).contains(qLower)) ||
                                    (e.getMusculo() != null && e.getMusculo().toLowerCase(Locale.ROOT).contains(qLower))
                    )
                    .collect(Collectors.toList());
        }
        model.addAttribute("ejercicios", lista);
        model.addAttribute("q", q);
        return "instructor/ejercicios";
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        // @ModelAttribute("ejercicio") ya provee la instancia; aquí la sobreescribimos explícitamente por claridad
        model.addAttribute("ejercicio", new Ejercicio());
        return "instructor/ejercicio-form";
    }

    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Ejercicio e = ejercicioRepository.findById(id).orElse(null);
        if (e == null) {
            ra.addFlashAttribute("error", "Ejercicio no encontrado");
            return "redirect:/instructor/ejercicios";
        }
        model.addAttribute("ejercicio", e);
        return "instructor/ejercicio-form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Ejercicio ejercicio, RedirectAttributes ra) {
        ejercicioRepository.save(ejercicio);
        ra.addFlashAttribute("success", "Ejercicio guardado correctamente");
        return "redirect:/instructor/ejercicios";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        ejercicioRepository.deleteById(id);
        ra.addFlashAttribute("success", "Ejercicio eliminado");
        return "redirect:/instructor/ejercicios";
    }
}