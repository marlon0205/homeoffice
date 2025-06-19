// src/main/java/com/yourcompany/homeofficecalendar/service/HomeofficeService.java
package de.marlon.homeoffice.service;

import de.marlon.homeoffice.dto.HomeofficeRequestDto;
import de.marlon.homeoffice.entity.HomeofficeRequest;
import de.marlon.homeoffice.entity.User;
import de.marlon.homeoffice.repository.HomeofficeRequestRepository;
import de.marlon.homeoffice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class HomeofficeService {

    private final HomeofficeRequestRepository homeofficeRequestRepository;
    private final UserRepository userRepository; // Benötigt, um User-Objekte zu holen
    private final HomeofficeDaysCalculationService calculationService; // Service für komplexe HO-Tage-Logik

    public HomeofficeService(HomeofficeRequestRepository homeofficeRequestRepository, UserRepository userRepository, HomeofficeDaysCalculationService calculationService) {
        this.homeofficeRequestRepository = homeofficeRequestRepository;
        this.userRepository = userRepository;
        this.calculationService = calculationService;
    }

    // Methode für /api/calendar-data
    public List<HomeofficeRequest> getHomeofficeRequestsForMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month + 1);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // NEU: Verwende die Methode, die den User eager lädt
        return homeofficeRequestRepository.findByRequestDateBetweenWithUser(startDate, endDate);
    }

    // Methode für /api/homeoffice-request
    @Transactional // Stellt sicher, dass die Operation atomar ist
    public HomeofficeRequest createHomeofficeRequest(HomeofficeRequestDto requestDto, UUID userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Benutzer nicht gefunden"));

        // Überprüfen, ob bereits ein Antrag für dieses Datum existiert (UNIQUE Constraint)
        homeofficeRequestRepository.findByUserAndRequestDate(currentUser, requestDto.getRequestDate())
                .ifPresent(req -> {
                    throw new IllegalArgumentException("Es existiert bereits ein Homeoffice-Antrag für dieses Datum.");
                });

        // Hier könnte auch die Logik für die "Überschreitet max. Tage!" Prüfung erfolgen
        // calculationService.checkIfRequestExceedsMaxDays(currentUser, requestDto.getRequestDate(), requestDto.isHalfDay());

        HomeofficeRequest newRequest = new HomeofficeRequest(currentUser, requestDto.getRequestDate(), requestDto.isHalfDay());
        newRequest.setStatus(HomeofficeRequest.RequestStatus.PENDING); // Initialer Status
        return homeofficeRequestRepository.save(newRequest);
    }

    // Methode für /api/admin/requests/pending
    public List<HomeofficeRequest> getPendingHomeofficeRequests() {
        // Auch hier könntest du eine spezielle Query brauchen, die User eager lädt:
        // @Query("SELECT hr FROM HomeofficeRequest hr JOIN FETCH hr.user WHERE hr.status = :status")
        // oder einfach .findByStatus() nutzen und darauf vertrauen, dass es in der Transaktion zu DTOs konvertiert wird.
        return homeofficeRequestRepository.findByStatus(HomeofficeRequest.RequestStatus.PENDING);
    }

    // Methode für /api/admin/requests/{id}/approve
    @Transactional
    public HomeofficeRequest approveRequest(UUID requestId, UUID approverId) {
        HomeofficeRequest request = homeofficeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Antrag nicht gefunden"));

        // Hier könnte Logik eingefügt werden, um zu prüfen, ob der genehmigende Benutzer ein Ausbilder ist
        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("Genehmigender Benutzer nicht gefunden"));

        if (request.getStatus() == HomeofficeRequest.RequestStatus.PENDING) {
            request.setStatus(HomeofficeRequest.RequestStatus.ACCEPTED);
            request.setApprovalDate(ZonedDateTime.now());
            request.setApprovedBy(approver);
            // Hier könnte noch eine Prüfung stattfinden, ob dieser Antrag die max. HO-Tage nicht sprengt
            // if (calculationService.isApprovalValid(request)) { ... } else { warn; }
            return homeofficeRequestRepository.save(request);
        } else {
            throw new IllegalArgumentException("Antrag hat nicht den Status PENDING.");
        }
    }

    // Methode für /api/admin/requests/{id}/reject
    @Transactional
    public HomeofficeRequest rejectRequest(UUID requestId, UUID rejecterId, String comment) {
        HomeofficeRequest request = homeofficeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Antrag nicht gefunden"));
        User rejecter = userRepository.findById(rejecterId)
                .orElseThrow(() -> new RuntimeException("Ablehnender Benutzer nicht gefunden"));

        if (request.getStatus() == HomeofficeRequest.RequestStatus.PENDING) {
            request.setStatus(HomeofficeRequest.RequestStatus.REJECTED);
            request.setApprovalDate(ZonedDateTime.now());
            request.setApprovedBy(rejecter);
            request.setComment(comment);
            return homeofficeRequestRepository.save(request);
        } else {
            throw new IllegalArgumentException("Antrag hat nicht den Status PENDING.");
        }
    }

    // Dummy für die komplexere Logik der HO-Tage-Berechnung (wäre ein eigener Service)
    // src/main/java/com/yourcompany/homeofficecalendar/service/HomeofficeDaysCalculationService.java
    // ... würde hier injiziert werden
}