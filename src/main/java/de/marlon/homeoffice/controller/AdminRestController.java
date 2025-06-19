package de.marlon.homeoffice.controller;

import de.marlon.homeoffice.dto.GroupCreateDto;
import de.marlon.homeoffice.dto.UserUpdatePersonalMaxHoDaysDto;
import de.marlon.homeoffice.entity.Group;
import de.marlon.homeoffice.entity.HomeofficeRequest;
import de.marlon.homeoffice.entity.User;
import de.marlon.homeoffice.service.GroupService;
import de.marlon.homeoffice.service.HomeofficeService;
import de.marlon.homeoffice.service.UserService;
import lombok.RequiredArgsConstructor; // Für automatischen Konstruktor
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('AUSBILDER')") // Alle Methoden in diesem Controller erfordern AUSBILDER-Rolle
@RequiredArgsConstructor // Lombok für Konstruktor-Injektion
public class AdminRestController {

    private final GroupService groupService;
    private final UserService userService;
    private final HomeofficeService homeofficeService;

    @GetMapping("/groups")
    public List<Group> getAllGroups() {
        return groupService.getAllGroups();
    }

    @PostMapping("/groups")
    public ResponseEntity<Group> createGroup(@RequestBody GroupCreateDto groupCreateDto) {
        Group newGroup = groupService.createGroup(groupCreateDto.getName(), groupCreateDto.getMaxHoDaysPerWeek());
        return ResponseEntity.status(HttpStatus.CREATED).body(newGroup);
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/groups/{id}/max-days")
    public ResponseEntity<Group> updateGroupMaxDays(@PathVariable UUID id, @RequestParam int maxDays) {
        Group updatedGroup = groupService.updateGroupMaxDays(id, maxDays);
        return ResponseEntity.ok(updatedGroup);
    }

    @GetMapping("/users") // Alle Azubis abrufen
    public List<User> getAllAzubis() {
        return userService.getAllUsersByRole(User.UserRole.AZUBI);
    }

    @PutMapping("/users/{userId}/personal-max-ho-days")
    public ResponseEntity<User> updateAzubiPersonalMaxHoDays(
            @PathVariable UUID userId,
            @RequestBody UserUpdatePersonalMaxHoDaysDto dto
    ) {
        User updatedUser = userService.updateUserPersonalMaxHoDays(userId, dto.getPersonalMaxHoDaysPerWeek());
        return ResponseEntity.ok(updatedUser);
    }


    @PutMapping("/groups/{groupId}/users")
    public ResponseEntity<Group> assignUsersToGroup(
            @PathVariable UUID groupId,
            @RequestBody List<UUID> userIds // Liste der User-IDs, die zur Gruppe gehören sollen
    ) {
        Group updatedGroup = groupService.assignUsersToGroup(groupId, userIds);
        return ResponseEntity.ok(updatedGroup);
    }


    @GetMapping("/requests/pending")
    public List<HomeofficeRequest> getPendingRequests() {
        return homeofficeService.getPendingHomeofficeRequests();
    }

    @PutMapping("/requests/{id}/approve")
    public ResponseEntity<HomeofficeRequest> approveRequest(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails // Ausbilder, der genehmigt
    ) {
        User approver = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Genehmigender Benutzer nicht gefunden."));
        HomeofficeRequest approvedRequest = homeofficeService.approveRequest(id, approver.getId());
        return ResponseEntity.ok(approvedRequest);
    }

    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<HomeofficeRequest> rejectRequest(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal UserDetails userDetails // Ausbilder, der ablehnt
    ) {
        User rejecter = userService.getUserByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Ablehnender Benutzer nicht gefunden."));
        HomeofficeRequest rejectedRequest = homeofficeService.rejectRequest(id, rejecter.getId(), comment);
        return ResponseEntity.ok(rejectedRequest);
    }
}