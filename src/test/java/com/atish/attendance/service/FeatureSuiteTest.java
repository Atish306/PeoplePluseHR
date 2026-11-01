package com.atish.attendance.service;

import com.atish.attendance.entity.*;
import com.atish.attendance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class FeatureSuiteTest {

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeExcelService employeeExcelService;

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OtpAuthService otpAuthService;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testSuperAdmin;
    private User testAdmin;
    private User testEmployee;

    @BeforeEach
    void setUp() {
        testSuperAdmin = userRepository.findByUsername("superadmin").orElseGet(() -> {
            User sa = new User("superadmin", "superadmin@company.com", "9999999999", passwordEncoder.encode("superadmin123"), "Super Admin", Role.ROLE_SUPER_ADMIN, null, null);
            return userRepository.save(sa);
        });

        testAdmin = userRepository.findByUsername("admin").orElseGet(() -> {
            User a = new User("admin", "admin@company.com", "9876543201", passwordEncoder.encode("admin123"), "Admin", Role.ROLE_ADMIN, null, null);
            return userRepository.save(a);
        });

        testEmployee = userRepository.findByUsername("employee").orElseGet(() -> {
            User e = new User("employee", "employee@company.com", "9876543210", passwordEncoder.encode("employee123"), "Employee", Role.ROLE_EMPLOYEE, null, null);
            return userRepository.save(e);
        });
    }

    @Test
    @DisplayName("Feature 1: Super Admin can disable employee details for Admin user")
    void testSuperAdminDisableEmpDetailsForAdmin() {
        // Super Admin can always view
        assertTrue(systemSettingService.canUserViewEmployeeDetails(testSuperAdmin), "Super Admin should always have access to view employee details");

        // When globally enabled and admin is allowed
        systemSettingService.setEmployeeDetailsVisibleToAdmin(true);
        testAdmin.setCanViewEmpDetails(true);
        userRepository.save(testAdmin);
        assertTrue(systemSettingService.canUserViewEmployeeDetails(testAdmin), "Admin should be able to view when globally enabled and permitted");

        // Super Admin disables globally
        systemSettingService.setEmployeeDetailsVisibleToAdmin(false);
        assertFalse(systemSettingService.canUserViewEmployeeDetails(testAdmin), "Admin must NOT see employee details when Super Admin disables the setting");

        // Super Admin enables globally, but disables specifically for this admin
        systemSettingService.setEmployeeDetailsVisibleToAdmin(true);
        testAdmin.setCanViewEmpDetails(false);
        userRepository.save(testAdmin);
        assertFalse(systemSettingService.canUserViewEmployeeDetails(testAdmin), "Admin must NOT see employee details when specific permission is revoked");
    }

    @Test
    @DisplayName("Feature 2: Admin can download Excel template and bulk import employees")
    void testExcelTemplateAndBulkImport() throws IOException {
        // 1. Download template
        byte[] templateBytes = employeeExcelService.generateExcelTemplate();
        assertNotNull(templateBytes, "Template bytes should not be null");
        assertTrue(templateBytes.length > 0, "Template should contain valid Excel data");

        // 2. Upload sample template
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Employees_Template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                templateBytes
        );

        EmployeeExcelService.ImportResult result = employeeExcelService.importEmployees(file);
        assertTrue(result.getTotalRows() >= 2, "Template has 2 sample employee rows");
        assertTrue(result.getSuccessCount() >= 1, "At least 1 employee should be successfully created");

        // Verify user exists in database
        assertTrue(userRepository.existsByUsername("rahul.sharma"), "Imported employee rahul.sharma should exist in DB");
    }

    @Test
    @DisplayName("Feature 3: Leave apply -> approval sends in-app notification & email")
    void testLeaveApprovalTriggersNotification() {
        // Apply leave
        com.atish.attendance.dto.LeaveApplyDto dto = new com.atish.attendance.dto.LeaveApplyDto();
        dto.setLeaveType(LeaveType.CASUAL);
        dto.setStartDate(LocalDate.of(2026, 11, 10));
        dto.setEndDate(LocalDate.of(2026, 11, 11));
        dto.setReason("Personal family function");

        LeaveRequest leave = leaveService.applyLeave(testEmployee, dto);
        assertNotNull(leave.getId());
        assertEquals(LeaveStatus.PENDING, leave.getStatus());

        // Approve leave
        LeaveRequest approved = leaveService.approveLeave(leave.getId(), testAdmin, "Approved. Have a good time!");
        assertEquals(LeaveStatus.APPROVED, approved.getStatus());

        // Verify In-App Notification was generated for employee
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(testEmployee);
        assertFalse(notifications.isEmpty(), "Employee should receive an in-app notification upon approval");
        Notification latest = notifications.get(0);
        assertTrue(latest.getTitle().contains("Approved"), "Notification title should indicate approval");
        assertFalse(latest.isReadStatus(), "New notification should be unread");
    }

    @Test
    @DisplayName("Feature 4: Employee OTP login via Email or Mobile")
    void testEmployeeOtpLoginFlow() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        // 1. Send OTP via Email
        OtpAuthService.OtpResponse sendRes = otpAuthService.sendOtp("employee@company.com");
        assertTrue(sendRes.isSuccess(), "OTP dispatch should succeed for valid employee email");

        // 2. Retrieve generated OTP from DB
        OtpToken token = otpTokenRepository.findTopByIdentifierAndUsedFalseOrderByCreatedAtDesc("employee@company.com")
                .orElseThrow(() -> new AssertionError("OTP token should be saved in DB"));

        assertEquals(6, token.getOtpCode().length(), "OTP must be 6 digits");

        // 3. Test wrong OTP
        OtpAuthService.OtpResponse failRes = otpAuthService.verifyOtpAndLogin("employee@company.com", "000000", request);
        assertFalse(failRes.isSuccess(), "Invalid OTP must fail authentication");

        // 4. Test correct OTP
        OtpAuthService.OtpResponse successRes = otpAuthService.verifyOtpAndLogin("employee@company.com", token.getOtpCode(), request);
        assertTrue(successRes.isSuccess(), "Correct OTP must authenticate employee successfully");
        assertEquals("/dashboard", successRes.getRedirectUrl());

        // 5. Verify token is marked as used
        OtpToken refreshed = otpTokenRepository.findById(token.getId()).get();
        assertTrue(refreshed.isUsed(), "OTP token must be marked as used after login");

        // 6. Test OTP request with Mobile number
        OtpAuthService.OtpResponse mobileSendRes = otpAuthService.sendOtp("9876543210");
        assertTrue(mobileSendRes.isSuccess(), "OTP dispatch should succeed for registered mobile number");
    }
}
