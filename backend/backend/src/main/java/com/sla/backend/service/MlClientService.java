package com.sla.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.sla.backend.entity.Sla;
import com.sla.backend.repository.SlaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MlClientService {
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${ml.service.url:http://localhost:5001}")
    private String mlServiceUrl;
    
    @Autowired
    private SlaRepository slaRepository;
    
    /**
     * Check if ML service is available
     */
    public boolean isServiceAvailable() {
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.getForEntity(
                mlServiceUrl + "/health",
                Map.class
            );
            return response.getStatusCode().is2xxSuccessful() && 
                   "healthy".equals(response.getBody().get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    // ── Rich result from /predict ─────────────────────────────────────────────

    /**
     * Full result from the Random Forest /predict endpoint.
     * Carries:
     *   riskScore          - overall 0-100 breach risk
     *   predictions        - RF-predicted next-tick value per metric
     *   predictedBreaches  - metric names RF says will breach next tick
     *   breachProbability  - riskScore / 100.0
     */
    public static class MlPredictionResult {
        public final double riskScore;
        public final double breachProbability;
        public final Map<String, Double> predictions;   // metric -> RF next-tick value
        public final List<String> predictedBreaches;    // metrics RF says will breach

        public MlPredictionResult(double riskScore,
                                  Map<String, Double> predictions,
                                  List<String> predictedBreaches) {
            this.riskScore         = riskScore;
            this.breachProbability = riskScore / 100.0;
            this.predictions       = predictions;
            this.predictedBreaches = predictedBreaches;
        }
    }

    /**
     * Call the Random Forest /predict endpoint and return the FULL result,
     * including per-metric predicted next-tick values and predicted_breaches list.
     *
     * This is the correct entry point — callers should use this so RF predictions
     * are actually used rather than discarded.
     *
     * Returns null if ML service is unavailable.
     */
    @SuppressWarnings("unchecked")
    public MlPredictionResult callPredict(Double avgResponseTime, Double errorRate, Double cpuUsage,
                                          Double memoryUsage, Double p95Latency, Long requestCount,
                                          Double networkLatency, Double diskUsage) {
        if (!isServiceAvailable()) {
            System.err.println("[MlClientService] ML service not available");
            return null;
        }

        try {
            // Collect active SLA thresholds so RF can do threshold-aware risk scoring
            Map<String, Double> dynamicThresholds = new HashMap<>();
            try {
                List<Sla> activeSlas = slaRepository.findAll().stream()
                    .filter(Sla::isActive)
                    .collect(Collectors.toList());
                for (Sla sla : activeSlas) {
                    dynamicThresholds.put(sla.getMetricName(), sla.getThreshold());
                }
            } catch (Exception e) {
                System.err.println("[MlClientService] Error fetching dynamic thresholds: " + e.getMessage());
            }

            Map<String, Object> request = new HashMap<>();
            request.put("avg_response_time", avgResponseTime != null ? avgResponseTime : 150.0);
            request.put("error_rate",        errorRate       != null ? errorRate       : 1.0);
            request.put("cpu_usage",         cpuUsage        != null ? cpuUsage        : 30.0);
            request.put("memory_usage",      memoryUsage     != null ? memoryUsage     : 400.0);
            request.put("p95_latency",       p95Latency      != null ? p95Latency      : 200.0);
            request.put("request_count",     requestCount    != null ? requestCount.doubleValue() : 200.0);
            request.put("network_latency",   networkLatency  != null ? networkLatency  : 200.0);
            request.put("disk_usage",        diskUsage       != null ? diskUsage       : 60.0);
            request.put("thresholds",        dynamicThresholds);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request);
            ResponseEntity<Map<String, Object>> response =
                (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate
                    .postForEntity(mlServiceUrl + "/predict", entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("risk_score")) {
                System.err.println("[MlClientService] Unexpected RF response: " + body);
                return null;
            }

            double riskScore = ((Number) body.get("risk_score")).doubleValue();

            // ── Extract per-metric RF predictions ────────────────────────────
            Map<String, Double> predictions = new HashMap<>();
            Object rawPredictions = body.get("predictions");
            if (rawPredictions instanceof Map<?,?> predMap) {
                for (Map.Entry<?,?> e : predMap.entrySet()) {
                    if (e.getValue() instanceof Number) {
                        predictions.put(e.getKey().toString(), ((Number) e.getValue()).doubleValue());
                    }
                }
            }

            // ── Extract predicted_breaches list ───────────────────────────────
            List<String> predictedBreaches = new java.util.ArrayList<>();
            Object rawBreaches = body.get("predicted_breaches");
            if (rawBreaches instanceof List<?> breachList) {
                for (Object b : breachList) {
                    if (b != null) predictedBreaches.add(b.toString());
                }
            }

            System.out.println("[MlClientService] RF predict: riskScore=" + riskScore
                + " | predictedBreaches=" + predictedBreaches
                + " | predictions=" + predictions);

            return new MlPredictionResult(riskScore, predictions, predictedBreaches);

        } catch (Exception e) {
            System.err.println("[MlClientService] Error calling RF /predict: " + e.getMessage());
            return null;
        }
    }

    /**
     * Backwards-compatible wrapper - returns only risk score (0-100).
     * Used by MLPredictionScheduler. Prefer callPredict() for new code.
     */
    public Double predictBreachProbability(Double avgResponseTime, Double errorRate, Double cpuUsage,
                                           Double memoryUsage, Double p95Latency, Long requestCount,
                                           Double networkLatency, Double diskUsage) {
        MlPredictionResult result = callPredict(avgResponseTime, errorRate, cpuUsage,
                                                memoryUsage, p95Latency, requestCount,
                                                networkLatency, diskUsage);
        return result != null ? result.riskScore : null;
    }

    /**
     * Linear trend extrapolation - kept as a fallback ONLY when the ML service
     * is down. Prefer the RF predictions from callPredict().
     */
    public Double Predictvalue(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        if (values.size() < 2) return values.get(0);

        int n = Math.min(5, values.size());
        List<Double> recent = values.subList(values.size() - n, values.size());

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i, y = recent.get(i);
            sumX += x; sumY += y; sumXY += x * y; sumX2 += x * x;
        }
        double meanX = sumX / n, meanY = sumY / n;
        double denom = sumX2 - n * meanX * meanX;
        if (Math.abs(denom) < 0.001) return recent.get(n - 1) * 1.05;

        double slope     = (sumXY - n * meanX * meanY) / denom;
        double intercept = meanY - slope * meanX;
        double prediction = intercept + slope * n;

        double lastValue = recent.get(n - 1);
        prediction = Math.max(lastValue * 0.7, Math.min(lastValue * 1.5, prediction));

        System.out.println("[MlClientService] Fallback linear prediction: slope=" +
                           String.format("%.2f", slope) +
                           ", last=" + String.format("%.2f", lastValue) +
                           ", predicted=" + String.format("%.2f", prediction));
        return prediction;
    }
    
    /**
     * Detect if current value is anomalous
     */
    public AnomalyResult detectAnomaly(List<Double> historicalValues, Double currentValue) {
        if (!isServiceAvailable()) {
            throw new RuntimeException("ML service is not available");
        }
        
        Map<String, Object> request = Map.of(
            "values", historicalValues,
            "current", currentValue
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request);
        
        @SuppressWarnings("unchecked")
        ResponseEntity<Map<String, Object>> response = (ResponseEntity<Map<String, Object>>) (ResponseEntity<?>) restTemplate.postForEntity(
            mlServiceUrl + "/anomaly",
            entity,
            Map.class
        );
        
        Map<String, Object> body = response.getBody();
        return new AnomalyResult(
            (Boolean) body.get("is_anomaly"),
            (String) body.get("severity"),
            ((Number) body.get("z_score")).doubleValue()
        );
    }
    
    /**
     * DTO for anomaly detection results
     */
    public static class AnomalyResult {
        private final boolean isAnomaly;
        private final String severity;
        private final double zScore;
        
        public AnomalyResult(boolean isAnomaly, String severity, double zScore) {
            this.isAnomaly = isAnomaly;
            this.severity = severity;
            this.zScore = zScore;
        }
        
        public boolean isAnomaly() { return isAnomaly; }
        public String getSeverity() { return severity; }
        public double getZScore() { return zScore; }
    }
}
