package de.marlon.homeoffice.controller;

import de.marlon.homeoffice.dto.HomeofficeRequestDTO;
import de.marlon.homeoffice.model.HomeofficeRequest;
import de.marlon.homeoffice.model.User;
import de.marlon.homeoffice.service.HomeofficeRequestService;
import de.marlon.homeoffice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/homeoffice")
public class HomeOfficeRequestController {

    private final HomeofficeRequestService homeofficeRequestService;
    private final UserService userService; // Um den User aus UserDetails zu bekommen

    public HomeOfficeRequestController(HomeofficeRequestService homeofficeRequestService, UserService userService) {
        this.homeofficeRequestService = homeofficeRequestService;
        this.userService = userService;
    }

    @PostMapping("/request")
    public ResponseEntity<?> requestHomeoffice(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody HomeofficeRequestDTO requestDTO) {
        try {
            if (principal == null) {
                return ResponseEntity.badRequest().body("Nicht authentifiziert");
            }

            String username = principal.getPreferredUsername();
            if (username == null) {
                return ResponseEntity.badRequest().body("Benutzername nicht gefunden");
            }

            User user = userService.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(principal.getEmail());
                    newUser.setActive(true);
                    return userService.save(newUser);
                });

            HomeofficeRequest request = homeofficeRequestService.createRequest(
                    user,
                    requestDTO.getRequestDate(),
                    requestDTO.isHalfDay()
            );

            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fehler beim Beantragen des Homeoffice: " + e.getMessage());
        }
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal OidcUser principal) {
        try {
            if (principal == null) {
                return ResponseEntity.badRequest().body("Nicht authentifiziert");
            }

            String username = principal.getPreferredUsername();
            if (username == null) {
                return ResponseEntity.badRequest().body("Benutzername nicht gefunden");
            }

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden"));

            var requests = homeofficeRequestService.getUserRequests(user);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fehler beim Laden der Anträge: " + e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingRequests() {
        try {
            var requests = homeofficeRequestService.getPendingRequests();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Fehler beim Laden der ausstehenden Anträge: " + e.getMessage());
        }
    }

    // Endpoint für Ausbilder zum Annehmen/Ablehnen von Anträgen
    @PutMapping("/request/{id}/status")
    @PreAuthorize("hasRole('AUSBILDER')")
    public ResponseEntity<?> updateRequestStatus(@PathVariable Long id, @RequestParam HomeofficeRequest.RequestStatus status) {
        try {
            HomeofficeRequest updatedRequest = homeofficeRequestService.updateRequestStatus(id, status);
            return ResponseEntity.ok(convertToDto(updatedRequest));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    // Endpoint zum Abrufen aller Homeoffice-Anträge (z.B. für Kalenderansicht)
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('AZUBI', 'AUSBILDER')")
    public ResponseEntity<List<HomeofficeRequestDTO>> getAllHomeofficeRequests() {
        List<HomeofficeRequest> requests = homeofficeRequestService.getAllHomeofficeRequests();
        return ResponseEntity.ok(requests.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    // Endpoint zum Abrufen von Homeoffice-Anträgen für einen bestimmten Monat
    @GetMapping("/month/{year}/{month}")
    @PreAuthorize("hasAnyRole('AZUBI', 'AUSBILDER')")
    public ResponseEntity<List<HomeofficeRequestDTO>> getHomeofficeRequestsForMonth(@PathVariable int year, @PathVariable int month) {
        List<HomeofficeRequest> requests = homeofficeRequestService.getHomeofficeRequestsForMonth(year, month);
        return ResponseEntity.ok(requests.stream().map(this::convertToDto).collect(Collectors.toList()));
    }

    private HomeofficeRequestDTO convertToDto(HomeofficeRequest request) {
        return new HomeofficeRequestDTO(
                request.getId(),
                request.getUser().getId(),
                request.getUser().getUsername(),
                request.getRequestDate(),
                request.getStatus(),
                request.isHalfDay()
        );
    }
}