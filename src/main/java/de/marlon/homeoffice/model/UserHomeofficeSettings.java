package de.marlon.homeoffice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_homeoffice_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserHomeofficeSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Priorität: Diese Einstellung überschreibt die Gruppeneinstellung
    private Integer maxHomeofficeDaysPerWeek; // Kann null sein, wenn keine individuelle Regelung
}