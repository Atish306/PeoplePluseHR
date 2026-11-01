package com.atish.attendance.controller;

import com.atish.attendance.logging.EndpointDebugEntry;
import com.atish.attendance.logging.EndpointDebugService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller providing administrative access to endpoint diagnostic logs,
 * live performance metrics, and endpoint bug inspection.
 */
@Controller
@RequestMapping("/admin/debug")
public class DebugController {

    private final EndpointDebugService debugService;

    public DebugController(EndpointDebugService debugService) {
        this.debugService = debugService;
    }

    @GetMapping("/logs")
    public String showDebugLogs(@RequestParam(value = "errorsOnly", defaultValue = "false") boolean errorsOnly,
                                @RequestParam(value = "search", required = false) String search,
                                Model model) {

        List<EndpointDebugEntry> entries = debugService.getRecentEntries(errorsOnly, search);
        model.addAttribute("entries", entries);
        model.addAttribute("totalCount", debugService.getTotalCount());
        model.addAttribute("errorCount", debugService.getErrorCount());
        model.addAttribute("slowCount", debugService.getSlowCount());
        model.addAttribute("errorsOnly", errorsOnly);
        model.addAttribute("search", search != null ? search : "");
        return "admin/debug-logs";
    }

    @GetMapping("/api/logs")
    @ResponseBody
    public ResponseEntity<List<EndpointDebugEntry>> getLogsApi(
            @RequestParam(value = "errorsOnly", defaultValue = "false") boolean errorsOnly,
            @RequestParam(value = "search", required = false) String search) {
        return ResponseEntity.ok(debugService.getRecentEntries(errorsOnly, search));
    }

    @PostMapping("/clear")
    public String clearLogs() {
        debugService.clear();
        return "redirect:/admin/debug/logs";
    }

    /**
     * Test endpoint designed to deliberately trigger an exception
     * to verify real-time endpoint bug logging and diagnostic banner output.
     */
    @GetMapping("/simulate-error")
    public String simulateError(@RequestParam(value = "param", defaultValue = "test-value") String param) {
        throw new IllegalStateException("Deliberate diagnostic test exception triggered on /admin/debug/simulate-error with parameter: " + param);
    }

    /**
     * Test endpoint to verify custom printSuccessLog and printErrorLog outputs.
     */
    @GetMapping("/test-custom-logger")
    public String testCustomLogger(@RequestParam(value = "action", defaultValue = "success") String action) {
        if ("error".equalsIgnoreCase(action)) {
            com.atish.attendance.logging.CustomLogger.printErrorLog(
                    "GET /admin/debug/test-custom-logger?action=error",
                    "Simulated error via CustomLogger.printErrorLog",
                    new IllegalArgumentException("Sample deliberate exception for printErrorLog testing")
            );
            return "redirect:/admin/debug/logs?errorsOnly=true";
        } else {
            com.atish.attendance.logging.CustomLogger.printSuccessLog(
                    "GET /admin/debug/test-custom-logger?action=success",
                    "Sample successful operation recorded via CustomLogger.printSuccessLog",
                    18L,
                    java.util.Collections.singletonMap("sampleKey", "sampleValue")
            );
            return "redirect:/admin/debug/logs";
        }
    }
}
