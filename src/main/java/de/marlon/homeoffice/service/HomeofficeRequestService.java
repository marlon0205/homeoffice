package de.marlon.homeoffice.service;

import de.marlon.homeoffice.model.HomeofficeRequest;
import de.marlon.homeoffice.model.User;
import de.marlon.homeoffice.repository.HomeofficeRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class HomeofficeRequestService {

    private final HomeofficeRequestRepository homeofficeRequestRepository;
    private final UserService userService; // Zum Abrufen der maximalen HO-Tage

    public HomeofficeRequestService(HomeofficeRequestRepository homeofficeRequestRepository, UserService userService) {
        this.homeofficeRequestRepository = homeofficeRequestRepository;
        this.userService = userService;
    }

    @Transactional
    public HomeofficeRequest createRequest(User user, LocalDate date, boolean halfDay) {
        // Check if user already has a request for this date
        if (!homeofficeRequestRepository.findByUserAndRequestDate(user, date).isEmpty()) {
            throw new IllegalArgumentException("You already have a homeoffice request for this date");
        }

        HomeofficeRequest request = new HomeofficeRequest();
        request.setUser(user);
        request.setRequestDate(date);
        request.setHalfDay(halfDay);
        request.setStatus(HomeofficeRequest.RequestStatus.PENDING);

        return homeofficeRequestRepository.save(request);
    }

    public List<HomeofficeRequest> getUserRequests(User user) {
        return homeofficeRequestRepository.findByUser(user);
    }

    public List<HomeofficeRequest> getPendingRequests() {
        return homeofficeRequestRepository.findByStatus(HomeofficeRequest.RequestStatus.PENDING);
    }

    @Transactional
    public HomeofficeRequest updateRequestStatus(Long requestId, HomeofficeRequest.RequestStatus status) {
        HomeofficeRequest request = homeofficeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        request.setStatus(status);
        return homeofficeRequestRepository.save(request);
    }

    public List<HomeofficeRequest> getAllHomeofficeRequests() {
        return homeofficeRequestRepository.findAll();
    }

    public List<HomeofficeRequest> getHomeofficeRequestsForMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        return homeofficeRequestRepository.findByRequestDateBetween(startDate, endDate);
    }
}