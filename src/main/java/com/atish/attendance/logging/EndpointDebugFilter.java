package com.atish.attendance.logging;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servlet filter that captures HTTP endpoint invocations, measures latency,
 * associates an MDC Trace ID, and collects request context for bug diagnostics.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class EndpointDebugFilter extends OncePerRequestFilter {

    public static final String DEBUG_ENTRY_ATTR = "ENDPOINT_DEBUG_ENTRY";
    private static final Set<String> SENSITIVE_PARAMS = new HashSet<>(
            Arrays.asList("password", "confirmpassword", "secret", "token", "pwd")
    );

    private final EndpointDebugService debugService;

    public EndpointDebugFilter(EndpointDebugService debugService) {
        this.debugService = debugService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip static resources and health endpoints to prevent log clutter
        return path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/fonts/") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String traceId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-ID", traceId);

        EndpointDebugEntry entry = new EndpointDebugEntry();
        entry.setTraceId(traceId);
        entry.setMethod(request.getMethod());
        entry.setUri(request.getRequestURI());
        entry.setQueryString(request.getQueryString());
        entry.setClientIp(extractClientIp(request));
        entry.setParameters(extractSanitizedParams(request));

        request.setAttribute(DEBUG_ENTRY_ATTR, entry);

        try {
            filterChain.doFilter(request, response);
        } catch (Throwable ex) {
            // Handled or bubbling exception
            if (!entry.isHasError()) {
                debugService.logEndpointBugBanner(entry, ex);
            }
            throw ex;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            entry.setExecutionTimeMs(duration);
            entry.setStatusCode(response.getStatus());

            // Extract logged-in user context
            enrichUserContext(entry);

            // Record into service / log
            debugService.recordEntry(entry);

            MDC.remove("traceId");
        }
    }

    private void enrichUserContext(EndpointDebugEntry entry) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            entry.setUsername(auth.getName());
            String roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(", "));
            entry.setUserRoles(roles);
        } else {
            entry.setUsername("anonymous");
            entry.setUserRoles("NONE");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.trim().isEmpty()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Map<String, String> extractSanitizedParams(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap == null || parameterMap.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : parameterMap.entrySet()) {
            String key = e.getKey();
            if (SENSITIVE_PARAMS.contains(key.toLowerCase())) {
                sanitized.put(key, "******");
            } else {
                sanitized.put(key, Arrays.toString(e.getValue()));
            }
        }
        return sanitized;
    }
}
