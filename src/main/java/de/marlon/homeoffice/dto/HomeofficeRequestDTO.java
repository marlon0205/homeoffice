package de.marlon.homeoffice.dto;

import de.marlon.homeoffice.model.HomeofficeRequest;
import java.time.LocalDate;

public class HomeofficeRequestDTO {
    private Long id;
    private Long userId;
    private String username;
    private LocalDate requestDate;
    private HomeofficeRequest.RequestStatus status;
    private boolean halfDay;

    // Default constructor
    public HomeofficeRequestDTO() {
    }

    // Full constructor
    public HomeofficeRequestDTO(Long id, Long userId, String username, LocalDate requestDate, 
                              HomeofficeRequest.RequestStatus status, boolean halfDay) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.requestDate = requestDate;
        this.status = status;
        this.halfDay = halfDay;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public HomeofficeRequest.RequestStatus getStatus() {
        return status;
    }

    public void setStatus(HomeofficeRequest.RequestStatus status) {
        this.status = status;
    }

    public boolean isHalfDay() {
        return halfDay;
    }

    public void setHalfDay(boolean halfDay) {
        this.halfDay = halfDay;
    }
}