package com.sla.backend.service;

import com.sla.backend.entity.Metrics;
import com.sla.backend.entity.Sla;
import com.sla.backend.entity.Alert;
import com.sla.backend.entity.MLPredictionHistory;
import com.sla.backend.repository.MetricRepository;
import com.sla.backend.repository.MLPredictionHistoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
@Service
public class MetricService {
    private final MetricRepository metricRepository;
    private final SlaService slaservice;
    private final AlertService alertService;
    private final MlClientService mlClientService;
    private final MLPredictionHistoryRepository mlPredictionHistoryRepository;
    
    public MetricService(MetricRepository metricRepository,
                        SlaService slaservice,
                        AlertService alertService,
                        MlClientService mlClientService,
                        MLPredictionHistoryRepository mlPredictionHistoryRepository){
        this.metricRepository=metricRepository;
        this.slaservice=slaservice;
        this.alertService=alertService;
        this.mlClientService=mlClientService;
        this.mlPredictionHistoryRepository=mlPredictionHistoryRepository;
    }

    public Metrics saveMetric(String MetricName, Double value) {
        System.out.println("[MetricService] ===== SAVING METRIC =====");
        System.out.println("[MetricService] Metric: " + MetricName + ", Value: " + value);

        Metrics metric = new Metrics(MetricName, value, LocalDateTime.now());
        metricRepository.save(metric);

        // Only check SLAs and create alerts for specific metrics that we monitor
        if (!isMonitoredMetric(MetricName)) {
            System.out.println("[MetricService] Skipping SLA check for non-monitored metric: " + MetricName);
            return metric;
        }

        Sla sla = slaservice.getactiveSla(MetricName);
        System.out.println("[MetricService] SLA for " + MetricName + ": " + (sla != null ? "threshold " + sla.getThreshold() : "null"));

        if (sla != null) {
            // ── Use a rolling average of recent values (10-tick window = ~80s) ──
            // 10-tick window ensures maximum stability and eliminates "jitter" or "spikes".
            var recentMetrics = metricRepository.findTop10ByMetricNameOrderByDateTimeDesc(MetricName);
            var recentValues  = recentMetrics.stream().map(Metrics::getValue).toList();

            double rollingAvg = recentValues.isEmpty()
                ? value
                : recentValues.stream().mapToDouble(Double::doubleValue).average().orElse(value);

            double threshold = sla.getThreshold();

            System.out.println("[MetricService] SLA for " + MetricName + ": " + rollingAvg + " / " + threshold);

            boolean isPredictedRisk = false;
            double predictedValueOutput = -1;

            // ── 1. Intelligence Layer Check: PREDICTED_RISK (PRIORITY) ──────────
            if (recentValues.size() >= 3) {
                try {
                    double predictedValue = -1;
                    try {
                        Optional<MLPredictionHistory> latest = mlPredictionHistoryRepository.findMostRecent();
                        if (latest.isPresent()) {
                            String json = latest.get().getPredictedMetrics();
                            if (json != null && !json.isBlank()) {
                                String rfKey = mapSlaNameToRfFeature(MetricName);
                                String search = "\"" + rfKey + "\":";
                                int idx = json.indexOf(search);
                                if (idx >= 0) {
                                    int start = idx + search.length();
                                    while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"' || json.charAt(start) == ':')) {
                                        start++;
                                    }
                                    int end = json.indexOf(',', start);
                                    if (end < 0) end = json.indexOf('}', start);
                                    if (end > start) {
                                        String valStr = json.substring(start, end).replace("\"", "").trim();
                                        predictedValue = Double.parseDouble(valStr);
                                        predictedValue = applyPhysicalClamp(MetricName, predictedValue, rollingAvg);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {}

                    if (predictedValue < 0) {
                        predictedValue = mlClientService.Predictvalue(recentValues);
                    }
                    predictedValueOutput = predictedValue;

                    // Trigger PREDICTED_RISK if AI sees threat (75% threshold) AND it is rising
                    // We only warn if the value is heading for a breach AND is higher than current
                    try {
                        Optional<MLPredictionHistory> latest = mlPredictionHistoryRepository.findMostRecent();
                        if (latest.isPresent()) {
                            Double breachProb = latest.get().getBreachProbability();
                            boolean modelConfidence = (breachProb != null && breachProb >= 0.05);
                            boolean headingForBreach = (predictedValue > (threshold * 0.75));
                            boolean trendingUp = (predictedValue > rollingAvg); // ← ONLY alert if rising
                            
                            isPredictedRisk = modelConfidence && headingForBreach && trendingUp;
                        }
                    } catch (Exception e) {}

                    if (isPredictedRisk) {
                        LocalDateTime quickCheck = LocalDateTime.now().minusSeconds(20);
                        if (!alertService.hasActivePredictedAlert(MetricName, quickCheck)) {
                            System.out.println("[MetricService] AI Prediction Triggered: " + MetricName + " -> " + String.format("%.2f", predictedValue));
                            alertService.createPredictedAlert(MetricName, rollingAvg, predictedValue, threshold);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[MetricService] AI Prediction error: " + e.getMessage());
                }
            }

            // ── 2. Threshold Checks: CRITICAL / WARNING (SECONDARY) ─────────
            // We only trigger static alerts if NO immediate prediction alert was just created,
            // or if the prediction is severely trending toward critical.
            if (rollingAvg > threshold) {
                if (!alertService.hasRecentAlertOfSeverity(MetricName, "CRITICAL")) {
                    alertService.createAlert(MetricName, rollingAvg, threshold, "CRITICAL");
                }
            } 
            else if (rollingAvg > threshold * 0.9 && !isPredictedRisk) {
                // If AI is already warning us (75%-89%), we SUPPRESS the static Warning (90%)
                // to keep the dashboard clean and focused on Intelligence.
                if (!alertService.hasRecentAlertOfSeverity(MetricName, "WARNING")) {
                    alertService.createAlert(MetricName, rollingAvg, threshold, "WARNING");
                }
            }

        } else {
            System.out.println("[MetricService] No active SLA found for: " + MetricName);
        }
        return metric;
    }

    private boolean isMonitoredMetric(String metricName) {
        return List.of("response_time", "p95_latency", "error_rate", "cpu_usage",
                       "memory_usage", "network_latency", "disk_usage").contains(metricName);
    }

    /**
     * Maps the SLA/DB metric name to the feature key used in the RF predictions JSON.
     * The RF model uses Python feature names (e.g. "avg_response_time"),
     * while the DB/SLA side uses "response_time".
     */
    private String mapSlaNameToRfFeature(String slaMetricName) {
        if (slaMetricName == null) return "";
        String name = slaMetricName.toLowerCase().replace(" ", "_").trim();
        
        return switch (name) {
            case "response_time", "avg_response_time" -> "avg_response_time";
            case "p95_latency", "p95"               -> "p95_latency";
            case "error_rate", "errors"             -> "error_rate";
            case "cpu_usage", "cpu"                 -> "cpu_usage";
            case "memory_usage", "memory"           -> "memory_usage";
            case "network_latency", "latency"       -> "network_latency";
            case "disk_usage", "disk"               -> "disk_usage";
            case "request_count", "rpm", "requests" -> "request_count";
            default                                 -> name;
        };
    }

    /**
     * Clamps an RF-predicted value to:
     *   (a) the physical maximum possible for that metric, AND
     *   (b) at most 1.5× the current rolling average.
     *
     * This is a hard safety net. If the RF model ever predicts an impossible
     * value (e.g. disk=329%), it gets corrected here before triggering an alert.
     */
    private double applyPhysicalClamp(String metricName, double predicted, double currentAvg) {
        // Physical upper limits matching SystemLoadSimulator.java clamp() calls
        double physicalMax = switch (metricName) {
            case "cpu_usage"       -> 100.0;
            case "disk_usage"      -> 100.0;
            case "error_rate"      -> 100.0;
            case "memory_usage"    -> 1024.0;
            case "response_time"   -> 5000.0;
            case "p95_latency"     -> 15000.0;
            case "network_latency" -> 1000.0;
            default                -> predicted;   // unknown: no clamp
        };

        // Max 1.5× relative increase from current rolling average
        double relativeMax = currentAvg * 1.5;

        double clamped = Math.min(predicted, Math.min(physicalMax, relativeMax));

        if (clamped < predicted) {
            System.out.println("[MetricService] RF prediction clamped: " + metricName
                + " " + String.format("%.2f", predicted) + " -> " + String.format("%.2f", clamped)
                + " (physMax=" + physicalMax + ", relMax=" + String.format("%.2f", relativeMax) + ")");
        }
        return Math.max(0, clamped);
    }

    public List<Metrics> getAllMetrics() {
        return metricRepository.findAll();
    }
}

