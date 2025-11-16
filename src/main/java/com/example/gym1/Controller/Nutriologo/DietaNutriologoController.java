package com.example.gym1.Controller.Nutriologo;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.ClienteRepository;
import com.example.gym1.Poo.Dieta;
import com.example.gym1.Poo.DietaRepository;
import com.example.gym1.Poo.Platillo;
import com.example.gym1.Poo.PlatilloRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;

@Controller
@RequestMapping("/nutriologo/dietas")
public class    DietaNutriologoController {

    private final DietaRepository dietaRepository;
    private final ClienteRepository clienteRepository;
    private final PlatilloRepository platilloRepository;

    public DietaNutriologoController(DietaRepository dietaRepository,
                                     ClienteRepository clienteRepository,
                                     PlatilloRepository platilloRepository) {
        this.dietaRepository = dietaRepository;
        this.clienteRepository = clienteRepository;
        this.platilloRepository = platilloRepository;
    }

    // ================= LISTAR DIETAS DE UN PACIENTE =================
    @GetMapping
    public String listar(@RequestParam("clienteId") Integer clienteId, Model model,
                         RedirectAttributes ra) {

        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            ra.addFlashAttribute("error", "Paciente no encontrado.");
            return "redirect:/nutriologo/clientes";
        }

        List<Dieta> dietas = dietaRepository.findByCliente_Id(clienteId);

        model.addAttribute("cliente", cliente);
        model.addAttribute("dietas", dietas);

        return "Nutriologo/dietas";
    }
    // ================= PANEL PRINCIPAL DE DIETAS =================
    @GetMapping("/panel")
    public String panelDietas() {
        return "Nutriologo/dietas-panel";
    }

    // ================= NUEVA DIETA =================
    @GetMapping("/nuevo")
    public String nuevo(@RequestParam("clienteId") Integer clienteId,
                        Model model,
                        RedirectAttributes ra) {

        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            ra.addFlashAttribute("error", "Paciente no encontrado.");
            return "redirect:/nutriologo/clientes";
        }

        Dieta d = new Dieta();
        d.setCliente(cliente);

        model.addAttribute("dieta", d);
        model.addAttribute("cliente", cliente);

        // platillos para checkboxes
        List<Platillo> platillos = platilloRepository.findAll();
        model.addAttribute("platillosDisponibles", platillos);

        return "Nutriologo/dieta-form";
    }

    // ================= EDITAR DIETA =================
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id,
                         @RequestParam("clienteId") Integer clienteId,
                         Model model,
                         RedirectAttributes ra) {

        Dieta d = dietaRepository.findById(id).orElse(null);
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);

        if (d == null || cliente == null) {
            ra.addFlashAttribute("error", "Dieta o paciente no encontrado.");
            return "redirect:/nutriologo/clientes";
        }

        model.addAttribute("dieta", d);
        model.addAttribute("cliente", cliente);

        List<Platillo> platillos = platilloRepository.findAll();
        model.addAttribute("platillosDisponibles", platillos);

        return "Nutriologo/dieta-form";
    }

    // ================= GUARDAR (NUEVA / EDITADA) =================
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("dieta") Dieta dieta,
                          @RequestParam("clienteId") Integer clienteId,
                          @RequestParam(value = "platillosIds", required = false) List<Integer> platillosIds,
                          RedirectAttributes ra) {

        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            ra.addFlashAttribute("error", "Paciente no encontrado.");
            return "redirect:/nutriologo/clientes";
        }

        dieta.setCliente(cliente);

        // asociar platillos seleccionados
        if (platillosIds != null && !platillosIds.isEmpty()) {
            List<Platillo> seleccionados = platilloRepository.findAllById(platillosIds);
            dieta.setPlatillos(new HashSet<>(seleccionados));
        } else {
            dieta.setPlatillos(new HashSet<>());
        }

        dietaRepository.save(dieta);

        ra.addFlashAttribute("success", "Dieta guardada correctamente.");
        return "redirect:/nutriologo/dietas?clienteId=" + clienteId;
    }

    // (opcional) métodos de asignar dieta existente que ya habíamos hecho...
}
