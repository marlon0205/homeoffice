// src/main/java/com/yourcompany/homeofficecalendar/model/UserGroup.java
package de.marlon.homeoffice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_groups")
@IdClass(UserGroup.UserGroupId.class) // Definiert den zusammengesetzten Primärschlüssel
@Data
@NoArgsConstructor // Lombok generiert NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true) // Nur für PK-Felder
@ToString(exclude = {"user", "group"}) // Vermeidet Rekursion
public class UserGroup implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "group_id")
    private UUID groupId;

    @ManyToOne
    @MapsId("userId") // Mapped userId auf dieses Feld
    @JoinColumn(name = "user_id")
    @JsonBackReference // Vermeidet unendliche Rekursion bei JSON-Serialisierung
    private User user;

    @ManyToOne
    @MapsId("groupId") // Mapped groupId auf dieses Feld
    @JoinColumn(name = "group_id")
    @JsonBackReference // Vermeidet unendliche Rekursion bei JSON-Serialisierung
    private Group group;

    @Column(name = "assigned_at", nullable = false)
    private ZonedDateTime assignedAt;

    public UserGroup(User user, Group group) {
        this.user = user;
        this.group = group;
        this.userId = user.getId();
        this.groupId = group.getId();
        this.assignedAt = ZonedDateTime.now();
    }

    // Innere Klasse für den zusammengesetzten Primärschlüssel
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class UserGroupId implements Serializable {
        private UUID userId;
        private UUID groupId;
    }
}