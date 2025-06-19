package de.marlon.homeoffice.dto;

import de.marlon.homeoffice.entity.User;
import lombok.Data;
import java.util.UUID;

@Data
public class UserSummaryDto {
    private UUID id;
    private String username;
    private String firstName;
    private String lastName;
    // Füge hier KEINE Beziehungen hinzu (wie homeofficeRequests oder userGroups),
    // um Rekursionen zu vermeiden.

    public UserSummaryDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
    }
}