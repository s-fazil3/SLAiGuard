package com.sla.backend.service;

import com.sla.backend.dto.AlertResponse;
import com.sla.backend.entity.Alert;
import com.sla.backend.exception.AlertNotFoundException;
import com.sla.backend.repository.AlertRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertService {
    private AlertRepository alertRepository;
    private static final int ALERT_COOLDOWN_MINUTES = 1; // Only create one alert per metric every 1 minute
    
    public AlertService(AlertRepository alertRepository){
        this.alertRepository=alertRepository;
    }

    public Alert createAlert(String metricname,double actualValue,double threshold,String severity){
        System.out.println("[AlertService] Creating alert: metric=" + metricname + ", value=" + actualValue + ", threshold=" + threshold + ", severity=" + severity);
        Alert alert=new Alert(metricname,actualValue,severity,threshold,LocalDateTime.now());
        Alert savedAlert = alertRepository.save(alert);
        System.out.println("[AlertService] Alert saved with ID: " + savedAlert.getId() + ", status: " + savedAlert.getStatus());
        return savedAlert;
    }

    public Alert createPredictedAlert(String metricname, double currentValue, double predictedValue, double threshold){
        System.out.println("[AlertService] Creating PREDICTED alert: metric=" + metricname + ", current=" + currentValue + ", predicted=" + predictedValue + ", threshold=" + threshold);
        Alert alert = new Alert(metricname, currentValue, predictedValue, "PREDICTED_RISK", threshold, LocalDateTime.now());
        Alert savedAlert = alertRepository.save(alert);
        System.out.println("[AlertService] Predicted alert saved with ID: " + savedAlert.getId() + ", status: " + savedAlert.getStatus());
        System.out.println("[AlertService] Saved alert currentValue: " + savedAlert.getCurrentValue() + ", actualValue: " + savedAlert.getActualValue());
        return savedAlert;
    }

    private AlertResponse mapToresponse(Alert alert){
        String status = alert.getStatus() != null ? alert.getStatus() : (alert.isAcknowledged() ? "ACKNOWLEDGED" : "ACTIVE");
        
        if ("PREDICTED_RISK".equals(alert.getSeverity())) {
            System.out.println("[AlertService] Mapping PREDICTED_RISK alert ID: " + alert.getId());
            System.out.println("[AlertService] Retrieved currentValue: " + alert.getCurrentValue() + ", actualValue: " + alert.getActualValue());
            // For PREDICTED_RISK alerts, include both current and predicted values
            return new AlertResponse(
                alert.getId(),
                alert.getMetricname(),
                alert.getCurrentValue(), // current value
                alert.getActualValue(), // predicted value
                alert.getThreshold(),
                alert.getSeverity(),
                status,
                alert.getTimestamp()
            );
        } else {
            // For CRITICAL/WARNING alerts, use the regular constructor
            return new AlertResponse(
                alert.getId(),
                alert.getMetricname(),
                alert.getActualValue(),
                alert.getThreshold(),
                alert.getSeverity(),
                status,
                alert.getTimestamp()
            );
        }
    }
    public Alert acknowledgeAlert(Long alertId){
        Alert alert = alertRepository.findById(alertId).orElseThrow(() -> new AlertNotFoundException(alertId));
        alert.setAcknowledged(true);
        return alertRepository.save(alert);
    }

    public Alert resolveAlert(Long alertId, String resolution) {
        Alert alert = alertRepository.findById(alertId).orElseThrow(() -> new AlertNotFoundException(alertId));
        alert.resolve();
        return alertRepository.save(alert);
    }

    public void resolveAlertsByService(String serviceName) {
        // Allow resolving acknowledged alerts that aren't already resolved
        List<Alert> activeAlerts = alertRepository.findByMetricnameAndStatusNot(serviceName, "RESOLVED");
        activeAlerts.forEach(alert -> {
            alert.resolve();
            alertRepository.save(alert);
        });
    }

    public void resolveAllActiveAlerts() {
        System.out.println("[AlertService] resolveAllActiveAlerts() called");
        try {
            // Get all alerts first to check what we have
            List<Alert> allAlerts = alertRepository.findAll();
            System.out.println("[AlertService] Total alerts in database: " + allAlerts.size());
            
            // Filter manually with null safety
            List<Alert> alertsToResolve = new java.util.ArrayList<>();
            for (Alert alert : allAlerts) {
                String status = alert.getStatus();
                System.out.println("[AlertService] Alert ID: " + alert.getId() + " has status: " + status);
                
                // Handle null status - treat as ACTIVE
                if (status == null) {
                    System.out.println("[AlertService] Alert ID " + alert.getId() + " has null status, treating as ACTIVE");
                    alert.setStatus("ACTIVE");
                    alertsToResolve.add(alert);
                } else if (!"RESOLVED".equals(status)) {
                    alertsToResolve.add(alert);
                }
            }
            
            System.out.println("[AlertService] Found " + alertsToResolve.size() + " alerts to resolve");
            
            if (alertsToResolve.isEmpty()) {
                System.out.println("[AlertService] No active alerts found to resolve");
                return;
            }
            
            int resolvedCount = 0;
            int failedCount = 0;
            
            for (Alert alert : alertsToResolve) {
                try {
                    System.out.println("[AlertService] Resolving alert ID: " + alert.getId() + " (current status: " + alert.getStatus() + ")");
                    
                    // Ensure status is not null before resolving
                    if (alert.getStatus() == null) {
                        alert.setStatus("ACTIVE");
                    }
                    
                    alert.resolve();
                    alertRepository.save(alert);
                    resolvedCount++;
                    System.out.println("[AlertService] Successfully resolved alert ID: " + alert.getId() + ", new status: " + alert.getStatus());
                } catch (Exception e) {
                    failedCount++;
                    System.err.println("[AlertService] FAILED to resolve alert ID " + alert.getId() + ": " + e.getClass().getName() + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("[AlertService] Summary: Resolved " + resolvedCount + ", Failed " + failedCount + " out of " + alertsToResolve.size() + " alerts");
            
            if (failedCount > 0) {
                throw new RuntimeException(failedCount + " out of " + alertsToResolve.size() + " alerts failed to resolve. Check logs for details.");
            }
            
        } catch (Exception e) {
            System.err.println("[AlertService] CRITICAL ERROR in resolveAllActiveAlerts: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to resolve all alerts: " + e.getMessage(), e);
        }
    }

    public long countTotalAlerts() {
        return alertRepository.count();
    }
    
    public long countActiveAlerts() {
        return alertRepository.findByStatusNot("RESOLVED").size();
    }

    public boolean hasActiveAlerts(String serviceName) {
        return alertRepository.existsByAcknowledgedFalseAndMetricname(serviceName);
    }
    
    public boolean hasRecentAlert(String metricName) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(ALERT_COOLDOWN_MINUTES);
        return alertRepository.findTopByMetricnameAndTimestampAfterOrderByTimestampDesc(metricName, oneMinuteAgo).isPresent();
    }

    /**
     * Checks if there is a recent alert of a SPECIFIC severity for this metric.
     * Used to allow "Predicted Risk" cards to appear even if a "Warning" exists.
     */
    public boolean hasRecentAlertOfSeverity(String metricName, String severity) {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(ALERT_COOLDOWN_MINUTES);
        return alertRepository.findAll().stream()
            .filter(a -> a.getMetricname().equals(metricName))
            .filter(a -> a.getSeverity().equals(severity))
            .filter(a -> a.getTimestamp().isAfter(oneMinuteAgo))
            .findFirst()
            .isPresent();
    }

    public boolean hasActivePredictedAlert(String metricName, LocalDateTime since) {
        return alertRepository.findAll().stream()
            .filter(a -> a.getMetricname().equals(metricName))
            .filter(a -> a.getSeverity().equals("PREDICTED_RISK"))
            .filter(a -> a.getTimestamp().isAfter(since))
            .findFirst()
            .isPresent();
    }

    
    public Page<AlertResponse> getActiveAlerts(int page,int size,String severity){
        Pageable pageable= PageRequest.of(page,size, Sort.by("timestamp").descending());
        Page<Alert> alertPage;
        if(severity!=null){
            // Show acknowledged alerts that aren't resolved for admin management
            alertPage=alertRepository.findByStatusNot("RESOLVED",pageable);
        }
        else{
            // Show acknowledged alerts that aren't resolved for admin management
            alertPage=alertRepository.findByStatusNot("RESOLVED",pageable);
        }
        return alertPage.map(this::mapToresponse);
    }
    public List<AlertResponse> getAlertHistory() {
        return alertRepository.findByAcknowledgedTrue()
                .stream()
                .map(this::mapToresponse)
                .toList();
    }
}
