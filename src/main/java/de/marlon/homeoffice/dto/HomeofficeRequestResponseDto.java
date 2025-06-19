package de.marlon.homeoffice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.marlon.homeoffice.dto.UserSummaryDto;
import de.marlon.homeoffice.entity.HomeofficeRequest;
import lombok.Data;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
public class HomeofficeRequestResponseDto {
    private UUID id;
    @JsonFormat(pattern = "yyyy-MM-dd") // Das Standard-Datum von LocalDate in JSON
    private LocalDate requestDate;
    private boolean isHalfDay;
    private String status;
    private ZonedDateTime submissionDate;
    // approvalDate und comment könnten auch hier sein

    // Verschachteltes DTO für den Benutzer, um nur relevante Infos anzuzeigen
    private UserSummaryDto user;
    private UserSummaryDto approvedBy; // Optional, wenn du den Genehmiger anzeigen willst

    // Konstruktor zum Umwandeln von Entity zu DTO
    public HomeofficeRequestResponseDto(HomeofficeRequest request) {
        this.id = request.getId();
        this.requestDate = request.getRequestDate();
        this.isHalfDay = request.isHalfDay();
        this.status = request.getStatus().name(); // Enum zu String
        this.submissionDate = request.getSubmissionDate();
        this.user = new UserSummaryDto(request.getUser());
        if (request.getApprovedBy() != null) {
            this.approvedBy = new UserSummaryDto(request.getApprovedBy());
        }
    }
}