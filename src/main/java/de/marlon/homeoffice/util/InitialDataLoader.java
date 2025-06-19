package de.marlon.homeoffice.util;

import de.marlon.homeoffice.entity.Group;
import de.marlon.homeoffice.entity.HomeofficeRequest;
import de.marlon.homeoffice.entity.User;
import de.marlon.homeoffice.repository.GroupRepository;
import de.marlon.homeoffice.repository.HomeofficeRequestRepository;
import de.marlon.homeoffice.repository.UserRepository;
import de.marlon.homeoffice.service.GroupService;
import de.marlon.homeoffice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
public class InitialDataLoader implements CommandLineRunner {

    private final UserService userService;
    private final GroupService groupService;
    private final HomeofficeRequestRepository homeofficeRequestRepository;
    private final UserRepository userRepository; // Für direkten Zugriff auf User-Objekte
    private final GroupRepository groupsRepository;

    @Override
    @Transactional // Stellt sicher, dass alle Operationen in einer Transaktion sind
    public void run(String... args) throws Exception {
        System.out.println("--- Initializing Demo Data ---");

        // 1. Benutzer erstellen
        User max = createUserIfNotExists("max.mustermann", "password", User.UserRole.AZUBI, "Max", "Mustermann", 3);
        User anna = createUserIfNotExists("anna.schmidt", "password", User.UserRole.AZUBI, "Anna", "Schmidt", 2);
        User tim = createUserIfNotExists("tim.becker", "password", User.UserRole.AZUBI, "Tim", "Becker", null); // Gruppenstandard
        User lena = createUserIfNotExists("lena.mayer", "password", User.UserRole.AZUBI, "Lena", "Mayer", null); // Gruppenstandard
        User ausbilder = createUserIfNotExists("oliver.ausbilder", "password", User.UserRole.AUSBILDER, "Oliver", "Ausbilder", null);

        // 2. Gruppen erstellen
        Group azubis2024 = createGroupIfNotExists("Azubis 2024", 4);
        Group azubis2025 = createGroupIfNotExists("Azubis 2025", 3);
        Group specialProj = createGroupIfNotExists("Special Project", 5);

        // 3. Benutzer zu Gruppen zuweisen
        assignUsersToGroup(azubis2024.getId(), Arrays.asList(max.getId(), tim.getId()));
        assignUsersToGroup(azubis2025.getId(), Arrays.asList(anna.getId(), tim.getId(), lena.getId())); // Tim in 2 Gruppen
        assignUsersToGroup(specialProj.getId(), Collections.singletonList(max.getId())); // Max in 2 Gruppen

        // 4. Homeoffice-Anträge erstellen
        // Anträge für Max Mustermann (heute und morgen, da aktuelle Demo-Datum 18.06.2025 ist)
        createHomeofficeRequest(max, LocalDate.of(2025, 6, 18), false, HomeofficeRequest.RequestStatus.ACCEPTED, null); // Heute
        createHomeofficeRequest(max, LocalDate.of(2025, 6, 19), false, HomeofficeRequest.RequestStatus.PENDING, null); // Morgen, PENDING

        // Anträge für Anna Schmidt
        createHomeofficeRequest(anna, LocalDate.of(2025, 6, 18), true, HomeofficeRequest.RequestStatus.ACCEPTED, null); // Heute halbtags
        createHomeofficeRequest(anna, LocalDate.of(2025, 6, 20), true, HomeofficeRequest.RequestStatus.PENDING, null); // Übermorgen, PENDING

        // Anträge für Tim Becker
        createHomeofficeRequest(tim, LocalDate.of(2025, 6, 25), false, HomeofficeRequest.RequestStatus.ACCEPTED, null);

        // Anträge, die auf Genehmigung warten (sichtbar im Admin-Bereich)
        createHomeofficeRequest(max, LocalDate.of(2025, 6, 22), false, HomeofficeRequest.RequestStatus.PENDING, null);
        createHomeofficeRequest(lena, LocalDate.of(2025, 6, 23), false, HomeofficeRequest.RequestStatus.PENDING, null);


        System.out.println("--- Demo Data Initialized ---");
    }

    private User createUserIfNotExists(String username, String password, User.UserRole role, String firstName, String lastName, Integer personalMaxHoDays) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            User user = userService.createUser(username, password, role, firstName, lastName);
            user.setPersonalMaxHoDaysPerWeek(personalMaxHoDays); // Setze das persönliche Limit direkt
            return userRepository.save(user); // Speichere aktualisierten User
        });
    }

    private Group createGroupIfNotExists(String name, int maxHoDaysPerWeek) {
        return groupsRepository.findByName(name).orElseGet(() ->
                groupService.createGroup(name, maxHoDaysPerWeek)
        );
    }

    private void assignUsersToGroup(UUID groupId, List<UUID> userIds) {
        try {
            groupService.assignUsersToGroup(groupId, userIds);
        } catch (IllegalArgumentException e) {
            System.err.println("Fehler beim Zuweisen von Benutzern zu Gruppe " + groupId + ": " + e.getMessage());
        }
    }

    private void createHomeofficeRequest(User user, LocalDate date, boolean isHalfDay, HomeofficeRequest.RequestStatus status, User approvedBy) {
        homeofficeRequestRepository.findByUserAndRequestDate(user, date).orElseGet(() -> {
            HomeofficeRequest request = new HomeofficeRequest(user, date, isHalfDay);
            request.setStatus(status);
            request.setSubmissionDate(date.atStartOfDay(ZonedDateTime.now().getZone())); // Setzt die Uhrzeit auf 00:00 Uhr des Tages
            if (status == HomeofficeRequest.RequestStatus.ACCEPTED || status == HomeofficeRequest.RequestStatus.REJECTED) {
                request.setApprovalDate(ZonedDateTime.now());
                request.setApprovedBy(approvedBy);
            }
            return homeofficeRequestRepository.save(request);
        });
    }
}