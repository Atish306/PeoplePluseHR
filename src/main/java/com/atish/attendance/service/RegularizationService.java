package com.atish.attendance.service;

import com.atish.attendance.entity.*;
import com.atish.attendance.repository.AttendanceRepository;
import com.atish.attendance.repository.NotificationRepository;
import com.atish.attendance.repository.RegularizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class RegularizationService {

    private static final Logger log = LoggerFactory.getLogger(RegularizationService.class);

    private final RegularizationRepository regularizationRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;

    public RegularizationService(RegularizationRepository regularizationRepository,
                                 AttendanceRepository attendanceRepository,
                                 NotificationRepository notificationRepository,
                                 EmailNotificationService emailNotificationService) {
        this.regularizationRepository = regularizationRepository;
        this.attendanceRepository = attendanceRepository;
        this.notificationRepository = notificationRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Employee applies for Attendance Regularization.
     */
    @Transactional
    public AttendanceRegularization applyRegularization(User user, LocalDate startDate, LocalDate endDate,
                                                         String reason, LocalTime checkIn, LocalTime checkOut) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and End date are mandatory.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("A valid reason for regularization is required.");
        }

        AttendanceRegularization reg = new AttendanceRegularization(user, startDate, endDate, reason.trim());
        if (checkIn != null) reg.setCheckInTime(checkIn);
        if (checkOut != null) reg.setCheckOutTime(checkOut);

        AttendanceRegularization saved = regularizationRepository.save(reg);
        log.info("Attendance regularization #{} applied by user {} for {} days ({} to {})",
                saved.getId(), user.getUsername(), saved.getTotalDays(), startDate, endDate);
        return saved;
    }

    /**
     * Manager Approval Rule:
     * - If totalDays <= 7: Manager approves all days -> Status = APPROVED.
     * - If totalDays > 7 (e.g. 9 days): Manager approves up to 7 days -> Status = PENDING_ADMIN (remaining days forwarded to Admin).
     */
    @Transactional
    public AttendanceRegularization managerApprove(Long regularizationId, User manager, String comments) {
        AttendanceRegularization reg = regularizationRepository.findById(regularizationId)
                .orElseThrow(() -> new IllegalArgumentException("Regularization request not found: " + regularizationId));

        if (reg.getStatus() != RegularizationStatus.PENDING_MANAGER) {
            throw new IllegalStateException("Request is not pending Manager approval (current status: " + reg.getStatus() + ")");
        }

        reg.setManagerReviewer(manager);
        reg.setManagerComments(comments != null ? comments.trim() : "Approved by Manager");
        reg.setManagerReviewedAt(LocalDateTime.now());

        long totalDays = reg.getTotalDays();
        User employee = reg.getUser();

        if (totalDays <= 7) {
            // Case 1: All days approved by Manager
            reg.setManagerApprovedDays(totalDays);
            reg.setAdminApprovedDays(0);
            reg.setStatus(RegularizationStatus.APPROVED);

            syncAttendanceRange(employee, reg.getStartDate(), reg.getEndDate(), reg.getCheckInTime(), reg.getCheckOutTime(),
                    "Regularized (" + totalDays + " days approved by Manager)");

            // In-App Notification
            notificationRepository.save(new Notification(
                    employee,
                    "Attendance Regularization Approved ✅",
                    "Your attendance regularization for " + totalDays + " day(s) from " + reg.getStartDate()
                            + " to " + reg.getEndDate() + " has been approved by Manager (" + manager.getFullName() + ").",
                    "REGULARIZATION_APPROVED"
            ));
        } else {
            // Case 2: 7 days approved by Manager; remaining days forward to Admin
            reg.setManagerApprovedDays(7);
            reg.setAdminApprovedDays(0);
            reg.setStatus(RegularizationStatus.PENDING_ADMIN);

            // Sync first 7 days to attendance
            LocalDate sevenDaysEnd = reg.getStartDate().plusDays(6);
            syncAttendanceRange(employee, reg.getStartDate(), sevenDaysEnd, reg.getCheckInTime(), reg.getCheckOutTime(),
                    "Regularized: 7 days approved by Manager (Remaining days pending Admin)");

            long remainingDays = totalDays - 7;

            // In-App Notification to employee
            notificationRepository.save(new Notification(
                    employee,
                    "Attendance Regularization: 7 Days Approved by Manager ⏳",
                    "Manager approved 7 days of your " + totalDays + "-day request (" + reg.getStartDate() + " to " + sevenDaysEnd + "). "
                            + "The remaining " + remainingDays + " day(s) have been forwarded to Admin for approval.",
                    "REGULARIZATION_STAGE1"
            ));
        }

        AttendanceRegularization saved = regularizationRepository.save(reg);
        log.info("Regularization #{} reviewed by Manager {}: status={}, managerApprovedDays={}",
                saved.getId(), manager.getUsername(), saved.getStatus(), saved.getManagerApprovedDays());
        return saved;
    }

    /**
     * Admin Approval Rule:
     * - Approves the remaining days (e.g. if 9 days total and 7 approved by manager, approves the remaining 2 days).
     * - Status becomes APPROVED.
     */
    @Transactional
    public AttendanceRegularization adminApprove(Long regularizationId, User admin, String comments) {
        AttendanceRegularization reg = regularizationRepository.findById(regularizationId)
                .orElseThrow(() -> new IllegalArgumentException("Regularization request not found: " + regularizationId));

        if (reg.getStatus() == RegularizationStatus.APPROVED || reg.getStatus() == RegularizationStatus.REJECTED) {
            throw new IllegalStateException("Request is already finalized (status: " + reg.getStatus() + ")");
        }

        reg.setAdminReviewer(admin);
        reg.setAdminComments(comments != null ? comments.trim() : "Approved by Admin");
        reg.setAdminReviewedAt(LocalDateTime.now());

        User employee = reg.getUser();
        long totalDays = reg.getTotalDays();

        if (reg.getStatus() == RegularizationStatus.PENDING_ADMIN) {
            // Manager already approved 7 days; Admin approves the rest (e.g. 2 days)
            long remainingDays = totalDays - reg.getManagerApprovedDays();
            reg.setAdminApprovedDays(remainingDays);
            reg.setStatus(RegularizationStatus.APPROVED);

            LocalDate restStart = reg.getStartDate().plusDays(reg.getManagerApprovedDays());
            syncAttendanceRange(employee, restStart, reg.getEndDate(), reg.getCheckInTime(), reg.getCheckOutTime(),
                    "Regularized: Remaining " + remainingDays + " days approved by Admin");

            notificationRepository.save(new Notification(
                    employee,
                    "Attendance Regularization Fully Approved ✅",
                    "Your " + totalDays + "-day attendance regularization (" + reg.getStartDate() + " to " + reg.getEndDate()
                            + ") is now fully approved! (7 days by Manager, " + remainingDays + " days by Admin).",
                    "REGULARIZATION_APPROVED"
            ));
        } else if (reg.getStatus() == RegularizationStatus.PENDING_MANAGER) {
            // Direct Admin full approval
            long mngDays = Math.min(totalDays, 7);
            long admDays = Math.max(0, totalDays - 7);
            reg.setManagerApprovedDays(mngDays);
            reg.setAdminApprovedDays(admDays);
            reg.setStatus(RegularizationStatus.APPROVED);

            syncAttendanceRange(employee, reg.getStartDate(), reg.getEndDate(), reg.getCheckInTime(), reg.getCheckOutTime(),
                    "Regularized & Approved directly by Admin");

            notificationRepository.save(new Notification(
                    employee,
                    "Attendance Regularization Approved by Admin ✅",
                    "Your " + totalDays + "-day attendance regularization has been approved directly by Admin (" + admin.getFullName() + ").",
                    "REGULARIZATION_APPROVED"
            ));
        }

        AttendanceRegularization saved = regularizationRepository.save(reg);
        log.info("Regularization #{} finalized by Admin {}: totalDays={}, mngDays={}, admDays={}",
                saved.getId(), admin.getUsername(), saved.getTotalDays(), saved.getManagerApprovedDays(), saved.getAdminApprovedDays());
        return saved;
    }

    /**
     * Reject Regularization request.
     */
    @Transactional
    public AttendanceRegularization reject(Long regularizationId, User reviewer, String comments) {
        AttendanceRegularization reg = regularizationRepository.findById(regularizationId)
                .orElseThrow(() -> new IllegalArgumentException("Regularization request not found: " + regularizationId));

        reg.setStatus(RegularizationStatus.REJECTED);
        if (reviewer.getRole() == Role.ROLE_ADMIN || reviewer.getRole() == Role.ROLE_SUPER_ADMIN) {
            reg.setAdminReviewer(reviewer);
            reg.setAdminComments(comments != null ? comments.trim() : "Rejected by Admin");
            reg.setAdminReviewedAt(LocalDateTime.now());
        } else {
            reg.setManagerReviewer(reviewer);
            reg.setManagerComments(comments != null ? comments.trim() : "Rejected by Manager");
            reg.setManagerReviewedAt(LocalDateTime.now());
        }

        notificationRepository.save(new Notification(
                reg.getUser(),
                "Attendance Regularization Rejected ❌",
                "Your regularization request from " + reg.getStartDate() + " to " + reg.getEndDate() + " was rejected. Reason: "
                        + (comments != null ? comments : "Not specified"),
                "REGULARIZATION_REJECTED"
        ));

        return regularizationRepository.save(reg);
    }

    private void syncAttendanceRange(User user, LocalDate from, LocalDate to, LocalTime checkIn, LocalTime checkOut, String note) {
        LocalDate curr = from;
        while (!curr.isAfter(to)) {
            Optional<Attendance> existing = attendanceRepository.findByUserAndWorkDate(user, curr);
            Attendance att = existing.orElseGet(Attendance::new);
            att.setUser(user);
            att.setWorkDate(curr);
            att.setPunchInTime(LocalDateTime.of(curr, checkIn != null ? checkIn : LocalTime.of(9, 0)));
            att.setPunchOutTime(LocalDateTime.of(curr, checkOut != null ? checkOut : LocalTime.of(18, 0)));
            att.setStatus(AttendanceStatus.PRESENT);
            att.setTotalHours(9.0);
            att.setNotes(note);
            attendanceRepository.save(att);
            curr = curr.plusDays(1);
        }
    }

    public List<AttendanceRegularization> getUserRequests(User user) {
        return regularizationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<AttendanceRegularization> getPendingManagerRequests() {
        return regularizationRepository.findByStatusOrderByCreatedAtDesc(RegularizationStatus.PENDING_MANAGER);
    }

    public List<AttendanceRegularization> getPendingAdminRequests() {
        return regularizationRepository.findByStatusOrderByCreatedAtDesc(RegularizationStatus.PENDING_ADMIN);
    }

    public List<AttendanceRegularization> getAllRequests() {
        return regularizationRepository.findAllByOrderByCreatedAtDesc();
    }
}
