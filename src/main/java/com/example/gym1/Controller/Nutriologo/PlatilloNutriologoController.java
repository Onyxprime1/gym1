package com.example.gym1.Controller.Nutriologo;

import com.example.gym1.Poo.Platillo;
import com.example.gym1.Poo.PlatilloRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/nutriologo/platillos")
public class PlatilloNutriologoController {

    private final PlatilloRepository platilloRepository;

    public PlatilloNutriologoController(PlatilloRepository platilloRepository) {
        this.platilloRepository = platilloRepository;
    }

    @ModelAttribute("platillo")
    public Platillo platilloModel() {
        return new Platillo();
    }

    // LISTAR + BUSCAR
    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String q, Model model) {

        List<Platillo> lista = platilloRepository.findAll();

        if (q != null && !q.isBlank()) {
            String search = q.toLowerCase(Locale.ROOT);

            lista = lista.stream().filter(p ->
                    (p.getNombre() != null && p.getNombre().toLowerCase(Locale.ROOT).contains(search)) ||
                            (p.getIngredientes() != null && p.getIngredientes().toLowerCase(Locale.ROOT).contains(search)) ||
                            (p.getPreparacion() != null && p.getPreparacion().toLowerCase(Locale.ROOT).contains(search))
            ).collect(Collectors.toList());
        }

        model.addAttribute("platillos", lista);
        model.addAttribute("q", q);

        return "Nutriologo/platillos";
    }

    // FORMULARIO NUEVO
    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("platillo", new Platillo());
        return "Nutriologo/platillo-form";
    }

    // FORMULARIO EDITAR
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Integer id, Model model, RedirectAttributes ra) {

        Platillo p = platilloRepository.findById(id).orElse(null);

        if (p == null) {
            ra.addFlashAttribute("error", "Platillo no encontrado");
            return "redirect:/nutriologo/platillos";
        }

        model.addAttribute("platillo", p);
        return "Nutriologo/platillo-form";
    }

    // GUARDAR NUEVO O EDITADO
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Platillo platillo, RedirectAttributes ra) {

        platilloRepository.save(platillo);
        ra.addFlashAttribute("success", "Platillo guardado correctamente");

        return "redirect:/nutriologo/platillos";
    }

    // ELIMINAR
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            platilloRepository.deleteById(id);
            ra.addFlashAttribute("success", "Platillo eliminado correctamente.");
        } catch (DataIntegrityViolationException e) {

            ra.addFlashAttribute(
                    "error",
                    "⚠️ Este platillo está asignado a una dieta y no se puede eliminar."
            );
        }

        return "redirect:/nutriologo/platillos";
    }
}
