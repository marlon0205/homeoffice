package de.marlon.homeoffice.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import jakarta.persistence.*;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "groups")
@Data
@EqualsAndHashCode(exclude = "userGroups")
@ToString(exclude = "userGroups")
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "max_ho_days_per_week", nullable = false)
    private int maxHoDaysPerWeek; // Standardwert 0

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Vermeidet unendliche Rekursion
    private Set<UserGroup> userGroups = new HashSet<>();

    public Group() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
        this.maxHoDaysPerWeek = 0;
    }

    public Group(String name, int maxHoDaysPerWeek) {
        this();
        this.name = name;
        this.maxHoDaysPerWeek = maxHoDaysPerWeek;
    }

    // Hilfsmethode, um Users direkt aus UserGroups zu bekommen
    @Transient // Wird nicht in der DB gespeichert
    public Set<User> getUsers() {
        Set<User> users = new HashSet<>();
        if (userGroups != null) {
            userGroups.forEach(userGroup -> users.add(userGroup.getUser()));
        }
        return users;
    }
}