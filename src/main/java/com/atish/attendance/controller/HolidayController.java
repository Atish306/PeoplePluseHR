package com.atish.attendance.controller;

import com.atish.attendance.entity.Holiday;
import com.atish.attendance.entity.User;
import com.atish.attendance.security.CustomUserDetails;
import com.atish.attendance.service.HolidayService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping("/holidays")
    public String showHolidayCalendar(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        User user = userDetails.getUser();
        LocalDate today = LocalDate.now();

        List<Holiday> allHolidays = holidayService.getAllHolidays();
        List<Holiday> upcomingHolidays = holidayService.getUpcomingHolidays(today);
        Optional<Holiday> nextHoliday = holidayService.getNextHoliday(today);

        model.addAttribute("currentUser", user);
        model.addAttribute("allHolidays", allHolidays);
        model.addAttribute("upcomingHolidays", upcomingHolidays);
        model.addAttribute("nextHoliday", nextHoliday.orElse(null));
        model.addAttribute("today", today);
        model.addAttribute("newHoliday", new Holiday());

        return "holidays/calendar";
    }

    @PostMapping("/admin/holidays/add")
    public String addHoliday(@RequestParam("name") String name,
                             @RequestParam("holidayDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate holidayDate,
                             @RequestParam(value = "holidayType", defaultValue = "National Holiday") String holidayType,
                             @RequestParam(value = "description", required = false) String description,
                             @RequestParam(value = "mandatory", defaultValue = "true") boolean mandatory,
                             RedirectAttributes redirectAttributes) {
        try {
            Holiday holiday = new Holiday(name, holidayDate, holidayType, description, mandatory);
            holidayService.createHoliday(holiday);
            redirectAttributes.addFlashAttribute("successMessage", "Holiday '" + name + "' added successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/holidays";
    }

    @PostMapping("/admin/holidays/{id}/delete")
    public String deleteHoliday(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            holidayService.deleteHoliday(id);
            redirectAttributes.addFlashAttribute("successMessage", "Holiday deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/holidays";
    }
}
