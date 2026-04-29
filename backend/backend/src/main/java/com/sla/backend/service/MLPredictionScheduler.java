package com.sla.backend.service;

import com.sla.backend.entity.AggregatedMetrics;
import com.sla.backend.entity.MLPredictionHistory;
import com.sla.backend.repository.AggregatedMetricsRepository;
import com.sla.backend.repository.MLPredictionHistoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class MLPredictionScheduler {

    private final AggregatedMetricsRepository aggregatedMetricsRepository;
    private final MLPredictionHistoryRepository mlPredictionHistoryRepository;
    private final MlClientService mlClientService;

    public MLPredictionScheduler(AggregatedMetricsRepository aggregatedMetricsRepository,
                               MLPredictionHistoryRepository mlPredictionHistoryRepository,
                               MlClientService mlClientService) {
        this.aggregatedMetricsRepository = aggregatedMetricsRepository;
        this.mlPredictionHistoryRepository = mlPredictionHistoryRepository;
        this.mlClientService = mlClientService;
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void runMLPredictions() {
        try {
            System.out.println("[MLPredictionScheduler] Starting ML predictions at " + LocalDateTime.now());

            List<AggregatedMetrics> latestMetrics = aggregatedMetricsRepository.findLatestAll();
            
            if (latestMetrics.isEmpty()) {
                System.out.println("[MLPredictionScheduler] No aggregated metrics found for ML prediction");
                return;
            }
            for (AggregatedMetrics metric : latestMetrics) {
                try {
                    runMLPredictionForService(metric);
                } catch (Exception e) {
                    System.err.println("[MLPredictionScheduler] Error running ML prediction for service " + 
                        metric.getServiceName() + ": " + e.getMessage());
                }
            }

            System.out.println("[MLPredictionScheduler] ML predictions completed successfully");
        } catch (Exception e) {
            System.err.println("[MLPredictionScheduler] Error during ML prediction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void runMLPredictionForService(AggregatedMetrics metric) {
        try {
            System.out.println("[MLPredictionScheduler] Running ML prediction for service: " + metric.getServiceName());

            // ── Call the Random Forest /predict endpoint — get the FULL result ──
            MlClientService.MlPredictionResult rfResult = mlClientService.callPredict(
                metric.getAvgResponseTime(),
                metric.getErrorRate(),
                metric.getCpuUsage(),
                metric.getMemoryUsage(),
                metric.getP95Latency(),
                metric.getRequestCount(),
                metric.getNetworkLatency(),
                metric.getDiskUsage()
            );

            Double breachProbability;
            Double mlRiskScore;
            String severity;
            String predictedMetricsJson = null;

            if (rfResult != null) {
                mlRiskScore       = rfResult.riskScore;
                breachProbability = rfResult.breachProbability;

                // ── Severity from RF risk score ───────────────────────────────
                if (breachProbability >= 0.7)      severity = "CRITICAL";
                else if (breachProbability >= 0.4) severity = "WARNING";
                else if (breachProbability >= 0.2) severity = "MEDIUM";
                else                               severity = "LOW";

                // ── Serialize RF per-metric predictions to JSON ───────────────
                // e.g. {"cpu_usage":52.3,"memory_usage":481.0,"avg_response_time":310.5,...}
                if (rfResult.predictions != null && !rfResult.predictions.isEmpty()) {
                    StringBuilder sb = new StringBuilder("{");
                    boolean first = true;
                    for (java.util.Map.Entry<String, Double> e : rfResult.predictions.entrySet()) {
                        if (!first) sb.append(",");
                        sb.append("\"").append(e.getKey()).append("\":")
                          .append(String.format("%.3f", e.getValue()));
                        first = false;
                    }
                    sb.append("}");
                    predictedMetricsJson = sb.toString();
                }

                System.out.println("[MLPredictionScheduler] RF result for " + metric.getServiceName()
                    + ": riskScore=" + mlRiskScore + "% severity=" + severity
                    + " predictedBreaches=" + rfResult.predictedBreaches
                    + " predictedMetrics=" + predictedMetricsJson);

            } else {
                System.out.println("[MLPredictionScheduler] ML service unavailable, using fallback");
                mlRiskScore       = calculateFallbackRiskScore(metric);
                breachProbability = mlRiskScore / 100.0;
                severity          = "LOW";
            }

            MLPredictionHistory prediction = new MLPredictionHistory(
                metric.getServiceName(),
                metric.getAvgResponseTime(),
                metric.getErrorRate(),
                metric.getCpuUsage(),
                metric.getMemoryUsage(),
                metric.getP95Latency(),
                metric.getRequestCount(),
                breachProbability,
                mlRiskScore,
                severity,
                LocalDateTime.now()
            );
            prediction.setPredictedMetrics(predictedMetricsJson);  // ← RF predictions stored

            MLPredictionHistory saved = mlPredictionHistoryRepository.save(prediction);
            System.out.println("[MLPredictionScheduler] ML prediction saved with ID: " + saved.getId());

        } catch (Exception e) {
            System.err.println("[MLPredictionScheduler] Error in ML prediction for " + metric.getServiceName() + ": " + e.getMessage());
            saveFallbackPrediction(metric, e.getMessage());
        }
    }

    private Double calculateFallbackRiskScore(AggregatedMetrics metric) {
        // Simple rule-based fallback calculation
        double riskScore = 0.0;
        
        if (metric.getErrorRate() != null) {
            riskScore += metric.getErrorRate() * 0.4;
        }
        if (metric.getAvgResponseTime() != null) {
            riskScore += Math.min(30.0, metric.getAvgResponseTime() / 10.0);
        }
        if (metric.getCpuUsage() != null) {
            riskScore += metric.getCpuUsage() * 0.2;
        }
        if (metric.getMemoryUsage() != null && metric.getMemoryUsage() > 500) {
            riskScore += (metric.getMemoryUsage() - 500) / 50.0;
        }
        
        return Math.min(100.0, Math.max(2.0, riskScore));
    }

    private void saveFallbackPrediction(AggregatedMetrics metric, String errorMessage) {
        try {
            Double fallbackRiskScore = calculateFallbackRiskScore(metric);
            Double breachProbability = fallbackRiskScore / 100.0;
            
            MLPredictionHistory fallbackPrediction = new MLPredictionHistory(
                metric.getServiceName(),
                metric.getAvgResponseTime(),
                metric.getErrorRate(),
                metric.getCpuUsage(),
                metric.getMemoryUsage(),
                metric.getP95Latency(),
                metric.getRequestCount(),
                breachProbability,
                fallbackRiskScore,
                "LOW", // Conservative severity for fallback
                LocalDateTime.now()
            );

            mlPredictionHistoryRepository.save(fallbackPrediction);
            
            System.out.println("[MLPredictionScheduler] Saved fallback prediction for " + metric.getServiceName() + 
                " due to error: " + errorMessage);
        } catch (Exception saveError) {
            System.err.println("[MLPredictionScheduler] Failed to save fallback prediction: " + saveError.getMessage());
        }
    }
}
