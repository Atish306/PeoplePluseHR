package com.atish.attendance.controller;

import com.atish.attendance.dto.MonthlyMusterDto;
import com.atish.attendance.dto.PunchRequest;
import com.atish.attendance.entity.Attendance;
import com.atish.attendance.entity.Holiday;
import com.atish.attendance.entity.Role;
import com.atish.attendance.entity.User;
import com.atish.attendance.repository.UserRepository;
import com.atish.attendance.security.CustomUserDetails;
import com.atish.attendance.service.AttendanceService;
import com.atish.attendance.service.HolidayService;
import com.atish.attendance.util.GeoUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final HolidayService holidayService;

    public AttendanceController(AttendanceService attendanceService,
                                UserRepository userRepository,
                                HolidayService holidayService) {
        this.attendanceService = attendanceService;
        this.userRepository = userRepository;
        this.holidayService = holidayService;
    }

    @PostMapping("/attendance/punch-in")
    public String punchIn(@ModelAttribute PunchRequest request,
                          @AuthenticationPrincipal CustomUserDetails userDetails,
                          HttpServletRequest httpRequest,
                          RedirectAttributes redirectAttributes) {
        try {
            String clientIp = GeoUtils.getClientIp(httpRequest);
            Attendance attendance = attendanceService.recordPunchIn(userDetails.getUser(), request, clientIp);

            String timeStr = attendance.getPunchInTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
            com.atish.attendance.logging.CustomLogger.printSuccessLog(
                    "POST /attendance/punch-in",
                    "Punch in successful: status=" + attendance.getStatus() + " at " + timeStr,
                    request
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Punched in successfully at " + timeStr + "! Status: " + attendance.getStatus());
        } catch (Exception ex) {
            com.atish.attendance.logging.CustomLogger.printErrorLog(
                    "POST /attendance/punch-in",
                    "Punch in failed for user " + (userDetails != null ? userDetails.getUsername() : "anonymous") + ": " + ex.getMessage(),
                    request,
                    ex
            );
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/attendance/punch-out")
    public String punchOut(@AuthenticationPrincipal CustomUserDetails userDetails,
                           HttpServletRequest httpRequest,
                           RedirectAttributes redirectAttributes) {
        try {
            String clientIp = GeoUtils.getClientIp(httpRequest);
            Attendance attendance = attendanceService.recordPunchOut(userDetails.getUser(), clientIp);

            String timeStr = attendance.getPunchOutTime().format(DateTimeFormatter.ofPattern("hh:mm a"));
            com.atish.attendance.logging.CustomLogger.printSuccessLog(
                    "POST /attendance/punch-out",
                    "Punch out successful for " + userDetails.getUsername() + ": totalHours=" + attendance.getTotalHours() + " hrs",
                    "IP=" + clientIp
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Punched out successfully at " + timeStr + ". Total hours worked today: " + attendance.getTotalHours() + " hrs.");
        } catch (Exception ex) {
            com.atish.attendance.logging.CustomLogger.printErrorLog(
                    "POST /attendance/punch-out",
                    "Punch out failed for user " + (userDetails != null ? userDetails.getUsername() : "anonymous") + ": " + ex.getMessage(),
                    ex
            );
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/dashboard";
    }

    @GetMapping("/attendance/history")
    public String historyPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                              @RequestParam(value = "year", required = false) Integer year,
                              @RequestParam(value = "month", required = false) Integer month,
                              @RequestParam(value = "userId", required = false) Long userId,
                              Model model) {
        User loggedInUser = userDetails.getUser();
        LocalDate now = LocalDate.now();
        int queryYear = (year != null && year >= 2020 && year <= 2030) ? year : now.getYear();
        int queryMonth = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();

        User targetUser = loggedInUser;
        if (userId != null && (loggedInUser.getRole() == Role.ROLE_ADMIN || loggedInUser.getRole() == Role.ROLE_MANAGER)) {
            targetUser = userRepository.findById(userId).orElse(loggedInUser);
        }

        MonthlyMusterDto monthlyMuster = attendanceService.getMonthlyMuster(targetUser, queryYear, queryMonth);
        List<Attendance> history = attendanceService.getUserAttendanceHistory(targetUser);
        List<Holiday> allHolidays = holidayService.getAllHolidays();
        List<Holiday> upcomingHolidays = holidayService.getUpcomingHolidays(LocalDate.now());

        model.addAttribute("currentUser", loggedInUser);
        model.addAttribute("targetUser", targetUser);
        model.addAttribute("records", history);
        model.addAttribute("monthlyMuster", monthlyMuster);
        model.addAttribute("selectedYear", queryYear);
        model.addAttribute("selectedMonth", queryMonth);
        model.addAttribute("allEmployees", userRepository.findByActiveTrue());
        model.addAttribute("allHolidays", allHolidays);
        model.addAttribute("upcomingHolidays", upcomingHolidays);

        return "attendance/history";
    }
}
