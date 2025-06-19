package de.marlon.homeoffice.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody; // Für CSRF Token als JSON

import org.springframework.security.web.csrf.CsrfToken; // Importiere CsrfToken

import jakarta.servlet.http.HttpServletRequest; // Importiere HttpServletRequest

@Controller
public class ViewController {

    @GetMapping("/")
    public String index(Model model, HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            // "String" ist für den Fall, dass es sich um einen anonymen Benutzer handelt
            model.addAttribute("loggedInUser", authentication.getName()); // Benutzername des angemeldeten Benutzers
        } else {
            model.addAttribute("loggedInUser", "Gast");
        }

        // CSRF Token für AJAX Anfragen bereitstellen
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }

        return "index"; // Rendert src/main/resources/templates/index.html
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // Rendert src/main/resources/templates/login.html
    }

    // Endpoint, um den CSRF-Token für JS abzurufen (optional, wenn nicht im HTML gerendert)
    @GetMapping("/csrf-token")
    @ResponseBody
    public CsrfToken getCsrfToken(HttpServletRequest request) {
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
}