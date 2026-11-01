package com.atish.attendance.logging;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

/**
 * Data model representing an HTTP endpoint execution event,
 * capturing request parameters, client identity, execution time, and potential errors.
 */
public class EndpointDebugEntry implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String traceId;
    private LocalDateTime timestamp;
    private String method;
    private String uri;
    private String queryString;
    private String clientIp;
    private String username;
    private String userRoles;
    private Map<String, String> parameters;
    private int statusCode;
    private long executionTimeMs;
    private boolean slow;
    private boolean hasError;
    private String handlerMethod;
    private String exceptionClass;
    private String exceptionMessage;
    private String stackTrace;

    public EndpointDebugEntry() {
        this.timestamp = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.format(FORMATTER) : "";
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryString(String queryString) {
        this.queryString = queryString;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(String userRoles) {
        this.userRoles = userRoles;
    }

    public Map<String, String> getParameters() {
        return parameters != null ? parameters : Collections.emptyMap();
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
        this.slow = executionTimeMs > 500;
    }

    public boolean isSlow() {
        return slow;
    }

    public void setSlow(boolean slow) {
        this.slow = slow;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getHandlerMethod() {
        return handlerMethod;
    }

    public void setHandlerMethod(String handlerMethod) {
        this.handlerMethod = handlerMethod;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getStatusBadgeClass() {
        if (hasError || statusCode >= 500) {
            return "bg-danger text-white";
        }
        if (statusCode >= 400) {
            return "bg-warning text-dark";
        }
        if (statusCode >= 300) {
            return "bg-info text-dark";
        }
        return "bg-success text-white";
    }

    public String getMethodBadgeClass() {
        if ("GET".equalsIgnoreCase(method)) return "bg-primary text-white";
        if ("POST".equalsIgnoreCase(method)) return "bg-success text-white";
        if ("PUT".equalsIgnoreCase(method)) return "bg-info text-dark";
        if ("DELETE".equalsIgnoreCase(method)) return "bg-danger text-white";
        return "bg-secondary text-white";
    }
}
