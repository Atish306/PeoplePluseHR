package com.atish.attendance.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom Logger providing standardized printSuccessLog and printErrorLog methods
 * for clear endpoint tracking, execution monitoring, and bug debugging.
 * Supports both Spring bean injection and static access.
 */
@Component
public class CustomLogger {

    private static final Logger log = LoggerFactory.getLogger("EndpointLogger");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static CustomLogger instance;

    public CustomLogger() {
        instance = this;
    }

    public static CustomLogger getInstance() {
        if (instance == null) {
            instance = new CustomLogger();
        }
        return instance;
    }

    // =========================================================================
    // printSuccessLog Overloads
    // =========================================================================

    /**
     * Prints a standardized success log for an action or endpoint.
     */
    public static void printSuccessLog(String endpointOrAction, String message) {
        getInstance().logSuccess(endpointOrAction, message, null, -1, null);
    }

    /**
     * Prints a standardized success log with latency in milliseconds.
     */
    public static void printSuccessLog(String endpointOrAction, String message, long executionTimeMs) {
        getInstance().logSuccess(endpointOrAction, message, null, executionTimeMs, null);
    }

    /**
     * Prints a standardized success log with custom parameters or payload details.
     */
    public static void printSuccessLog(String endpointOrAction, String message, Object details) {
        getInstance().logSuccess(endpointOrAction, message, details, -1, null);
    }

    /**
     * Prints a standardized success log with latency and custom details.
     */
    public static void printSuccessLog(String endpointOrAction, String message, long executionTimeMs, Object details) {
        getInstance().logSuccess(endpointOrAction, message, details, executionTimeMs, null);
    }

    /**
     * Prints a standardized success log extracting context from an HttpServletRequest.
     */
    public static void printSuccessLog(HttpServletRequest request, String message, long executionTimeMs) {
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String clientIp = extractClientIp(request);
        getInstance().logSuccess(endpoint, message, request.getParameterMap(), executionTimeMs, clientIp);
    }

    // =========================================================================
    // printErrorLog Overloads
    // =========================================================================

    /**
     * Prints a standardized error log for an action or endpoint.
     */
    public static void printErrorLog(String endpointOrAction, String errorMessage) {
        getInstance().logError(endpointOrAction, errorMessage, null, null, null);
    }

    /**
     * Prints a standardized error log with an exception and stack trace.
     */
    public static void printErrorLog(String endpointOrAction, String errorMessage, Throwable throwable) {
        getInstance().logError(endpointOrAction, errorMessage, null, throwable, null);
    }

    /**
     * Prints a standardized error log with context data and an exception.
     */
    public static void printErrorLog(String endpointOrAction, String errorMessage, Object contextData, Throwable throwable) {
        getInstance().logError(endpointOrAction, errorMessage, contextData, throwable, null);
    }

    /**
     * Prints a standardized error log extracting context from an HttpServletRequest.
     */
    public static void printErrorLog(HttpServletRequest request, String errorMessage, Throwable throwable) {
        String endpoint = request.getMethod() + " " + request.getRequestURI();
        String clientIp = extractClientIp(request);
        getInstance().logError(endpoint, errorMessage, request.getParameterMap(), throwable, clientIp);
    }

    // =========================================================================
    // Core Formatting Implementation
    // =========================================================================

    private void logSuccess(String endpoint, String message, Object details, long executionTimeMs, String clientIp) {
        String traceId = getOrGenerateTraceId();
        String userContext = getCurrentUserContext();
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);

        StringBuilder sb = new StringBuilder();
        sb.append("\n--------------------------------------------------------------------------------\n");
        sb.append(String.format(">>> [SUCCESS] %s | Trace: [%s]%n", timestamp, traceId));
        sb.append(String.format("    Endpoint / Action : %s%n", endpoint));
        sb.append(String.format("    User              : %s%n", userContext));
        if (clientIp != null) {
            sb.append(String.format("    Client IP         : %s%n", clientIp));
        }
        if (executionTimeMs >= 0) {
            sb.append(String.format("    Execution Latency : %d ms%n", executionTimeMs));
        }
        sb.append(String.format("    Message           : %s%n", message));
        if (details != null) {
            sb.append(String.format("    Details / Data    : %s%n", formatDetails(details)));
        }
        sb.append("--------------------------------------------------------------------------------");

        log.info(sb.toString());
    }

    private void logError(String endpoint, String errorMessage, Object contextData, Throwable throwable, String clientIp) {
        String traceId = getOrGenerateTraceId();
        String userContext = getCurrentUserContext();
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);

        StringBuilder sb = new StringBuilder();
        sb.append("\n================================================================================\n");
        sb.append(String.format("!!! [ERROR BUG DETECTED] %s | Trace: [%s]%n", timestamp, traceId));
        sb.append("================================================================================\n");
        sb.append(String.format("    Endpoint / Action : %s%n", endpoint));
        sb.append(String.format("    User Context      : %s%n", userContext));
        if (clientIp != null) {
            sb.append(String.format("    Client IP         : %s%n", clientIp));
        }
        sb.append(String.format("    Error Summary     : %s%n", errorMessage));

        if (contextData != null) {
            sb.append(String.format("    Request / Payload : %s%n", formatDetails(contextData)));
        }

        if (throwable != null) {
            sb.append("--------------------------------------------------------------------------------\n");
            sb.append(String.format("    Exception Type    : %s%n", throwable.getClass().getName()));
            sb.append(String.format("    Root Message      : %s%n", throwable.getMessage() != null ? throwable.getMessage() : "N/A"));
            sb.append("    Stack Trace (Application Frames):\n");
            sb.append(formatStackTrace(throwable));
        }
        sb.append("\n================================================================================");

        log.error(sb.toString());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private static String getOrGenerateTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = "REQ-ADHOC";
        }
        return traceId;
    }

    private static String getCurrentUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            return auth.getName() + " (" + roles + ")";
        }
        return "anonymous";
    }

    private static String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.trim().isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String formatDetails(Object details) {
        if (details instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> map = (Map<String, ?>) details;
            return map.entrySet().stream()
                    .map(e -> {
                        Object val = e.getValue();
                        if (val instanceof Object[]) {
                            val = Arrays.toString((Object[]) val);
                        }
                        return e.getKey() + "=" + val;
                    })
                    .collect(Collectors.joining(", ", "{", "}"));
        }
        return String.valueOf(details);
    }

    private static String formatStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String[] lines = sw.toString().split("\r?\n");

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (count == 0) {
                count++;
                continue; // skip the exception header line already printed
            }
            if (line.contains("com.atish.attendance") || count < 8) {
                sb.append("        ").append(line.trim()).append("\n");
                count++;
                if (count >= 12) break;
            }
        }
        return sb.toString();
    }
}
