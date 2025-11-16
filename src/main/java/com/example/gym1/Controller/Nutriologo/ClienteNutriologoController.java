package com.example.gym1.Controller.Nutriologo;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.ClienteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/nutriologo/clientes")
public class ClienteNutriologoController {

    private final ClienteRepository clienteRepository;

    public ClienteNutriologoController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping
    public String listar(@RequestParam(value = "q", required = false) String q,
                         @RequestParam(value = "modo", required = false) String modo,
                         Model model) {

        List<Cliente> lista = clienteRepository.findAll();

        if (q != null && !q.isBlank()) {
            String search = q.toLowerCase(Locale.ROOT);
            lista = lista.stream()
                    .filter(c -> c.getNombre() != null &&
                            c.getNombre().toLowerCase(Locale.ROOT).contains(search))
                    .collect(Collectors.toList());
        }

        boolean modoNuevaDieta = "nuevaDieta".equals(modo);
        boolean modoAsignarDieta = "asignarDieta".equals(modo);

        model.addAttribute("clientes", lista);
        model.addAttribute("q", q);
        model.addAttribute("modo", modo);
        model.addAttribute("modoNuevaDieta", modoNuevaDieta);
        model.addAttribute("modoAsignarDieta", modoAsignarDieta);

        return "Nutriologo/clientes";
    }


}
