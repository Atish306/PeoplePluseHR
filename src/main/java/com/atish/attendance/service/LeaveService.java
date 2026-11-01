package com.atish.attendance.service;

import com.atish.attendance.dto.LeaveApplyDto;
import com.atish.attendance.entity.*;
import com.atish.attendance.repository.AttendanceRepository;
import com.atish.attendance.repository.LeaveBalanceRepository;
import com.atish.attendance.repository.LeaveRequestRepository;
import com.atish.attendance.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailNotificationService;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        AttendanceRepository attendanceRepository,
                        LeaveBalanceRepository leaveBalanceRepository,
                        NotificationRepository notificationRepository,
                        EmailNotificationService emailNotificationService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.notificationRepository = notificationRepository;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Retrieve or initialize 2026 leave balances for the given user.
     */
    @Transactional
    public List<LeaveBalance> getUserLeaveBalances(User user, int year) {
        List<LeaveType> standardTypes = Arrays.asList(
                LeaveType.CASUAL,
                LeaveType.SICK,
                LeaveType.EARNED,
                LeaveType.COMP_OFF,
                LeaveType.LOSS_OF_PAY
        );

        List<LeaveBalance> balances = new ArrayList<>();
        for (LeaveType type : standardTypes) {
            Optional<LeaveBalance> existing = leaveBalanceRepository.findByUserAndLeaveTypeAndCalendarYear(user, type, year);
            if (existing.isPresent()) {
                balances.add(existing.get());
            } else {
                double quota;
                switch (type) {
                    case CASUAL:
                        quota = 12.0;
                        break;
                    case SICK:
                        quota = 10.0;
                        break;
                    case EARNED:
                        quota = 15.0;
                        break;
                    case COMP_OFF:
                        quota = 3.0;
                        break;
                    default:
                        quota = 0.0;
                        break;
                }
                LeaveBalance newBal = new LeaveBalance(user, type, quota, 0.0, 0.0, year);
                balances.add(leaveBalanceRepository.save(newBal));
            }
        }
        return balances;
    }

    @Transactional
    public LeaveBalance getOrInitBalance(User user, LeaveType leaveType, int year) {
        return leaveBalanceRepository.findByUserAndLeaveTypeAndCalendarYear(user, leaveType, year)
                .orElseGet(() -> {
                    double quota;
                    switch (leaveType) {
                        case CASUAL:
                            quota = 12.0;
                            break;
                        case SICK:
                            quota = 10.0;
                            break;
                        case EARNED:
                        case ANNUAL:
                            quota = 15.0;
                            break;
                        case COMP_OFF:
                            quota = 3.0;
                            break;
                        default:
                            quota = 0.0;
                            break;
                    }
                    LeaveBalance newBal = new LeaveBalance(user, leaveType, quota, 0.0, 0.0, year);
                    return leaveBalanceRepository.save(newBal);
                });
    }

    @Transactional
    public LeaveRequest applyLeave(User user, LeaveApplyDto dto) {
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and End date are mandatory.");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be prior to start date.");
        }
        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("A valid reason for leave must be provided.");
        }

        long requestedDays = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        if (requestedDays <= 0) {
            throw new IllegalArgumentException("Leave duration must be at least 1 day.");
        }

        LeaveType type = dto.getLeaveType();
        int calendarYear = dto.getStartDate().getYear();

        // Check if there is an overlapping leave request
        List<LeaveRequest> existingLeaves = leaveRequestRepository.findByUserOrderByCreatedAtDesc(user);
        for (LeaveRequest req : existingLeaves) {
            if (req.getStatus() != LeaveStatus.REJECTED) {
                boolean overlap = !dto.getStartDate().isAfter(req.getEndDate()) && !dto.getEndDate().isBefore(req.getStartDate());
                if (overlap) {
                    throw new IllegalStateException("Overlapping leave request already exists between "
                            + req.getStartDate() + " and " + req.getEndDate() + " (Status: " + req.getStatus() + ")");
                }
            }
        }

        // Quota check: Loss of pay has unlimited allowance (unpaid tracker)
        if (type != LeaveType.LOSS_OF_PAY) {
            LeaveBalance balance = getOrInitBalance(user, type, calendarYear);
            double available = balance.getAvailable();
            if (requestedDays > available) {
                throw new IllegalStateException(String.format(
                        "Insufficient %s balance! Requested: %d days, Available: %.1f days (Total: %.1f, Used: %.1f, Pending: %.1f)",
                        type.getDisplayName(), requestedDays, available, balance.getTotalQuota(), balance.getUsed(), balance.getPending()
                ));
            }

            // Reserve the requested days into pending balance
            balance.setPending(balance.getPending() + requestedDays);
            leaveBalanceRepository.save(balance);
        }

        LeaveRequest leaveRequest = new LeaveRequest(
                user,
                type,
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getReason().trim()
        );

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request #{} ({} for {} days) submitted by user {}",
                saved.getId(), type.getDisplayName(), requestedDays, user.getUsername());
        return saved;
    }

    /**
     * Requirement 3: Approve leave and send notification + email to employee.
     */
    @Transactional
    public LeaveRequest approveLeave(Long leaveRequestId, User reviewer, String adminComments) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found with ID: " + leaveRequestId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request has already been reviewed.");
        }

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setReviewedByUser(reviewer);
        leaveRequest.setAdminComments(adminComments != null ? adminComments.trim() : "Approved");

        long days = leaveRequest.getDurationInDays();
        User employee = leaveRequest.getUser();
        int year = leaveRequest.getStartDate().getYear();

        // Update Leave Balance: Deduct from pending, credit to used
        LeaveBalance balance = getOrInitBalance(employee, leaveRequest.getLeaveType(), year);
        balance.setPending(Math.max(0.0, balance.getPending() - days));
        balance.setUsed(balance.getUsed() + days);
        leaveBalanceRepository.save(balance);

        // Sync with Attendance table for each date in the approved leave range
        LocalDate curr = leaveRequest.getStartDate();
        LocalDate end = leaveRequest.getEndDate();

        while (!curr.isAfter(end)) {
            Optional<Attendance> existingAtt = attendanceRepository.findByUserAndWorkDate(employee, curr);
            if (existingAtt.isPresent()) {
                Attendance att = existingAtt.get();
                att.setStatus(AttendanceStatus.ON_LEAVE);
                att.setNotes("Approved " + leaveRequest.getLeaveType().getDisplayName());
                attendanceRepository.save(att);
            } else {
                Attendance att = new Attendance();
                att.setUser(employee);
                att.setWorkDate(curr);
                att.setStatus(AttendanceStatus.ON_LEAVE);
                att.setTotalHours(0.0);
                att.setNotes("Approved " + leaveRequest.getLeaveType().getDisplayName());
                attendanceRepository.save(att);
            }
            curr = curr.plusDays(1);
        }

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request #{} approved by reviewer {}", leaveRequestId, reviewer.getUsername());

        // 1. In-App Notification
        try {
            Notification notif = new Notification(
                    employee,
                    "Leave Request Approved ✅",
                    "Your leave application for " + leaveRequest.getLeaveType().getDisplayName()
                            + " (" + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + ") has been approved.",
                    "LEAVE_APPROVAL"
            );
            notificationRepository.save(notif);
        } catch (Exception ex) {
            log.error("Failed to create in-app notification for leave #{}", leaveRequestId, ex);
        }

        // 2. Dispatch Email to Employee
        try {
            emailNotificationService.sendLeaveStatusNotification(saved, "APPROVED");
        } catch (Exception ex) {
            log.error("Failed to dispatch leave approval email for leave #{}", leaveRequestId, ex);
        }

        return saved;
    }

    /**
     * Requirement 3: Reject leave and send notification + email to employee.
     */
    @Transactional
    public LeaveRequest rejectLeave(Long leaveRequestId, User reviewer, String adminComments) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found with ID: " + leaveRequestId));

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request has already been reviewed.");
        }

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setReviewedByUser(reviewer);
        leaveRequest.setAdminComments(adminComments != null ? adminComments.trim() : "Rejected by management");

        long days = leaveRequest.getDurationInDays();
        User employee = leaveRequest.getUser();
        int year = leaveRequest.getStartDate().getYear();

        // Release reserved pending balance back to available quota
        LeaveBalance balance = getOrInitBalance(employee, leaveRequest.getLeaveType(), year);
        balance.setPending(Math.max(0.0, balance.getPending() - days));
        leaveBalanceRepository.save(balance);

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Leave request #{} rejected by reviewer {}", leaveRequestId, reviewer.getUsername());

        // 1. In-App Notification
        try {
            Notification notif = new Notification(
                    employee,
                    "Leave Request Rejected ❌",
                    "Your leave application for " + leaveRequest.getLeaveType().getDisplayName()
                            + " was rejected. Reason: " + saved.getAdminComments(),
                    "LEAVE_REJECTION"
            );
            notificationRepository.save(notif);
        } catch (Exception ex) {
            log.error("Failed to create in-app notification for leave #{}", leaveRequestId, ex);
        }

        // 2. Dispatch Email to Employee
        try {
            emailNotificationService.sendLeaveStatusNotification(saved, "REJECTED");
        } catch (Exception ex) {
            log.error("Failed to dispatch leave rejection email for leave #{}", leaveRequestId, ex);
        }

        return saved;
    }

    public List<LeaveRequest> getUserLeaves(User user) {
        return leaveRequestRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<LeaveRequest> getPendingLeaves() {
        return leaveRequestRepository.findByStatusOrderByCreatedAtDesc(LeaveStatus.PENDING);
    }

    public List<LeaveRequest> getAllLeaves() {
        return leaveRequestRepository.findAllByOrderByCreatedAtDesc();
    }
}
