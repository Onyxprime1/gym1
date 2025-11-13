// src/main/java/com/example/gym1/Controller/HomeController.java
package com.example.gym1.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // HomeController
    @GetMapping({"/","/index"})
    public String home() {
        return "index"; // templates/inicio.html
    }

}
