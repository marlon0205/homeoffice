package de.marlon.homeoffice.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Dieser Endpunkt könnte für einen "echten" Logout auf Authentik umleiten
    // Oder einfach nur die Session auf der Spring Boot Seite invalidieren
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        SecurityContextHolder.clearContext(); // Löscht die Spring Security Session
        // Optional: Client-Seite umleiten zum Authentik Logout-Endpunkt
        // return ResponseEntity.ok("Logged out successfully. Redirect to Authentik logout if needed.");
        return ResponseEntity.ok("Logged out successfully.");
    }

    // Wenn du eine manuelle Registrierung außerhalb von Authentik erlauben möchtest (nicht empfohlen mit OIDC als primärer Auth)
    // Dann würde hier eine signup-Methode sein, die Benutzer in deiner DB anlegt, aber kein Passwort authentifiziert.
    // Das Login erfolgt IMMER über Authentik.

    // Ein einfacher Endpunkt, um den aktuellen Benutzer zu testen (Debugging)
    @GetMapping("/me")
    public ResponseEntity<?> currentUser(@AuthenticationPrincipal OAuth2User oauth2User) {
        if (oauth2User != null) {
            return ResponseEntity.ok(oauth2User.getAttributes());
        }
        return ResponseEntity.status(401).body("Not authenticated");
    }
}