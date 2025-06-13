package de.marlon.homeoffice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "homeoffice_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeofficeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate requestDate;

    @Column(nullable = false)
    private boolean halfDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}