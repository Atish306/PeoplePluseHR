package com.atish.attendance.controller;

import com.atish.attendance.entity.Attendance;
import com.atish.attendance.entity.AttendanceStatus;
import com.atish.attendance.repository.DepartmentRepository;
import com.atish.attendance.repository.UserRepository;
import com.atish.attendance.service.ReportService;
import com.atish.attendance.util.CsvExportUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ReportController {

    private final ReportService reportService;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public ReportController(ReportService reportService,
                            DepartmentRepository departmentRepository,
                            UserRepository userRepository) {
        this.reportService = reportService;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/reports/summary")
    public String showReportSummary(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "status", required = false) AttendanceStatus status,
            Model model) {

        // Default to current month if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Attendance> records = reportService.getFilteredRecords(startDate, endDate, departmentId, userId, status);
        ReportService.ReportMetrics metrics = reportService.calculateMetrics(records);

        model.addAttribute("records", records);
        model.addAttribute("metrics", metrics);
        model.addAttribute("departments", departmentRepository.findAll());
        model.addAttribute("users", userRepository.findByActiveTrue());
        model.addAttribute("statuses", AttendanceStatus.values());
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("selectedDept", departmentId);
        model.addAttribute("selectedUser", userId);
        model.addAttribute("selectedStatus", status);

        return "reports/summary";
    }

    @GetMapping("/reports/view/{id}")
    @ResponseBody
    public ResponseEntity<?> getTimesheetDetailJson(@PathVariable("id") Long id) {
        Optional<Attendance> recordOpt = reportService.getRecordById(id);
        if (!recordOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Attendance r = recordOpt.get();
        Map<String, Object> data = new HashMap<>();
        data.put("id", r.getId());
        data.put("workDate", r.getWorkDate().toString());
        data.put("employeeName", r.getUser() != null ? r.getUser().getFullName() : "N/A");
        data.put("employeeEmail", r.getUser() != null ? r.getUser().getEmail() : "N/A");
        data.put("department", (r.getUser() != null && r.getUser().getDepartment() != null)
                ? r.getUser().getDepartment().getName() : "General");
        data.put("shiftName", (r.getUser() != null && r.getUser().getShift() != null)
                ? r.getUser().getShift().getName() : "General Shift");
        data.put("shiftTime", (r.getUser() != null && r.getUser().getShift() != null)
                ? (r.getUser().getShift().getStartTime() + " - " + r.getUser().getShift().getEndTime()) : "09:00 - 18:00");
        data.put("punchIn", r.getPunchInTime() != null ? r.getPunchInTime().toString() : null);
        data.put("punchOut", r.getPunchOutTime() != null ? r.getPunchOutTime().toString() : null);
        data.put("totalHours", r.getTotalHours() != null ? r.getTotalHours() : 0.0);
        data.put("status", r.getStatus() != null ? r.getStatus().name() : "N/A");
        data.put("latitude", r.getInLatitude());
        data.put("longitude", r.getInLongitude());
        data.put("ipAddress", r.getIpAddress() != null ? r.getIpAddress() : "127.0.0.1");
        data.put("notes", r.getNotes() != null ? r.getNotes() : "-");

        // Calculate variance vs standard 8 hours
        double variance = (r.getTotalHours() != null ? r.getTotalHours() : 0.0) - 8.0;
        data.put("varianceHours", Math.round(variance * 100.0) / 100.0);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/reports/timesheet-slip/{id}")
    public String viewFormalTimesheetSlip(@PathVariable("id") Long id, Model model) {
        Optional<Attendance> recordOpt = reportService.getRecordById(id);
        if (!recordOpt.isPresent()) {
            return "redirect:/reports/summary";
        }

        Attendance r = recordOpt.get();
        model.addAttribute("record", r);
        double variance = (r.getTotalHours() != null ? r.getTotalHours() : 0.0) - 8.0;
        model.addAttribute("varianceHours", Math.round(variance * 100.0) / 100.0);

        return "reports/timesheet-slip";
    }

    @GetMapping("/reports/export-csv")
    public void exportCsv(
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "status", required = false) AttendanceStatus status,
            HttpServletResponse response) throws IOException {

        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<Attendance> records = reportService.getFilteredRecords(startDate, endDate, departmentId, userId, status);

        response.setContentType("text/csv; charset=UTF-8");
        String filename = String.format("attendance_report_%s_to_%s.csv", startDate, endDate);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        CsvExportUtil.writeAttendanceToCsv(response.getWriter(), records);
    }
}
