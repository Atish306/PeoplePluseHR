package com.atish.attendance.service;

import com.atish.attendance.entity.DemoBooking;
import com.atish.attendance.entity.LeaveRequest;
import com.atish.attendance.logging.CustomLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailNotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@peoplepulsehr.com}")
    private String mailFrom;

    @Value("${app.mail.admin-email:sales@peoplepulsehr.com}")
    private String adminEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

    /**
     * Sends an email notification to the prospect confirming their booked demo.
     */
    public boolean sendDemoConfirmationToProspect(DemoBooking booking) {
        String recipient = booking.getWorkEmail();
        String subject = "Confirmed: Your PeoplePulse HR Demo Walkthrough [" + booking.getBookingReference() + "]";
        String formattedDate = booking.getPreferredDate() != null ? booking.getPreferredDate().format(DATE_FORMATTER) : "To be confirmed";

        String htmlBody = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 620px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">"
                + "<div style=\"background: linear-gradient(135deg, #0d1b3e 0%, #1b4fc2 100%); padding: 30px; text-align: center; color: #ffffff;\">"
                + "<h1 style=\"margin: 0; font-size: 24px; font-weight: bold;\">PeoplePulse HR</h1>"
                + "<p style=\"margin: 8px 0 0; color: #cbd5e1; font-size: 14px;\">Workplace Attendance & Employee Platform</p>"
                + "</div>"
                + "<div style=\"padding: 30px; color: #1e293b;\">"
                + "<h2 style=\"margin-top: 0; color: #0f172a; font-size: 20px;\">Hello " + booking.getFirstName() + ",</h2>"
                + "<p style=\"font-size: 15px; line-height: 1.6; color: #475569;\">Thank you for scheduling a personalized demonstration of <strong>PeoplePulse HR</strong>. We have received your booking request and our solutions specialist is preparing a walkthrough tailored to <strong>" + booking.getCompanyName() + "</strong>.</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; margin: 24px 0;\">"
                + "<div style=\"font-size: 13px; text-transform: uppercase; color: #64748b; font-weight: 600; margin-bottom: 12px;\">Booking Summary</div>"
                + "<table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Reference ID:</td><td style=\"padding: 6px 0; font-weight: 600; color: #1b4fc2;\">" + booking.getBookingReference() + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Preferred Date:</td><td style=\"padding: 6px 0; font-weight: 600; color: #0f172a;\">" + formattedDate + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Time Slot:</td><td style=\"padding: 6px 0; font-weight: 600; color: #0f172a;\">" + booking.getPreferredTimeSlot() + "</td></tr>"
                + (booking.getCountry() != null ? "<tr><td style=\"padding: 6px 0; color: #64748b;\">Country / Region:</td><td style=\"padding: 6px 0; color: #0f172a;\">" + booking.getCountry() + "</td></tr>" : "")
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Company Size:</td><td style=\"padding: 6px 0; color: #0f172a;\">" + booking.getCompanySize() + " employees</td></tr>"
                + (booking.getInterestedModules() != null && !booking.getInterestedModules().isEmpty() ? "<tr><td style=\"padding: 6px 0; color: #64748b;\">Key Focus:</td><td style=\"padding: 6px 0; color: #0f172a;\">" + booking.getInterestedModules() + "</td></tr>" : "")
                + "</table>"
                + "</div>"
                + "<p style=\"font-size: 14px; color: #64748b; margin-top: 24px;\">Need to reschedule or have urgent questions? Reply directly to this email or reach us at <a href=\"mailto:support@peoplepulsehr.com\" style=\"color: #1b4fc2; text-decoration: none;\">support@peoplepulsehr.com</a>.</p>"
                + "</div>"
                + "<div style=\"background: #f1f5f9; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8;\">"
                + "&copy; 2026 PeoplePulse HR Inc. All rights reserved. &bull; Pune Headquarters"
                + "</div>"
                + "</div>";

        return sendEmailOrLog(recipient, subject, htmlBody, "Demo Confirmation", booking.getBookingReference());
    }

    /**
     * Sends an alert notification to the internal Sales/Admin team.
     */
    public boolean sendDemoAlertToSalesTeam(DemoBooking booking) {
        String recipient = adminEmail;
        String subject = "🔔 New Demo Lead: " + booking.getFullName() + " (" + booking.getCompanyName() + ") - " + booking.getBookingReference();
        String formattedDate = booking.getPreferredDate() != null ? booking.getPreferredDate().format(DATE_FORMATTER) : "N/A";

        String htmlBody = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 620px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 24px;\">"
                + "<h2 style=\"color: #1b4fc2; margin-top: 0;\">New Demo Booking Received</h2>"
                + "<p style=\"font-size: 14px; color: #475569;\">A new demo booking request was just submitted on the website.</p>"
                + "<table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-weight: 600;\">Prospect Name:</td><td style=\"padding: 8px 0;\">" + booking.getFullName() + " (" + (booking.getJobTitle() != null ? booking.getJobTitle() : "N/A") + ")</td></tr>"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-weight: 600;\">Company:</td><td style=\"padding: 8px 0; font-weight: bold;\">" + booking.getCompanyName() + "</td></tr>"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-weight: 600;\">Work Email:</td><td style=\"padding: 8px 0;\"><a href=\"mailto:" + booking.getWorkEmail() + "\">" + booking.getWorkEmail() + "</a></td></tr>"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-weight: 600;\">Phone:</td><td style=\"padding: 8px 0;\">" + booking.getPhone() + "</td></tr>"
                + "<tr><td style=\"padding: 8px 0; color: #64748b; font-weight: 600;\">Target Date:</td><td style=\"padding: 8px 0;\">" + formattedDate + " (" + booking.getPreferredTimeSlot() + ")</td></tr>"
                + "</table>"
                + "</div>";

        return sendEmailOrLog(recipient, subject, htmlBody, "Sales Alert", booking.getBookingReference());
    }

    /**
     * Requirement 3: Sends email notification to employee when their leave request is Approved or Rejected.
     */
    public boolean sendLeaveStatusNotification(LeaveRequest leaveRequest, String status) {
        if (leaveRequest == null || leaveRequest.getUser() == null || leaveRequest.getUser().getEmail() == null) {
            return false;
        }

        String recipient = leaveRequest.getUser().getEmail();
        boolean isApproved = "APPROVED".equalsIgnoreCase(status);
        String subject = (isApproved ? "✅ Leave Request Approved: " : "❌ Leave Request Rejected: ")
                + leaveRequest.getLeaveType().getDisplayName() + " [" + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + "]";

        String statusBadgeColor = isApproved ? "#16a34a" : "#dc2626";
        String statusText = isApproved ? "APPROVED" : "REJECTED";

        String htmlBody = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">"
                + "<div style=\"background: linear-gradient(135deg, #0d1b3e 0%, #1b4fc2 100%); padding: 24px; text-align: center; color: #ffffff;\">"
                + "<h2 style=\"margin: 0; font-size: 22px;\">PeoplePulse HR &bull; Leave Notification</h2>"
                + "</div>"
                + "<div style=\"padding: 24px; color: #1e293b;\">"
                + "<p style=\"font-size: 16px;\">Dear <strong>" + leaveRequest.getUser().getFullName() + "</strong>,</p>"
                + "<p style=\"font-size: 14px; color: #475569;\">Your leave application has been reviewed and marked as "
                + "<span style=\"display: inline-block; padding: 4px 10px; background: " + statusBadgeColor + "; color: #ffffff; border-radius: 20px; font-weight: bold; font-size: 12px;\">"
                + statusText + "</span>.</p>"
                + "<div style=\"background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 16px; margin: 20px 0;\">"
                + "<table style=\"width: 100%; border-collapse: collapse; font-size: 14px;\">"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Leave Type:</td><td style=\"padding: 6px 0; font-weight: 600;\">" + leaveRequest.getLeaveType().getDisplayName() + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">From Date:</td><td style=\"padding: 6px 0; font-weight: 600;\">" + leaveRequest.getStartDate() + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">To Date:</td><td style=\"padding: 6px 0; font-weight: 600;\">" + leaveRequest.getEndDate() + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Duration:</td><td style=\"padding: 6px 0; font-weight: 600;\">" + leaveRequest.getDurationInDays() + " Day(s)</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Reason:</td><td style=\"padding: 6px 0;\">" + leaveRequest.getReason() + "</td></tr>"
                + "<tr><td style=\"padding: 6px 0; color: #64748b;\">Admin Comments:</td><td style=\"padding: 6px 0; font-style: italic;\">" + (leaveRequest.getAdminComments() != null ? leaveRequest.getAdminComments() : "None") + "</td></tr>"
                + (leaveRequest.getReviewedByUser() != null ? "<tr><td style=\"padding: 6px 0; color: #64748b;\">Reviewed By:</td><td style=\"padding: 6px 0;\">" + leaveRequest.getReviewedByUser().getFullName() + "</td></tr>" : "")
                + "</table>"
                + "</div>"
                + "<p style=\"font-size: 13px; color: #64748b;\">You can view your updated leave balance in the <a href=\"http://localhost:8085/dashboard\" style=\"color: #1b4fc2;\">Employee Portal</a>.</p>"
                + "</div>"
                + "<div style=\"background: #f1f5f9; padding: 12px; text-align: center; font-size: 11px; color: #94a3b8;\">"
                + "Automated notification from PeoplePulse HR Attendance Platform"
                + "</div>"
                + "</div>";

        return sendEmailOrLog(recipient, subject, htmlBody, "Leave Status Notification", "LEAVE-" + leaveRequest.getId());
    }

    /**
     * Requirement 4: Sends OTP verification email to employee for portal sign-in.
     */
    public boolean sendLoginOtpEmail(String recipientEmail, String otpCode, String employeeName) {
        String subject = "Your PeoplePulse HR Sign-In OTP: " + otpCode;

        String htmlBody = "<div style=\"font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto; background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; overflow: hidden;\">"
                + "<div style=\"background: linear-gradient(135deg, #0d1b3e 0%, #1b4fc2 100%); padding: 20px; text-align: center; color: #ffffff;\">"
                + "<h2 style=\"margin: 0; font-size: 20px;\">PeoplePulse HR Verification</h2>"
                + "</div>"
                + "<div style=\"padding: 24px; text-align: center; color: #1e293b;\">"
                + "<p style=\"font-size: 15px; margin-bottom: 8px;\">Hello <strong>" + (employeeName != null ? employeeName : "Employee") + "</strong>,</p>"
                + "<p style=\"font-size: 14px; color: #64748b; margin-bottom: 20px;\">Use the one-time password below to authenticate and sign into your employee dashboard:</p>"
                + "<div style=\"display: inline-block; background: #f0f4ff; border: 2px dashed #1b4fc2; border-radius: 8px; padding: 12px 28px; margin: 10px 0 20px;\">"
                + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1b4fc2;\">" + otpCode + "</span>"
                + "</div>"
                + "<p style=\"font-size: 12px; color: #94a3b8;\">This OTP is valid for 5 minutes. Do not share this code with anyone.</p>"
                + "</div>"
                + "<div style=\"background: #f1f5f9; padding: 10px; text-align: center; font-size: 11px; color: #94a3b8;\">"
                + "&copy; 2026 PeoplePulse HR Inc. &bull; Enterprise Secure Access"
                + "</div>"
                + "</div>";

        return sendEmailOrLog(recipientEmail, subject, htmlBody, "Login OTP", "OTP-" + otpCode);
    }

    private boolean sendEmailOrLog(String recipient, String subject, String htmlBody, String emailType, String reference) {
        if (mailEnabled && mailSender != null) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(mailFrom, "PeoplePulse HR");
                helper.setTo(recipient);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);

                Map<String, Object> details = new HashMap<>();
                details.put("recipient", recipient);
                details.put("subject", subject);
                details.put("reference", reference);
                details.put("status", "SENT_SMTP");
                CustomLogger.printSuccessLog("EmailNotificationService." + emailType, "Dispatched email notification", 0, details);
                return true;
            } catch (Exception e) {
                Map<String, Object> details = new HashMap<>();
                details.put("recipient", recipient);
                details.put("error", e.getMessage());
                details.put("reference", reference);
                CustomLogger.printErrorLog("EmailNotificationService." + emailType, "Failed to send email via SMTP", details, e);
            }
        }

        // Development fallback / Simulation logging
        Map<String, Object> simDetails = new HashMap<>();
        simDetails.put("recipient", recipient);
        simDetails.put("subject", subject);
        simDetails.put("reference", reference);
        simDetails.put("deliveryMode", "SIMULATED_DEV (Set app.mail.enabled=true & configure SMTP to send live)");
        CustomLogger.printSuccessLog("EmailNotificationService." + emailType + ".Simulated", "Simulated email notification", 0, simDetails);
        return true;
    }
}
