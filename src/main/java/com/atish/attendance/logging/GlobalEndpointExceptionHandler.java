package com.atish.attendance.logging;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler that catches uncaught exceptions across all controllers,
 * logs high-visibility diagnostic bug reports, and formats response based on request type.
 */
@ControllerAdvice
public class GlobalEndpointExceptionHandler {

    private final EndpointDebugService debugService;

    public GlobalEndpointExceptionHandler(EndpointDebugService debugService) {
        this.debugService = debugService;
    }

    @ExceptionHandler(Throwable.class)
    public Object handleEndpointException(Throwable ex,
                                          HandlerMethod handlerMethod,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {

        EndpointDebugEntry entry = (EndpointDebugEntry) request.getAttribute(EndpointDebugFilter.DEBUG_ENTRY_ATTR);
        if (entry == null) {
            entry = new EndpointDebugEntry();
            entry.setTraceId("REQ-UNKNOWN");
            entry.setUri(request.getRequestURI());
            entry.setMethod(request.getMethod());
            entry.setClientIp(request.getRemoteAddr());
        }

        if (handlerMethod != null) {
            entry.setHandlerMethod(handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName());
        }

        entry.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // Emit high-visibility terminal banner and SLF4J bug report
        debugService.logEndpointBugBanner(entry, ex);
        CustomLogger.printErrorLog(entry.getMethod() + " " + entry.getUri(),
                "Unhandled exception: " + (ex.getMessage() != null ? ex.getMessage() : ex.toString()),
                entry.getParameters(),
                ex);

        // Check if request is JSON / AJAX
        boolean isApiOrAjax = isJsonOrAjaxRequest(request);

        if (isApiOrAjax) {
            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("timestamp", LocalDateTime.now());
            errorBody.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            errorBody.put("error", "Internal Server Error");
            errorBody.put("traceId", entry.getTraceId());
            errorBody.put("endpoint", entry.getMethod() + " " + entry.getUri());
            errorBody.put("message", ex.getMessage() != null ? ex.getMessage() : ex.toString());
            errorBody.put("exception", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(errorBody);
        }

        // Standard HTML view error page
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mav.addObject("traceId", entry.getTraceId());
        mav.addObject("endpoint", entry.getMethod() + " " + entry.getUri());
        mav.addObject("errorMessage", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.");
        mav.addObject("exceptionClass", ex.getClass().getName());
        mav.addObject("timestamp", entry.getFormattedTimestamp());
        mav.addObject("stackTrace", entry.getStackTrace());
        return mav;
    }

    private boolean isJsonOrAjaxRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/")) {
            return true;
        }
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        String requestedWith = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }
}
