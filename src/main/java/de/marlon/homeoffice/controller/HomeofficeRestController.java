package de.marlon.homeoffice.controller;

import de.marlon.homeoffice.dto.HomeofficeRequestDto;
import de.marlon.homeoffice.dto.HomeofficeRequestResponseDto;
import de.marlon.homeoffice.entity.HomeofficeRequest;
import de.marlon.homeoffice.entity.User;
import de.marlon.homeoffice.service.HomeofficeDaysCalculationService;
import de.marlon.homeoffice.service.HomeofficeService;
import de.marlon.homeoffice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class HomeofficeRestController {

    private final HomeofficeService homeofficeService;
    private final HomeofficeDaysCalculationService calculationService;
    private final UserService userService; // Um den User aus UserDetails zu bekommen

    public HomeofficeRestController(HomeofficeService homeofficeService,
                                    HomeofficeDaysCalculationService calculationService,
                                    UserService userService) {
        this.homeofficeService = homeofficeService;
        this.calculationService = calculationService;
        this.userService = userService;
    }

    @GetMapping("/calendar-data")
    public List<HomeofficeRequestResponseDto> getCalendarData(@RequestParam int year, @RequestParam int month) {
        // Die Service-Methode gibt weiterhin Entitäten zurück
        List<HomeofficeRequest> requests = homeofficeService.getHomeofficeRequestsForMonth(year, month);
        // Hier konvertieren wir die Entitäten in DTOs
        return requests.stream()
                .map(HomeofficeRequestResponseDto::new) // Nutzt den neuen DTO-Konstruktor
                .collect(Collectors.toList());
    }

    @PostMapping("/homeoffice-request")
    @PreAuthorize("hasRole('AZUBI')") // Nur Azubis dürfen Anträge stellen
    public ResponseEntity<HomeofficeRequest> submitHomeofficeRequest(
            @RequestBody HomeofficeRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails // Aktuellen Benutzer bekommen
    ) {
        // userId aus dem Spring Security Kontext holen
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Aktueller Benutzer nicht gefunden."));

        try {
            // Optional: Prüfen, ob der Antrag die max. Tage überschreitet (für Warnung im FE)
            // int maxDays = calculationService.getMaxHomeofficeDaysPerWeek(currentUser);
            // ... Logik zur Prüfung

            HomeofficeRequest createdRequest = homeofficeService.createHomeofficeRequest(requestDto, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build(); // Oder ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/available-homeoffice-days")
    @PreAuthorize("hasRole('AZUBI')")
    public ResponseEntity<Integer> getAvailableHomeofficeDays(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Aktueller Benutzer nicht gefunden."));

        // Hier müsstest du die Logik für "verfügbare Tage aktuelle KW" und "nächste KW" implementieren
        // sowie die "maximal möglichen Tage pro KW"
        int maxDaysPerWeek = calculationService.getMaxHomeofficeDaysPerWeek(currentUser);
        // int usedDaysThisWeek = homeofficeService.getUsedDaysForUserAndWeek(currentUser, LocalDate.now());
        // int usedDaysNextWeek = homeofficeService.getUsedDaysForUserAndWeek(currentUser, LocalDate.now().plusWeeks(1));

        // Für Demo-Zwecke nur das max. Limit zurückgeben
        return ResponseEntity.ok(maxDaysPerWeek);
    }
}