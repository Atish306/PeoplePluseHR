package com.atish.attendance.controller;

import com.atish.attendance.repository.AttendanceRepository;
import com.atish.attendance.repository.DepartmentRepository;
import com.atish.attendance.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AttendanceRepository attendanceRepository;

    public HomeController(UserRepository userRepository,
                          DepartmentRepository departmentRepository,
                          AttendanceRepository attendanceRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("appName", "Attendance Management System");
        model.addAttribute("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalDepartments", departmentRepository.count());
        model.addAttribute("todayPunches", attendanceRepository.countByWorkDate(LocalDate.now()));
        return "index";
    }

    @GetMapping("/api/health")
    @ResponseBody
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Attendance Management System");
        response.put("totalUsers", userRepository.count());
        response.put("totalDepartments", departmentRepository.count());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
