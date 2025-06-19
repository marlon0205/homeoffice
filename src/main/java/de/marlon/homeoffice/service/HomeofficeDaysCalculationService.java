// src/main/java/com/yourcompany/homeofficecalendar/service/HomeofficeDaysCalculationService.java
package de.marlon.homeoffice.service;

import de.marlon.homeoffice.entity.Group;
import de.marlon.homeoffice.entity.User;
import de.marlon.homeoffice.repository.GroupRepository;
import de.marlon.homeoffice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class HomeofficeDaysCalculationService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;

    public HomeofficeDaysCalculationService(UserRepository userRepository, GroupRepository groupRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    /**
     * Berechnet die maximal möglichen Homeoffice-Tage pro Woche für einen Azubi.
     *
     * Prioritätsregeln:
     * 1. Wenn Azubi ein persönliches Limit hat, zählt dieses.
     * 2. Wenn Azubi kein persönliches Limit hat, aber in mehreren Gruppen ist,
     * zählt das höchste Limit aus diesen Gruppen.
     * 3. Wenn Azubi kein persönliches Limit und in keiner Gruppe ist, dann 0.
     *
     * @param user Der Azubi-Benutzer.
     * @return Die maximalen Homeoffice-Tage pro Woche.
     */
    public int getMaxHomeofficeDaysPerWeek(User user) {
        // Regel 1: Persönliches Limit hat höchste Priorität
        if (user.getPersonalMaxHoDaysPerWeek() != null) {
            return user.getPersonalMaxHoDaysPerWeek();
        }

        // Regel 2: Wenn kein persönliches Limit, dann das höchste der Gruppen
        List<Group> userGroups = (List<Group>) user.getGroups(); // Annahme: User-Entität hat @ManyToMany zu Groups
        if (userGroups != null && !userGroups.isEmpty()) {
            Optional<Integer> maxGroupDays = userGroups.stream()
                    .map(Group::getMaxHoDaysPerWeek)
                    .max(Comparator.naturalOrder());
            return maxGroupDays.orElse(0); // Wenn keine Gruppen mit Tagen gefunden, Standard 0
        }

        // Regel 3: Wenn kein persönliches Limit und in keiner Gruppe
        return 0;
    }

    // Hier weitere Methoden, z.B. um zu prüfen, ob ein Antrag die Tage überschreiten würde
    // public boolean checkIfRequestExceedsMaxDays(User user, LocalDate requestDate, boolean isHalfDay) { ... }
    // public boolean isApprovalValid(HomeofficeRequest request) { ... }
}