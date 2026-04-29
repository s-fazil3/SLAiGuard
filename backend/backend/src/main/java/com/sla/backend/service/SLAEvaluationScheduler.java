package com.sla.backend.service;
import com.sla.backend.entity.AggregatedMetrics;
import com.sla.backend.entity.Sla;
import com.sla.backend.entity.Alert;
import com.sla.backend.repository.AggregatedMetricsRepository;
import com.sla.backend.repository.SlaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SLAEvaluationScheduler {

    private final AggregatedMetricsRepository aggregatedMetricsRepository;
    private final SlaRepository slaRepository;
    private final AlertService alertService;

    public SLAEvaluationScheduler(AggregatedMetricsRepository aggregatedMetricsRepository,
                               SlaRepository slaRepository,
                               AlertService alertService) {
        this.aggregatedMetricsRepository = aggregatedMetricsRepository;
        this.slaRepository = slaRepository;
        this.alertService = alertService;
    }

     @Scheduled(fixedRate = 30000) // Every 30 seconds - TEMPORARILY DISABLED
    public void evaluateSLAs() {
        try {
            System.out.println("[SLAEvaluationScheduler] Starting SLA evaluation at " + LocalDateTime.now());

            // Get latest aggregated metrics
            List<AggregatedMetrics> latestMetrics = aggregatedMetricsRepository.findLatestAll();
            
            if (latestMetrics.isEmpty()) {
                System.out.println("[SLAEvaluationScheduler] No aggregated metrics found for SLA evaluation");
                return;
            }

            // Get all active SLAs
            List<Sla> activeSlas = slaRepository.findAll().stream()
                .filter(Sla::isActive)
                .collect(java.util.stream.Collectors.toList());
            
            if (activeSlas.isEmpty()) {
                System.out.println("[SLAEvaluationScheduler] No active SLAs found");
                return;
            }

            // Evaluate each aggregated metric against its SLA
            for (AggregatedMetrics metric : latestMetrics) {
                for (Sla sla : activeSlas) {
                    if (metric.getServiceName().equals(sla.getMetricName()) || 
                        shouldEvaluateMetric(metric, sla.getMetricName())) {
                        
                        evaluateMetricAgainstSLA(metric, sla);
                    }
                }
            }

            System.out.println("[SLAEvaluationScheduler] SLA evaluation completed successfully");
        } catch (Exception e) {
            System.err.println("[SLAEvaluationScheduler] Error during SLA evaluation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean shouldEvaluateMetric(AggregatedMetrics metric, String slaMetricName) {
        // Map aggregated metrics to SLA metric names
        switch (slaMetricName) {
            case "response_time":
                return metric.getAvgResponseTime() != null;
            case "p95_latency":
                return metric.getP95Latency() != null;
            case "error_rate":
                return metric.getErrorRate() != null;
            case "cpu_usage":
                return metric.getCpuUsage() != null;
            case "memory_usage":
                return metric.getMemoryUsage() != null;
            case "requests_per_minute":
                return metric.getRequestCount() != null;
            default:
                return false;
        }
    }

    private void evaluateMetricAgainstSLA(AggregatedMetrics metric, Sla sla) {
        Double currentValue = getCurrentValueForMetric(metric, sla.getMetricName());
        
        if (currentValue == null) {
            return;
        }

        String metricName = sla.getMetricName();
        double threshold = sla.getThreshold();

        System.out.println("[SLAEvaluationScheduler] Evaluating " + metricName + 
            ": current=" + currentValue + ", threshold=" + threshold);

        try {
            if (currentValue > threshold) {
                // CRITICAL alert
                if (!alertService.hasRecentAlert(metricName)) {
                    System.out.println("[SLAEvaluationScheduler] Creating CRITICAL alert for " + metricName);
                    Alert alert = alertService.createAlert(metricName, currentValue, threshold, "CRITICAL");
                    if (alert != null) {
                        System.out.println("[SLAEvaluationScheduler] CRITICAL alert created with ID: " + alert.getId());
                    }
                } else {
                    System.out.println("[SLAEvaluationScheduler] Skipping CRITICAL alert for " + metricName + " - recent alert exists");
                }
            } else if (currentValue > threshold * 0.9) {
                // WARNING alert
                if (!alertService.hasRecentAlert(metricName)) {
                    System.out.println("[SLAEvaluationScheduler] Creating WARNING alert for " + metricName);
                    Alert alert = alertService.createAlert(metricName, currentValue, threshold, "WARNING");
                    if (alert != null) {
                        System.out.println("[SLAEvaluationScheduler] WARNING alert created with ID: " + alert.getId());
                    }
                } else {
                    System.out.println("[SLAEvaluationScheduler] Skipping WARNING alert for " + metricName + " - recent alert exists");
                }
            } else {
                System.out.println("[SLAEvaluationScheduler] " + metricName + " is within SLA limits: " + currentValue + " <= " + threshold);
            }
        } catch (Exception e) {
            System.err.println("[SLAEvaluationScheduler] Error evaluating " + metricName + ": " + e.getMessage());
        }
    }

    private Double getCurrentValueForMetric(AggregatedMetrics metric, String metricName) {
        switch (metricName) {
            case "response_time":
                return metric.getAvgResponseTime();
            case "p95_latency":
                return metric.getP95Latency();
            case "error_rate":
                return metric.getErrorRate();
            case "cpu_usage":
                return metric.getCpuUsage();
            case "memory_usage":
                return metric.getMemoryUsage();
            case "requests_per_minute":
                return metric.getRequestCount() != null ? metric.getRequestCount().doubleValue() : null;
            default:
                return null;
        }
    }
}
