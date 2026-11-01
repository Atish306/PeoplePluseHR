package com.atish.attendance.service;

import com.atish.attendance.dto.DayMusterDto;
import com.atish.attendance.dto.MonthlyMusterDto;
import com.atish.attendance.dto.PunchRequest;
import com.atish.attendance.entity.*;
import com.atish.attendance.repository.AttendanceRepository;
import com.atish.attendance.repository.HolidayRepository;
import com.atish.attendance.repository.LeaveRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final com.atish.attendance.config.GeofenceProperties geofenceProperties;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             HolidayRepository holidayRepository,
                             LeaveRequestRepository leaveRequestRepository,
                             com.atish.attendance.config.GeofenceProperties geofenceProperties) {
        this.attendanceRepository = attendanceRepository;
        this.holidayRepository = holidayRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.geofenceProperties = geofenceProperties;
    }

    public com.atish.attendance.config.GeofenceProperties getGeofenceProperties() {
        return geofenceProperties;
    }

    /**
     * Records an employee's punch-in for today.
     */
    @Transactional
    public Attendance recordPunchIn(User user, PunchRequest request, String clientIp) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Optional<Attendance> existing = attendanceRepository.findByUserAndWorkDate(user, today);
        if (existing.isPresent()) {
            throw new IllegalStateException("You have already punched in for today (" + today + ").");
        }

        // Determine status according to user's assigned shift & grace period
        LocalTime shiftStart = LocalTime.of(9, 0);
        int graceMinutes = 15;

        Shift shift = user.getShift();
        if (shift != null) {
            shiftStart = shift.getStartTime();
            graceMinutes = shift.getGracePeriodMinutes();
        }

        LocalTime lateThreshold = shiftStart.plusMinutes(graceMinutes);
        AttendanceStatus status = now.toLocalTime().isAfter(lateThreshold)
                ? AttendanceStatus.LATE
                : AttendanceStatus.PRESENT;

        Attendance attendance = new Attendance();
        attendance.setUser(user);
        attendance.setWorkDate(today);
        attendance.setPunchInTime(now);
        attendance.setStatus(status);
        attendance.setIpAddress(clientIp);

        // Geofencing Validation & Distance Calculation
        if (geofenceProperties != null && geofenceProperties.isEnabled()) {
            if (request == null || request.getLatitude() == null || request.getLongitude() == null) {
                String errMsg = "Punch rejected: GPS location is required to verify office presence. Please allow browser location permissions.";
                com.atish.attendance.logging.CustomLogger.printErrorLog("POST /attendance/punch-in", errMsg);
                throw new IllegalStateException(errMsg);
            }

            double distance = com.atish.attendance.util.GeoUtils.calculateDistanceMeters(
                    request.getLatitude(),
                    request.getLongitude(),
                    geofenceProperties.getLatitude(),
                    geofenceProperties.getLongitude()
            );

            attendance.setDistanceMeters(Math.round(distance * 10.0) / 10.0);

            if (distance > geofenceProperties.getRadiusMeters()) {
                attendance.setWithinGeofence(false);
                String errMsg = String.format("Punch rejected: You are %.0fm away from %s (Allowed perimeter: %.0fm).",
                        distance, geofenceProperties.getOfficeName(), geofenceProperties.getRadiusMeters());
                com.atish.attendance.logging.CustomLogger.printErrorLog("POST /attendance/punch-in", errMsg,
                        java.util.Collections.singletonMap("distanceMeters", distance), null);
                throw new IllegalStateException(errMsg);
            }

            attendance.setWithinGeofence(true);
        }

        if (request != null) {
            attendance.setInLatitude(request.getLatitude());
            attendance.setInLongitude(request.getLongitude());
            if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                attendance.setNotes(request.getNotes().trim());
            } else {
                attendance.setNotes(status == AttendanceStatus.LATE ? "Late check-in" : "On-time check-in");
            }
        }

        Attendance saved = attendanceRepository.save(attendance);
        log.info("User {} punched in at {} with status {}", user.getUsername(), now, status);
        return saved;
    }

    /**
     * Records an employee's punch-out for today and computes working duration.
     */
    @Transactional
    public Attendance recordPunchOut(User user, String clientIp) {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Attendance record = attendanceRepository.findByUserAndWorkDate(user, today)
                .orElseThrow(() -> new IllegalStateException("No punch-in record found for today. Please punch in first."));

        if (record.getPunchOutTime() != null) {
            throw new IllegalStateException("You have already punched out for today.");
        }

        record.setPunchOutTime(now);

        // Compute total hours worked rounded to 2 decimal places
        long minutesWorked = Duration.between(record.getPunchInTime(), now).toMinutes();
        double hours = BigDecimal.valueOf(minutesWorked / 60.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        record.setTotalHours(hours);

        // If worked less than 4 hours and was originally marked PRESENT, flag as HALF_DAY
        if (hours < 4.0 && record.getStatus() == AttendanceStatus.PRESENT) {
            record.setStatus(AttendanceStatus.HALF_DAY);
            record.setNotes((record.getNotes() != null ? record.getNotes() + "; " : "") + "Duration under 4 hours (Half Day)");
        }

        Attendance saved = attendanceRepository.save(record);
        log.info("User {} punched out at {} (Total Hours: {})", user.getUsername(), now, hours);
        return saved;
    }

    public Optional<Attendance> getTodayAttendance(User user) {
        return attendanceRepository.findByUserAndWorkDate(user, LocalDate.now());
    }

    public List<Attendance> getUserAttendanceHistory(User user) {
        return attendanceRepository.findByUserOrderByWorkDateDesc(user);
    }

    public List<Attendance> getAttendanceBetween(LocalDate start, LocalDate end) {
        return attendanceRepository.findByWorkDateBetween(start, end);
    }

    /**
     * Computes the complete Day-wise Monthly Attendance Muster & Breakdown
     * - Saturday & Sunday -> "R" (Rest Day)
     * - Holiday Calendar -> "H" (Company Holiday)
     * - Mon to Fri (Clocked in) -> "P" (Present)
     * - Mon to Fri (On Leave) -> "L" (Approved Leave)
     * - Mon to Fri (Past/Today not signed in) -> "A" (Absent)
     * - Mon to Fri (Future dates) -> "-" (Scheduled)
     */
    public MonthlyMusterDto getMonthlyMuster(User user, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<Attendance> attendances = attendanceRepository.findByUserAndWorkDateBetweenOrderByWorkDateAsc(user, start, end);
        Map<LocalDate, Attendance> attMap = new HashMap<>();
        for (Attendance a : attendances) {
            attMap.put(a.getWorkDate(), a);
        }

        List<Holiday> holidays = holidayRepository.findByHolidayDateBetweenOrderByHolidayDateAsc(start, end);
        Map<LocalDate, Holiday> holidayMap = new HashMap<>();
        for (Holiday h : holidays) {
            holidayMap.put(h.getHolidayDate(), h);
        }

        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByUserAndStatus(user, LeaveStatus.APPROVED);

        List<DayMusterDto> dayList = new ArrayList<>();
        int present = 0;
        int absent = 0;
        int holidayCnt = 0;
        int restDays = 0;
        int leaves = 0;
        int workingDays = 0;

        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            DayMusterDto dayDto = new DayMusterDto();
            dayDto.setDate(date);
            dayDto.setDayNumber(d);
            dayDto.setDayOfWeekShort(date.getDayOfWeek().name().substring(0, 3));
            dayDto.setToday(date.equals(today));
            dayDto.setFuture(date.isAfter(today));

            DayOfWeek dow = date.getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            boolean isHoliday = holidayMap.containsKey(date);
            Attendance att = attMap.get(date);

            if (isHoliday) {
                // Any holy day then mark H (Company Holiday)
                dayDto.setCode("H");
                dayDto.setLabel("Holiday");
                dayDto.setBadgeClass("badge-holiday");
                dayDto.setColor("#3b82f6");
                dayDto.setHolidayName(holidayMap.get(date).getName());
                holidayCnt++;
            } else if (isWeekend) {
                // Saturday & Sunday date mark R (Rest Day / Weekly Off)
                dayDto.setCode("R");
                dayDto.setLabel("Rest Day");
                dayDto.setBadgeClass("badge-rest");
                dayDto.setColor("#64748b");
                restDays++;
            } else {
                // Mon to Fri working days
                workingDays++;
                boolean onApprovedLeave = isDateInApprovedLeaves(approvedLeaves, date);
                if (att != null && (att.getStatus() == AttendanceStatus.PRESENT || att.getStatus() == AttendanceStatus.LATE || att.getStatus() == AttendanceStatus.HALF_DAY || (att.getTotalHours() != null && att.getTotalHours() > 0))) {
                    // User signed in -> mark P
                    dayDto.setCode("P");
                    dayDto.setLabel("Present");
                    dayDto.setBadgeClass("badge-present");
                    dayDto.setColor("#10b981");
                    dayDto.setAttendance(att);
                    present++;
                } else if (onApprovedLeave || (att != null && att.getStatus() == AttendanceStatus.ON_LEAVE)) {
                    // On approved leave -> mark L
                    dayDto.setCode("L");
                    dayDto.setLabel("On Leave");
                    dayDto.setBadgeClass("badge-leave");
                    dayDto.setColor("#f59e0b");
                    leaves++;
                } else if (date.isAfter(today)) {
                    // Future upcoming working days
                    dayDto.setCode("-");
                    dayDto.setLabel("Scheduled");
                    dayDto.setBadgeClass("badge-future");
                    dayDto.setColor("#94a3b8");
                } else {
                    // Mon to Fri not signed in -> mark A (Absent)
                    dayDto.setCode("A");
                    dayDto.setLabel("Absent");
                    dayDto.setBadgeClass("badge-absent");
                    dayDto.setColor("#ef4444");
                    absent++;
                }
            }
            dayList.add(dayDto);
        }

        MonthlyMusterDto dto = new MonthlyMusterDto();
        dto.setYear(year);
        dto.setMonth(month);
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        dto.setMonthName(start.format(monthFmt));
        dto.setTotalDays(ym.lengthOfMonth());

        // Alignment to Monday: DayOfWeek.getValue() returns 1 (Monday) to 7 (Sunday)
        int startOffset = start.getDayOfWeek().getValue() - 1; // 0 if Mon, 1 if Tue, ..., 6 if Sun
        int totalGridCells = startOffset + ym.lengthOfMonth();
        int trailingOffset = (7 - (totalGridCells % 7)) % 7;
        dto.setStartDayOfWeekOffset(startOffset);
        dto.setTrailingOffset(trailingOffset);

        dto.setPresentCount(present);
        dto.setAbsentCount(absent);
        dto.setHolidayCount(holidayCnt);
        dto.setRestDayCount(restDays);
        dto.setLeaveCount(leaves);
        dto.setWorkingDaysCount(workingDays);

        int pastWorkingDays = Math.max(1, present + absent + leaves);
        double pct = BigDecimal.valueOf((double) present / pastWorkingDays * 100.0)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
        dto.setAttendancePercentage(pct);
        dto.setDays(dayList);

        return dto;
    }

    private boolean isDateInApprovedLeaves(List<LeaveRequest> approvedLeaves, LocalDate date) {
        if (approvedLeaves == null) return false;
        for (LeaveRequest lr : approvedLeaves) {
            if (!date.isBefore(lr.getStartDate()) && !date.isAfter(lr.getEndDate())) {
                return true;
            }
        }
        return false;
    }
}
