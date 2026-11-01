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
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OtpAuthService otpAuthService;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private org.springframework.context.MessageSource messageSource;

    @Autowired
    private RegularizationService regularizationService;

    @Autowired
    private RegularizationRepository regularizationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private User testSuperAdmin;
    private User testAdmin;
    private User testManager;
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

        testManager = userRepository.findByUsername("manager").orElseGet(() -> {
            User m = new User("manager", "manager@company.com", "9876543202", passwordEncoder.encode("manager123"), "Manager", Role.ROLE_MANAGER, null, null);
            return userRepository.save(m);
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
        // Clean up any previously seeded test users to allow fresh import
        userRepository.findByUsername("rahul.sharma").ifPresent(u -> {
            leaveBalanceRepository.deleteAll(leaveBalanceRepository.findByUser(u));
            userRepository.delete(u);
        });
        userRepository.findByUsername("priya.patil").ifPresent(u -> {
            leaveBalanceRepository.deleteAll(leaveBalanceRepository.findByUser(u));
            userRepository.delete(u);
        });

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

    @Test
    @DisplayName("Feature 5: Multiple Language Support (i18n) for English, Hindi, and Marathi")
    void testMultipleLanguageSupport() {
        assertNotNull(messageSource, "MessageSource should be injected");

        // English
        assertEquals("Sign In", messageSource.getMessage("login.title", null, java.util.Locale.ENGLISH));
        assertEquals("Total Employees", messageSource.getMessage("kpi.total_employees", null, java.util.Locale.ENGLISH));
        assertEquals("Employee OTP Sign In", messageSource.getMessage("login.tab.otp", null, java.util.Locale.ENGLISH));

        // Hindi
        assertEquals("साइन इन", messageSource.getMessage("login.title", null, new java.util.Locale("hi")));
        assertEquals("कुल कर्मचारी", messageSource.getMessage("kpi.total_employees", null, new java.util.Locale("hi")));
        assertEquals("कर्मचारी ओटीपी साइन इन", messageSource.getMessage("login.tab.otp", null, new java.util.Locale("hi")));

        // Marathi
        assertEquals("साइन इन", messageSource.getMessage("login.title", null, new java.util.Locale("mr")));
        assertEquals("एकूण कर्मचारी", messageSource.getMessage("kpi.total_employees", null, new java.util.Locale("mr")));
        assertEquals("कर्मचारी ओटीपी लॉगिन", messageSource.getMessage("login.tab.otp", null, new java.util.Locale("mr")));
    }

    @Test
    @DisplayName("Feature 6: Attendance Regularization: Up to 7 days approved by Manager; > 7 days (9 days) -> 7 approved by Manager & rest (2 days) approved by Admin")
    void testAttendanceRegularizationApprovalWorkflow() {
        // --- Scenario 1: 9-day Regularization Request (> 7 days) ---
        LocalDate startDate9 = LocalDate.of(2026, 11, 1);
        LocalDate endDate9 = LocalDate.of(2026, 11, 9); // exactly 9 days

        AttendanceRegularization reg9 = regularizationService.applyRegularization(
                testEmployee, startDate9, endDate9, "Biometric scanner down during project sprint",
                java.time.LocalTime.of(9, 0), java.time.LocalTime.of(18, 0)
        );

        assertNotNull(reg9.getId(), "Regularization request must be persisted with an ID");
        assertEquals(9, reg9.getTotalDays(), "Total duration must be 9 days");
        assertEquals(RegularizationStatus.PENDING_MANAGER, reg9.getStatus(), "Initial status must be PENDING_MANAGER");
        assertEquals(0, reg9.getManagerApprovedDays());
        assertEquals(0, reg9.getAdminApprovedDays());

        // Step 1: Manager approves the request (Rule: Manager approves up to 7 days)
        AttendanceRegularization mngApproved9 = regularizationService.managerApprove(reg9.getId(), testManager, "Manager verified first 7 days");
        assertEquals(7, mngApproved9.getManagerApprovedDays(), "Manager can approve a maximum of 7 days");
        assertEquals(0, mngApproved9.getAdminApprovedDays());
        assertEquals(2, mngApproved9.getRemainingAdminDays(), "Remaining 2 days must require Admin approval");
        assertEquals(RegularizationStatus.PENDING_ADMIN, mngApproved9.getStatus(), "Status should be PENDING_ADMIN for remaining days");

        // Verify Manager In-App notification was sent to employee
        List<Notification> mngNotifs = notificationRepository.findByUserOrderByCreatedAtDesc(testEmployee);
        assertFalse(mngNotifs.isEmpty());
        assertTrue(mngNotifs.get(0).getTitle().contains("7 Days Approved"), "Notification should state 7 days approved by Manager");

        // Step 2: Admin approves the remaining days
        AttendanceRegularization admApproved9 = regularizationService.adminApprove(reg9.getId(), testAdmin, "Admin approved remaining 2 days");
        assertEquals(7, admApproved9.getManagerApprovedDays());
        assertEquals(2, admApproved9.getAdminApprovedDays(), "Admin should approve the remaining 2 days");
        assertEquals(9, admApproved9.getManagerApprovedDays() + admApproved9.getAdminApprovedDays());
        assertEquals(RegularizationStatus.APPROVED, admApproved9.getStatus(), "Status should be fully APPROVED");

        // Verify Admin In-App notification was sent to employee
        List<Notification> admNotifs = notificationRepository.findByUserOrderByCreatedAtDesc(testEmployee);
        assertTrue(admNotifs.get(0).getTitle().contains("Fully Approved"), "Notification should state Fully Approved");

        // Verify Attendance table was synchronized with PRESENT records for the entire 9-day range
        for (int i = 0; i < 9; i++) {
            LocalDate day = startDate9.plusDays(i);
            Attendance att = attendanceRepository.findByUserAndWorkDate(testEmployee, day)
                    .orElseThrow(() -> new AssertionError("Attendance record must exist for day: " + day));
            assertEquals(AttendanceStatus.PRESENT, att.getStatus());
            assertEquals(9.0, att.getTotalHours());
        }

        // --- Scenario 2: 5-day Regularization Request (<= 7 days) ---
        LocalDate startDate5 = LocalDate.of(2026, 11, 15);
        LocalDate endDate5 = LocalDate.of(2026, 11, 19); // exactly 5 days

        AttendanceRegularization reg5 = regularizationService.applyRegularization(
                testEmployee, startDate5, endDate5, "Client on-site deployment",
                java.time.LocalTime.of(9, 30), java.time.LocalTime.of(18, 30)
        );

        assertEquals(5, reg5.getTotalDays());
        assertEquals(RegularizationStatus.PENDING_MANAGER, reg5.getStatus());

        // Manager approves: Because totalDays <= 7, all 5 days should be approved directly
        AttendanceRegularization mngApproved5 = regularizationService.managerApprove(reg5.getId(), testManager, "All 5 days approved");
        assertEquals(5, mngApproved5.getManagerApprovedDays(), "Manager approves all 5 days directly");
        assertEquals(0, mngApproved5.getAdminApprovedDays(), "Admin approval not needed when <= 7 days");
        assertEquals(0, mngApproved5.getRemainingAdminDays());
        assertEquals(RegularizationStatus.APPROVED, mngApproved5.getStatus(), "Status becomes APPROVED directly by Manager");
    }
}
