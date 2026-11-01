package com.atish.attendance.controller;

import com.atish.attendance.entity.Attendance;
import com.atish.attendance.entity.AttendanceStatus;
import com.atish.attendance.entity.DemoStatus;
import com.atish.attendance.entity.LeaveStatus;
import com.atish.attendance.entity.Role;
import com.atish.attendance.entity.User;
import com.atish.attendance.repository.AttendanceRepository;
import com.atish.attendance.repository.DemoBookingRepository;
import com.atish.attendance.repository.DepartmentRepository;
import com.atish.attendance.repository.LeaveRequestRepository;
import com.atish.attendance.repository.UserRepository;
import com.atish.attendance.security.CustomUserDetails;
import com.atish.attendance.service.HolidayService;
import com.atish.attendance.service.LeaveService;
import com.atish.attendance.service.SystemSettingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

@Controller
public class DashboardController {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveService leaveService;
    private final HolidayService holidayService;
    private final DemoBookingRepository demoBookingRepository;
    private final SystemSettingService systemSettingService;

    public DashboardController(AttendanceRepository attendanceRepository,
                               UserRepository userRepository,
                               DepartmentRepository departmentRepository,
                               LeaveRequestRepository leaveRequestRepository,
                               LeaveService leaveService,
                               HolidayService holidayService,
                               DemoBookingRepository demoBookingRepository,
                               SystemSettingService systemSettingService) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveService = leaveService;
        this.holidayService = holidayService;
        this.demoBookingRepository = demoBookingRepository;
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/dashboard")
    public String employeeDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        LocalDate today = LocalDate.now();

        Optional<Attendance> todayAttendance = attendanceRepository.findByUserAndWorkDate(user, today);
        model.addAttribute("currentUser", user);
        model.addAttribute("todayAttendance", todayAttendance.orElse(null));
        model.addAttribute("recentRecords", attendanceRepository.findByUserOrderByWorkDateDesc(user));
        model.addAttribute("leaveBalances", leaveService.getUserLeaveBalances(user, 2026));
        model.addAttribute("upcomingHolidays", holidayService.getUpcomingHolidays(today));
        model.addAttribute("nextHoliday", holidayService.getNextHoliday(today).orElse(null));
        return "dashboard/employee-dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        LocalDate today = LocalDate.now();
        User currentUser = userDetails.getUser();

        boolean isSuperAdmin = currentUser.getRole() == Role.ROLE_SUPER_ADMIN;
        boolean canViewEmpDetails = systemSettingService.canUserViewEmployeeDetails(currentUser);

        long totalUsers = userRepository.countByActiveTrue();
        long presentCount = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.PRESENT);
        long lateCount = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.LATE);
        long absentCount = attendanceRepository.countByWorkDateAndStatus(today, AttendanceStatus.ABSENT);
        long pendingLeaves = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isSuperAdmin", isSuperAdmin);
        model.addAttribute("canViewEmployees", canViewEmpDetails);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("presentCount", presentCount);
        model.addAttribute("lateCount", lateCount);
        model.addAttribute("absentCount", absentCount);
        model.addAttribute("pendingLeaves", pendingLeaves);

        // Requirement 1: If Admin is disabled from viewing employee details, do not expose employee punches
        if (canViewEmpDetails) {
            model.addAttribute("todayPunches", attendanceRepository.findByWorkDate(today));
        } else {
            model.addAttribute("todayPunches", Collections.emptyList());
        }

        model.addAttribute("upcomingHolidays", holidayService.getUpcomingHolidays(today));
        model.addAttribute("totalHolidays", holidayService.getAllHolidays().size());
        model.addAttribute("pendingDemoCount", demoBookingRepository.countByStatus(DemoStatus.PENDING));

        return "dashboard/admin-dashboard";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        LocalDate today = LocalDate.now();
        User user = userDetails.getUser();

        model.addAttribute("currentUser", user);
        model.addAttribute("pendingLeaves", leaveRequestRepository.findByStatusOrderByCreatedAtDesc(LeaveStatus.PENDING));
        model.addAttribute("todayPunches", attendanceRepository.findByWorkDate(today));
        model.addAttribute("upcomingHolidays", holidayService.getUpcomingHolidays(today));
        model.addAttribute("nextHoliday", holidayService.getNextHoliday(today).orElse(null));
        return "dashboard/manager-dashboard";
    }
}
