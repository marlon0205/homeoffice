// src/main/java/com/yourcompany/homeofficecalendar/model/HomeofficeRequest.java
package de.marlon.homeoffice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import de.marlon.homeoffice.entity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "homeoffice_requests", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "request_date"}))
public class HomeofficeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // Für UUIDs oft @GenericGenerator in Hibernate 5
    // Wenn gen_random_uuid() in DB verwendet, dann oft wie folgt:
    // @GeneratedValue(generator = "uuid2")
    // @GenericGenerator(name = "uuid2", strategy = "uuid2")
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER) // Ändere dies
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @ManyToOne(fetch = FetchType.EAGER) // Auch hier, falls approvedBy ebenfalls Probleme macht
    @JoinColumn(name = "approved_by_user_id")
    @JsonBackReference("approvedBy")
    private User approvedBy;

    @Column(name = "request_date", nullable = false)
    private LocalDate requestDate;

    @Column(name = "is_half_day", nullable = false)
    private boolean isHalfDay;

    @Enumerated(EnumType.STRING) // Speichert den Enum-Namen als String in der DB
    @Column(name = "status", nullable = false)
    private RequestStatus status; // PENDING, ACCEPTED, REJECTED

    @Column(name = "submission_date", nullable = false)
    private ZonedDateTime submissionDate;

    @Column(name = "approval_date")
    private ZonedDateTime approvalDate;

    @Column(name = "comment")
    private String comment;

    // Constructors, Getters, Setters
    public HomeofficeRequest() {
        this.status = RequestStatus.PENDING;
        this.submissionDate = ZonedDateTime.now();
    }

    // Beispiel-Konstruktor für einfache Erstellung
    public HomeofficeRequest(User user, LocalDate requestDate, boolean isHalfDay) {
        this(); // Ruft den Standard-Konstruktor auf
        this.user = user;
        this.requestDate = requestDate;
        this.isHalfDay = isHalfDay;
    }

    public enum RequestStatus {
        PENDING, ACCEPTED, REJECTED
    }

    // Getters and Setters for all fields
    // ...
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }
    public boolean isHalfDay() { return isHalfDay; }
    public void setHalfDay(boolean halfDay) { isHalfDay = halfDay; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public ZonedDateTime getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(ZonedDateTime submissionDate) { this.submissionDate = submissionDate; }
    public ZonedDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(ZonedDateTime approvalDate) { this.approvalDate = approvalDate; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}