package com.sla.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "aggregated_metrics", indexes = {
    @Index(name = "idx_aggregated_service_name", columnList = "service_name"),
    @Index(name = "idx_aggregated_timestamp", columnList = "timestamp"),
    @Index(name = "idx_aggregated_service_timestamp", columnList = "service_name, timestamp")
})
public class AggregatedMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "avg_response_time")
    private Double avgResponseTime;

    @Column(name = "error_rate")
    private Double errorRate;

    @Column(name = "cpu_usage")
    private Double cpuUsage;

    @Column(name = "memory_usage")
    private Double memoryUsage;

    @Column(name = "request_count")
    private Long requestCount;

    @Column(name = "p95_latency")
    private Double p95Latency;

    @Column(name = "network_latency")
    private Double networkLatency;

    @Column(name = "disk_usage")
    private Double diskUsage;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public AggregatedMetrics() {
    }

    public AggregatedMetrics(String serviceName, Double avgResponseTime, Double errorRate, 
                          Double cpuUsage, Double memoryUsage, Long requestCount, 
                          Double p95Latency, Double networkLatency, Double diskUsage, LocalDateTime timestamp) {
        this.serviceName = serviceName;
        this.avgResponseTime = avgResponseTime;
        this.errorRate = errorRate;
        this.cpuUsage = cpuUsage;
        this.memoryUsage = memoryUsage;
        this.requestCount = requestCount;
        this.p95Latency = p95Latency;
        this.networkLatency = networkLatency;
        this.diskUsage = diskUsage;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Double getAvgResponseTime() {
        return avgResponseTime;
    }

    public void setAvgResponseTime(Double avgResponseTime) {
        this.avgResponseTime = avgResponseTime;
    }

    public Double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(Double errorRate) {
        this.errorRate = errorRate;
    }

    public Double getCpuUsage() {
        return cpuUsage;
    }

    public void setCpuUsage(Double cpuUsage) {
        this.cpuUsage = cpuUsage;
    }

    public Double getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(Double memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public Long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }

    public Double getP95Latency() {
        return p95Latency;
    }

    public void setP95Latency(Double p95Latency) {
        this.p95Latency = p95Latency;
    }

    public Double getNetworkLatency() {
        return networkLatency;
    }

    public void setNetworkLatency(Double networkLatency) {
        this.networkLatency = networkLatency;
    }

    public Double getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(Double diskUsage) {
        this.diskUsage = diskUsage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
