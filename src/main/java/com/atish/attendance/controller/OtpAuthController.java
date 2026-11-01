package com.atish.attendance.controller;

import com.atish.attendance.service.OtpAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
@RequestMapping("/auth/otp")
public class OtpAuthController {

    private final OtpAuthService otpAuthService;

    public OtpAuthController(OtpAuthService otpAuthService) {
        this.otpAuthService = otpAuthService;
    }

    @PostMapping("/send")
    @ResponseBody
    public ResponseEntity<OtpAuthService.OtpResponse> sendOtp(@RequestBody Map<String, String> payload) {
        String identifier = payload.get("identifier");
        OtpAuthService.OtpResponse response = otpAuthService.sendOtp(identifier);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    @ResponseBody
    public ResponseEntity<OtpAuthService.OtpResponse> verifyOtp(@RequestBody Map<String, String> payload,
                                                                HttpServletRequest request) {
        String identifier = payload.get("identifier");
        String otpCode = payload.get("otpCode");
        OtpAuthService.OtpResponse response = otpAuthService.verifyOtpAndLogin(identifier, otpCode, request);
        return ResponseEntity.ok(response);
    }
}
