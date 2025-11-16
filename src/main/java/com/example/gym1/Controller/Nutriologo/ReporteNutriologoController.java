package com.example.gym1.Controller.Nutriologo;

import com.example.gym1.Poo.Cliente;
import com.example.gym1.Poo.ClienteRepository;
import com.example.gym1.Poo.DietaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@Controller
public class ReporteNutriologoController {

    private final ClienteRepository clienteRepository;
    private final DietaRepository dietaRepository;

    public ReporteNutriologoController(ClienteRepository clienteRepository,
                                       DietaRepository dietaRepository) {
        this.clienteRepository = clienteRepository;
        this.dietaRepository = dietaRepository;
    }

    // 👉 Cuando entras a /nutriologo/reportes SIN id, te mando a la lista de pacientes
    @GetMapping("/nutriologo/reportes")
    public String seleccionarPaciente() {
        return "redirect:/nutriologo/clientes";
    }

    // 👉 Reporte de un paciente concreto: /nutriologo/reportes/cliente/1
    @GetMapping("/nutriologo/reportes/cliente/{id}")
    public String reporteCliente(@PathVariable("id") Integer id,
                                 Model model) {

        Cliente c = clienteRepository.findById(id).orElse(null);
        if (c == null) {
            model.addAttribute("error", "Paciente no encontrado.");
            return "Nutriologo/reporte-cliente";
        }

        // Cálculo IMC usando BigDecimal (por si peso/altura son DECIMAL)
        BigDecimal pesoBD = c.getPeso();
        BigDecimal alturaBD = c.getAltura();
        Double imc = null;

        if (pesoBD != null && alturaBD != null &&
                alturaBD.compareTo(BigDecimal.ZERO) > 0) {

            double peso = pesoBD.doubleValue();
            double altura = alturaBD.doubleValue();
            imc = peso / (altura * altura);
        }

        long totalDietas = dietaRepository.countByCliente_Id(id);

        model.addAttribute("cliente", c);
        model.addAttribute("imc", imc);
        model.addAttribute("totalDietas", totalDietas);

        return "Nutriologo/reporte-cliente";
    }
}
