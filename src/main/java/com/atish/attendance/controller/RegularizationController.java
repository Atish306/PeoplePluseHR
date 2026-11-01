package com.atish.attendance.controller;

import com.atish.attendance.entity.AttendanceRegularization;
import com.atish.attendance.entity.Role;
import com.atish.attendance.entity.User;
import com.atish.attendance.security.CustomUserDetails;
import com.atish.attendance.service.RegularizationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class RegularizationController {

    private final RegularizationService regularizationService;

    public RegularizationController(RegularizationService regularizationService) {
        this.regularizationService = regularizationService;
    }

    /**
     * Employee view & apply page for Attendance Regularization.
     */
    @GetMapping("/regularization/apply")
    public String showApplyPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<AttendanceRegularization> myRequests = regularizationService.getUserRequests(user);

        model.addAttribute("currentUser", user);
        model.addAttribute("myRequests", myRequests);
        return "regularization/apply";
    }

    /**
     * Employee submits attendance regularization request.
     */
    @PostMapping("/regularization/apply")
    public String submitRegularization(@RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                       @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                       @RequestParam("reason") String reason,
                                       @RequestParam(value = "checkInTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime checkInTime,
                                       @RequestParam(value = "checkOutTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime checkOutTime,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            AttendanceRegularization reg = regularizationService.applyRegularization(
                    userDetails.getUser(), startDate, endDate, reason, checkInTime, checkOutTime
            );

            String msg = "Regularization request submitted for " + reg.getTotalDays() + " day(s). ";
            if (reg.getTotalDays() > 7) {
                msg += "First 7 days will be reviewed by Manager, and the remaining " + (reg.getTotalDays() - 7) + " day(s) will be reviewed by Admin.";
            } else {
                msg += "Pending Manager review.";
            }

            redirectAttributes.addFlashAttribute("successMessage", msg);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/regularization/apply";
    }

    /**
     * Manager & Admin Review Page.
     */
    @GetMapping("/manager/regularization")
    public String reviewRegularizationsPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User currentUser = userDetails.getUser();
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_SUPER_ADMIN;

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("pendingManagerRequests", regularizationService.getPendingManagerRequests());
        model.addAttribute("pendingAdminRequests", regularizationService.getPendingAdminRequests());
        model.addAttribute("allRequests", regularizationService.getAllRequests());

        return "regularization/manage";
    }

    /**
     * Manager approves up to 7 days (or all days if <= 7).
     */
    @PostMapping("/manager/regularization/{id}/approve")
    public String managerApprove(@PathVariable("id") Long id,
                                 @RequestParam(value = "comments", required = false) String comments,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            AttendanceRegularization reg = regularizationService.managerApprove(id, userDetails.getUser(), comments);
            if (reg.getTotalDays() <= 7) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Regularization #" + id + " fully approved by Manager (" + reg.getTotalDays() + " days).");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Manager approved 7 days for Regularization #" + id + ". The remaining "
                                + (reg.getTotalDays() - 7) + " day(s) have been forwarded to Admin for approval.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/manager/regularization";
    }

    /**
     * Admin approves remaining days (or full approval).
     */
    @PostMapping("/admin/regularization/{id}/approve")
    public String adminApprove(@PathVariable("id") Long id,
                               @RequestParam(value = "comments", required = false) String comments,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            AttendanceRegularization reg = regularizationService.adminApprove(id, userDetails.getUser(), comments);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Regularization #" + id + " approved by Admin! Total: " + reg.getTotalDays()
                            + " days (7 days Manager + " + reg.getAdminApprovedDays() + " days Admin).");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/manager/regularization";
    }

    /**
     * Reject regularization (Manager or Admin).
     */
    @PostMapping({"/manager/regularization/{id}/reject", "/regularization/{id}/reject"})
    public String rejectRegularization(@PathVariable("id") Long id,
                                       @RequestParam(value = "comments", required = false) String comments,
                                       @AuthenticationPrincipal CustomUserDetails userDetails,
                                       RedirectAttributes redirectAttributes) {
        try {
            User user = userDetails.getUser();
            if (user.getRole() == Role.ROLE_EMPLOYEE) {
                throw new IllegalStateException("Employees are not authorized to reject regularization requests.");
            }
            regularizationService.reject(id, user, comments);
            redirectAttributes.addFlashAttribute("successMessage", "Regularization #" + id + " has been rejected.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:/manager/regularization";
    }
}
