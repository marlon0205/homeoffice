package de.marlon.homeoffice.repository;

import de.marlon.homeoffice.entity.HomeofficeRequest;
import de.marlon.homeoffice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Import hinzufügen
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomeofficeRequestRepository extends JpaRepository<HomeofficeRequest, UUID> {

    // Aktuelle Methode, die das Problem verursacht (da User LAZY ist)
    // List<HomeofficeRequest> findByRequestDateBetween(LocalDate startDate, LocalDate endDate);

    // NEU: Query, die den 'user' und 'approvedBy' (falls genutzt) EAGERLY lädt
    @Query("SELECT hr FROM HomeofficeRequest hr JOIN FETCH hr.user WHERE hr.requestDate BETWEEN :startDate AND :endDate")
    List<HomeofficeRequest> findByRequestDateBetweenWithUser(LocalDate startDate, LocalDate endDate);

    // Wenn du auch den "approvedBy" User brauchst, dann so:
    // @Query("SELECT hr FROM HomeofficeRequest hr JOIN FETCH hr.user LEFT JOIN FETCH hr.approvedBy WHERE hr.requestDate BETWEEN :startDate AND :endDate")
    // List<HomeofficeRequest> findByRequestDateBetweenWithUserAndApprover(LocalDate startDate, LocalDate endDate);


    // Anträge für einen bestimmten Azubi in einem Datumsbereich
    List<HomeofficeRequest> findByUserAndRequestDateBetween(User user, LocalDate startDate, LocalDate endDate);

    // Ausstehende Anträge (für Admin-Bereich) - diese brauchen eventuell auch JOINS
    // @Query("SELECT hr FROM HomeofficeRequest hr JOIN FETCH hr.user WHERE hr.status = :status")
    List<HomeofficeRequest> findByStatus(HomeofficeRequest.RequestStatus status);

    // Optional: Überprüfen, ob bereits ein Antrag für diesen Tag existiert
    Optional<HomeofficeRequest> findByUserAndRequestDate(User user, LocalDate requestDate);
}