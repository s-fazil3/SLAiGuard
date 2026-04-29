package com.sla.backend.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * SystemLoadSimulator
 *
 * Generates highly realistic, correlated production metrics using a 
 * "Shared Stress Factor" model.
 *
 * Architecture:
 *   1. A hidden 'stressFactor' drifts via a mean-reverting random walk.
 *   2. All 8 physical metrics (CPU, RT, etc.) are functions of this stress.
 *   3. This ensures that when the 'system' is under load, ALL metrics 
 *      deteriorate together, matching real-world failure patterns.
 */
@Service
public class SystemLoadSimulator {

    private final MetricService metricService;
    private final Random random = new Random();

    // ────────────────────────────────────────────────────────────────────────
    // Shared State: The 'Hidden' Stress Factor (0.0 = Healthy, 1.0+ = Overload)
    // ────────────────────────────────────────────────────────────────────────
    private double stressFactor = 0.0;
    private static final double STRESS_REVERSION = 0.05; // Pull back to healthy 0.0
    private static final double STRESS_SHOCK     = 0.15; // Random jump per tick

    // ────────────────────────────────────────────────────────────────────────
    // Realistic Baselines & Sensitivities
    // ────────────────────────────────────────────────────────────────────────
    // Metric = BASE + (stressFactor * SENSITIVITY) + Gaussian(NOISE)

    private record MetricConfig(double base, double sensitivity, double noise) {}

    private final MetricConfig cpuCfg  = new MetricConfig(25.0,  50.0, 2.0);  // 25% base, +50% at stress=1.0
    private final MetricConfig memCfg  = new MetricConfig(320.0, 450.0, 10.0); // 320MB base
    private final MetricConfig rtCfg   = new MetricConfig(110.0, 350.0, 12.0); // 110ms base
    private final MetricConfig erCfg   = new MetricConfig(0.4,   8.0,   0.2);  // 0.4% base
    private final MetricConfig rpsCfg  = new MetricConfig(150.0, 250.0, 15.0); // 150 rps base
    private final MetricConfig netCfg  = new MetricConfig(12.0,  90.0,  4.0);  // 12ms base
    private final MetricConfig diskCfg = new MetricConfig(52.0,  35.0,  0.5);  // 52% base

    public SystemLoadSimulator(MetricService metricService) {
        this.metricService = metricService;
    }

    @Scheduled(fixedRate = 8_000)
    public void generateAndPersistMetrics() {
        try {
            // 1. Evolve the hidden stress (The "Realism" engine)
            // next = current + pull_to_zero + random_jump
            stressFactor = Math.max(0.0, stressFactor + (0.0 - stressFactor) * STRESS_REVERSION + (random.nextGaussian() * STRESS_SHOCK));

            // 2. Compute physical metrics from stress
            double cpu  = clamp(cpuCfg.base  + (stressFactor * cpuCfg.sensitivity)  + (random.nextGaussian() * cpuCfg.noise), 1, 100);
            double mem  = clamp(memCfg.base  + (stressFactor * memCfg.sensitivity)  + (random.nextGaussian() * memCfg.noise), 50, 1024);
            double rt   = clamp(rtCfg.base   + (stressFactor * rtCfg.sensitivity)   + (random.nextGaussian() * rtCfg.noise), 5, 5000);
            double er   = clamp(erCfg.base   + (stressFactor * erCfg.sensitivity)   + (random.nextGaussian() * erCfg.noise), 0, 100);
            double rps  = clamp(rpsCfg.base  + (stressFactor * rpsCfg.sensitivity)  + (random.nextGaussian() * rpsCfg.noise), 1, 10000);
            double net  = clamp(netCfg.base  + (stressFactor * netCfg.sensitivity)  + (random.nextGaussian() * netCfg.noise), 1, 1000);
            double disk = clamp(diskCfg.base + (stressFactor * diskCfg.sensitivity) + (random.nextGaussian() * diskCfg.noise), 1, 100);
            
            double p95  = clamp(rt * 1.5 + (random.nextDouble() * 50), rt, rt * 3);

            // 3. Persist
            metricService.saveMetric("cpu_usage",        cpu);
            metricService.saveMetric("memory_usage",     mem);
            metricService.saveMetric("response_time",    rt);
            metricService.saveMetric("p95_latency",      p95);
            metricService.saveMetric("error_rate",       er);
            metricService.saveMetric("request_count",    rps);
            metricService.saveMetric("network_latency",  net);
            metricService.saveMetric("disk_usage",       disk);

            System.out.printf("[Simulator] stress=%.2f | cpu=%.1f%% mem=%.0fMB rt=%.0fms er=%.2f%%%n", 
                             stressFactor, cpu, mem, rt, er);

        } catch (Exception e) {
            System.err.println("[SystemLoadSimulator] Error: " + e.getMessage());
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Retained for manual trend simulation UI
    public GeneratedMetrics deriveMetrics(double loadFactor) {
        // Here we map the 0-1 load factor directly to stress
        double s = loadFactor * 1.5; 
        double r = rtCfg.base + (s * rtCfg.sensitivity);
        return new GeneratedMetrics(
            cpuCfg.base + (s * cpuCfg.sensitivity),
            memCfg.base + (s * memCfg.sensitivity),
            r, r * 1.5,
            erCfg.base + (s * erCfg.sensitivity),
            rpsCfg.base + (s * rpsCfg.sensitivity),
            netCfg.base + (s * netCfg.sensitivity),
            diskCfg.base + (s * diskCfg.sensitivity)
        );
    }
    
    public static class GeneratedMetrics {
        public double cpuUsage, memoryUsage, avgResponseTime, p95Latency, errorRate, requestCount, networkLatency, diskUsage;
        public GeneratedMetrics(double c, double m, double r, double p, double e, double q, double n, double d) {
            this.cpuUsage=c; this.memoryUsage=m; this.avgResponseTime=r; this.p95Latency=p; this.errorRate=e;
            this.requestCount=q; this.networkLatency=n; this.diskUsage=d;
        }
    }
}
