package de.marlon.homeoffice.controller; // Or de.marlon.homeoffice.config, if it's more general

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaRedirectController {

    @GetMapping({"/", "/home", "/api/**"})
    public String redirectToIndex() {
        return "index.html";
    }
}