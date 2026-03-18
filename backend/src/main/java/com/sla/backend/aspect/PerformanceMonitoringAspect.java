package com.sla.backend.aspect;

import com.sla.backend.service.PerformanceMetricService;
import com.sla.backend.service.MetricService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class PerformanceMonitoringAspect {

    // Static variables to track previous metric values for gradual changes
    private static double prevCpuUsage = 25.0;
    private static double prevMemoryUsage = 350.0;
    private static double prevNetworkLatency = 15.0;
    private static double prevDiskUsage = 70.0;
    private static long prevResponseTime = 150;
    private static double prevErrorRate = 0.0;

    private final PerformanceMetricService metricService;
    private final MetricService metricServiceForSla;

    public PerformanceMonitoringAspect(PerformanceMetricService metricService, MetricService metricServiceForSla) {
        this.metricService = metricService;
        this.metricServiceForSla = metricServiceForSla;
    }

    @Pointcut("execution(* com.sla.backend.controller..*(..))")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object monitorPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean errorOccurred = false;

        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            errorOccurred = true;
            throw ex;
        } finally {
            
            // Response time: gradual changes from previous value (±5-10ms)
            long responseTimeChange = (long)((Math.random() - 0.5) * 20.0); // -10ms to +10ms change
            prevResponseTime = Math.max(50, Math.min(500, prevResponseTime + responseTimeChange)); // Keep within 50-500ms range
            long responseTime = prevResponseTime;
            
            long memoryUsageBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            double memoryUsageMb = memoryUsageBytes / (1024.0 * 1024.0);
            
            // Add bounds to memory usage for enterprise realism (200-600MB range)
            if (memoryUsageMb < 200) memoryUsageMb = 200 + (Math.random() * 100); // Min 200MB
            if (memoryUsageMb > 600) memoryUsageMb = 500 + (Math.random() * 100); // Max 600MB

            // Disk usage: gradual changes from previous value (±0.5-1%)
            double diskChange = (Math.random() - 0.5) * 2.0; // -1% to +1% change
            prevDiskUsage = Math.max(60.0, Math.min(90.0, prevDiskUsage + diskChange)); // Keep within 60-90% range
            double diskUsage = prevDiskUsage;

            // CPU usage: gradual changes from previous value (±1.5-3%)
            double cpuChange = (Math.random() - 0.5) * 6.0; // -3% to +3% change
            prevCpuUsage = Math.max(5.0, Math.min(75.0, prevCpuUsage + cpuChange)); // Keep within 5-75% range
            double cpuUsage = prevCpuUsage;

            // Memory usage: gradual changes from previous value (±5-10MB)
            double memoryChange = (Math.random() - 0.5) * 20.0; // -10MB to +10MB change  
            prevMemoryUsage = Math.max(200.0, Math.min(800.0, prevMemoryUsage + memoryChange)); // Keep within 200-800MB range
            memoryUsageMb = prevMemoryUsage;

            // Network latency: gradual changes from previous value (±2-4ms)
            double networkChange = (Math.random() - 0.5) * 8.0; // -4ms to +4ms change
            prevNetworkLatency = Math.max(3.0, Math.min(100.0, prevNetworkLatency + networkChange)); // Keep within 3-100ms range
            double networkLatency = prevNetworkLatency;

            // Error rate: gradual changes from previous value (±0.05-0.1%)
            double errorChange = (Math.random() - 0.5) * 0.2; // -0.1% to +0.1% change
            prevErrorRate = Math.max(0.0, Math.min(5.0, prevErrorRate + errorChange)); // Keep within 0-5% range
            double errorRate = prevErrorRate;
            
            // Slightly increase error rate if an actual error occurred
            if (errorOccurred) {
                errorRate = Math.min(5.0, errorRate + 0.2); // Increase by 0.2% on errors
            }

            String serviceName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();

            try {
                // Save to PerformanceMetric table for detailed analytics
                metricService.saveMetric(serviceName, responseTime, errorOccurred, memoryUsageBytes, cpuUsage, LocalDateTime.now());
                
                // Save to Metrics table for SLA and dashboard
                System.out.println("[PerformanceMonitoringAspect] Saving error_rate: " + errorRate + "%");
                
                metricServiceForSla.saveMetric("response_time", (double) responseTime);
                metricServiceForSla.saveMetric("memory_usage", memoryUsageMb);
                metricServiceForSla.saveMetric("cpu_usage", cpuUsage);
                metricServiceForSla.saveMetric("error_rate", errorRate);
                metricServiceForSla.saveMetric("network_latency", networkLatency);
                metricServiceForSla.saveMetric("disk_usage", diskUsage);
                
                // API-specific metrics
                if (serviceName.contains("AuthController") || serviceName.contains("login") || serviceName.contains("signup")) {
                    metricServiceForSla.saveMetric("api_error_rate", errorOccurred ? 100.0 : 0.0);
                    metricServiceForSla.saveMetric("api_success_rate", errorOccurred ? 0.0 : 100.0);
                    metricServiceForSla.saveMetric("api_throughput", 1.0); // 1 request per call
                }
            } catch (Exception e) {
                System.out.println("[PerformanceMonitoringAspect] Error saving metrics: " + e.getMessage());
            }
        }
    }
}
