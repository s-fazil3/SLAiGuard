package com.sla.backend.controller;

import com.sla.backend.dto.AlertResponse;
import com.sla.backend.entity.Alert;
import com.sla.backend.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/alerts")
public class AlertController {
    private final AlertService alertService;
    public AlertController(AlertService alertService){
        this.alertService=alertService;
    }

    @GetMapping
    public Page<AlertResponse> getActiveAlerts(@RequestParam(defaultValue = "0")int page,@RequestParam(defaultValue = "10")int size, @RequestParam(required = false)String severity){
        return alertService.getActiveAlerts(page,size,severity);
    }

    @PutMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public Alert acknowledgeAlert(@PathVariable Long alertId) {
        return alertService.acknowledgeAlert(alertId);
    }

    @PutMapping("/{id}/resolve")
    public Alert resolveAlert(@PathVariable Long id, @RequestParam(required = false) String resolution){
        return alertService.resolveAlert(id, resolution);
    }

    @GetMapping("/test")
    public ResponseEntity<String> testAlerts() {
        try {
            long totalAlerts = alertService.countTotalAlerts();
            long activeAlerts = alertService.countActiveAlerts();
            return ResponseEntity.ok("Alerts test - Total: " + totalAlerts + ", Active: " + activeAlerts);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Test failed: " + e.getMessage());
        }
    }

    @PutMapping("/resolve-all")
    public ResponseEntity<String> resolveAllAlerts(){
        System.out.println("[AlertController] resolveAllAlerts() called at " + java.time.LocalDateTime.now());
        try {
            alertService.resolveAllActiveAlerts();
            System.out.println("[AlertController] resolveAllAlerts() completed successfully");
            return ResponseEntity.ok("All alerts resolved successfully");
        } catch (Exception e) {
            System.err.println("[AlertController] ERROR in resolveAllAlerts: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            // Write detailed error to file for debugging
            try {
                java.nio.file.Path logPath = java.nio.file.Paths.get("error-resolve-all.log");
                StringBuilder sb = new StringBuilder();
                sb.append("Error Time: ").append(java.time.LocalDateTime.now()).append("\n");
                sb.append("Exception: ").append(e.getClass().getName()).append("\n");
                sb.append("Message: ").append(e.getMessage()).append("\n");
                sb.append("Stack Trace:\n");
                for (StackTraceElement ste : e.getStackTrace()) {
                    sb.append("  at ").append(ste.toString()).append("\n");
                }
                java.nio.file.Files.write(logPath, sb.toString().getBytes(), 
                    java.nio.file.StandardOpenOption.CREATE, 
                    java.nio.file.StandardOpenOption.APPEND);
                System.out.println("[AlertController] Error details written to: " + logPath.toAbsolutePath());
            } catch (Exception logEx) {
                System.err.println("[AlertController] Failed to write error log: " + logEx.getMessage());
            }
            
            return ResponseEntity.status(500).body("Failed to resolve alerts: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @PutMapping("/resolve-by-service/{serviceName}")
    public void resolveAlertsByService(@PathVariable String serviceName){
        alertService.resolveAlertsByService(serviceName);
    }

    @GetMapping("/history")
    public List<AlertResponse> getAlertHistory(){
        return alertService.getAlertHistory();
    }
}
