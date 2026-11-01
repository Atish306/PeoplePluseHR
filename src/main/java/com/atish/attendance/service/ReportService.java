package com.atish.attendance.service;

import com.atish.attendance.entity.Attendance;
import com.atish.attendance.entity.AttendanceStatus;
import com.atish.attendance.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final AttendanceRepository attendanceRepository;

    public ReportService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<Attendance> getFilteredRecords(LocalDate startDate, LocalDate endDate, Long departmentId, Long userId, AttendanceStatus status) {
        List<Attendance> records = attendanceRepository.findByWorkDateBetween(startDate, endDate);

        return records.stream()
                .filter(r -> {
                    if (departmentId != null) {
                        return r.getUser() != null &&
                                r.getUser().getDepartment() != null &&
                                departmentId.equals(r.getUser().getDepartment().getId());
                    }
                    return true;
                })
                .filter(r -> {
                    if (userId != null) {
                        return r.getUser() != null && userId.equals(r.getUser().getId());
                    }
                    return true;
                })
                .filter(r -> {
                    if (status != null) {
                        return r.getStatus() == status;
                    }
                    return true;
                })
                .sorted((a, b) -> b.getWorkDate().compareTo(a.getWorkDate()))
                .collect(Collectors.toList());
    }

    public Optional<Attendance> getRecordById(Long id) {
        return attendanceRepository.findById(id);
    }

    public ReportMetrics calculateMetrics(List<Attendance> records) {
        ReportMetrics metrics = new ReportMetrics();
        metrics.setTotalRecords(records.size());

        double totalHours = 0.0;
        int present = 0;
        int late = 0;
        int halfDay = 0;
        int absent = 0;
        int onLeave = 0;

        for (Attendance r : records) {
            if (r.getTotalHours() != null) {
                totalHours += r.getTotalHours();
            }

            if (r.getStatus() != null) {
                switch (r.getStatus()) {
                    case PRESENT: present++; break;
                    case LATE: late++; break;
                    case HALF_DAY: halfDay++; break;
                    case ABSENT: absent++; break;
                    case ON_LEAVE: onLeave++; break;
                }
            }
        }

        metrics.setTotalHours(BigDecimal.valueOf(totalHours).setScale(2, RoundingMode.HALF_UP).doubleValue());
        metrics.setPresentCount(present);
        metrics.setLateCount(late);
        metrics.setHalfDayCount(halfDay);
        metrics.setAbsentCount(absent);
        metrics.setOnLeaveCount(onLeave);

        double avgHours = records.isEmpty() ? 0.0 : totalHours / records.size();
        metrics.setAverageHours(BigDecimal.valueOf(avgHours).setScale(2, RoundingMode.HALF_UP).doubleValue());

        int totalPunched = present + late;
        double punctualityRate = (totalPunched > 0) ? ((double) present / totalPunched) * 100.0 : 100.0;
        metrics.setPunctualityRate(BigDecimal.valueOf(punctualityRate).setScale(1, RoundingMode.HALF_UP).doubleValue());

        return metrics;
    }

    public static class ReportMetrics {
        private int totalRecords = 0;
        private double totalHours = 0.0;
        private double averageHours = 0.0;
        private double punctualityRate = 100.0;
        private int presentCount = 0;
        private int lateCount = 0;
        private int halfDayCount = 0;
        private int absentCount = 0;
        private int onLeaveCount = 0;

        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

        public double getTotalHours() { return totalHours; }
        public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

        public double getAverageHours() { return averageHours; }
        public void setAverageHours(double averageHours) { this.averageHours = averageHours; }

        public double getPunctualityRate() { return punctualityRate; }
        public void setPunctualityRate(double punctualityRate) { this.punctualityRate = punctualityRate; }

        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }

        public int getLateCount() { return lateCount; }
        public void setLateCount(int lateCount) { this.lateCount = lateCount; }

        public int getHalfDayCount() { return halfDayCount; }
        public void setHalfDayCount(int halfDayCount) { this.halfDayCount = halfDayCount; }

        public int getAbsentCount() { return absentCount; }
        public void setAbsentCount(int absentCount) { this.absentCount = absentCount; }

        public int getOnLeaveCount() { return onLeaveCount; }
        public void setOnLeaveCount(int onLeaveCount) { this.onLeaveCount = onLeaveCount; }
    }
}
