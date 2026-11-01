package com.atish.attendance.controller;

import com.atish.attendance.dto.DemoBookingRequestDto;
import com.atish.attendance.entity.DemoBooking;
import com.atish.attendance.repository.CountryRepository;
import com.atish.attendance.service.DemoBookingService;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class DemoBookingController {

    private final DemoBookingService demoBookingService;
    private final CountryRepository countryRepository;

    public DemoBookingController(DemoBookingService demoBookingService, CountryRepository countryRepository) {
        this.demoBookingService = demoBookingService;
        this.countryRepository = countryRepository;
    }

    @GetMapping({"/book-demo", "/forms/book-demo"})
    public String showBookDemoForm(Model model) {
        if (!model.containsAttribute("bookingDto")) {
            DemoBookingRequestDto dto = new DemoBookingRequestDto();
            dto.setPreferredDate(LocalDate.now().plusDays(2));
            dto.setPreferredTimeSlot("Morning (10:00 - 11:00 AM)");
            dto.setCountry("India");
            dto.setModules(Arrays.asList("Web Clock-in & GPS Geofencing", "Leave Balances & Approvals"));
            model.addAttribute("bookingDto", dto);
        }

        model.addAttribute("countries", countryRepository.findByActiveTrueOrderBySortOrderAscNameAsc());

        model.addAttribute("minDate", LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("maxDate", LocalDate.now().plusDays(45).format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("availableModules", Arrays.asList(
                "Web Clock-in & GPS Geofencing",
                "Leave Balances & Approvals",
                "7-Column Attendance Muster",
                "Shift Rosters & Grace Periods",
                "Timesheets & Payroll Export",
                "Audit & Endpoint Logging"
        ));
        model.addAttribute("companySizes", Arrays.asList(
                "1 - 25",
                "26 - 100",
                "101 - 500",
                "501 - 1,000",
                "1,000+"
        ));
        model.addAttribute("timeSlots", Arrays.asList(
                "Morning (10:00 - 11:00 AM)",
                "Mid-Day (12:00 - 01:00 PM)",
                "Afternoon (03:00 - 04:00 PM)",
                "Evening (05:00 - 06:00 PM)"
        ));

        return "forms/book-demo";
    }

    @PostMapping("/book-demo")
    public String processBooking(@ModelAttribute("bookingDto") DemoBookingRequestDto bookingDto,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        // Validation: verify required fields
        if (isEmpty(bookingDto.getFirstName()) ||
            isEmpty(bookingDto.getLastName()) ||
            isEmpty(bookingDto.getWorkEmail()) ||
            isEmpty(bookingDto.getPhone()) ||
            isEmpty(bookingDto.getCompanyName())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please complete all mandatory fields marked with *");
            redirectAttributes.addFlashAttribute("bookingDto", bookingDto);
            return "redirect:/book-demo";
        }

        String clientIp = extractClientIp(request);
        DemoBooking saved = demoBookingService.createBooking(bookingDto, clientIp);

        return "redirect:/book-demo/confirmation?ref=" + saved.getBookingReference();
    }

    @GetMapping("/book-demo/confirmation")
    public String showConfirmation(@RequestParam("ref") String reference, Model model) {
        Optional<DemoBooking> opt = demoBookingService.getBookingByReference(reference);
        if (!opt.isPresent()) {
            return "redirect:/book-demo";
        }

        DemoBooking booking = opt.get();
        model.addAttribute("booking", booking);
        model.addAttribute("formattedDate", booking.getPreferredDate() != null
                ? booking.getPreferredDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
                : "Scheduled");

        return "forms/demo-confirmation";
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty() && !"unknown".equalsIgnoreCase(xf)) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
