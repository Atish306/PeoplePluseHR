package com.atish.attendance.controller;

import com.atish.attendance.dto.LeaveApplyDto;
import com.atish.attendance.entity.LeaveType;
import com.atish.attendance.entity.User;
import com.atish.attendance.security.CustomUserDetails;
import com.atish.attendance.service.LeaveService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/leaves")
    public String defaultLeaveRedirect(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser().getRole() == com.atish.attendance.entity.Role.ROLE_EMPLOYEE) {
            return "redirect:/leaves/apply";
        }
        return "redirect:/manager/leaves";
    }

    @GetMapping("/leaves/apply")
    public String showApplyPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        model.addAttribute("currentUser", user);
        
        java.util.List<LeaveType> selectableTypes = java.util.Arrays.asList(
                LeaveType.CASUAL,
                LeaveType.SICK,
                LeaveType.EARNED,
                LeaveType.COMP_OFF,
                LeaveType.LOSS_OF_PAY
        );
        model.addAttribute("leaveTypes", selectableTypes);
        model.addAttribute("leaveBalances", leaveService.getUserLeaveBalances(user, 2026));
        model.addAttribute("leaveDto", new LeaveApplyDto());
        model.addAttribute("myLeaves", leaveService.getUserLeaves(user));
        return "leaves/apply";
    }

    @PostMapping("/leaves/apply")
    public String submitLeave(@ModelAttribute LeaveApplyDto leaveDto,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            leaveService.applyLeave(userDetails.getUser(), leaveDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave request submitted successfully! Pending manager review.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/leaves/apply";
    }

    @GetMapping("/manager/leaves")
    public String manageLeavesPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("currentUser", userDetails.getUser());
        model.addAttribute("pendingLeaves", leaveService.getPendingLeaves());
        model.addAttribute("allLeaves", leaveService.getAllLeaves());
        return "leaves/manage";
    }

    @PostMapping("/manager/leaves/{id}/approve")
    public String approveLeave(@PathVariable("id") Long id,
                               @RequestParam(value = "comments", required = false) String comments,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            leaveService.approveLeave(id, userDetails.getUser(), comments);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave request #" + id + " has been approved and synced to attendance logs.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/leaves";
    }

    @PostMapping("/manager/leaves/{id}/reject")
    public String rejectLeave(@PathVariable("id") Long id,
                              @RequestParam(value = "comments", required = false) String comments,
                              @AuthenticationPrincipal CustomUserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            leaveService.rejectLeave(id, userDetails.getUser(), comments);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Leave request #" + id + " has been rejected.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/manager/leaves";
    }
}
