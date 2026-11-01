package com.atish.attendance.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Custom logger service for recording endpoint operations, diagnosing endpoint bugs,
 * and maintaining a live in-memory circular buffer for administrative inspection.
 */
@Service
public class EndpointDebugService {

    private static final Logger log = LoggerFactory.getLogger(EndpointDebugService.class);
    private static final int MAX_BUFFER_SIZE = 100;

    private final Deque<EndpointDebugEntry> buffer = new ConcurrentLinkedDeque<>();
    private final AtomicLong entryIdCounter = new AtomicLong(1);

    /**
     * Records a completed endpoint transaction.
     */
    public void recordEntry(EndpointDebugEntry entry) {
        if (entry.getId() == null) {
            entry.setId("LOG-" + entryIdCounter.getAndIncrement());
        }

        // Add to front of circular buffer
        buffer.addFirst(entry);
        while (buffer.size() > MAX_BUFFER_SIZE) {
            buffer.removeLast();
        }

        // SLF4J Output
        if (entry.isHasError()) {
            log.error("[{}] {} {} -> Status {} ({}ms) [BUG DETECTED: {}]",
                    entry.getTraceId(), entry.getMethod(), entry.getUri(),
                    entry.getStatusCode(), entry.getExecutionTimeMs(),
                    entry.getExceptionMessage());
        } else if (entry.isSlow()) {
            log.warn("[{}] {} {} -> Status {} ({}ms) [SLOW ENDPOINT]",
                    entry.getTraceId(), entry.getMethod(), entry.getUri(),
                    entry.getStatusCode(), entry.getExecutionTimeMs());
        } else {
            log.info("[{}] {} {} -> Status {} ({}ms)",
                    entry.getTraceId(), entry.getMethod(), entry.getUri(),
                    entry.getStatusCode(), entry.getExecutionTimeMs());
        }
    }

    /**
     * Emits a formatted visual diagnostic banner specifically for endpoint bugs.
     */
    public void logEndpointBugBanner(EndpointDebugEntry entry, Throwable throwable) {
        if (entry.getUsername() == null) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                entry.setUsername(auth.getName());
                String roles = auth.getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(", "));
                entry.setUserRoles(roles);
            } else {
                entry.setUsername("anonymous");
                entry.setUserRoles("NONE");
            }
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String fullStackTrace = sw.toString();

        entry.setHasError(true);
        entry.setExceptionClass(throwable.getClass().getName());
        entry.setExceptionMessage(throwable.getMessage() != null ? throwable.getMessage() : throwable.toString());
        entry.setStackTrace(filterStackTrace(fullStackTrace));

        StringBuilder sb = new StringBuilder();
        sb.append("\n================================================================================\n");
        sb.append("[!] ENDPOINT BUG DETECTED\n");
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("Trace ID      : %s%n", entry.getTraceId()));
        sb.append(String.format("Timestamp     : %s%n", entry.getFormattedTimestamp()));
        sb.append(String.format("Endpoint      : %s %s%n", entry.getMethod(), entry.getUri()));
        if (entry.getQueryString() != null && !entry.getQueryString().isEmpty()) {
            sb.append(String.format("Query Params  : %s%n", entry.getQueryString()));
        }
        sb.append(String.format("Form Data     : %s%n", entry.getParameters()));
        sb.append(String.format("Client IP     : %s%n", entry.getClientIp()));
        sb.append(String.format("User / Roles  : %s (%s)%n", entry.getUsername(), entry.getUserRoles()));
        if (entry.getHandlerMethod() != null) {
            sb.append(String.format("Controller    : %s%n", entry.getHandlerMethod()));
        }
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append(String.format("Exception     : %s%n", entry.getExceptionClass()));
        sb.append(String.format("Message       : %s%n", entry.getExceptionMessage()));
        sb.append("--------------------------------------------------------------------------------\n");
        sb.append("Stack Trace (Top Relevant Lines):\n");
        sb.append(entry.getStackTrace()).append("\n");
        sb.append("================================================================================\n");

        log.error(sb.toString());
    }

    /**
     * Filter stack trace to show top 15 application-relevant frames.
     */
    private String filterStackTrace(String fullStackTrace) {
        String[] lines = fullStackTrace.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (count == 0) {
                // Header (exception name + message)
                sb.append(line).append("\n");
                count++;
            } else if (line.contains("com.atish.attendance") || count < 10) {
                sb.append(line).append("\n");
                count++;
                if (count >= 15) break;
            }
        }
        return sb.toString().trim();
    }

    public List<EndpointDebugEntry> getRecentEntries(boolean onlyErrors, String search) {
        return buffer.stream()
                .filter(e -> !onlyErrors || e.isHasError())
                .filter(e -> {
                    if (search == null || search.trim().isEmpty()) return true;
                    String s = search.toLowerCase();
                    return (e.getUri() != null && e.getUri().toLowerCase().contains(s)) ||
                            (e.getTraceId() != null && e.getTraceId().toLowerCase().contains(s)) ||
                            (e.getUsername() != null && e.getUsername().toLowerCase().contains(s)) ||
                            (e.getExceptionMessage() != null && e.getExceptionMessage().toLowerCase().contains(s));
                })
                .collect(Collectors.toList());
    }

    public void clear() {
        buffer.clear();
    }

    public long getTotalCount() {
        return buffer.size();
    }

    public long getErrorCount() {
        return buffer.stream().filter(EndpointDebugEntry::isHasError).count();
    }

    public long getSlowCount() {
        return buffer.stream().filter(EndpointDebugEntry::isSlow).count();
    }
}
