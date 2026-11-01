package com.atish.attendance.controller;

import com.atish.attendance.entity.Notification;
import com.atish.attendance.entity.User;
import com.atish.attendance.repository.NotificationRepository;
import com.atish.attendance.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping("/notifications")
    public String viewNotifications(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userDetails.getUser();
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        model.addAttribute("currentUser", user);
        model.addAttribute("notifications", notifications);
        return "notifications/list";
    }

    @GetMapping("/api/notifications")
    @ResponseBody
    public ResponseEntity<List<Notification>> getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(notificationRepository.findByUserOrderByCreatedAtDesc(userDetails.getUser()));
    }

    @PostMapping("/api/notifications/{id}/read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("id") Long id,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        Notification notif = notificationRepository.findById(id).orElse(null);
        if (notif != null && notif.getUser().getId().equals(userDetails.getUser().getId())) {
            notif.setReadStatus(true);
            notificationRepository.save(notif);
        }
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }

    @PostMapping("/api/notifications/mark-all-read")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            List<Notification> list = notificationRepository.findByUserOrderByCreatedAtDesc(userDetails.getUser());
            for (Notification n : list) {
                n.setReadStatus(true);
            }
            notificationRepository.saveAll(list);
        }
        return ResponseEntity.ok(Collections.singletonMap("success", true));
    }
}
