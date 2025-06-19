package de.marlon.homeoffice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore; // Import für @JsonIgnore
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
@Table(name = "users")
@Data // Lombok für Getter, Setter, etc.
@EqualsAndHashCode(exclude = {"userGroups", "homeofficeRequests", "approvedRequests"}) // Vermeide StackOverflow bei bidirektionalen Beziehungen
@ToString(exclude = {"userGroups", "homeofficeRequests", "approvedRequests"}) // Vermeide StackOverflow bei bidirektionalen Beziehungen
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "personal_max_ho_days_per_week")
    private Integer personalMaxHoDaysPerWeek; // Kann NULL sein

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    // Beziehung zu Gruppen (N:M durch UserGroup)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // Dies ist für die User <-> UserGroup Beziehung korrekt
    private Set<UserGroup> userGroups = new HashSet<>();

    // Beziehung zu Homeoffice-Anträgen
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // <--- NEU: Dies unterbricht die Rekursion HomeofficeRequest -> User -> HomeofficeRequest
    // Wenn du HomeofficeRequests für einen User sehen willst, dann nur, wenn du den User direkt abfragst
    // (z.B. über /api/users/{id} und dann dort eine DTO mit Requests lieferst).
    // Ansonsten würde die JsonManagedReference hier eine unendliche Schleife verursachen.
    private Set<HomeofficeRequest> homeofficeRequests = new HashSet<>();

    // Für Anträge, die dieser Benutzer genehmigt hat (als Ausbilder)
    @OneToMany(mappedBy = "approvedBy", cascade = CascadeType.DETACH)
    @JsonManagedReference("approvedBy") // Eindeutiger Name, da es zwei User-Beziehungen gibt
    private Set<HomeofficeRequest> approvedRequests = new HashSet<>();

    public User() {
        this.createdAt = ZonedDateTime.now();
        this.updatedAt = ZonedDateTime.now();
    }

    public User(String username, String passwordHash, UserRole role, String firstName, String lastName) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public enum UserRole {
        AZUBI, AUSBILDER
    }

    // Hilfsmethode, um Groups direkt aus UserGroups zu bekommen
    @Transient // Wird nicht in der DB gespeichert
    public Set<Group> getGroups() {
        Set<Group> groups = new HashSet<>();
        if (userGroups != null) {
            userGroups.forEach(userGroup -> groups.add(userGroup.getGroup()));
        }
        return groups;
    }
}