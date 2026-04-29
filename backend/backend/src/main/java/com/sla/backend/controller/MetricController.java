package com.sla.backend.controller;


import com.sla.backend.service.LoadGeneratorService;
import com.sla.backend.dto.ApiResponse;
import jakarta.validation.Valid;
import com.sla.backend.entity.AggregatedMetrics;
import com.sla.backend.entity.Metric;
import com.sla.backend.entity.Metrics;
import com.sla.backend.repository.AggregatedMetricsRepository;
import com.sla.backend.repository.MLPredictionHistoryRepository;
import com.sla.backend.repository.MetricRepository;
import com.sla.backend.repository.PerformanceMetricRepository;
import com.sla.backend.service.MetricService;
import com.sla.backend.service.PerformanceMetricService;
import com.sla.backend.dto.DashboardMetricsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.Map;
@RestController
@RequestMapping("api/metrics")
public class MetricController {
    private final MetricService metricService;
    private final LoadGeneratorService loadGeneratorService;
    private final PerformanceMetricRepository performanceMetricRepository;
    private final PerformanceMetricService performanceMetricService;
    private final AggregatedMetricsRepository aggregatedMetricsRepository;
    private final MLPredictionHistoryRepository mlPredictionHistoryRepository;
    private final MetricRepository metricRepository;

    public MetricController(MetricService metricService, 
                            LoadGeneratorService loadGeneratorService, 
                            PerformanceMetricRepository performanceMetricRepository, 
                            PerformanceMetricService performanceMetricService, 
                            AggregatedMetricsRepository aggregatedMetricsRepository, 
                            MLPredictionHistoryRepository mlPredictionHistoryRepository,
                            MetricRepository metricRepository){
        this.metricService = metricService;
        this.loadGeneratorService = loadGeneratorService;
        this.performanceMetricRepository = performanceMetricRepository;
        this.performanceMetricService = performanceMetricService;
        this.aggregatedMetricsRepository = aggregatedMetricsRepository;
        this.mlPredictionHistoryRepository = mlPredictionHistoryRepository;
        this.metricRepository = metricRepository;
        
        try {
            this.mlPredictionHistoryRepository.deleteAll();
            System.out.println("[MetricController] ML history cleared");
        } catch (Exception e) {}
    }

    @PostMapping
    public Metrics addMetric(@Valid @RequestBody MetricRequest metricRequest){
        return metricService.saveMetric(metricRequest.getMetricname(),
                metricRequest.getValue());
    }

    @GetMapping
    public List<Metrics> getAllMetrics() {
        return metricService.getAllMetrics();
    }

    @GetMapping("/performance")
    public List<Metric> getPerformanceMetrics() {
        return performanceMetricRepository.findAll();
    }

    @GetMapping("/dashboard")
    public DashboardMetricsDTO getDashboardMetrics() {
        // Use cached aggregated metrics for better performance
        // Only refresh cache every 2 seconds to reduce database load
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDashboardUpdate > 2000) { // 2 second cache
            cachedDashboardMetrics = getDashboardMetricsFromAggregated();
            lastDashboardUpdate = currentTime;
        }
        return cachedDashboardMetrics;
    }

    // Add cache fields
    private volatile DashboardMetricsDTO cachedDashboardMetrics;
    private volatile long lastDashboardUpdate = 0;

    private DashboardMetricsDTO getDashboardMetricsFromAggregated() {
        try {
            // 1. Fetch ALL RAW LATEST values in single optimized query
            Map<String, Double> latestValues = getAllLatestMetrics();
            Double latestRT   = latestValues.get("response_time");
            Double latestP95  = latestValues.get("p95_latency");
            Double latestER   = latestValues.get("error_rate");
            Double latestCPU  = latestValues.get("cpu_usage");
            Double latestMem  = latestValues.get("memory_usage");
            Double latestNet  = latestValues.get("network_latency");
            Double latestDisk = latestValues.get("disk_usage");

            // 2. Fetch Aggregated base for long-term stats (Uptime, RPM)
            Optional<AggregatedMetrics> latestAggregated = aggregatedMetricsRepository.findMostRecent();

            // 3. Fetch latest ML Prediction
            Double mlRiskScore = mlPredictionHistoryRepository.findMostRecent()
                .map(p -> p.getRiskScore() != null ? p.getRiskScore() : 0.0)
                .orElse(0.0);

            if (latestAggregated.isPresent()) {
                AggregatedMetrics agg = latestAggregated.get();
                return new DashboardMetricsDTO(
                    latestRT   != null ? latestRT   : agg.getAvgResponseTime(),
                    latestP95  != null ? latestP95  : agg.getP95Latency(),
                    latestER   != null ? latestER   : agg.getErrorRate(),
                    agg.getRequestCount(),
                    latestCPU  != null ? latestCPU  : agg.getCpuUsage(),
                    latestMem  != null ? latestMem  : agg.getMemoryUsage(),
                    latestNet  != null ? latestNet  : agg.getNetworkLatency(),
                    latestDisk != null ? latestDisk : agg.getDiskUsage(),
                    100.0 - (latestER != null ? latestER : agg.getErrorRate()),
                    calculateRuleBasedRiskScore(agg),
                    mlRiskScore
                );
            } else {
                // Fallback to purely raw latest
                return new DashboardMetricsDTO(
                    latestRT != null ? latestRT : 0.0,
                    latestP95 != null ? latestP95 : 0.0,
                    latestER != null ? latestER : 0.0,
                    0L,
                    latestCPU != null ? latestCPU : 0.0,
                    latestMem != null ? latestMem : 0.0,
                    latestNet != null ? latestNet : 0.0,
                    latestDisk != null ? latestDisk : 0.0,
                    100.0, 0.0, mlRiskScore
                );
            }
        } catch (Exception e) {
            System.err.println("[MetricController] Dashboard error: " + e.getMessage());
            // Fallback to empty if both systems fail, to avoid inconsistent legacy data
            return new DashboardMetricsDTO(0.0, 0.0, 0.0, 0L, 0.0, 0.0, 0.0, 0.0, 100.0, 0.0, 0.0);
        }
    }

    private Map<String, Double> getAllLatestMetrics() {
        // Return the rolling average of the last 10 raw values per metric.
        // Match the window used by MetricService for perfect consistency.
        List<String> metricNames = List.of("response_time", "p95_latency", "error_rate", "cpu_usage",
                                           "memory_usage", "network_latency", "disk_usage");

        Map<String, Double> rollingAverages = new java.util.HashMap<>();

        for (String metricName : metricNames) {
            try {
                List<Metrics> recent = metricRepository.findTop10ByMetricNameOrderByDateTimeDesc(metricName);
                if (!recent.isEmpty()) {
                    double avg = recent.stream()
                            .mapToDouble(Metrics::getValue)
                            .average()
                            .orElse(recent.get(0).getValue());
                    rollingAverages.put(metricName, avg);
                }
            } catch (Exception e) {
                System.err.println("[MetricController] Error fetching rolling avg for " + metricName + ": " + e.getMessage());
            }
        }

        return rollingAverages;
    }

    private Double calculateRuleBasedRiskScore(AggregatedMetrics aggregated) {
        double totalRisk = 0.0;
        int activeBreaches = 0;
        
        try {
            List<com.sla.backend.entity.Sla> slas = performanceMetricService.getAllSlas();
            for (com.sla.backend.entity.Sla sla : slas) {
                if (!sla.isActive()) continue;
                
                Double value = getMetricValueByName(aggregated, sla.getMetricName());
                if (value != null && value > sla.getThreshold()) {
                    activeBreaches++;
                    double excessRatio = (value - sla.getThreshold()) / sla.getThreshold();
                    
                    // Critical Impact: Each breach adds significant weight
                    // One breach = ~60%, Two = ~90%, Three = 99%
                    double metricImpact = 0.6 + (Math.min(0.4, excessRatio)); 
                    totalRisk += metricImpact;
                }
            }
        } catch (Exception e) {
            System.err.println("[MetricController] Error calculating impact risk: " + e.getMessage());
        }
        
        // Final Score Calculation (Non-Diluted)
        double finalScore = 5.0; // Baseline
        if (activeBreaches > 0) {
            // Formula: 60% for the first breach + 30% for others
            finalScore = 60.0 + ((activeBreaches - 1) * 20.0) + (totalRisk * 5.0);
        }
        
        return Math.min(99.9, Math.max(5.0, finalScore));
    }

    private Double getMetricValueByName(AggregatedMetrics aggregated, String name) {
        if (name == null) return null;
        return switch (name.toLowerCase()) {
            case "response_time" -> aggregated.getAvgResponseTime();
            case "error_rate" -> aggregated.getErrorRate();
            case "cpu_usage" -> aggregated.getCpuUsage();
            case "memory_usage" -> aggregated.getMemoryUsage();
            case "p95_latency" -> aggregated.getP95Latency();
            case "network_latency" -> aggregated.getNetworkLatency();
            case "disk_usage" -> aggregated.getDiskUsage();
            case "request_count" -> aggregated.getRequestCount() != null ? aggregated.getRequestCount().doubleValue() : null;
            default -> null;
        };
    }

    @PostMapping("/simulate-trend")
    public ResponseEntity<ApiResponse<String>> simulateTrend(
            @RequestParam(defaultValue = "increasing") String type,
            @RequestParam(defaultValue = "5") int durationMinutes) {

        try {
            loadGeneratorService.simulateTrend(type, durationMinutes);
            String message = String.format("Successfully simulated %s trend for %d minutes", type, durationMinutes);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to simulate trend: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-load")
    public ResponseEntity<ApiResponse<String>> resetLoad() {
        try {
            loadGeneratorService.resetLoad();
            return ResponseEntity.ok(ApiResponse.success("Load generator reset to baseline values"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to reset load: " + e.getMessage()));
        }
    }
}
