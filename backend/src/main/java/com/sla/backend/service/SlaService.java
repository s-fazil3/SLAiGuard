package com.sla.backend.service;

import com.sla.backend.dto.SlaComplianceDTO;
import com.sla.backend.entity.Sla;
import com.sla.backend.entity.Metrics;
import com.sla.backend.entity.Metric;
import com.sla.backend.repository.SlaRepository;
import com.sla.backend.repository.MetricRepository;
import com.sla.backend.repository.PerformanceMetricRepository;
import com.sla.backend.repository.MLPredictionHistoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class SlaService {
    private final SlaRepository slaRepository;
    private final MetricRepository metricRepository;
    private final PerformanceMetricRepository performanceMetricRepository;
    private final MLPredictionHistoryRepository mlPredictionHistoryRepository; // ← Added

    public SlaService(SlaRepository slaRepository, MetricRepository metricRepository, PerformanceMetricRepository performanceMetricRepository, MLPredictionHistoryRepository mlPredictionHistoryRepository){
        this.slaRepository = slaRepository;
        this.metricRepository = metricRepository;
        this.performanceMetricRepository = performanceMetricRepository;
        this.mlPredictionHistoryRepository = mlPredictionHistoryRepository;
    }

    public Sla createSla(String metricname,double threshold){
        System.out.println("[SlaService] ===== CREATING SLA =====");
        System.out.println("[SlaService] Metric: " + metricname + ", Threshold: " + threshold);
        Sla newSla = new Sla(metricname, threshold, true);
        Sla savedSla = slaRepository.save(newSla);
        System.out.println("[SlaService] SLA created with ID: " + savedSla.getId());
        System.out.println("[SlaService] Created SLA: metric=" + savedSla.getMetricName() +
                         ", active=" + savedSla.isActive() + ", threshold=" + savedSla.getThreshold());
        return savedSla;
    }

    public Sla updateSla(Long id, String metricname, double threshold) {
        Sla sla = slaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SLA not found with id: " + id));
        sla.setMetricName(metricname);
        sla.setThreshold(threshold);
        return slaRepository.save(sla);
    }

    public Sla toggleSlaStatus(Long id) {
        Sla sla = slaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SLA not found with id: " + id));
        sla.setActive(!sla.isActive());
        return slaRepository.save(sla);
    }

    public Sla getactiveSla(String metricName){
        if (metricName == null) return null;
        
        // Normalize: "P95 Latency" -> "p95_latency"
        String normalizedName = metricName.toLowerCase().replace(" ", "_").trim();
        
        System.out.println("[SlaService] Looking for active SLA for: " + metricName + " (Normal: " + normalizedName + ")");
        
        // First try the normalized snake_case name
        Optional<Sla> activeSla = slaRepository.findByMetricNameAndActiveTrue(normalizedName);
        
        // Fallback: try the literal input name if normalized failed
        if (activeSla.isEmpty()) {
            activeSla = slaRepository.findByMetricNameAndActiveTrue(metricName);
        }
        
        System.out.println("[SlaService] Active SLA result for " + metricName + ": " + 
                         (activeSla.isPresent() ? "threshold=" + activeSla.get().getThreshold() : "null"));
        return activeSla.orElse(null);
    }
    public List<Sla> getAllSlas() {
    return slaRepository.findAll(); 
}
    public void deleteSla(Long id) {
        slaRepository.deleteById(id);
    }

    public SlaComplianceDTO calculateSlaCompliance(String metricname) {
        // 1. Get active SLA
        Sla activeSla = getactiveSla(metricname);
        if (activeSla == null) {
            return new SlaComplianceDTO(metricname, null, null, null, null, null, "Not Defined");
        }

        // 2. Get current smoothed (rolling) value (10-tick window for stability)
        List<Metrics> recentMetrics = metricRepository.findTop10ByMetricNameOrderByDateTimeDesc(metricname);
        if (recentMetrics.isEmpty()) {
            return new SlaComplianceDTO(metricname, null, null, null, null, null, "No Data");
        }

        double avgMetricValue = recentMetrics.stream()
                .mapToDouble(Metrics::getValue)
                .average()
                .orElse(0.0);

        List<Metric> performanceMetrics = performanceMetricRepository.findAll().stream()
                .filter(m -> m != null && m.getServiceName() != null && 
                        m.getServiceName().equals(metricname))
                .collect(Collectors.toList());

        double errorRate = 0.0;
        if (!performanceMetrics.isEmpty()) {
            long errorCount = performanceMetrics.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getErrorOccurred()))
                    .count();
            errorRate = (double) errorCount / performanceMetrics.size() * 100;
        }
        Double mlPredictedValue = null;
        Double mlRiskScore = 0.0;
        try {
            Optional<com.sla.backend.entity.MLPredictionHistory> latest = mlPredictionHistoryRepository.findMostRecent();
            if (latest.isPresent()) {
                String json = latest.get().getPredictedMetrics();
                if (json != null && !json.isBlank()) {
                    String rfKey = mapSlaNameToRfFeature(metricname);
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
                                mlPredictedValue = Double.parseDouble(valStr);
                                mlRiskScore = (mlPredictedValue / activeSla.getThreshold()) * 100.0;
                            }
                        }
                }
            }
        } catch (Exception e) {
            System.err.println("[SlaService] Error fetching RF prediction for compliance: " + e.getMessage());
        }
        double compliancePercentage = Math.max(0, 100.0 - mlRiskScore);

        return new SlaComplianceDTO(
            metricname, 
            avgMetricValue, 
            errorRate, 
            compliancePercentage, 
            mlRiskScore, 
            mlPredictedValue, 
            "Defined"
        );
    }

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

    public void createEnterpriseStandardSlas() {
        java.util.Map<String, Double> defaults = new java.util.LinkedHashMap<>();
        defaults.put("response_time",   400.0);   // 400 ms
        defaults.put("p95_latency",     600.0);   // 600 ms
        defaults.put("error_rate",        5.0);   // 5 %
        defaults.put("cpu_usage",        80.0);   // 80 %
        defaults.put("memory_usage",    800.0);   // 800 MB
        defaults.put("network_latency", 100.0);   // 100 ms
        defaults.put("disk_usage",       85.0);   // 85 %

        for (java.util.Map.Entry<String, Double> entry : defaults.entrySet()) {
            String metric    = entry.getKey();
            Double threshold = entry.getValue();

            boolean alreadyExists = slaRepository.findByMetricNameAndActiveTrue(metric).isPresent();
            if (!alreadyExists) {
                Sla sla = new Sla(metric, threshold, true);
                slaRepository.save(sla);
                System.out.println("[SlaService] Enterprise SLA created: " + metric + " <= " + threshold);
            } else {
                System.out.println("[SlaService] Enterprise SLA skipped (already exists): " + metric);
            }
        }
    }
}

