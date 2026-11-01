package com.atish.attendance.controller;

import com.atish.attendance.entity.DemoBooking;
import com.atish.attendance.entity.DemoStatus;
import com.atish.attendance.entity.Role;
import com.atish.attendance.entity.Shift;
import com.atish.attendance.entity.User;
import com.atish.attendance.repository.UserRepository;
import com.atish.attendance.security.CustomUserDetails;
import com.atish.attendance.service.DemoBookingService;
import com.atish.attendance.service.EmployeeExcelService;
import com.atish.attendance.service.ScheduledAttendanceTasks;
import com.atish.attendance.service.ShiftService;
import com.atish.attendance.service.SystemSettingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

@Controller
public class AdminController {

    private final ShiftService shiftService;
    private final UserRepository userRepository;
    private final ScheduledAttendanceTasks scheduledTasks;
    private final DemoBookingService demoBookingService;
    private final EmployeeExcelService employeeExcelService;
    private final SystemSettingService systemSettingService;

    public AdminController(ShiftService shiftService,
                           UserRepository userRepository,
                           ScheduledAttendanceTasks scheduledTasks,
                           DemoBookingService demoBookingService,
                           EmployeeExcelService employeeExcelService,
                           SystemSettingService systemSettingService) {
        this.shiftService = shiftService;
        this.userRepository = userRepository;
        this.scheduledTasks = scheduledTasks;
        this.demoBookingService = demoBookingService;
        this.employeeExcelService = employeeExcelService;
        this.systemSettingService = systemSettingService;
    }

    /**
     * Requirement 1 & 2: Employee Management & Excel Import Page.
     * Respects Super Admin restriction: If disabled, regular Admin cannot see employee details.
     */
    @GetMapping("/admin/employees")
    public String viewEmployees(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User currentUser = userDetails.getUser();
        boolean isSuperAdmin = currentUser.getRole() == Role.ROLE_SUPER_ADMIN;
        boolean canView = systemSettingService.canUserViewEmployeeDetails(currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isSuperAdmin", isSuperAdmin);
        model.addAttribute("canViewEmployees", canView);
        model.addAttribute("globalEmpViewEnabled", systemSettingService.isEmployeeDetailsVisibleToAdmin());

        if (canView) {
            model.addAttribute("employees", userRepository.findByRole(Role.ROLE_EMPLOYEE));
        } else {
            model.addAttribute("employees", Collections.emptyList());
        }

        if (isSuperAdmin) {
            model.addAttribute("adminUsers", userRepository.findByRole(Role.ROLE_ADMIN));
        }

        return "admin/employees";
    }

    /**
     * Requirement 1: Super Admin toggles global employee visibility for Admin users.
     */
    @PostMapping("/admin/settings/toggle-emp-details")
    public String toggleGlobalEmployeeView(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           RedirectAttributes redirectAttributes) {
        User currentUser = userDetails.getUser();
        if (currentUser.getRole() != Role.ROLE_SUPER_ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only Super Admin can change employee visibility settings.");
            return "redirect:/admin/employees";
        }

        boolean current = systemSettingService.isEmployeeDetailsVisibleToAdmin();
        boolean newSetting = !current;
        systemSettingService.setEmployeeDetailsVisibleToAdmin(newSetting);

        redirectAttributes.addFlashAttribute("successMessage",
                "Employee details visibility for Admin users has been " + (newSetting ? "ENABLED" : "DISABLED") + ".");
        return "redirect:/admin/employees";
    }

    /**
     * Requirement 1: Super Admin toggles employee visibility for a specific Admin user.
     */
    @PostMapping("/admin/users/{id}/toggle-emp-view")
    public String toggleAdminUserEmpView(@PathVariable("id") Long id,
                                         @AuthenticationPrincipal CustomUserDetails userDetails,
                                         RedirectAttributes redirectAttributes) {
        User currentUser = userDetails.getUser();
        if (currentUser.getRole() != Role.ROLE_SUPER_ADMIN) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only Super Admin can toggle individual Admin permissions.");
            return "redirect:/admin/employees";
        }

        User targetAdmin = userRepository.findById(id).orElse(null);
        if (targetAdmin != null && targetAdmin.getRole() == Role.ROLE_ADMIN) {
            targetAdmin.setCanViewEmpDetails(!targetAdmin.isCanViewEmpDetails());
            userRepository.save(targetAdmin);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee details access for Admin '" + targetAdmin.getUsername() + "' updated to " + (targetAdmin.isCanViewEmpDetails() ? "ALLOWED" : "RESTRICTED") + ".");
        }

        return "redirect:/admin/employees";
    }

    /**
     * Requirement 2: Download Employee Excel Import Template (.xlsx).
     */
    @GetMapping("/admin/employees/template")
    public ResponseEntity<byte[]> downloadEmployeeExcelTemplate() {
        try {
            byte[] fileBytes = employeeExcelService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Employees_Import_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(fileBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Requirement 2: Admin user imports employees via Excel file (.xlsx / .xls).
     */
    @PostMapping("/admin/employees/import-excel")
    public String importEmployeesExcel(@RequestParam("file") MultipartFile file,
                                       RedirectAttributes redirectAttributes) {
        try {
            EmployeeExcelService.ImportResult result = employeeExcelService.importEmployees(file);
            if (result.getSuccessCount() > 0) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Excel Import Successful! Created " + result.getSuccessCount() + " employee(s)."
                                + (result.getFailureCount() > 0 ? " (" + result.getFailureCount() + " skipped due to errors)" : ""));
            }
            if (result.getFailureCount() > 0 || !result.getErrorMessages().isEmpty()) {
                redirectAttributes.addFlashAttribute("importErrors", result.getErrorMessages());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to process Excel file: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @GetMapping("/admin/shifts")
    public String viewShifts(Model model) {
        List<Shift> shifts = shiftService.getAllShifts();
        List<User> users = userRepository.findByActiveTrue();
        model.addAttribute("shifts", shifts);
        model.addAttribute("users", users);
        return "admin/shifts";
    }

    @PostMapping("/admin/shifts/create")
    public String createShift(@RequestParam("name") String name,
                              @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
                              @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
                              @RequestParam(value = "gracePeriodMinutes", defaultValue = "15") int gracePeriodMinutes,
                              RedirectAttributes redirectAttributes) {
        try {
            shiftService.createShift(name, startTime, endTime, gracePeriodMinutes);
            redirectAttributes.addFlashAttribute("successMessage", "New shift schedule '" + name + "' created successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to create shift: " + e.getMessage());
        }
        return "redirect:/admin/shifts";
    }

    @PostMapping("/admin/shifts/assign")
    public String assignShift(@RequestParam("userId") Long userId,
                              @RequestParam("shiftId") Long shiftId,
                              RedirectAttributes redirectAttributes) {
        try {
            shiftService.assignShiftToUser(userId, shiftId);
            redirectAttributes.addFlashAttribute("successMessage", "Shift assignment updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to assign shift: " + e.getMessage());
        }
        return "redirect:/admin/shifts";
    }

    @PostMapping("/admin/audit/run-daily-check")
    public String triggerManualAudit(RedirectAttributes redirectAttributes) {
        LocalDate today = LocalDate.now();
        ScheduledAttendanceTasks.AuditResult result = scheduledTasks.runDailyAttendanceAudit(today);

        String message = String.format("Audit executed for %s! Absentees marked: %d, On-Leave recorded: %d, Unclosed punches resolved: %d.",
                today, result.getAbsenteesMarked(), result.getOnLeaveMarked(), result.getUnclosedResolved());

        redirectAttributes.addFlashAttribute("auditSuccessMessage", message);
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/demo-bookings")
    public String viewDemoBookings(@RequestParam(value = "status", required = false) DemoStatus status, Model model) {
        model.addAttribute("bookings", demoBookingService.getBookingsByStatus(status));
        model.addAttribute("metrics", demoBookingService.getMetrics());
        model.addAttribute("currentStatus", status != null ? status.name() : "ALL");
        model.addAttribute("statuses", DemoStatus.values());
        return "admin/demo-bookings";
    }

    @PostMapping("/admin/demo-bookings/{id}/status")
    public String updateDemoStatus(@PathVariable("id") Long id,
                                   @RequestParam("status") DemoStatus status,
                                   RedirectAttributes redirectAttributes) {
        try {
            DemoBooking updated = demoBookingService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Status for demo booking " + updated.getBookingReference() + " updated to " + status + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating demo booking: " + e.getMessage());
        }
        return "redirect:/admin/demo-bookings";
    }
}
