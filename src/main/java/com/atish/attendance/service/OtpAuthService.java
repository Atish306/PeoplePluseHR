package com.atish.attendance.service;

import com.atish.attendance.entity.OtpToken;
import com.atish.attendance.entity.Role;
import com.atish.attendance.entity.User;
import com.atish.attendance.logging.CustomLogger;
import com.atish.attendance.repository.OtpTokenRepository;
import com.atish.attendance.repository.UserRepository;
import com.atish.attendance.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class OtpAuthService {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpAuthService(UserRepository userRepository,
                          OtpTokenRepository otpTokenRepository,
                          EmailNotificationService emailNotificationService) {
        this.userRepository = userRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.emailNotificationService = emailNotificationService;
    }

    public static class OtpResponse {
        private boolean success;
        private String message;
        private String redirectUrl;

        public OtpResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public OtpResponse(boolean success, String message, String redirectUrl) {
            this.success = success;
            this.message = message;
            this.redirectUrl = redirectUrl;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getRedirectUrl() { return redirectUrl; }
    }

    /**
     * Finds the user by either Email or Mobile Number.
     */
    public Optional<User> findByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        String clean = identifier.trim();
        Optional<User> byEmail = userRepository.findByEmail(clean.toLowerCase());
        if (byEmail.isPresent()) {
            return byEmail;
        }
        return userRepository.findByMobile(clean);
    }

    /**
     * Requirement 4: Generates and dispatches a 6-digit OTP to the employee's email or mobile.
     */
    @Transactional
    public OtpResponse sendOtp(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return new OtpResponse(false, "Please provide an Email address or Mobile number.");
        }

        String cleanIdentifier = identifier.trim().toLowerCase();
        Optional<User> userOpt = findByIdentifier(cleanIdentifier);

        if (!userOpt.isPresent()) {
            return new OtpResponse(false, "No registered employee account found with: " + identifier);
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            return new OtpResponse(false, "Your employee account is inactive. Please contact HR or System Administrator.");
        }

        // Generate 6-digit OTP code
        int code = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(code);

        // Store OTP in database with 5 minutes validity
        OtpToken token = new OtpToken(cleanIdentifier, otpCode, 5);
        otpTokenRepository.save(token);

        boolean isEmail = cleanIdentifier.contains("@");

        if (isEmail) {
            emailNotificationService.sendLoginOtpEmail(cleanIdentifier, otpCode, user.getFullName());
        } else {
            // Mobile SMS Dispatch simulation
            Map<String, Object> smsDetails = new HashMap<>();
            smsDetails.put("mobile", cleanIdentifier);
            smsDetails.put("employee", user.getFullName());
            smsDetails.put("otpCode", otpCode);
            smsDetails.put("channel", "SMS_GATEWAY (Simulated)");
            CustomLogger.printSuccessLog("OtpAuthService.MobileSMS", "Dispatched Login OTP via SMS", 0, smsDetails);
        }

        // Always print helpful debug log
        Map<String, Object> logDetails = new HashMap<>();
        logDetails.put("identifier", cleanIdentifier);
        logDetails.put("user", user.getUsername());
        logDetails.put("otpCode", otpCode);
        logDetails.put("expiresIn", "5 minutes");
        CustomLogger.printSuccessLog("OtpAuthService.sendOtp", "Generated Portal Login OTP", 0, logDetails);

        return new OtpResponse(true, "OTP code has been sent to your registered "
                + (isEmail ? "Email address" : "Mobile number") + ". Please verify below.");
    }

    /**
     * Requirement 4: Verifies the entered OTP and logs the employee in.
     */
    @Transactional
    public OtpResponse verifyOtpAndLogin(String identifier, String otpCode, HttpServletRequest request) {
        if (identifier == null || identifier.trim().isEmpty() || otpCode == null || otpCode.trim().isEmpty()) {
            return new OtpResponse(false, "Please provide both identifier and 6-digit OTP code.");
        }

        String cleanIdentifier = identifier.trim().toLowerCase();
        Optional<OtpToken> tokenOpt = otpTokenRepository.findTopByIdentifierAndUsedFalseOrderByCreatedAtDesc(cleanIdentifier);

        if (!tokenOpt.isPresent()) {
            return new OtpResponse(false, "No active OTP request found. Please request a new OTP.");
        }

        OtpToken token = tokenOpt.get();

        if (token.isExpired()) {
            return new OtpResponse(false, "This OTP code has expired. Please request a fresh OTP.");
        }

        if (!token.getOtpCode().equals(otpCode.trim())) {
            return new OtpResponse(false, "Invalid OTP code. Please enter the correct 6-digit code.");
        }

        // Mark OTP as used
        token.setUsed(true);
        otpTokenRepository.save(token);

        // Fetch User and sign in
        Optional<User> userOpt = findByIdentifier(cleanIdentifier);
        if (!userOpt.isPresent()) {
            return new OtpResponse(false, "User account could not be found.");
        }

        User user = userOpt.get();
        CustomUserDetails userDetails = new CustomUserDetails(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        // Save into HTTP Session
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        String redirectUrl = "/dashboard";
        if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_SUPER_ADMIN) {
            redirectUrl = "/admin/dashboard";
        } else if (user.getRole() == Role.ROLE_MANAGER) {
            redirectUrl = "/manager/dashboard";
        }

        Map<String, Object> details = new HashMap<>();
        details.put("username", user.getUsername());
        details.put("role", user.getRole().name());
        details.put("redirectUrl", redirectUrl);
        CustomLogger.printSuccessLog("OtpAuthService.verifyOtpAndLogin", "Employee verified OTP and authenticated successfully", 0, details);

        return new OtpResponse(true, "Authentication successful! Redirecting...", redirectUrl);
    }
}
