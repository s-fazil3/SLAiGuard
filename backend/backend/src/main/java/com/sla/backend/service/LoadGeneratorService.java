package com.sla.backend.service;

import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * LoadGeneratorService
 *
 * Retained for backward-compatibility (controllers still reference it).
 * Metric generation is now fully delegated to {@link SystemLoadSimulator},
 * which runs on its own 10-second schedule.
 *
 * The {@code simulateTrend} and {@code resetLoad} methods remain functional
 * so the existing REST endpoints keep working without modification.
 */
@Service
public class LoadGeneratorService {

    private final MetricService metricService;
    private final SystemLoadSimulator systemLoadSimulator;
    private final Random random = new Random();

    public LoadGeneratorService(MetricService metricService,
                                SystemLoadSimulator systemLoadSimulator) {
        this.metricService        = metricService;
        this.systemLoadSimulator  = systemLoadSimulator;
    }

    // -----------------------------------------------------------------------
    // simulateTrend – forced load scenario for manual demos/tests
    // -----------------------------------------------------------------------

    public void simulateTrend(String type, int durationMinutes) {
        // Number of ticks to simulate; each tick represents 10 s of real time.
        int ticks = durationMinutes * 6; // 6 ticks per minute

        for (int i = 0; i < ticks; i++) {
            double systemLoad;

            switch (type.toLowerCase()) {
                case "increasing":
                    systemLoad = 0.1 + (i / (double) ticks) * 0.9;
                    break;
                case "spiky":
                    systemLoad = 0.3 + random.nextDouble() * 0.4;
                    if (random.nextDouble() < 0.15) systemLoad = Math.min(1.0, systemLoad + 0.45);
                    break;
                case "recovery":
                    systemLoad = 0.9 - (i / (double) ticks) * 0.7;
                    break;
                default:
                    systemLoad = 0.2 + random.nextDouble() * 0.4;
            }

            systemLoad = Math.max(0.1, Math.min(1.0, systemLoad));
            SystemLoadSimulator.GeneratedMetrics m = systemLoadSimulator.deriveMetrics(systemLoad);

            metricService.saveMetric("cpu_usage",       m.cpuUsage);
            metricService.saveMetric("memory_usage",    m.memoryUsage);
            metricService.saveMetric("response_time",   m.avgResponseTime);
            metricService.saveMetric("p95_latency",     m.p95Latency);
            metricService.saveMetric("error_rate",      m.errorRate);
            metricService.saveMetric("request_count",   m.requestCount);
            metricService.saveMetric("network_latency", m.networkLatency);
            metricService.saveMetric("disk_usage",      m.diskUsage);
        }

        System.out.println("[LoadGeneratorService] Simulated '" + type + "' trend for "
                + durationMinutes + " minutes (" + ticks + " ticks).");
    }

    // -----------------------------------------------------------------------
    // resetLoad – no-op now that state is maintained in SystemLoadSimulator
    // -----------------------------------------------------------------------

    public void resetLoad() {
        // SystemLoadSimulator is stateless apart from the tick counter which
        // intentionally keeps the sine-wave phase coherent across restarts.
        System.out.println("[LoadGeneratorService] resetLoad called – "
                + "metric generation continues via SystemLoadSimulator (no state to reset).");
    }
}
