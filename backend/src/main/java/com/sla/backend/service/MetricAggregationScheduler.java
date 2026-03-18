package com.sla.backend.service;

import com.sla.backend.entity.AggregatedMetrics;
import com.sla.backend.entity.Metrics;
import com.sla.backend.repository.AggregatedMetricsRepository;
import com.sla.backend.repository.MetricRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MetricAggregationScheduler {

    private final MetricRepository metricRepository;
    private final AggregatedMetricsRepository aggregatedMetricsRepository;

    public MetricAggregationScheduler(MetricRepository metricRepository,
                                   AggregatedMetricsRepository aggregatedMetricsRepository) {
        this.metricRepository = metricRepository;
        this.aggregatedMetricsRepository = aggregatedMetricsRepository;
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds for slower dashboard updates
    public void aggregateMetrics() {
        try {
            System.out.println("[MetricAggregationScheduler] ===== STARTING AGGREGATION =====");

            // SLOWER DASHBOARD: Query the last 30 seconds from DB for slower value updates.
            LocalDateTime thirtySecondsAgo = LocalDateTime.now().minusSeconds(30);
            List<Metrics> recentMetrics = metricRepository.findByDateTimeAfter(thirtySecondsAgo);

            System.out.println("[MetricAggregationScheduler] Found " + recentMetrics.size() + " recent metrics in the last 30s");

            if (recentMetrics.isEmpty()) {
                return;
            }

            // Group metrics by name and calculate averages
            var metricsByName = recentMetrics.stream()
                .collect(Collectors.groupingBy(Metrics::getMetricName));

            // Debug: show counts for each metric type
            System.out.println("[MetricAggregationScheduler] === RECENT METRICS BY TYPE ===");
            metricsByName.forEach((name, metrics) -> 
                System.out.println("[MetricAggregationScheduler] " + name + ": " + metrics.size() + " values"));

            // Extract aggregated values
            Double avgResponseTime = getAverageValue(metricsByName, "response_time");
            Double errorRate = getAverageValue(metricsByName, "error_rate");
            Double cpuUsage = getAverageValue(metricsByName, "cpu_usage");
            Double memoryUsage = getAverageValue(metricsByName, "memory_usage");
            Double p95Latency = getAverageValue(metricsByName, "p95_latency");
            Double avgNetworkLatency = getAverageValue(metricsByName, "network_latency");
            Double avgDiskUsage = getAverageValue(metricsByName, "disk_usage");

            // Debug: show calculated values
            System.out.println("[MetricAggregationScheduler] === CALCULATED VALUES ===");
            System.out.println("[MetricAggregationScheduler] avgResponseTime: " + avgResponseTime);
            System.out.println("[MetricAggregationScheduler] errorRate: " + errorRate);
            System.out.println("[MetricAggregationScheduler] p95Latency: " + p95Latency);
            System.out.println("[MetricAggregationScheduler] cpuUsage: " + cpuUsage);
            System.out.println("[MetricAggregationScheduler] memoryUsage: " + memoryUsage);
            System.out.println("[MetricAggregationScheduler] avgNetworkLatency: " + avgNetworkLatency);
            System.out.println("[MetricAggregationScheduler] avgDiskUsage: " + avgDiskUsage);

            // Calculate request count (use the count of any metric as proxy)
            Long requestCount = (long) recentMetrics.size();

            // Use a default service name since load generator doesn't specify one
            String serviceName = "load-generator-service";

            // Create and save aggregated metrics
            AggregatedMetrics aggregated = new AggregatedMetrics(
                serviceName,
                avgResponseTime != null ? avgResponseTime : 0.0,
                errorRate != null ? errorRate : 0.0,
                cpuUsage != null ? cpuUsage : 0.0,
                memoryUsage != null ? memoryUsage : 0.0,
                requestCount,
                p95Latency != null ? p95Latency : 0.0,
                avgNetworkLatency != null ? avgNetworkLatency : 0.0,
                avgDiskUsage != null ? avgDiskUsage : 0.0,
                LocalDateTime.now()
            );

            aggregatedMetricsRepository.save(aggregated);

            System.out.println("[MetricAggregationScheduler] Aggregated metrics for " + serviceName + ": " +
                "avgResponseTime=" + aggregated.getAvgResponseTime() + "ms, " +
                "errorRate=" + aggregated.getErrorRate() + "%, " +
                "cpuUsage=" + aggregated.getCpuUsage() + "%, " +
                "memoryUsage=" + aggregated.getMemoryUsage() + "MB, " +
                "requestCount=" + aggregated.getRequestCount() + ", " +
                "p95Latency=" + aggregated.getP95Latency() + "ms, " +
                "networkLatency=" + aggregated.getNetworkLatency() + "ms, " +
                "diskUsage=" + aggregated.getDiskUsage() + "%");

            System.out.println("[MetricAggregationScheduler] Metric aggregation completed successfully");
        } catch (Exception e) {
            System.err.println("[MetricAggregationScheduler] Error during metric aggregation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Double getAverageValue(java.util.Map<String, List<Metrics>> metricsByName, String metricName) {
        // Try exact match, snake_case, and Display Name variations
        List<Metrics> metrics = metricsByName.get(metricName);
        
        if (metrics == null || metrics.isEmpty()) {
            // Try common aliases
            String displayName = metricName.replace("_", " ").toLowerCase();
            for (String key : metricsByName.keySet()) {
                if (key.toLowerCase().equals(displayName) || 
                    key.toLowerCase().replace(" ", "_").equals(metricName.toLowerCase())) {
                    metrics = metricsByName.get(key);
                    break;
                }
            }
        }

        if (metrics == null || metrics.isEmpty()) {
            return null;
        }
        
        return metrics.stream()
            .mapToDouble(Metrics::getValue)
            .average()
            .orElse(0.0);
    }
}
