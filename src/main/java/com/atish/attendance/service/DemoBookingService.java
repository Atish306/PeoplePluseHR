package com.atish.attendance.service;

import com.atish.attendance.dto.DemoBookingRequestDto;
import com.atish.attendance.entity.DemoBooking;
import com.atish.attendance.entity.DemoStatus;
import com.atish.attendance.logging.CustomLogger;
import com.atish.attendance.repository.DemoBookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DemoBookingService {

    private final DemoBookingRepository demoBookingRepository;
    private final EmailNotificationService emailNotificationService;

    public DemoBookingService(DemoBookingRepository demoBookingRepository,
                              EmailNotificationService emailNotificationService) {
        this.demoBookingRepository = demoBookingRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public DemoBooking createBooking(DemoBookingRequestDto dto, String clientIp) {
        String reference = generateBookingReference();

        String joinedModules = "";
        if (dto.getModules() != null && !dto.getModules().isEmpty()) {
            joinedModules = String.join(", ", dto.getModules());
        }

        LocalDate preferredDate = dto.getPreferredDate();
        if (preferredDate == null) {
            preferredDate = LocalDate.now().plusDays(2);
        }

        String preferredTimeSlot = dto.getPreferredTimeSlot();
        if (preferredTimeSlot == null || preferredTimeSlot.trim().isEmpty()) {
            preferredTimeSlot = "Morning (10:00 - 11:00 AM IST)";
        }

        String country = dto.getCountry() != null && !dto.getCountry().trim().isEmpty() ? dto.getCountry().trim() : "India";

        DemoBooking booking = new DemoBooking(
                reference,
                dto.getFirstName() != null ? dto.getFirstName().trim() : "",
                dto.getLastName() != null ? dto.getLastName().trim() : "",
                dto.getWorkEmail() != null ? dto.getWorkEmail().trim().toLowerCase() : "",
                dto.getPhone() != null ? dto.getPhone().trim() : "",
                dto.getCompanyName() != null ? dto.getCompanyName().trim() : "",
                dto.getJobTitle() != null ? dto.getJobTitle().trim() : "",
                country,
                dto.getCompanySize() != null ? dto.getCompanySize() : "1 - 25",
                joinedModules,
                preferredDate,
                preferredTimeSlot,
                dto.getNotes() != null ? dto.getNotes().trim() : "",
                clientIp
        );

        DemoBooking saved = demoBookingRepository.save(booking);

        // Send notifications
        emailNotificationService.sendDemoConfirmationToProspect(saved);
        emailNotificationService.sendDemoAlertToSalesTeam(saved);

        Map<String, Object> logDetails = new HashMap<>();
        logDetails.put("bookingReference", saved.getBookingReference());
        logDetails.put("company", saved.getCompanyName());
        logDetails.put("country", saved.getCountry());
        logDetails.put("prospect", saved.getFullName());
        logDetails.put("email", saved.getWorkEmail());
        logDetails.put("preferredDate", saved.getPreferredDate().toString());
        CustomLogger.printSuccessLog("DemoBookingService.createBooking", "New demo booking created", 0, logDetails);

        return saved;
    }

    public Optional<DemoBooking> getBookingByReference(String reference) {
        return demoBookingRepository.findByBookingReference(reference);
    }

    public List<DemoBooking> getAllBookings() {
        return demoBookingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<DemoBooking> getBookingsByStatus(DemoStatus status) {
        if (status == null) {
            return getAllBookings();
        }
        return demoBookingRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public DemoBooking updateStatus(Long id, DemoStatus newStatus) {
        DemoBooking booking = demoBookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demo booking not found for ID: " + id));
        DemoStatus oldStatus = booking.getStatus();
        booking.setStatus(newStatus);
        DemoBooking updated = demoBookingRepository.save(booking);

        Map<String, Object> logDetails = new HashMap<>();
        logDetails.put("bookingId", id);
        logDetails.put("reference", updated.getBookingReference());
        logDetails.put("oldStatus", oldStatus);
        logDetails.put("newStatus", newStatus);
        CustomLogger.printSuccessLog("DemoBookingService.updateStatus", "Demo booking status updated", 0, logDetails);

        return updated;
    }

    public Map<String, Long> getMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("total", demoBookingRepository.count());
        metrics.put("pending", demoBookingRepository.countByStatus(DemoStatus.PENDING));
        metrics.put("confirmed", demoBookingRepository.countByStatus(DemoStatus.CONFIRMED));
        metrics.put("completed", demoBookingRepository.countByStatus(DemoStatus.COMPLETED));
        metrics.put("cancelled", demoBookingRepository.countByStatus(DemoStatus.CANCELLED));
        return metrics;
    }

    private String generateBookingReference() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String randPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "DEMO-" + datePart + "-" + randPart;
    }
}
