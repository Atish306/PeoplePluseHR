package com.atish.attendance.service;

import com.atish.attendance.entity.*;
import com.atish.attendance.repository.AttendanceRepository;
import com.atish.attendance.repository.LeaveRequestRepository;
import com.atish.attendance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class ScheduledAttendanceTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledAttendanceTasks.class);

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final com.atish.attendance.repository.HolidayRepository holidayRepository;

    public ScheduledAttendanceTasks(UserRepository userRepository,
                                    AttendanceRepository attendanceRepository,
                                    LeaveRequestRepository leaveRequestRepository,
                                    com.atish.attendance.repository.HolidayRepository holidayRepository) {
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.holidayRepository = holidayRepository;
    }

    /**
     * Runs automatically every night at 23:59:00 to finalize daily attendance records.
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void executeMidnightAudit() {
        LocalDate today = LocalDate.now();
        log.info("Starting automated midnight attendance audit for date: {}", today);
        AuditResult result = runDailyAttendanceAudit(today);
        log.info("Midnight audit completed. Absentees marked: {}, On-Leave marked: {}, Unclosed punches resolved: {}",
                result.getAbsenteesMarked(), result.getOnLeaveMarked(), result.getUnclosedResolved());
    }

    /**
     * Executes the attendance reconciliation for a given date.
     * Can be invoked either automatically by cron or manually on-demand by an administrator.
     */
    @Transactional
    public AuditResult runDailyAttendanceAudit(LocalDate auditDate) {
        AuditResult result = new AuditResult();
        List<User> activeEmployees = userRepository.findByActiveTrue();

        for (User user : activeEmployees) {
            // Skip Super Admins from mandatory daily attendance checks
            if (user.getRole() == Role.ROLE_ADMIN) {
                continue;
            }

            result.incrementTotalProcessed();
            Optional<Attendance> recordOpt = attendanceRepository.findByUserAndWorkDate(user, auditDate);

            if (!recordOpt.isPresent()) {
                // Check if date is a Company Holiday
                Optional<Holiday> holidayOpt = holidayRepository.findByHolidayDate(auditDate);

                // Check if user has an approved leave on this date
                List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedLeaveOnDate(user, auditDate);

                Attendance attendance = new Attendance();
                attendance.setUser(user);
                attendance.setWorkDate(auditDate);
                attendance.setTotalHours(0.0);

                if (holidayOpt.isPresent()) {
                    attendance.setStatus(AttendanceStatus.HOLIDAY);
                    attendance.setNotes("Company Holiday: " + holidayOpt.get().getName());
                    result.incrementOnLeave();
                } else if (!approvedLeaves.isEmpty()) {
                    attendance.setStatus(AttendanceStatus.ON_LEAVE);
                    attendance.setNotes("Approved " + approvedLeaves.get(0).getLeaveType().getDisplayName());
                    result.incrementOnLeave();
                } else {
                    attendance.setStatus(AttendanceStatus.ABSENT);
                    attendance.setNotes("Automatically marked absent by system (no clock-in recorded)");
                    result.incrementAbsentees();
                }
                attendanceRepository.save(attendance);
            } else {
                // If user punched in but forgot to punch out before midnight
                Attendance record = recordOpt.get();
                if (record.getPunchInTime() != null && record.getPunchOutTime() == null) {
                    LocalDateTime autoPunchOut = LocalDateTime.of(auditDate, LocalTime.of(23, 59));
                    record.setPunchOutTime(autoPunchOut);

                    long minutes = Duration.between(record.getPunchInTime(), autoPunchOut).toMinutes();
                    double hours = BigDecimal.valueOf(minutes / 60.0)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
                    record.setTotalHours(hours);

                    String note = record.getNotes() != null ? record.getNotes() + "; " : "";
                    record.setNotes(note + "Auto-closed: missing punch-out before midnight");
                    attendanceRepository.save(record);
                    result.incrementUnclosed();
                }
            }
        }

        return result;
    }

    /**
     * Result metrics transfer object for the audit run.
     */
    public static class AuditResult {
        private int totalProcessed = 0;
        private int absenteesMarked = 0;
        private int onLeaveMarked = 0;
        private int unclosedResolved = 0;

        public void incrementTotalProcessed() { this.totalProcessed++; }
        public void incrementAbsentees() { this.absenteesMarked++; }
        public void incrementOnLeave() { this.onLeaveMarked++; }
        public void incrementUnclosed() { this.unclosedResolved++; }

        public int getTotalProcessed() { return totalProcessed; }
        public int getAbsenteesMarked() { return absenteesMarked; }
        public int getOnLeaveMarked() { return onLeaveMarked; }
        public int getUnclosedResolved() { return unclosedResolved; }
    }
}
