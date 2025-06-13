package de.marlon.homeoffice.repository;

import de.marlon.homeoffice.model.HomeofficeRequest;
import de.marlon.homeoffice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HomeofficeRequestRepository extends JpaRepository<HomeofficeRequest, Long> {
    List<HomeofficeRequest> findByUser(User user);
    List<HomeofficeRequest> findByUserAndRequestDate(User user, LocalDate date);
    List<HomeofficeRequest> findByStatus(HomeofficeRequest.RequestStatus status);
    List<HomeofficeRequest> findByUserAndRequestDateBetween(User user, LocalDate startDate, LocalDate endDate);
    List<HomeofficeRequest> findByRequestDateBetween(LocalDate startDate, LocalDate endDate);
    long countByUserAndRequestDateBetweenAndStatus(User user, LocalDate startDate, LocalDate endDate, HomeofficeRequest.RequestStatus status);
}