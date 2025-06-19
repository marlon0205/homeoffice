// src/main/java/com/yourcompany/homeofficecalendar/service/GroupService.java
package de.marlon.homeoffice.service;

import de.marlon.homeoffice.entity.Group;
import de.marlon.homeoffice.entity.User;
import de.marlon.homeoffice.entity.UserGroup;
import de.marlon.homeoffice.repository.GroupRepository;
import de.marlon.homeoffice.repository.UserGroupRepository;
import de.marlon.homeoffice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;

    @Transactional
    public Group createGroup(String name, int maxHoDaysPerWeek) {
        if (groupRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Gruppe mit diesem Namen existiert bereits: " + name);
        }
        if (maxHoDaysPerWeek < 0 || maxHoDaysPerWeek > 7) {
            throw new IllegalArgumentException("Maximale HO-Tage müssen zwischen 0 und 7 liegen.");
        }
        Group newGroup = new Group(name, maxHoDaysPerWeek);
        return groupRepository.save(newGroup);
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new IllegalArgumentException("Gruppe nicht gefunden: " + groupId);
        }
        groupRepository.deleteById(groupId);
    }

    @Transactional
    public Group updateGroupMaxDays(UUID groupId, int maxDays) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppe nicht gefunden: " + groupId));
        if (maxDays < 0 || maxDays > 7) {
            throw new IllegalArgumentException("Maximale HO-Tage müssen zwischen 0 und 7 liegen.");
        }
        group.setMaxHoDaysPerWeek(maxDays);
        return groupRepository.save(group);
    }

    @Transactional
    public Group assignUsersToGroup(UUID groupId, List<UUID> userIds) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppe nicht gefunden: " + groupId));

        // Aktuelle Zuweisungen für diese Gruppe
        Set<UUID> currentAssignedUserIds = group.getUserGroups().stream()
                .map(UserGroup::getUserId)
                .collect(Collectors.toSet());

        // Benutzer, die hinzugefügt werden sollen
        Set<UUID> toAdd = new HashSet<>(userIds);
        toAdd.removeAll(currentAssignedUserIds); // Entferne bereits zugewiesene

        // Benutzer, die entfernt werden sollen
        Set<UUID> toRemove = new HashSet<>(currentAssignedUserIds);
        toRemove.removeAll(userIds); // Entferne die, die weiterhin zugewiesen sein sollen

        // Hinzufügen
        toAdd.forEach(userId -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Benutzer nicht gefunden: " + userId));
            UserGroup userGroup = new UserGroup(user, group);
            userGroupRepository.save(userGroup);
            group.getUserGroups().add(userGroup); // Aktualisiere die Entität im Speicher
        });

        // Entfernen
        toRemove.forEach(userId -> {
            userGroupRepository.deleteByUserIdAndGroupId(userId, groupId);
            // Manuelles Entfernen aus der Collection, da orphanRemoval true ist
            group.getUserGroups().removeIf(ug -> ug.getUserId().equals(userId));
        });

        // Speichern der Gruppe ist hier nicht streng notwendig, da Änderungen an den UserGroups kaskadiert werden
        // und die Group-Entität selbst sich nicht direkt ändert, aber kann explizit gemacht werden
        return groupRepository.save(group);
    }
}